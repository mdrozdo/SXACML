package net.drozdowicz.sxacml.test

import java.io.File
import java.net.URI

import org.apache.commons.io.FileUtils
import org.junit.Assert.assertThat
import org.scalatest.{Matchers, OneInstancePerTest, path}
import org.xmlunit.matchers.CompareMatcher.isSimilarTo

import scala.net.drozdowicz.sxacml.SemanticPDP

/**
  * Created by michal on 2015-03-13.
  */
class EhealthUseCases extends path.FunSpec with Matchers with OneInstancePerTest {

  describe("SXACML ehealth use cases") {

    val policyLocation = relativeToAbsolute("ehealth/policies/src-gen")

    describe("physician reading blood pressure") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ehealth/ontologies"), "https://w3id.org/sxacml/sample-ehealth/ehealth-mapping",
        Map(
          (new URI("http://hl7.org/ontology/ObjectOntology.owl/1.0.0"), relativeToAbsoluteURI("ehealth/ontologies/ObjectOntology.owl")),
          (new URI("http://hl7.org/ontology/RoleOntology.owl/1.0.0"), relativeToAbsoluteURI("ehealth/ontologies/RoleOntology.owl"))
        )
      )
      val request = readFile("/ehealth/requests/request_permit.xml")
      val actualResponse = pdp.evaluate(request)

      it("is permitted") {
        val expectedResponse = readFile("basic/responses/Permit.xml")
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

  private def relativeToAbsoluteURI(relativePath: String): URI = {
    new URI("file:///" + relativeToAbsolute(relativePath).replace(File.separator, "/"))
  }
}
