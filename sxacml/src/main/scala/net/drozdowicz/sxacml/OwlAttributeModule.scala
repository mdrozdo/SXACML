package net.drozdowicz.sxacml

import java.io.File
import java.net.URI
import java.util
import java.util.{Properties, UUID}

import onto.utils.OntologyUtils
import org.apache.commons.logging.LogFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntology, OWLOntologyIRIMapper, OWLOntologyManager}
import org.semanticweb.owlapi.util.AutoIRIMapper
import org.wso2.balana.attr.{AttributeFactory, AttributeValue, BagAttribute}
import org.wso2.balana.cond.EvaluationResult
import org.wso2.balana.ctx.EvaluationCtx
import org.wso2.balana.finder.AttributeFinderModule
import org.wso2.carbon.identity.entitlement.pip.PIPAttributeFinder

import scala.collection.JavaConverters._
import scala.net.drozdowicz.sxacml.OwlAttributeStore
import scala.util.control.ControlThrowable

//TODO Make this a super simple wrapper over something more functional etc.
// There shouldn't be any logic in this class, only the stuff necessary to satisfy Balana's interfaces
class OwlAttributeModule extends AttributeFinderModule with PIPAttributeFinder {
  private val log = LogFactory.getLog(classOf[OwlAttributeModule])

  private var ontologyFolderPath: String = "/"
  private var rootOntologyId: String = "http://drozdowicz.net/sxacml/request"

  private var owlStore: Option[OwlAttributeStore] = None

  log.info("OwlAttributeModule defined.")


  override def findAttribute(attributeType: URI, attributeId: URI, issuer: String, category: URI, context: EvaluationCtx): EvaluationResult = {
    try {
      val attributeValues = findAttributeValues(attributeId, category, context)
      val values = attributeValues.map(flatAttr => flatAttr.createAttributeValue)

      new EvaluationResult(new BagAttribute(attributeType, values.toList.asJava))
    } catch safely {
      case e: Throwable => {
        log.error("Error while processing the request: findAttribute. " + e.getClass.getName + e.getMessage, e)
        throw e
      }
    }
  }

  override def getAttributeValues(attributeType: URI, attributeId: URI, category: URI, issuer: String, context: EvaluationCtx): util.Set[String] = {
    try {
      findAttributeValues(attributeId, category, context).map(f => f.valueString).asJava
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
    owlStore.map(store => store.findAttributeValues(attributeId, category, context)).getOrElse(Set.empty)
  }

  override def init(properties: Properties): Unit = {
    try {
      log.info("Initializing SXACML attribute finder")
      ontologyFolderPath = properties.getProperty("ontologyFolderPath")
      rootOntologyId = properties.getProperty("rootOntologyId")

      owlStore = Some(new OwlAttributeStore(ontologyFolderPath, rootOntologyId))
    } catch safely {
      case e: Throwable => {
        log.error("Error while processing the request: init. " + e.getClass.getName + e.getMessage, e)
        throw e
      }
    }
  }

  def init(owlAttributeStore: OwlAttributeStore): Unit = {
    log.info("Initializing SXACML attribute finder")

    owlStore = Some(owlAttributeStore)
  }

  override def isDesignatorSupported() = true

  override def getSupportedCategories(): java.util.Set[String] =
    Set("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:action",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:environment").asJava

  override def getSupportedAttributes: util.Set[String] = {
    try {
      owlStore.map(store => store.getSupportedAttributes).getOrElse(Set.empty).asJava
    } catch safely {
      case e: Throwable => {
        log.error("Error while processing the request: getSupportedAttributes. " + e.getClass.getName + e.getMessage, e)
        throw e
      }
    }
  }

  override def getModuleName: String = "OwlAttributeFinder"

  override def overrideDefaultCache(): Boolean = true

  //TODO: implement cache?
  override def clearCache(): Unit = {}

  //TODO: implement cache?
  override def clearCache(attributeId: Array[String]): Unit = {}
}