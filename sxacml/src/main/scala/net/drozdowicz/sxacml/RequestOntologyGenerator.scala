package net.drozdowicz.sxacml

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.io.SystemOutDocumentTarget
import org.semanticweb.owlapi.model.{OWLDataPropertyAssertionAxiom, IRI, OWLOntology}

/**
 * Created by michal on 2015-03-18.
 */
object RequestOntologyGenerator {
  def convertToOntology(requestId : String, requestAttributes : Set[FlatAttributeValue]) : OWLOntology = {
    val manager = OWLManager.createOWLOntologyManager()
    val factory = manager.getOWLDataFactory()

    val ontologyId = "http://drozdowicz.net/sxacml/onto/request/" + requestId
    val ontology = manager.createOntology(IRI.create(ontologyId))

    requestAttributes.foreach(attributeValue=>{
      val category = factory.getOWLNamedIndividual(IRI.create(attributeValue.categoryId + ":request_"+requestId))
      val categoryClass = factory.getOWLClass(IRI.create(attributeValue.categoryId))
      val classAxiom = factory.getOWLClassAssertionAxiom(categoryClass, category)
      manager.addAxiom(ontology, classAxiom)

      val attribute = factory.getOWLDataProperty(IRI.create(attributeValue.attributeId))
      val value = factory.getOWLLiteral(attributeValue.valueString, factory.getOWLDatatype(IRI.create(attributeValue.valueType)))
      val propertyAxiom = factory.getOWLDataPropertyAssertionAxiom(attribute, category, value);
      manager.addAxiom(ontology, propertyAxiom)
    })

    manager.saveOntology(ontology, new SystemOutDocumentTarget())
    ontology
  }
}
