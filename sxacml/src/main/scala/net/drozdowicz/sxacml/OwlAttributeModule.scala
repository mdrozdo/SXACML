package net.drozdowicz.sxacml

import java.io.File
import java.net.URI
import java.util
import java.util.{Properties, UUID}

import onto.utils.OntologyUtils
import org.apache.commons.logging.LogFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntology, OWLOntologyManager}
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
  private var rootOntologyId: String = "" //TODO: Set to the base XACML ontology


  log.info("OwlAttributeModule defined.")

  private val ontoMgr = OWLManager.createOWLOntologyManager()

  override def findAttribute(attributeType: URI, attributeId: URI, issuer: String, category: URI, context: EvaluationCtx): EvaluationResult = {
    val attributeValues = findAttributeValues(attributeId, category, context)
    val values = attributeValues.map(flatAttr => createAttributeValue(flatAttr)).toList

    new EvaluationResult(new BagAttribute(attributeType, values))
  }

  private def createAttributeValue(flatValue: FlatAttributeValue): AttributeValue = {
    AttributeFactory.getInstance().createValue(flatValue.valueType, flatValue.valueString)
  }

  override def getAttributeValues(attributeType: URI, attributeId: URI, category: URI, issuer: String, context: EvaluationCtx): util.Set[String] = {
    try {
      findAttributeValues(attributeId, category, context).map(f => f.valueString)
    } catch safely {
      case e: Throwable => {
        log.error("Error while processing the request. " + e.getClass.getName + e.getMessage, e)
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

  /*
         TODO Should there be links added between diff categories? Like "subject1" requests "resourceA"?? Later
         TODO Create an XACML ontology with appropriate classes etc.
         */
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
    log.info("Initializing SXACML attribute finder")
    ontologyFolderPath = properties.getProperty("ontologyFolderPath")
    rootOntologyId = properties.getProperty("rootOntologyId")

    loadAllOntologiesFromResources(ontoMgr, ontologyFolderPath)
  }

  private def loadAllOntologiesFromResources(manager: OWLOntologyManager, ontologyFolder: String) = {
    val ontologyPaths = ResourceFiles.getFilesFromResourceDirectory(getClass, ontologyFolder)
    ontologyPaths.foreach(op => loadOntology(manager, op))
  }

  private def loadOntology(manager: OWLOntologyManager, ontologyPath: String) = {
    val ontologyFile = getResourceFile(ontologyPath)
    log.debug("Importing ontology from file %s".format(ontologyFile.getAbsolutePath))
    manager.loadOntologyFromOntologyDocument(ontologyFile)
  }

  private def getResourceFile(filePath: String): File = {
    new File(getClass.getResource(filePath).toURI)
  }

  override def isDesignatorSupported() = true

  override def getSupportedCategories(): java.util.Set[String] =
    Set("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:action",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:environment")

  override def getSupportedAttributes: util.Set[String] =
    OntologyAttributeFinder.getAllSupportedAttributes(getOntologyById(rootOntologyId)) //assuming root onto imports others and

  private def getOntologyById(ontoId: String): OWLOntology = {
    val iri = IRI.create(ontoId)
    ontoMgr.getOntology(iri)
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