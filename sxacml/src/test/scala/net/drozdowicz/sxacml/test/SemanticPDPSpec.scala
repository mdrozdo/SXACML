package net.drozdowicz.sxacml.test

import java.io.File

import org.apache.commons.io.FileUtils
import org.custommonkey.xmlunit.{XMLAssert, XMLUnit}
import org.scalatest.{Matchers, path}

import scala.net.drozdowicz.sxacml.SemanticPDP

/**
 * Created by michal on 2015-03-13.
 */
class SemanticPDPSpec extends path.FunSpec with Matchers {

  describe("SemanticPDP") {
    XMLUnit.setIgnoreWhitespace(true)
    describe("for simple datatype property from ontology") {
      val policyLocation = relativeToAbsolute("basic/policies")
      val pdp = new SemanticPDP(policyLocation)

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
