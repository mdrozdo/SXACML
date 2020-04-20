package net.drozdowicz.sxacml.test

import java.io.File
import java.net.URI

import net.drozdowicz.sxacml.{FlatAttributeValue, OntologyAttributeFinder, RequestOntologyGenerator}
import onto.utils.OntologyUtils
import org.scalatest.{Matchers, path}
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntologyIRIMapper, OWLOntologyManager}
import org.semanticweb.owlapi.util.{AutoIRIMapper, SimpleIRIMapper}

import scala.collection.JavaConversions._

/**
  * Created by michal on 2015-03-13.
  */
class OntologyAttributeFinderSpec extends path.FunSpec with Matchers {

  describe("OntologyAttributeFinder") {
    val ontoMgr = OWLManager.createOWLOntologyManager()
    ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
      new AutoIRIMapper(new File("./sxacml/src/test/resources/ontologies/"), true))
    )

    val convertToOntology = RequestOntologyGenerator.convertToOntology(ontoMgr) _

    describe("for a request with data property") {

      val input = Seq(
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          new URI("urn:oasis:names:tc:xacml:2.0:conformance-test:age"),
          new URI("http://www.w3.org/2001/XMLSchema#integer"),
          "45"
        )
      )

      val toImport: IRI = getOntologyIRI(ontoMgr, "test1")

      val ontology = convertToOntology("123", input, Set(toImport))

      it("should find value of derived attribute ") {
        val values = OntologyAttributeFinder.findAttributeValues(ontology, "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject#request_123",
          "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject", "http://drozdowicz.net/sxacml/test1#isAdult")

        values should contain only (FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          URI.create("http://drozdowicz.net/sxacml/test1#isAdult"),
          URI.create("http://www.w3.org/2001/XMLSchema#boolean"),
          "true"))
      }

      it("should output type of the individual as an attribute value") {
        val values = OntologyAttributeFinder.findAttributeValues(ontology, "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject#request_123",
          "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject", "urn:sxacml:attributes:type")

        values should contain(FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          URI.create("urn:sxacml:attributes:type"),
          URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
          "http://drozdowicz.net/sxacml/test1#Adult"))
      }

    }

    describe("getSupportedAttributes") {
      val ontology = ontoMgr.loadOntology(IRI.create("http://drozdowicz.net/sxacml/test1"))

      it("should return data properties from ontology") {
        val attributes = OntologyAttributeFinder.getAllSupportedAttributes(ontology)
        attributes should contain("http://drozdowicz.net/sxacml/test1#isAdult")
      }

      it("should return type property") {
        val attributes = OntologyAttributeFinder.getAllSupportedAttributes(ontology)
        attributes should contain("urn:sxacml:attributes:type")
      }
    }

    describe("findAttributeValues") {
      describe("if multiple values can be inferred") {

        val input = Seq(
          FlatAttributeValue(
            new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
            new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"),
            new URI("http://www.w3.org/2001/XMLSchema#anyURI"),
            "http://dbpedia.org/page/Bart_Simpson"
          )
        )

        val toImport: IRI = getOntologyIRI(ontoMgr, "testMultiValues")

        val ontology = convertToOntology("456", input, Set(toImport))

        it("should return all inferred values") {
          val values = OntologyAttributeFinder.findAttributeValues(ontology, "http://dbpedia.org/page/Bart_Simpson",
            "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject", "http://drozdowicz.net/sxacml/testMultiValue#hasParentName")

          values.size should be(2)
          values should contain only(
            FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
              URI.create("http://drozdowicz.net/sxacml/testMultiValue#hasParentName"),
              URI.create("http://www.w3.org/2001/XMLSchema#string"),
              "Homer Simpson"),
            FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
              URI.create("http://drozdowicz.net/sxacml/testMultiValue#hasParentName"),
              URI.create("http://www.w3.org/2001/XMLSchema#string"),
              "Marge Simpson"))
        }

      }

      describe("when subject id is specified (and not anyURI)") {

        val input = Seq(
          FlatAttributeValue(
            new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
            new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"),
            new URI("urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name"),
            "bart@simpsons.com"
          )
        )

        val toImport: IRI = getOntologyIRI(ontoMgr, "testIdMatch")

        val ontology = convertToOntology("123", input, Set(toImport))

        it("should match subject by id") {
          val values = OntologyAttributeFinder.findAttributeValues(ontology, "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject#request_123",
            "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject", "http://drozdowicz.net/sxacml/testIdMatch#hasFirstName")

          values.size should be(1)
          values should contain only (
            FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
              URI.create("http://drozdowicz.net/sxacml/testIdMatch#hasFirstName"),
              URI.create("http://www.w3.org/2001/XMLSchema#string"),
              "Bart"))
        }

      }
    }

    describe("findInstancesOfClass") {
      val ontology = ontoMgr.loadOntology(IRI.create("http://drozdowicz.net/sxacml/testResourceHierarchy"))

      it("given class id should return individuals from class defined in attribute value") {
        val values = OntologyAttributeFinder.findInstancesOfClass(ontology,
          "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
          "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
          "http://drozdowicz.net/sxacml/testResourceHierarchy#foo")

        values.size should be(2)
        values should contain only(
          FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
            URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
            "http://drozdowicz.net/sxacml/testResourceHierarchy#foo1"),
          FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
            URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
            "http://drozdowicz.net/sxacml/testResourceHierarchy#foo2"))
      }

      it("given class name should return individuals from class defined in attribute value") {
        val values = OntologyAttributeFinder.findInstancesOfClass(ontology,
          "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
          "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
          "foo")

        values.size should be(2)
        values should contain only(
          FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
            URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
            "http://drozdowicz.net/sxacml/testResourceHierarchy#foo1"),
          FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
            URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
            "http://drozdowicz.net/sxacml/testResourceHierarchy#foo2"))
      }
    }

    describe("findHierarchyDesignator") {
      val ontology = ontoMgr.loadOntology(IRI.create("http://drozdowicz.net/sxacml/testResourceHierarchyByProperty"))

      it("should return id of property marked with hierarchyDesignator annotation") {
        var actual = OntologyAttributeFinder.getHierarchyDesignator(ontology)

        actual should be(Some("http://drozdowicz.net/sxacml/testResourceHierarchyByProperty#hasChild"))
      }
    }

    describe("findInstancesFromHierarchy") {
      val ontology = ontoMgr.loadOntology(IRI.create("http://drozdowicz.net/sxacml/testResourceHierarchyByProperty"))

      it("given individual id should return individuals related by the hierarchyDesignator property") {
        val values = OntologyAttributeFinder.findInstancesFromHierarchy(ontology,
          "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
          "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
          "http://drozdowicz.net/sxacml/testResourceHierarchyByProperty#fooRoot",
          "http://drozdowicz.net/sxacml/testResourceHierarchyByProperty#hasChild")

        values.size should be(2)
        values should contain only(
          FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
            URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
            "http://drozdowicz.net/sxacml/testResourceHierarchyByProperty#foo1"),
          FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
            URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
            "http://drozdowicz.net/sxacml/testResourceHierarchyByProperty#foo2"))
      }



//      it("given class name should return individuals from class defined in attribute value") {
//        val values = OntologyAttributeFinder.findInstancesOfClass(ontology,
//          "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
//          "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
//          "foo")
//
//        values.size should be(2)
//        values should contain only(
//          FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
//            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
//            URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
//            "http://drozdowicz.net/sxacml/testResourceHierarchy#foo1"),
//          FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
//            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
//            URI.create("http://www.w3.org/2001/XMLSchema#anyURI"),
//            "http://drozdowicz.net/sxacml/testResourceHierarchy#foo2"))
//      }
    }

    describe("queryOntologyWithSparql") {
      val ontology = ontoMgr.loadOntology(IRI.create("http://drozdowicz.net/sxacml/test1"))

      it("should return single result") {
        var query = "" +
          "PREFIX test: <http://drozdowicz.net/sxacml/testResourceHierarchy#>" +
          "PREFIX subject: <urn:oasis:names:tc:xacml:1.0:subject:>" +
          "SELECT ?id " +
          "WHERE {" +
          "test:alice subject:id ?id" +
          "}"

        var actual = OntologyAttributeFinder.queryOntologyWithSparql(query, ontology, Map(
          URI.create("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject") -> "fake",
          URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:resource") -> "fake",
          URI.create("urn:oasis:names:tc:xacml:3.0:attribute-category:action") -> "fake"
        ))

        actual should be(Some("alice"))
      }
    }
  }

  def getOntologyIRI(ontoMgr: OWLOntologyManager, ontoName: String): IRI = {
    IRI.create("http://drozdowicz.net/sxacml/" + ontoName)
  }

}
