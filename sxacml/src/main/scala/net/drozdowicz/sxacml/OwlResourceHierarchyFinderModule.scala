package scala.net.drozdowicz.sxacml

import java.io.File
import java.net.URI

import net.drozdowicz.sxacml.OntologyAttributeFinder
import org.apache.commons.logging.LogFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntologyIRIMapper}
import org.semanticweb.owlapi.util.AutoIRIMapper
import org.wso2.balana.attr.AttributeValue
import org.wso2.balana.ctx.EvaluationCtx
import org.wso2.balana.finder.{ResourceFinderModule, ResourceFinderResult}
import uk.ac.manchester.cs.owl.owlapi

import scala.collection.JavaConverters._
import scala.util.control.ControlThrowable

/**
  * Created by michal on 19.03.2016.
  */
class OwlResourceHierarchyFinderModule(ontologyFolderPath: String, rootOntologyId: String, ontologyURIMap: Map[URI, URI] = Map.empty[URI, URI]) extends ResourceFinderModule() {
  private val log = LogFactory.getLog(classOf[OwlResourceHierarchyFinderModule])

  log.info("OwlResourceHierarchyFinderModule defined.")

  private val ontoMgr = OWLManager.createOWLOntologyManager()

  val mapper = new owlapi.OWLOntologyIRIMapperImpl();
  ontologyURIMap.foreach {case (uri, path) => mapper.addMapping(IRI.create(uri), IRI.create(path))}

  ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
    mapper,
    new AutoIRIMapper(new File(ontologyFolderPath), true)
  ).asJava
  )


  private val rootOntology = ontoMgr.loadOntology(IRI.create(rootOntologyId))

  private val hierarchyDesignatorId = OntologyAttributeFinder.getHierarchyDesignator(rootOntology).getOrElse("https://w3id.org/sxacml/request#hasSubResource")

  override def isChildSupported() = false

  override def isDescendantSupported() = true

  override def findDescendantResources(parentResourceId: AttributeValue , context: EvaluationCtx ) = {
    try {

      new ResourceFinderResult(OntologyAttributeFinder.findInstancesFromHierarchy(rootOntology,
          "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
          "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
          parentResourceId.encode(),
        hierarchyDesignatorId)
        .map(f => f.createAttributeValue()).asJava
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
