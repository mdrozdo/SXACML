package net.drozdowicz.sxacml.test

import java.io.File
import java.net.URI

import com.hp.hpl.jena.query.QuerySolution
import net.drozdowicz.sxacml.{OntologyAttributeFinder, FlatAttributeValue, RequestOntologyGenerator}
import onto.sparql.{SingleValueResultProcessor, SparqlReader, ValueSetResultProcessor}
import onto.utils.OntologyUtils
import org.scalatest.{Matchers, path}
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{OWLOntologyManager, IRI, OWLOntology, OWLOntologyIRIMapper}
import org.semanticweb.owlapi.util.SimpleIRIMapper

import scala.collection.JavaConversions._

/**
 * Created by michal on 2015-03-13.
 */
class OntologyAttributeFinderSpec extends path.FunSpec with Matchers {

  describe("OntologyAttributeFinder") {
    val ontoMgr = OWLManager.createOWLOntologyManager()

    val convertToOntology = RequestOntologyGenerator.convertToOntology(ontoMgr) _

    describe("for a request with data property") {

      val input = Set(
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          new URI("urn:oasis:names:tc:xacml:2.0:conformance-test:age"),
          new URI("http://www.w3.org/2001/XMLSchema#integer"),
          "45"
        )
      )

      val toImport: IRI = importOntology(ontoMgr, "test1")

      val ontology = convertToOntology("123", input, Set(toImport))

      it("should find value of derived attribute ") {
        val values = OntologyAttributeFinder.findAttributeValues(ontology, "123", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject", "http://drozdowicz.net/sxacml/test1#isAdult")

        values should contain only (FlatAttributeValue(URI.create("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          URI.create("http://drozdowicz.net/sxacml/test1#isAdult"),
          URI.create("http://www.w3.org/2001/XMLSchema#boolean"),
          "true"))
      }

    }

    describe("getSupportedAttributes"){
      val ontology = OntologyUtils.loadOntology(getOntologyResourceIRI("test1"))

      it("should return data properties from ontology"){
        val attributes = OntologyAttributeFinder.getAllSupportedAttributes(ontology)
        attributes should contain ( "http://drozdowicz.net/sxacml/test1#isAdult")
      }
    }

    //TODO Check if multiple values of attribute can be inferred
//
//    describe("if multiple values can be inferred") {
//
//      val input = Set(
//        FlatAttributeValue(
//          new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
//          new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
//          new URI("http://www.w3.org/2001/XMLSchema#anyURI"),
//          "http://medico.com/record/patient/BartSimpson"
//        )
//      )
//
//      val toImport: IRI = importOntology(ontoMgr, "test1")
//
//      val ontology = convertToOntology("123", input, Set(toImport))
//
//      it("should return all inferred values") {
//        val values = OntologyAttributeFinder.findAttributeValues(ontology, "123",
//          "urn:oasis:names:tc:xacml:3.0:attribute-category:resource", "http://drozdowicz.net/sxacml/test1#isAdult")
//
//        values.size should be(2)
//        values should contain only ("false") //TODO better return flat attribute value?
//      }
//
//    }

  }

  def importOntology(ontoMgr: OWLOntologyManager, ontoName: String): IRI = {
    val toImport = IRI.create("http://drozdowicz.net/sxacml/" + ontoName)
    ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
      new SimpleIRIMapper(toImport, getOntologyResourceIRI(ontoName))))
    toImport
  }

  def getOntologyResourceIRI(ontoName: String): IRI = {
    IRI.create(new File(getClass.getResource("/" + ontoName + ".owl").toURI))
  }
}
