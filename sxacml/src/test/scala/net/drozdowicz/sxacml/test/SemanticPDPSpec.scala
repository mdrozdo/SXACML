package net.drozdowicz.sxacml.test

import java.io.File

import org.apache.commons.io.FileUtils
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.scalatest.{OneInstancePerTest, Matchers, path}
import org.xmlunit.diff.{ByNameAndTextRecSelector, ElementSelectors, DefaultNodeMatcher}
import org.xmlunit.matchers.CompareMatcher.isSimilarTo

import collection.JavaConversions._
import scala.net.drozdowicz.sxacml.SemanticPDP

/**
  * Created by michal on 2015-03-13.
  */
class SemanticPDPSpec extends path.FunSpec with Matchers with OneInstancePerTest {

  describe("SemanticPDP custom function") {

    val policyLocation = relativeToAbsolute("basic/policies_function")

    describe("for boolean text compare (sample) function") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ontologies"), "https://w3id.org/sxacml/test1")

      it("returns permit") {
        val request = readFile("basic/requests/Adult.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/Permit.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }
    }
  }

  describe("GeoXACML function") {

    val policyLocation = relativeToAbsolute("basic/policies_geoxacml")

    describe("if subject is close to Warsaw") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ontologies"), "https://w3id.org/sxacml/test1")

      it("returns permit") {
        val request = readFile("basic/requests/CloseToWarsaw.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/Permit.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }
    }

    describe("if subject is far from Warsaw") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ontologies"), "https://w3id.org/sxacml/test1")

      it("returns deny") {
        val request = readFile("basic/requests/FarFromWarsaw.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/Deny.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }
    }
  }

  describe("SemanticPDP") {

    val policyLocation = relativeToAbsolute("basic/policies")

    describe("for simple datatype property from ontology") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ontologies"), "https://w3id.org/sxacml/test1")

      it("if subject is adult returns permit") {
        val request = readFile("basic/requests/Adult.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/Permit.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }

      it("if subject is not null returns deny") {
        val request = readFile("basic/requests/NonAdult.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/Deny.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }

      it("should parse a sample request") {
        val request = readFile("basic/requests/SampleRequest.xml")
        val actualResponse = pdp.evaluate(request)

        //no assert, just playing around
        assertTrue(true)
      }

      it("should parse a sample request with content") {
        val request = readFile("basic/requests/SampleRequestWithContent.xml")
        val actualResponse = pdp.evaluate(request)

        //no assert, just playing around
        assertTrue(true)
      }
    }


    describe("for multiple requests in a single document") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ontologies"), "https://w3id.org/sxacml/testMultiRequests")

      it("returns multiple decisions in single response, with data from ontology") {
        val request = readFile("basic/requests/MultiRequest.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/MultiResponse.xml").replace("\r\n", "")

        assertThat(actualResponse, isSimilarTo(expectedResponse)
          .ignoreWhitespace()
          .withNodeMatcher(new DefaultNodeMatcher(
            ElementSelectors.conditionalBuilder()
              .whenElementIsNamed("Result")
              .thenUse(ElementSelectors.byXPath("./xacml:Decision",
                Map("xacml" -> "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"),
                ElementSelectors.byNameAndText))
              .whenElementIsNamed("Attributes")
              .thenUse(ElementSelectors.byNameAndAllAttributes)
              .whenElementIsNamed("Attribute")
              .thenUse(ElementSelectors.byNameAndAllAttributes)
              .elseUse(ElementSelectors.byName)
              .build())
          )
        )
      }
    }

    describe("when using resource finder") {

      it("retrieves individuals from class specified in request") {
        val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ontologies"), "https://w3id.org/sxacml/testResourceHierarchy")

        val request = readFile("basic/requests/ResourceClass.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/ResourceClass.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse)
          .ignoreWhitespace()
          .withNodeMatcher(new DefaultNodeMatcher(
            ElementSelectors.conditionalBuilder()
              .whenElementIsNamed("Result")
              .thenUse(ElementSelectors.byXPath("./xacml:Attributes/xacml:Attribute[@AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\"]/xacml:AttributeValue",
                Map("xacml" -> "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"),
                ElementSelectors.byNameAndText))
              .whenElementIsNamed("Attributes")
              .thenUse(ElementSelectors.byNameAndAllAttributes)
              .whenElementIsNamed("Attribute")
              .thenUse(ElementSelectors.byNameAndAllAttributes)
              .elseUse(ElementSelectors.byName)
              .build())
          )
        )
      }

      it("retrieves individuals from hierarchy defined by hierarchy designator property") {
        val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ontologies"), "https://w3id.org/sxacml/testResourceHierarchyByProperty")

        val request = readFile("basic/requests/ResourceClassProperty.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/ResourceClassProperty.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse)
          .ignoreWhitespace()
          .withNodeMatcher(new DefaultNodeMatcher(
            ElementSelectors.conditionalBuilder()
              .whenElementIsNamed("Result")
              .thenUse(ElementSelectors.byXPath("./xacml:Attributes/xacml:Attribute[@AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\"]/xacml:AttributeValue",
                Map("xacml" -> "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"),
                ElementSelectors.byNameAndText))
              .whenElementIsNamed("Attributes")
              .thenUse(ElementSelectors.byNameAndAllAttributes)
              .whenElementIsNamed("Attribute")
              .thenUse(ElementSelectors.byNameAndAllAttributes)
              .elseUse(ElementSelectors.byName)
              .build())
          )
        )
      }
    }

    describe("port ontology sample") {
      val pdp = new SemanticPDP(relativeToAbsolute("port2/policies"),
        relativeToAbsolute("ontologies"),
        "http://www.semanticweb.org/rafal/ontologies/2017/6/port2")

      it("driver should be permitted") {
        val request = readFile("port2/requests/Driver.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("port2/responses/Permit.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }
    }

    describe("port ontology sparql path sample") {
      val pdp = new SemanticPDP(relativeToAbsolute("port2/policies_sparql"),
        relativeToAbsolute("ontologies"),
        "http://www.semanticweb.org/rafal/ontologies/2017/6/port2")

      it("driver should be permitted") {
        val request = readFile("port2/requests/Driver.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("port2/responses/Permit.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }
    }

    describe("port ontology sparql sample") {
      val pdp = new SemanticPDP(relativeToAbsolute("port2/policies_sparql_full"),
        relativeToAbsolute("ontologies"),
        "http://www.semanticweb.org/rafal/ontologies/2017/6/port2")

      it("driver should be permitted") {
        val request = readFile("port2/requests/Driver.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("port2/responses/Permit.xml")

        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }
    }
  }

  private def readFile(relativeFilePath: String): String = {
    val absoluteFilePath = relativeToAbsolute(relativeFilePath)
    FileUtils.readFileToString(new File(absoluteFilePath))
  }

  private def relativeToAbsolute(relativePath: String): String = {
    (new File(".")).getCanonicalPath +
      File.separator + "src" +
      File.separator + "test" +
      File.separator + "resources" +
      File.separator + relativePath.replace("/", File.separator)
  }
}
