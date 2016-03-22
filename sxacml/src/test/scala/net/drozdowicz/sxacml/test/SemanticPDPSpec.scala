package net.drozdowicz.sxacml.test

import java.io.File

import org.apache.commons.io.FileUtils
import org.junit.Assert.assertThat
import org.scalatest.{OneInstancePerTest, Matchers, path}
import org.xmlunit.diff.{ByNameAndTextRecSelector, ElementSelectors, DefaultNodeMatcher}
import org.xmlunit.matchers.CompareMatcher.isSimilarTo

import collection.JavaConversions._
import scala.net.drozdowicz.sxacml.SemanticPDP

/**
 * Created by michal on 2015-03-13.
 */
class SemanticPDPSpec extends path.FunSpec with Matchers with OneInstancePerTest {

  describe("SemanticPDP") {

    val policyLocation = relativeToAbsolute("basic/policies")

    describe("for simple datatype property from ontology") {
      val pdp = new SemanticPDP(policyLocation, "/ontologies", "http://drozdowicz.net/sxacml/test1")

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
    }

    describe("for multiple requests in a single document"){
      val pdp = new SemanticPDP(policyLocation, "/ontologies", "http://drozdowicz.net/sxacml/testMultiRequests")

      it("returns multiple decisions in single response, with data from ontology") {
        val request = readFile("basic/requests/MultiRequest.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/MultiResponse.xml").replace("\r\n","" )

        assertThat(actualResponse, isSimilarTo(expectedResponse)
          .ignoreWhitespace()
          .withNodeMatcher(new DefaultNodeMatcher(
            ElementSelectors.conditionalBuilder()
             .whenElementIsNamed("Result")
             .thenUse(ElementSelectors.byXPath("./xacml:Decision",
               Map("xacml"->"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"),
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

    describe("when using resource finder"){
      val pdp = new SemanticPDP(policyLocation, "/ontologies", "http://drozdowicz.net/sxacml/testResourceHierarchy")

      it("retrieves individuals from class specified in request") {
        val request = readFile("basic/requests/ResourceClass.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/ResourceClass.xml")

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
      File.separator + "sxacml" +
      File.separator + "src" +
      File.separator + "test" +
      File.separator + "resources" +
      File.separator + relativePath.replace("/", File.separator)
  }
}
