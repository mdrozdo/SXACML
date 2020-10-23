package scala.net.drozdowicz.sxacml

import java.io.File
import java.net.URI
import java.util
import java.util.{Properties, UUID}

import scala.collection.JavaConverters._
import net.drozdowicz.sxacml._
import openllet.core.exceptions.InconsistentOntologyException
import org.apache.commons.logging.LogFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntology, OWLOntologyIRIMapper}
import org.semanticweb.owlapi.util.AutoIRIMapper
import org.wso2.balana.attr.{BagAttribute, DateAttribute, DateTimeAttribute, TimeAttribute}
import org.wso2.balana.cond.EvaluationResult
import org.wso2.balana.ctx.{Attribute, EvaluationCtx}
import org.wso2.balana.xacml3.Attributes
import uk.ac.manchester.cs.owl.owlapi
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyIRIMapperImpl

import scala.collection.{GenSeq, mutable}
import scala.util.control.ControlThrowable

/**
  * Created by drozd on 22.03.2020.
  */
class OwlAttributeStore(ontologyFolderPath: String, rootOntologyId: String, ontologyURIMap: Map[URI, URI] = Map.empty[URI, URI]) {

  private val log = LogFactory.getLog(classOf[OwlAttributeStore])

  private val ontoMgr = OWLManager.createOWLOntologyManager()

  log.info("Initializing SXACML OWL attribute manager")

  log.info("Mapping ontology folder to:" + ontologyFolderPath)

  val mapper = new owlapi.OWLOntologyIRIMapperImpl();
  ontologyURIMap.foreach { case (uri, path) => mapper.addMapping(IRI.create(uri), IRI.create(path)) }

  ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
    mapper,
    new AutoIRIMapper(new File(ontologyFolderPath), true)
  ).asJava
  )

  private var requestOntologies: Map[Option[String], RequestOntology] = Map.empty[Option[String], RequestOntology]

  private case class RequestOntology(requestOntology: OWLOntology, requestId: String, categoryIndividualIds: Map[URI, String])

  private def generateNewRequestOntology(context: EvaluationCtx, sessionId: Option[String], requestId: String, attributes: Seq[ContextAttributeValue]): RequestOntology = {
    //TODO Move RequestOntology to a real class and make generator return it.
    val ontologyId = RequestOntologyGenerator.createOntologyId(sessionId, requestId)
    val categoryIndividualIds = RequestOntologyGenerator.getCategoryIndividualIds(ontologyId, requestId, attributes)
    val requestIndividualId = RequestOntologyGenerator.getRequestIndividualId(ontologyId, requestId)
    val requestOntology = RequestOntologyGenerator.convertToOntology(ontoMgr)(sessionId, requestId, attributes, collection.immutable.Set(IRI.create(rootOntologyId)))

    new RequestOntology(requestOntology, requestId, categoryIndividualIds + (new URI(Constants.REQUEST_CLASS_ID) -> requestIndividualId))
  }

  private def updateRequestOntology(context: EvaluationCtx, existingOntology: RequestOntology, sessionId: Option[String], requestId: String, attributes: Seq[ContextAttributeValue]): RequestOntology = {
    //TODO Move RequestOntology to a real class and make generator return it.
    val ontologyId = RequestOntologyGenerator.createOntologyId(sessionId, requestId)
    val categoryIndividualIds = RequestOntologyGenerator.getCategoryIndividualIds(ontologyId, requestId, attributes)
    val requestIndividualId = RequestOntologyGenerator.getRequestIndividualId(ontologyId, requestId)
    RequestOntologyGenerator.updateOntology(ontoMgr)(existingOntology.requestOntology, sessionId, requestId, attributes, collection.immutable.Set(IRI.create(rootOntologyId)))

    new RequestOntology(existingOntology.requestOntology, requestId, categoryIndividualIds + (new URI(Constants.REQUEST_CLASS_ID) -> requestIndividualId))
  }


  private def getRequestOntology(context: EvaluationCtx) = {
    val requestAttributes = ContextParser.Parse(context.getRequestCtx)

    if (log.isDebugEnabled) {
      val attributesString = requestAttributes
        .map(_.toString)
        .mkString(";\r\n")
      log.debug(s"Received context with attributes: \r\n$attributesString")
    }

    val attributes = requestAttributes ++ getCurrentDateTimeAttributesMissingFromRequest(context)

    val sessionId = attributes.collectFirst {
      case attributeValue: FlatAttributeValue if attributeValue.attributeId.toString == Constants.SESSION_ID => attributeValue.valueString
    }

    val newRequestId = context.hashCode().toString

    requestOntologies.get(sessionId).collectFirst {
      case ontology: RequestOntology => {
        if (ontology.requestId == newRequestId)
          ontology
        else {
          if (sessionId == None) {
            val ontology = generateNewRequestOntology(context, sessionId, newRequestId, attributes)
            requestOntologies = requestOntologies + (sessionId -> ontology)
            ontology
          } else {
            val updatedOntology = updateRequestOntology(context, ontology, sessionId, newRequestId, attributes)
            requestOntologies = requestOntologies + (sessionId -> updatedOntology)
            updatedOntology
          }
        }
      }
    }.getOrElse({
      val ontology = generateNewRequestOntology(context, sessionId, newRequestId, attributes)
      requestOntologies = requestOntologies + (sessionId -> ontology)
      ontology
    })
  }

  def containsAttribute(requestAttributeSet: scala.collection.Seq[Attribute], id: URI) = {
    !requestAttributeSet.find(a=>a.getId.equals(id)).isEmpty
  }

  def getCurrentDateTimeAttributesMissingFromRequest(context: EvaluationCtx) = {
    val envCatUri = URI.create(Constants.CATEGORY_ENVIRONMENT)
    val requestAttributeSet = context.getRequestCtx.getAttributesSet.asScala.find(a=> a.getCategory.equals(envCatUri)).map(a=>a.getAttributes.asScala.toSeq).toSeq.flatten

    List(FlatAttributeValue(envCatUri, URI.create("urn:oasis:names:tc:xacml:1.0:environment:current-date"), URI.create(DateAttribute.identifier), context.getCurrentDate.encode()),
      FlatAttributeValue(envCatUri, URI.create("urn:oasis:names:tc:xacml:1.0:environment:current-time"), URI.create(TimeAttribute.identifier), context.getCurrentTime.encode()),
      FlatAttributeValue(envCatUri, URI.create("urn:oasis:names:tc:xacml:1.0:environment:current-dateTime"), URI.create(DateTimeAttribute.identifier), context.getCurrentDateTime.encode()))
      .flatMap(a => if(!containsAttribute(requestAttributeSet, a.attributeId)) Some(a) else None)
      .toSeq
  }


  def isClassAttributeId(attributeId: URI): Boolean = {
    return Constants.classIdForCategory.values.toList.contains(attributeId)
  }

  def findAttributeValues(attributeId: URI, category: URI, context: EvaluationCtx): Set[FlatAttributeValue] = {
    log.debug(s"Locating attribute: '$attributeId', category: '$category'.")

    val ontology = getRequestOntology(context)
    val result = if (isClassAttributeId(attributeId)) {
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
