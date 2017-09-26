package net.drozdowicz.sxacml.test

import java.io.File
import java.net.URI

import org.apache.jena.query.QuerySolution
import net.drozdowicz.sxacml.{FlatAttributeValue, NestedAttributeValue, RequestOntologyGenerator}
import onto.sparql.{SingleValueResultProcessor, SparqlReader, ValueSetResultProcessor}
import org.scalatest.{Matchers, OneInstancePerTest, path}
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntology, OWLOntologyIRIMapper}
import org.semanticweb.owlapi.util.SimpleIRIMapper

import scala.collection.JavaConversions._

/**
  * Created by michal on 2015-03-13.
  */
class RequestOntologyGeneratorSpec extends path.FunSpec with Matchers with OneInstancePerTest {

  describe("RequestOntologyGenerator") {
    def process(f: QuerySolution => Unit) = {
      new ValueSetResultProcessor {
        override def processSolution(sol: QuerySolution): Unit = {
          f(sol)
        }
      }
    }

    def getSingleSparqlResult(ontology: OWLOntology, query: String): Option[QuerySolution] = {
      val sparqlReader = new SparqlReader(ontology)
      var result: Option[QuerySolution] = None
      sparqlReader.executeQuery(query, new SingleValueResultProcessor {
        override def processSolution(sol: QuerySolution): Unit = {
          result = Some(sol)
        }
      })
      result
    }

    def getMultiSparqlResult(ontology: OWLOntology, query: String): Seq[QuerySolution] = {
      val sparqlReader = new SparqlReader(ontology)
      var result: List[QuerySolution] = List()

      sparqlReader.executeQuery(query, new ValueSetResultProcessor {
        override def processSolution(sol: QuerySolution): Unit = {
          result = sol :: result
        }
      })
      result
    }

    val ontoMgr = OWLManager.createOWLOntologyManager()
    val convertToOntology = RequestOntologyGenerator.convertToOntology(ontoMgr) _

    describe("for a simple request") {

      val input = Seq(FlatAttributeValue(
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
        val qry =
          """SELECT ?val WHERE
            |{
            |	?cat <urn:oasis:names:tc:xacml:1.0:action:action-id> ?val
            |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
        result.get.getLiteral("val").getDatatypeURI should equal("http://www.w3.org/2001/XMLSchema#string")
        result.get.getLiteral("val").getString should equal("buy")
      }

      it("should make the individual be of appropriate type") {
        val qry =
          """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            |SELECT ?cat WHERE
            |{
            |	?cat rdf:type <urn:oasis:names:tc:xacml:3.0:attribute-category:action>
            |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
      }


    }

    describe("for a category with multiple attribute values") {
      val input = Seq(
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

      it("should output first property") {
        val qry =
          """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
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

      it("should output second property") {
        val qry =
          """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
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

      it("should output property from other category") {
        val qry =
          """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
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

      it("should import additional ontologies") {
        val toImport = IRI.create("http://drozdowicz.net/sxacml/test1")
        ontoMgr.setIRIMappers(scala.collection.mutable.Set[OWLOntologyIRIMapper](
          new SimpleIRIMapper(IRI.create("http://drozdowicz.net/sxacml/test1"), IRI.create(new File(getClass.getResource("/ontologies/test1.owl").toURI))),
          new SimpleIRIMapper(IRI.create("http://drozdowicz.net/sxacml/request"), IRI.create(new File(getClass.getResource("/ontologies/request.owl").toURI)))
        ))

        val ontology = convertToOntology("456", input, Set(toImport))
        ontology.getDirectImports.size() should be(1)
        ontology.getDirectImports.head.getOntologyID.getOntologyIRI.get() should equal(toImport)
      }
    }


    describe("for a subject with anyURI id") {
      val input = Seq(
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"),
          new URI("http://www.w3.org/2001/XMLSchema#anyURI"),
          "http://dbpedia.org/page/Bart_Simpson"
        )
      )

      val ontology = convertToOntology("123", input, Set.empty[IRI])

      it("should output subject with matching URI") {
        val qry =
          """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            |SELECT ?uri WHERE
            |{
            |	?uri rdf:type <urn:oasis:names:tc:xacml:1.0:subject-category:access-subject>
            |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
        result.get.getResource("uri").getURI should equal("http://dbpedia.org/page/Bart_Simpson")
      }
    }

    describe("for a subject subject-class-id attribute") {
      val input = Seq(
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          new URI("sxacml:subject:subject-class-id"),
          new URI("http://www.w3.org/2001/XMLSchema#anyURI"),
          "http://drozdowicz.net/sxacml/test#SomeSubjectClass"
        )
      )

      val ontology = convertToOntology("123", input, Set.empty[IRI])

      it("should output subject of type matching class-id attribute") {
        val qry =
          """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            |SELECT ?uri WHERE
            |{
            |	?uri rdf:type <http://drozdowicz.net/sxacml/test#SomeSubjectClass>
            |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
      }
    }

    describe("for a subject with non XMLSchema datatype id") {
      val input = Seq(
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
          new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"),
          new URI("urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name"),
          "bart@simpsons.com"
        )
      )

      val ontology = convertToOntology("123", input, Set.empty[IRI])

      it("should output id with string datatype for reasoning compatibility") {
        val qry =
          """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            |SELECT ?val WHERE
            |{
            |	?cat rdf:type <urn:oasis:names:tc:xacml:1.0:subject-category:access-subject>.
            | ?cat <urn:oasis:names:tc:xacml:1.0:subject:subject-id> ?val
            |}""".stripMargin
        val result = getSingleSparqlResult(ontology, qry)

        result should not equal None
        result.get.getLiteral("val").getDatatypeURI should equal("http://www.w3.org/2001/XMLSchema#string")
        result.get.getLiteral("val").getString should equal("bart@simpsons.com")
      }

      describe("for a resource with nested attributes") {
        val input = Seq(
          NestedAttributeValue(
            new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
            None,
            "urn:example:med:schemas:record",
            "patient",
            Seq(
              FlatAttributeValue(
                new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
                new URI("urn:example:med:schemas:record#patientDoB"),
                new URI("http://www.w3.org/2001/XMLSchema#string"),
                "1992-03-21"
              ),
              FlatAttributeValue(
                new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
                new URI(
                  "urn:example:med:schemas:record#patient-number"),
                new URI("http://www.w3.org/2001/XMLSchema#string"),
                "555555"
              ),
              NestedAttributeValue(
                new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
                Some(new URI("urn:example:med:schemas:record#contact")),
                "urn:example:med:schemas:record",
                "patientContact",
                Seq(
                  FlatAttributeValue(
                    new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
                    new URI(
                      "urn:example:med:schemas:record#email"),
                    new URI("http://www.w3.org/2001/XMLSchema#string"),
                    "b.simpson@example.com"
                  )
                )
              )
            )
          )
        )

        val ontology = convertToOntology("111", input, Set.empty[IRI])

        it("should output individual of class id from element uri") {
          val qry =
            """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
              |SELECT ?ind WHERE
              |{
              |	?ind rdf:type <urn:example:med:schemas:record#patient>
              |}""".stripMargin
          val result = getSingleSparqlResult(ontology, qry)

          result should not equal None
          result.get.getResource("ind").getNameSpace should equal("urn:example:med:schemas:record#")
        }

        it("should output relation between category and nested individual") {
          val qry =
            """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
              |SELECT ?prop WHERE
              |{
              |	?cat rdf:type <urn:oasis:names:tc:xacml:3.0:attribute-category:resource>.
              | ?ind rdf:type <urn:example:med:schemas:record#patient>.
              | ?cat ?prop ?ind.
              |
              |}""".stripMargin
          val result = getMultiSparqlResult(ontology, qry)

          result.map(r => r.getResource("prop").getURI.toString) should contain("urn:example:med:schemas:record#hasPatient")
        }

        it("should output property with specified name") {
          val qry =
            """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
              |SELECT ?prop WHERE
              |{
              | ?patient rdf:type <urn:example:med:schemas:record#patient>.
              | ?email rdf:type <urn:example:med:schemas:record#patientContact>.
              | ?patient ?prop ?email.
              |
              |}""".stripMargin
          val result = getMultiSparqlResult(ontology, qry)

          result.map(r => r.getResource("prop").getURI.toString) should contain("urn:example:med:schemas:record#contact")
        }

        it("should output property values of nested individual") {
          val qry =
            """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
              |SELECT ?prop ?val WHERE
              |{
              |	?ind rdf:type <urn:example:med:schemas:record#patient>.
              | ?ind ?prop ?val
              |
              |}""".stripMargin
          val result = getMultiSparqlResult(ontology, qry)

          result.flatMap(r => {
            if (r.get("val").isLiteral) Some((r.getResource("prop").getURI, r.getLiteral("val").getString)) else None
          }) should contain allOf (
            ("urn:example:med:schemas:record#patientDoB", "1992-03-21"),
            ("urn:example:med:schemas:record#patient-number", "555555")
          )
        }
      }
    }
  }
}
