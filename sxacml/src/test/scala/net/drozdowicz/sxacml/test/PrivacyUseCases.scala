package net.drozdowicz.sxacml.test

import java.io.File

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
class PrivacyUseCases extends path.FunSpec with Matchers with OneInstancePerTest {

  describe("SXACML personal privacy use cases") {

    val policyLocation = relativeToAbsolute("privacy/policies/src-gen")

    describe("law enforcement accessing a location close to crime location") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("privacy/ontologies"), "http://drozdowicz.net/onto/privacy-mapping")
      val request = readFile("/privacy/requests/law_enforcement_permit_request.xml")
      val actualResponse = pdp.evaluate(request)

      it("is permitted") {
        val expectedResponse = readFile("basic/responses/Permit.xml")
        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }
    }

    describe("health center accessing an aggregate distance metric") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("privacy/ontologies"), "http://drozdowicz.net/onto/privacy-mapping")
      val request = readFile("/privacy/requests/health_center_permit_request.xml")
      val actualResponse = pdp.evaluate(request)

      it("is permitted") {
        val expectedResponse = readFile("basic/responses/Permit.xml")
        assertThat(actualResponse, isSimilarTo(expectedResponse).ignoreWhitespace())
      }
    }

    describe("health center accessing an non-aggregated distance metric") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("privacy/ontologies"), "http://drozdowicz.net/onto/privacy-mapping")
      val request = readFile("/privacy/requests/health_center_deny_request.xml")
      val actualResponse = pdp.evaluate(request)

      it("is denied") {
        val expectedResponse = readFile("basic/responses/Deny.xml")
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
