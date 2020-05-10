package scala.net.drozdowicz.sxacml

import java.io.File
import java.net.URI
import java.util
import java.util.{Properties, UUID}

import scala.collection.JavaConverters._
import net.drozdowicz.sxacml._
import org.apache.commons.logging.LogFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntology, OWLOntologyIRIMapper}
import org.semanticweb.owlapi.util.AutoIRIMapper
import org.wso2.balana.attr.BagAttribute
import org.wso2.balana.cond.EvaluationResult
import org.wso2.balana.ctx.EvaluationCtx

import scala.util.control.ControlThrowable

/**
  * Created by drozd on 22.03.2020.
  */
class OwlAttributeStore(ontologyFolderPath: String, rootOntologyId: String) {

  private val log = LogFactory.getLog(classOf[OwlAttributeStore])

  private val ontoMgr = OWLManager.createOWLOntologyManager()

  log.info("Initializing SXACML OWL attribute manager")

  log.info("Mapping ontology folder to:" + ontologyFolderPath)
  ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
    new AutoIRIMapper(new File(ontologyFolderPath), true)).asJava
  )

  private var requestOntology: Option[RequestOntology] = None

  private case class RequestOntology(context: EvaluationCtx, requestOntology: OWLOntology, categoryIndividualIds: Map[URI, String])

  private def generateNewRequestOntology(context: EvaluationCtx): RequestOntology = {
    val requestId = UUID.randomUUID().toString

    val attributes = ContextParser.Parse(context.getRequestCtx)
    if (log.isDebugEnabled) {
      val attributesString = attributes
        .map(_.toString)
        .mkString(";\r\n")
      log.debug(s"Received context with attributes: \r\n$attributesString")
    }

    val categoryIndividualIds = RequestOntologyGenerator.getCategoryIndividualIds(requestId, attributes)
    val requestOntology = RequestOntologyGenerator.convertToOntology(ontoMgr)(requestId, attributes, collection.immutable.Set(IRI.create(rootOntologyId)))

    new RequestOntology(context, requestOntology, categoryIndividualIds)
  }

  private def getRequestOntology(context: EvaluationCtx) = {
    val ontology =
      if (requestOntology.map(ro => ro.context.hashCode() != context.hashCode()).getOrElse(true))
        generateNewRequestOntology(context)
      else requestOntology.get

    requestOntology = Some(ontology)
    ontology
  }


  def isClassAttributeId(attributeId: URI): Boolean = {
    return Constants.classIdForCategory.values.toList.contains(attributeId)
  }

  def findAttributeValues(attributeId: URI, category: URI, context: EvaluationCtx): Set[FlatAttributeValue] = {
    log.debug(s"Locating attribute: '$attributeId', category: '$category'.")

    val ontology = getRequestOntology(context)
    val result = if(isClassAttributeId(attributeId)){
      OntologyAttributeFinder.findClassAttributeValues(ontology.requestOntology, ontology.categoryIndividualIds(category), category.toString, attributeId.toString)
    } else {
      OntologyAttributeFinder.findAttributeValues(ontology.requestOntology, ontology.categoryIndividualIds(category), category.toString, attributeId.toString)
    }

    if (log.isDebugEnabled) {
      //TODO: Move to a toString implementation.
      val attributesString = result
        .map(at => "'" + at.categoryId + "' : '" + at.attributeId + "' = '" + at.valueString + "@" + at.valueType)
        .mkString(";\r\n")
      log.debug(s"Retrieved attribute values: \r\n$attributesString")
    }
    result
  }

  def getSupportedAttributes = {
    val ontology = getOntologyById(rootOntologyId)
    if (ontology == null) {
      log.error("Couldn't load ontology with id: " + rootOntologyId)
      Set.empty[String]
    }

    OntologyAttributeFinder.getAllSupportedAttributes(ontology) //assuming root onto imports others and
  }


  def queryOntologyWithSparqlPath(sparql: String, context: EvaluationCtx): Set[String] = {
    val ontology = getRequestOntology(context)

    OntologyAttributeFinder.queryOntologyWithSparqlPath(sparql, ontology.requestOntology, ontology.categoryIndividualIds)
  }

  def queryOntologyWithSparql(sparql: String, context: EvaluationCtx): Set[String] = {
    val ontology = getRequestOntology(context)

    OntologyAttributeFinder.queryOntologyWithSparql(sparql, ontology.requestOntology, ontology.categoryIndividualIds)
  }

  private def getOntologyById(ontoId: String): OWLOntology = {
    val iri = IRI.create(ontoId)
    val ontology = ontoMgr.getOntology(iri)
    if (ontology != null) {
      ontology
    } else {
      ontoMgr.loadOntology(iri)
    }
  }

  private def createFileIri(filePath: String): IRI = {
    IRI.create(getClass.getResource(filePath).toURI)
  }

}
