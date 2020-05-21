package net.drozdowicz.sxacml

import java.net.URI
import java.util.jar.Attributes

import org.semanticweb.owlapi.io.SystemOutDocumentTarget
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.model.parameters.Imports

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.net.drozdowicz.sxacml.Constants

/**
  * Created by michal on 2015-03-18.
  */
object RequestOntologyGenerator {
  private val idAttributes = Array(
    "urn:oasis:names:tc:xacml:1.0:subject:subject-id",
    "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
    "urn:oasis:names:tc:xacml:1.0:action:action-id"
  )

  private val categoryAssignmentProperties = Map(
    new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject") -> new URI("http://drozdowicz.net/onto/access-control#requestedBy"),
    new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource") -> new URI("http://drozdowicz.net/onto/access-control#concernsResource"),
    new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:action") -> new URI("http://drozdowicz.net/onto/access-control#concernsAction"),
    new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:environment") -> new URI("http://drozdowicz.net/onto/access-control#inContextOf")
  )

  def getCategoryIndividualIds(requestId: String, attributes: Seq[ContextAttributeValue]) = {
    val categoryAttributes = attributes.groupBy(at => at.categoryId).toMap

    categoryAssignmentProperties.keySet.map(cat => {
      (cat, if (categoryAttributes.contains(cat))
        getCategoryIndividualUri(requestId, categoryAttributes(cat)) else
        getNewCategoryIndividualUri(requestId, cat))
    }).toMap
  }

  def getRequestIndividualId(requestId: String) = {
    getNewCategoryIndividualUri(requestId, new URI(Constants.REQUEST_CLASS_ID))
  }

  def convertToOntology(owlManager: OWLOntologyManager)(requestId: String, requestAttributes: Seq[ContextAttributeValue], otherOntologies: Set[IRI]): OWLOntology = {
    val requestAttributesOnlyCategories = requestAttributes.filter(at => categoryAssignmentProperties.contains(at.categoryId))

    val factory = owlManager.getOWLDataFactory()

    val ontologyId = "http://drozdowicz.net/sxacml/onto/request/" + requestId
    val ontology = owlManager.createOntology(IRI.create(ontologyId))

    otherOntologies.foreach(ontologyIRI => {
      val importDecl = factory.getOWLImportsDeclaration(ontologyIRI)
      owlManager.applyChange(new AddImport(ontology, importDecl))
      owlManager.loadOntology(ontologyIRI)
    })

    val categoryIndividualIds = getCategoryIndividualIds(requestId, requestAttributesOnlyCategories)

    val (categoryIndividuals, categoryAxioms) = axiomsFromCategories(factory, categoryIndividualIds)
    owlManager.addAxioms(ontology, categoryAxioms.toStream)

    val categoryAttributes = requestAttributesOnlyCategories.groupBy(at => at.categoryId)
    val attributeAxioms = categoryAttributes.flatMap { case (id, attributes) => axiomsFromAttributes(requestId, categoryIndividuals(id), attributes, categoryIndividuals, factory, ontology) }

    owlManager.addAxioms(ontology, createRequestAxioms(factory, requestId, categoryIndividuals))

    owlManager.addAxioms(ontology, attributeAxioms.toStream)

    owlManager.saveOntology(ontology, new SystemOutDocumentTarget())
    ontology
  }

  private def getCategoryIndividualUri(requestId: String, attributes: Seq[ContextAttributeValue]): String = {
    val catId = attributes.head.categoryId

    val flatAttributes = (attributes.head match {
      case nested: NestedAttributeValue => nested.children
      case _: FlatAttributeValue => attributes
    }).filter(at => at.isInstanceOf[FlatAttributeValue]).map(at => at.asInstanceOf[FlatAttributeValue])

    flatAttributes.find(at => attributeDescribesId(at)).map(av => av.valueString)
      .getOrElse(getNewCategoryIndividualUri(requestId, catId))
  }

  private def getNewCategoryIndividualUri(requestId: String, categoryId: URI) = categoryId + "#request_" + requestId

  private def getIndividualUri(requestId: String, elementId: URI, counter: Int): String = s"${elementId}_${counter}_${requestId}"

  private def isIRI(value: String): Boolean = {
    return "" != IRI.create(value).getNamespace
  }

  private def attributeDescribesId(attributeValue: FlatAttributeValue): Boolean = {
    idAttributes.contains(attributeValue.attributeId.toString) && isIRI(attributeValue.valueString)
  }

  private def axiomsFromCategories(factory: OWLDataFactory, categoryIndividualIds: Map[URI, String]) = {
    val categoryAxioms = ListBuffer[OWLAxiom]()
    val categoryIndividuals = categoryIndividualIds.map {
      case (cat, id) => {
        val categoryIndividual = factory.getOWLNamedIndividual(IRI.create(id))
        val categoryClass = factory.getOWLClass(IRI.create(cat))
        val classAxiom = factory.getOWLClassAssertionAxiom(categoryClass, categoryIndividual)
        categoryAxioms += classAxiom

        (cat, categoryIndividual)
      }
    }

    (categoryIndividuals, categoryAxioms)
  }

  private def axiomsFromAttributes(requestId: String, parent: OWLNamedIndividual, attributes: Seq[ContextAttributeValue], categoryIndividuals: Map[URI, OWLNamedIndividual], factory: OWLDataFactory, ontology: OWLOntology): Seq[OWLIndividualAxiom] = {
    attributes.zipWithIndex.flatMap {
      case (attributeValue: FlatAttributeValue, i) =>
        if (attributeValue.attributeId == Constants.classIdForCategory(attributeValue.categoryId.toString)) {
          val elementClass = factory.getOWLClass(IRI.create(attributeValue.valueString))
          val classAxiom = factory.getOWLClassAssertionAxiom(elementClass, parent)

          Seq(classAxiom)
        } else if (ontology.containsObjectPropertyInSignature(IRI.create(attributeValue.attributeId), Imports.INCLUDED)) {
          val attribute = factory.getOWLObjectProperty(IRI.create(attributeValue.attributeId))

          val value = factory.getOWLNamedIndividual(IRI.create(attributeValue.valueString))

          Seq(factory.getOWLObjectPropertyAssertionAxiom(attribute, parent, value))
        }
        else {
          val attribute = factory.getOWLDataProperty(IRI.create(attributeValue.attributeId))
          val datatype = if (attributeValue.valueType.toString.startsWith("http://www.w3.org/2001/XMLSchema#"))
            factory.getOWLDatatype(IRI.create(attributeValue.valueType))
          else
            factory.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#string"))

          val value = factory.getOWLLiteral(attributeValue.valueString, datatype)

          Seq(factory.getOWLDataPropertyAssertionAxiom(attribute, parent, value))
        }
      case (NestedAttributeValue(categoryId, propertyId, namespace, localName, children), i) =>
        val elementId = new URI(namespace + "#" + localName)
        val element = factory.getOWLNamedIndividual(IRI.create(getIndividualUri(requestId, elementId, i)))
        val elementClass = factory.getOWLClass(IRI.create(elementId))
        val classAxiom = factory.getOWLClassAssertionAxiom(elementClass, element)

        val propertyIdVal = propertyId.map(p => p.toString).getOrElse(namespace + "#has" + localName.capitalize)
        val property = factory.getOWLObjectProperty(propertyIdVal)
        val propertyAxiom = factory.getOWLObjectPropertyAssertionAxiom(property, parent, element)
        Seq(classAxiom, propertyAxiom) ++ axiomsFromAttributes(requestId, element, children, categoryIndividuals, factory, ontology)
    }
  }

  private def createRequestAxioms(factory: OWLDataFactory, requestId: String, categoryIndividuals: Map[URI, OWLNamedIndividual]) = {
    val requestIndividual = factory.getOWLNamedIndividual(getRequestIndividualId(requestId))
    val requestClass = factory.getOWLClass(IRI.create(Constants.REQUEST_CLASS_ID))
    val requestAxiom = factory.getOWLClassAssertionAxiom(requestClass, requestIndividual)

    val categoryPropertyAxioms = categoryIndividuals.map({
      case (k, v) => {
        val propURI = categoryAssignmentProperties(k)
        val property = factory.getOWLObjectProperty(IRI.create(propURI))

        factory.getOWLObjectPropertyAssertionAxiom(property, requestIndividual, v)
      }
    }).toList

    categoryPropertyAxioms :+ requestAxiom
  }

}
