package net.drozdowicz.sxacml

import java.io.File
import java.net.URI
import java.util
import java.util.{Properties, UUID}

import onto.utils.OntologyUtils
import org.apache.commons.logging.LogFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{OWLOntologyIRIMapper, IRI, OWLOntology, OWLOntologyManager}
import org.semanticweb.owlapi.util.AutoIRIMapper
import org.wso2.balana.attr.{AttributeFactory, AttributeValue, BagAttribute}
import org.wso2.balana.cond.EvaluationResult
import org.wso2.balana.ctx.EvaluationCtx
import org.wso2.balana.finder.AttributeFinderModule
import org.wso2.carbon.identity.entitlement.pip.PIPAttributeFinder

import scala.collection.JavaConversions._
import scala.util.control.ControlThrowable

//TODO Make this a super simple wrapper over something more functional etc.
// There shouldn't be any logic in this class, only the stuff necessary to satisfy Balana's interfaces
class OwlAttributeModule extends AttributeFinderModule with PIPAttributeFinder {
  private val log = LogFactory.getLog(classOf[OwlAttributeModule])

  private var ontologyFolderPath: String = "/"
  private var rootOntologyId: String = "http://drozdowicz.net/sxacml/request"


  log.info("OwlAttributeModule defined.")

  private val ontoMgr = OWLManager.createOWLOntologyManager()

  override def findAttribute(attributeType: URI, attributeId: URI, issuer: String, category: URI, context: EvaluationCtx): EvaluationResult = {
    try {
      val attributeValues = findAttributeValues(attributeId, category, context)
      val values = attributeValues.map(flatAttr => flatAttr.createAttributeValue).toList

      new EvaluationResult(new BagAttribute(attributeType, values))
    } catch safely {
      case e: Throwable => {
        log.error("Error while processing the request: findAttribute. " + e.getClass.getName + e.getMessage, e)
        throw e
      }
    }
  }

  override def getAttributeValues(attributeType: URI, attributeId: URI, category: URI, issuer: String, context: EvaluationCtx): util.Set[String] = {
    try {
      findAttributeValues(attributeId, category, context).map(f => f.valueString)
    } catch safely {
      case e: Throwable => {
        log.error("Error while processing the request: getAttributeValues. " + e.getClass.getName + e.getMessage, e)
        throw e
      }
    }
  }

  def safely[T](handler: PartialFunction[Throwable, T]): PartialFunction[Throwable, T] = {
    case ex: ControlThrowable => throw ex
    // case ex: OutOfMemoryError (Assorted other nasty exceptions you don't want to catch)

    //If it's an exception they handle, pass it on
    case ex: Throwable if handler.isDefinedAt(ex) => handler(ex)

    // If they didn't handle it, rethrow. This line isn't necessary, just for clarity
    case ex: Throwable => throw ex
  }

  private def findAttributeValues(attributeId: URI, category: URI, context: EvaluationCtx): Set[FlatAttributeValue] = {
    val requestId = UUID.randomUUID().toString

    val attributes = ContextParser.Parse(context.getRequestCtx)
    val categoryIndividualIds = attributes
      .map(at => (at.categoryId, RequestOntologyGenerator.getCategoryIndividualUri(requestId, at)))
      .toMap
    val requestOntology = RequestOntologyGenerator.convertToOntology(ontoMgr)(requestId, attributes, collection.immutable.Set(IRI.create(rootOntologyId)))
    OntologyAttributeFinder.findAttributeValues(requestOntology, categoryIndividualIds(category), category.toString, attributeId.toString)
  }

  override def init(properties: Properties): Unit = {
    try{
      log.info("Initializing SXACML attribute finder")
      ontologyFolderPath = properties.getProperty("ontologyFolderPath")
      rootOntologyId = properties.getProperty("rootOntologyId")

      log.info("Mapping ontology folder to:" + ontologyFolderPath)
      ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
        new AutoIRIMapper(new File(ontologyFolderPath), true))
      )
    } catch safely {
      case e: Throwable => {
        log.error("Error while processing the request: init. " + e.getClass.getName + e.getMessage, e)
        throw e
      }
    }
  }

  override def isDesignatorSupported() = true

  override def getSupportedCategories(): java.util.Set[String] =
    Set("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:action",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:environment")

  override def getSupportedAttributes: util.Set[String] = {
   try{
     val ontology = getOntologyById(rootOntologyId)
     if(ontology == null){
       log.error("Couldn't load ontology with id: " + rootOntologyId)
       return Set.empty[String]
     }
     OntologyAttributeFinder.getAllSupportedAttributes(ontology) //assuming root onto imports others and
  } catch safely {
     case e: Throwable => {
       log.error("Error while processing the request: getSupportedAttributes. " + e.getClass.getName + e.getMessage, e)
       throw e
     }
   }
  }

  private def getOntologyById(ontoId: String): OWLOntology = {
    val iri = IRI.create(ontoId)
    val ontology = ontoMgr.getOntology(iri)
    if(ontology != null){
      ontology
    } else {
      ontoMgr.loadOntology(iri)
    }
  }

  private def createFileIri(filePath: String): IRI = {
    IRI.create(getClass.getResource(filePath).toURI)
  }

  override def getModuleName: String = "OwlAttributeFinder"

  override def overrideDefaultCache(): Boolean = false

  //TODO: implement cache?
  override def clearCache(): Unit = {}

  //TODO: implement cache?
  override def clearCache(attributeId: Array[String]): Unit = {}
}