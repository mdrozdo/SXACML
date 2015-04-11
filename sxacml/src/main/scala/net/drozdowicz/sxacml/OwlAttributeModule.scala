package net.drozdowicz.sxacml

import java.io.File
import java.net.URI
import java.util
import java.util.Properties

import onto.sparql.SparqlReader
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{OWLOntologyIRIMapper, IRI, OWLOntology, OWLOntologyManager}
import org.semanticweb.owlapi.util.SimpleIRIMapper
import org.wso2.balana.attr.{BagAttribute, AttributeValue}
import org.wso2.balana.cond.EvaluationResult
import org.wso2.balana.ctx.{Status, EvaluationCtx}
import org.wso2.balana.finder.AttributeFinderModule
import org.wso2.balana.xacml3.Attributes
import org.wso2.carbon.identity.entitlement.pip.PIPAttributeFinder
import collection.JavaConversions._

class OwlAttributeModule extends AttributeFinderModule with PIPAttributeFinder{

  override def findAttribute(attributeType: URI, attributeId: URI, issuer: String, category: URI, context: EvaluationCtx): EvaluationResult = {
    val attributeValues = findAttributeValues(attributeId, category, context)
    val values = attributeValues.map(flatAttr=>createAttributeValue(flatAttr)).toList

    new EvaluationResult(new BagAttribute(attributeType, values))
  }

  override def getAttributeValues(attributeType: URI, attributeId: URI, category: URI, issuer: String, context: EvaluationCtx): util.Set[String] = {
    findAttributeValues(attributeId, category, context).map(f=>f.valueString)
  }

  override def init(properties: Properties): Unit = {}

  override def isDesignatorSupported() = true

  override def getSupportedCategories(): java.util.Set[String] = {
    Set("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:action",
      "urn:oasis:names:tc:xacml:3.0:attribute-category:environment")
  }

  //TODO: Generate supported attributes basing on the data properties and classes from the ontology
  override def getSupportedAttributes: util.Set[String] = {
    Set("http://drozdowicz.net/sxacml/test1#isAdult")
  }

  override def getModuleName: String = "OwlAttributeFinder"

  override def overrideDefaultCache(): Boolean = false

  //TODO: implement cache?
  override def clearCache(): Unit = {}

  //TODO: implement cache?
  override def clearCache(attributeId: Array[String]): Unit = {}

  /*
       DONE Convert the attributes from the EvaluationCtx to an ontology
        DONE Extract the attribute values from the EvaluationCtx
        DONE Create ontology values from attribute values - category should become an individual, attribute id should become property, value should become a literal
          DONE RequestOntologyGenerator.convertToOntology(Set[FlatAttributeValue])

       DONE Make the context ontology import the required other ontologies
       DONE Expand test ontology with something I can reason on -> isAdult
       DONE Run a SPARQL query for the value of the attribute to find
       TODO Convert the SPARQL result to the EvaluationResult
       TODO Should there be links added between diff categories? Like "subject1" requests "resourceA"?? Later
       TODO Type of individual should be reflected in some output attribute
       TODO Create an XACML ontology with appropriate classes etc.
       */
  private def findAttributeValues(attributeId: URI, category: URI, context: EvaluationCtx): Set[FlatAttributeValue] = {
    val requestId = "123" //TODO find a way to generate request Id
    val ontologyId = "http://drozdowicz.net/sxacml/test1"
    val ontologyFilePath = "/test1.owl"

    val ontoMgr = OWLManager.createOWLOntologyManager()
    importOntology(ontoMgr, ontologyId, ontologyFilePath)

    val attributes = ContextParser.Parse(context.getRequestCtx)
    val requestOntology = RequestOntologyGenerator.convertToOntology(ontoMgr)(requestId, attributes, collection.immutable.Set(IRI.create(ontologyId)))
    val attributeValues = OntologyAttributeFinder.findAttributeValues(requestOntology, requestId, category.toString, attributeId.toString)
    attributeValues
  }

  private def importOntology(manager: OWLOntologyManager, ontologyId: String, ontologyPath: String) = {
    val toImport = IRI.create(ontologyId)
    manager.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
      new SimpleIRIMapper(toImport, IRI.create(new File(getClass.getResource(ontologyPath).toURI)))))
  }

  private def createAttributeValue(flatValue: FlatAttributeValue): AttributeValue = {
    return new AttributeValue(flatValue.valueType) {
      override def encode(): String = {
        return flatValue.valueString
      }
    }
  }


}