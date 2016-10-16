package scala.net.drozdowicz.sxacml

import java.io.File

import net.drozdowicz.sxacml.OntologyAttributeFinder
import org.apache.commons.logging.LogFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntologyIRIMapper}
import org.semanticweb.owlapi.util.AutoIRIMapper
import org.wso2.balana.attr.AttributeValue
import org.wso2.balana.ctx.EvaluationCtx
import org.wso2.balana.finder.{ResourceFinderResult, ResourceFinderModule}

import scala.collection.JavaConversions._
import scala.util.control.ControlThrowable

/**
  * Created by michal on 19.03.2016.
  */
class OwlResourceClassFinderModule(ontologyFolderPath: String, rootOntologyId: String) extends ResourceFinderModule() {
  private val log = LogFactory.getLog(classOf[OwlResourceClassFinderModule])

  log.info("OwlResourceClassFinderModule defined.")

  private val ontoMgr = OWLManager.createOWLOntologyManager()
  ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
    new AutoIRIMapper(new File(ontologyFolderPath), true))
  )
  private val rootOntology = ontoMgr.loadOntology(IRI.create(rootOntologyId))

  override def isChildSupported() = false

  override def isDescendantSupported() = true

  override def findDescendantResources(parentResourceId: AttributeValue , context: EvaluationCtx ) = {
    try {
      val instances = OntologyAttributeFinder.findInstancesOfClass(rootOntology,
        "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
        "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
        parentResourceId.encode())

      if(instances.isEmpty)
        new ResourceFinderResult()
      else
        new ResourceFinderResult(instances.map(f => f.createAttributeValue())
      )
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

}
