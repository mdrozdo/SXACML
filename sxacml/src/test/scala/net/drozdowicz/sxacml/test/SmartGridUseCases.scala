package net.drozdowicz.sxacml.test

import java.io.File
import java.net.URI

import org.apache.commons.io.FileUtils
import org.junit.Assert.assertThat
import org.scalatest.{Matchers, OneInstancePerTest, path}
import org.xmlunit.diff.{DefaultNodeMatcher, ElementSelectors}
import org.xmlunit.matchers.CompareMatcher.isSimilarTo

import scala.collection.JavaConversions._
import scala.net.drozdowicz.sxacml.SemanticPDP

/**
  * Created by michal on 2015-03-13.
  */
class SmartGridUseCases extends path.FunSpec with Matchers with OneInstancePerTest {

  describe("SXACML smart grid use cases") {

    val policyLocation = relativeToAbsolute("smartgrid/policies/src-gen")

    describe("updating temperature in HVAC devices") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("smartgrid/ontologies"), "https://w3id.org/sxacml/sample-smartgrid/smartgrid-mapping",
        Map(
          (new URI("http://purl.org/dc/elements/1.1/"), relativeToAbsoluteURI("smartgrid/ontologies/dc.ttl")))
      )
      val request = readFile("smartgrid/requests/temperature_update_multi.xml")
      val actualResponse = pdp.evaluate(request)

      it("is permitted for a specific individual") {
        val expectedResponse = readFile("smartgrid/responses/temperature_update_multi_response.xml")
        assertMultipleResultEquals(actualResponse, expectedResponse);
      }
    }
  }

  private def assertMultipleResultEquals(actualResponse: String, expectedResponse: String): Unit ={
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


  private def relativeToAbsoluteURI(relativePath: String): URI = {
    new URI("file:///" + relativeToAbsolute(relativePath).replace(File.separator, "/"))
  }
}
