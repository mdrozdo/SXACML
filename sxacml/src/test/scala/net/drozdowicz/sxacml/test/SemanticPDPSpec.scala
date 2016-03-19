package net.drozdowicz.sxacml.test

import java.io.File

import org.apache.commons.io.FileUtils
import org.custommonkey.xmlunit.{XMLAssert, XMLUnit}
import org.scalatest.{OneInstancePerTest, Matchers, path}

import scala.net.drozdowicz.sxacml.SemanticPDP

/**
 * Created by michal on 2015-03-13.
 */
class SemanticPDPSpec extends path.FunSpec with Matchers with OneInstancePerTest {

  describe("SemanticPDP") {
    XMLUnit.setIgnoreWhitespace(true)
    val policyLocation = relativeToAbsolute("basic/policies")

    describe("for simple datatype property from ontology") {
      val pdp = new SemanticPDP(policyLocation, "/ontologies", "http://drozdowicz.net/sxacml/test1")

      it("if subject is adult returns permit") {
        val request = readFile("basic/requests/Adult.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/Permit.xml")

        XMLAssert.assertXMLEqual(expectedResponse, actualResponse)
      }

      it("if subject is not null returns deny") {
        val request = readFile("basic/requests/NonAdult.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/Deny.xml")

        XMLAssert.assertXMLEqual(expectedResponse, actualResponse)
      }
    }

    describe("for multiple requests in a single document"){
      val pdp = new SemanticPDP(policyLocation, "/ontologies", "http://drozdowicz.net/sxacml/testMultiRequests")

      it("returns multiple decisions in single response, with data from ontology") {
        val request = readFile("basic/requests/MultiRequest.xml")
        val actualResponse = pdp.evaluate(request)
        val expectedResponse = readFile("basic/responses/MultiResponse.xml")

        XMLAssert.assertXMLEqual(expectedResponse, actualResponse)
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
