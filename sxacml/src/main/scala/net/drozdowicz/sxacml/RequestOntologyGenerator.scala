package net.drozdowicz.sxacml

import java.net.URI
import java.util.jar.Attributes

import net.drozdowicz.sxacml.RequestOntologyGenerator.getCategoryIndividualUri
import org.semanticweb.owlapi.io.SystemOutDocumentTarget
import org.semanticweb.owlapi.model._
import scala.collection.JavaConversions._

/**
  * Created by michal on 2015-03-18.
  */
object RequestOntologyGenerator {
  val idAttributes = Array(
    "urn:oasis:names:tc:xacml:1.0:subject:subject-id",
    "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
    "urn:oasis:names:tc:xacml:1.0:action:action-id"
  )

  def getCategoryIndividualIds(requestId: String, attributes: Seq[ContextAttributeValue]) = {
    val categoryAttributes = attributes.groupBy(at => at.categoryId)
    categoryAttributes.map { case (cat, atts) => {
      (cat, getCategoryIndividualUri(requestId, atts))
    }
    }
  }

  //TODO: Split into subroutines
  def convertToOntology(owlManager: OWLOntologyManager)(requestId: String, requestAttributes: Seq[ContextAttributeValue], otherOntologies: Set[IRI]): OWLOntology = {

    def axiomsFromAttributes(parent: OWLNamedIndividual, attributes: Seq[ContextAttributeValue], categoryIndividuals: Map[URI, OWLNamedIndividual], factory: OWLDataFactory): Seq[OWLIndividualAxiom] = {
      attributes.flatMap {
        case attributeValue: FlatAttributeValue =>
          val attribute = factory.getOWLDataProperty(IRI.create(attributeValue.attributeId))
          val datatype = if (attributeValue.valueType.toString.startsWith("http://www.w3.org/2001/XMLSchema#"))
            factory.getOWLDatatype(IRI.create(attributeValue.valueType))
          else
            factory.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#string"))

          val value = factory.getOWLLiteral(attributeValue.valueString, datatype)

          Seq(factory.getOWLDataPropertyAssertionAxiom(attribute, parent, value))

        case NestedAttributeValue(categoryId, propertyId, namespace, localName, children) =>
          val elementId = new URI(namespace + "#" + localName)
          val element = factory.getOWLNamedIndividual(IRI.create(getIndividualUri(requestId, elementId)))
          val elementClass = factory.getOWLClass(IRI.create(elementId))
          val classAxiom = factory.getOWLClassAssertionAxiom(elementClass, element)

          val property = factory.getOWLObjectProperty(namespace + "#has" + localName.capitalize)
          val propertyAxiom = factory.getOWLObjectPropertyAssertionAxiom(property, parent, element)
          Seq(classAxiom, propertyAxiom) ++ axiomsFromAttributes(element, children, categoryIndividuals, factory)
      }
    }

    val factory = owlManager.getOWLDataFactory()

    val ontologyId = "http://drozdowicz.net/sxacml/onto/request/" + requestId
    val ontology = owlManager.createOntology(IRI.create(ontologyId))

    val categoryIds = getCategoryIndividualIds(requestId, requestAttributes)

    val categoryIndividuals = categoryIds.map {
      case (cat, id) => {
        val category = factory.getOWLNamedIndividual(IRI.create(id))
        val categoryClass = factory.getOWLClass(IRI.create(cat))
        val classAxiom = factory.getOWLClassAssertionAxiom(categoryClass, category)
        owlManager.addAxiom(ontology, classAxiom)

        (cat, category)
      }
    }
    val categoryAttributes = requestAttributes.groupBy(at => at.categoryId)

    val axioms = categoryAttributes.flatMap{case (id, attributes) => axiomsFromAttributes(categoryIndividuals(id), attributes, categoryIndividuals, factory)}

    owlManager.addAxioms(ontology, axioms.toStream)

    otherOntologies.foreach(ontologyIRI => {
      val importDecl = factory.getOWLImportsDeclaration(ontologyIRI)
      owlManager.applyChange(new AddImport(ontology, importDecl))
      owlManager.loadOntology(ontologyIRI)
    })

    owlManager.saveOntology(ontology, new SystemOutDocumentTarget())
    ontology


  }

  def getCategoryIndividualUri(requestId: String, attributes: Seq[ContextAttributeValue]): String = {
    val catId = attributes.head.categoryId

    val flatAttributes = (attributes.head match {
      case nested: NestedAttributeValue => nested.children
      case _: FlatAttributeValue => attributes
    }).filter(at => at.isInstanceOf[FlatAttributeValue]).map(at => at.asInstanceOf[FlatAttributeValue])

    flatAttributes.find(at => attributeDescribesId(at)).map(av => av.valueString)
      .getOrElse(catId + "#request_" + requestId)
  }

  def getIndividualUri(requestId: String, elementId: URI): String = elementId + "_request_" + requestId

  def attributeDescribesId(attributeValue: FlatAttributeValue): Boolean = {
    idAttributes.contains(attributeValue.attributeId.toString) &&
      "http://www.w3.org/2001/XMLSchema#anyURI".equalsIgnoreCase(attributeValue.valueType.toString)
  }
}
