package net.drozdowicz.sxacml

import java.net.URI

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.io.SystemOutDocumentTarget
import org.semanticweb.owlapi.model._

/**
 * Created by michal on 2015-03-18.
 */
object RequestOntologyGenerator {
  val idAttributes = Array(
    "urn:oasis:names:tc:xacml:1.0:subject:subject-id",
    "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
    "urn:oasis:names:tc:xacml:1.0:action:action-id"
  )

  def attributeDescribesId(attributeValue: FlatAttributeValue): Boolean = {
    idAttributes.contains(attributeValue.attributeId.toString) &&
      "http://www.w3.org/2001/XMLSchema#anyURI".equalsIgnoreCase(attributeValue.valueType.toString)
  }

  def convertToOntology(owlManager: OWLOntologyManager)(requestId : String, requestAttributes : Set[FlatAttributeValue], otherOntologies: Set[IRI]) : OWLOntology = {
    val factory = owlManager.getOWLDataFactory()

    val ontologyId = "http://drozdowicz.net/sxacml/onto/request/" + requestId
    val ontology = owlManager.createOntology(IRI.create(ontologyId))

    requestAttributes.foreach(attributeValue=>{

      val uri: String = getCategoryIndividualUri(requestId, attributeValue)
      val category = factory.getOWLNamedIndividual(IRI.create(uri))
      val categoryClass = factory.getOWLClass(IRI.create(attributeValue.categoryId))
      val classAxiom = factory.getOWLClassAssertionAxiom(categoryClass, category)
      owlManager.addAxiom(ontology, classAxiom)

      val attribute = factory.getOWLDataProperty(IRI.create(attributeValue.attributeId))
      val datatype = if(attributeValue.valueType.toString.startsWith("http://www.w3.org/2001/XMLSchema#"))
        factory.getOWLDatatype(IRI.create(attributeValue.valueType))
      else
        factory.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#string"))

      val value = factory.getOWLLiteral(attributeValue.valueString, datatype)
      val propertyAxiom = factory.getOWLDataPropertyAssertionAxiom(attribute, category, value);
      owlManager.addAxiom(ontology, propertyAxiom)
    })

    otherOntologies.foreach(ontologyIRI=>{
        val importDecl = factory.getOWLImportsDeclaration(ontologyIRI)
        owlManager.applyChange(new AddImport(ontology, importDecl))
        owlManager.loadOntology(ontologyIRI)
    })

    owlManager.saveOntology(ontology, new SystemOutDocumentTarget())
    ontology
  }

  def getCategoryIndividualUri(requestId: String, attributeValue: FlatAttributeValue): String = {
    val uri = if (attributeDescribesId(attributeValue)) attributeValue.valueString
    else attributeValue.categoryId + ":request_" + requestId;
    uri
  }
}
