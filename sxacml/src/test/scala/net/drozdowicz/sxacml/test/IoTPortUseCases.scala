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
class IoTPortUseCases extends path.FunSpec with Matchers with OneInstancePerTest {



  describe("SXACML port IoT use cases") {

    val policyLocation = relativeToAbsolute("policies/src-gen")

    describe("observed driver of expected transport accessing internal parking") {
      val pdp = new SemanticPDP(policyLocation, relativeToAbsolute("ontologies"), "http://drozdowicz.net/onto/port_mapping",
        Map(
          (new URI("http://purl.org/dc/elements/1.1/"), relativeToAbsoluteURI("ontologies/dc.ttl")),
          (new URI("http://ontology.tno.nl/transport"), relativeToAbsoluteURI("ontologies/transport.ttl")),
          (new URI("http://www.w3.org/ns/ssn/"), relativeToAbsoluteURI("ontologies/ssn.ttl")),
          (new URI("http://www.w3.org/ns/sosa/"), relativeToAbsoluteURI("ontologies/sosa.ttl")),
          (new URI("http://ontology.tno.nl/logiserv"), relativeToAbsoluteURI("ontologies/logiserv.ttl")),
          (new URI("http://ontology.tno.nl/logico"), relativeToAbsoluteURI("ontologies/logico.ttl"))
        ))
      val request = readFile("requests/driver_permit_request.xml")
      val actualResponse = pdp.evaluate(request)

      it("is permitted") {
        val expectedResponse = readFile("responses/Permit.xml")
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
      File.separator + "port-iot" +
      File.separator + relativePath.replace("/", File.separator)
  }

  private def relativeToAbsoluteURI(relativePath: String): URI = {
    new URI("file:///" + relativeToAbsolute(relativePath).replace(File.separator, "/"))
  }
}
