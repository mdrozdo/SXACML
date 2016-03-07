package net.drozdowicz.sxacml.test

import java.io.File
import java.net.URI

import com.hp.hpl.jena.query.QuerySolution
import net.drozdowicz.sxacml.{FlatAttributeValue, RequestOntologyGenerator}
import org.scalatest.{OneInstancePerTest, path, FunSpec, Matchers}
import onto.sparql.{SingleValueResultProcessor, ValueSetResultProcessor, SparqlReader}
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{OWLOntologyIRIMapper, IRI, OWLOntology}
import org.semanticweb.owlapi.util.SimpleIRIMapper
import scala.collection.JavaConversions._

import scala.io.Source

/**
 * Created by michal on 2015-03-13.
 */
class RequestOntologyGeneratorSpec extends path.FunSpec with Matchers with OneInstancePerTest {

  describe("RequestOntologyGenerator") {
    def process(f : QuerySolution => Unit) = {
      new ValueSetResultProcessor {
        override def processSolution(sol: QuerySolution): Unit = {
          f(sol)
        }
      }
    }

    def getSingleSparqlResult(ontology : OWLOntology, query: String):Option[QuerySolution] = {
      val sparqlReader = new SparqlReader(ontology)
      var result:Option[QuerySolution] = None
      sparqlReader.executeQuery(query, new SingleValueResultProcessor {
        override def processSolution(sol: QuerySolution): Unit = {
          result = Some(sol)
        }
      })
      result
    }

    val ontoMgr = OWLManager.createOWLOntologyManager()
    val convertToOntology = RequestOntologyGenerator.convertToOntology(ontoMgr)_

    describe("for a simple request") {

      val input = Set(FlatAttributeValue(
        new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:action"),
        new URI("urn:oasis:names:tc:xacml:1.0:action:action-id"),
        new URI("http://www.w3.org/2001/XMLSchema#string"),
        "buy"
      ))

      val ontology = convertToOntology("123", input, Set.empty[IRI])

      it("should create ontology including request id in ontology id") {
        ontology.getOntologyID.getOntologyIRI.toString should include("123")
      }

      it("should create an individual and property value") {
        val qry = """SELECT ?val WHERE
                    |{
                    |	?cat <urn:oasis:names:tc:xacml:1.0:action:action-id> ?val
                    |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
        result.get.getLiteral("val").getDatatypeURI should equal("http://www.w3.org/2001/XMLSchema#string")
        result.get.getLiteral("val").getString should equal("buy")
      }

      it("should make the individual be of appropriate type") {
        val qry = """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                    |SELECT ?cat WHERE
                    |{
                    |	?cat rdf:type <urn:oasis:names:tc:xacml:3.0:attribute-category:action>
                    |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
      }


    }

    describe("for a category with multiple attribute values"){
      val input = Set(
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"),
          new URI("http://www.w3.org/2001/XMLSchema#string"),
          "Julius Hibbert"
        ),
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          new URI("urn:oasis:names:tc:xacml:2.0:conformance-test:age"),
          new URI("http://www.w3.org/2001/XMLSchema#integer"),
          "45"
        ),
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
          new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
          new URI("http://www.w3.org/2001/XMLSchema#anyURI"),
          "http://medico.com/record/patient/BartSimpson"
        )
      )

      val ontology = convertToOntology("123", input, Set.empty[IRI])

      it("should output first property"){
        val qry = """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                    |SELECT ?val WHERE
                    |{
                    |	?cat rdf:type <urn:oasis:names:tc:xacml:1.0:subject-category:access-subject>.
                    | ?cat <urn:oasis:names:tc:xacml:1.0:subject:subject-id> ?val
                    |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
        result.get.getLiteral("val").getDatatypeURI should equal("http://www.w3.org/2001/XMLSchema#string")
        result.get.getLiteral("val").getString should equal("Julius Hibbert")
      }

      it("should output second property"){
        val qry = """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                    |SELECT ?val WHERE
                    |{
                    |	?cat rdf:type <urn:oasis:names:tc:xacml:1.0:subject-category:access-subject>.
                    | ?cat <urn:oasis:names:tc:xacml:2.0:conformance-test:age> ?val
                    |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
        result.get.getLiteral("val").getDatatypeURI should equal("http://www.w3.org/2001/XMLSchema#integer")
        result.get.getLiteral("val").getString should equal("45")
      }

      it("should output property from other category"){
        val qry = """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                    |SELECT ?val WHERE
                    |{
                    |	?cat rdf:type <urn:oasis:names:tc:xacml:3.0:attribute-category:resource>.
                    | ?cat <urn:oasis:names:tc:xacml:1.0:resource:resource-id> ?val
                    |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
        result.get.getLiteral("val").getDatatypeURI should equal("http://www.w3.org/2001/XMLSchema#anyURI")
        result.get.getLiteral("val").getString should equal("http://medico.com/record/patient/BartSimpson")
      }

      it("should import additional ontologies"){
        val toImport = IRI.create("http://drozdowicz.net/sxacml/test1")
        ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
          new SimpleIRIMapper(toImport, IRI.create(new File(getClass.getResource("/ontologies/test1.owl").toURI)))))

        val ontology = convertToOntology("456", input, Set(toImport))
        ontology.getDirectImports.size() should be (1)
        ontology.getDirectImports.head.getOntologyID.getOntologyIRI.get() should equal (toImport)
      }
    }


    describe("for a subject with anyURI id"){
      val input = Set(
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"),
          new URI("http://www.w3.org/2001/XMLSchema#anyURI"),
          "http://dbpedia.org/page/Bart_Simpson"
        )
      )

      val ontology = convertToOntology("123", input, Set.empty[IRI])

      it("should output subject with matching URI"){
        val qry = """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                    |SELECT ?uri WHERE
                    |{
                    |	?uri rdf:type <urn:oasis:names:tc:xacml:1.0:subject-category:access-subject>
                    |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
        result.get.getResource("uri").getURI should equal("http://dbpedia.org/page/Bart_Simpson")
      }
    }
  }
}
