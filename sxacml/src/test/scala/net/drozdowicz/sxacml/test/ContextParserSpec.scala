package net.drozdowicz.sxacml.test

import java.net.URI

import net.drozdowicz.sxacml.{FlatAttributeValue, ContextParser}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.wso2.balana.ctx.RequestCtxFactory


/**
 * Created by michal on 2015-03-13.
 */
class ContextParserSpec extends FunSpec with Matchers {

  describe("ContextParser") {

    it("should parse simple attribute value") {
      val reqStr = """<Request xmlns="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17" CombinedDecision="false" ReturnPolicyIdList="false">
                     |<Attributes Category="urn:oasis:names:tc:xacml:3.0:attribute-category:action">
                     |<Attribute AttributeId="urn:oasis:names:tc:xacml:1.0:action:action-id" IncludeInResult="false">
                     |<AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">buy</AttributeValue>
                     |</Attribute>
                     |</Attributes>
                     |</Request>""".stripMargin

      val reqCtx = RequestCtxFactory.getFactory.getRequestCtx(reqStr)
      val attributeValues = ContextParser.Parse(reqCtx)

      attributeValues should contain only FlatAttributeValue(
        new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:action"),
        new URI("urn:oasis:names:tc:xacml:1.0:action:action-id"),
        new URI("http://www.w3.org/2001/XMLSchema#string"),
        "buy"
      )
    }

    it("should parse list of simple attribute values") {
      val reqStr = """<?xml version="1.0" encoding="utf-8"?>
                     |<Request xsi:schemaLocation="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17 http://docs.oasis-open.org/xacml/3.0/xacml-core-v3-schema-wd-17.xsd" ReturnPolicyIdList="false" CombinedDecision="false" xmlns="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                     |  <Attributes Category="urn:oasis:names:tc:xacml:1.0:subject-category:access-subject">
                     |    <Attribute IncludeInResult="false" AttributeId="urn:oasis:names:tc:xacml:1.0:subject:subject-id">
                     |      <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">Julius Hibbert</AttributeValue>
                     |    </Attribute>
                     |    <Attribute IncludeInResult="false" AttributeId="urn:oasis:names:tc:xacml:2.0:conformance-test:age">
                     |      <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#integer">45</AttributeValue>
                     |    </Attribute>
                     |  </Attributes>
                     |</Request>""".stripMargin

      val reqCtx = RequestCtxFactory.getFactory.getRequestCtx(reqStr)
      val attributeValues = ContextParser.Parse(reqCtx)

      attributeValues should contain only(
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
        )
      )
    }

    it("should parse list of multiple categories and values") {
      val reqStr = """<?xml version="1.0" encoding="utf-8"?>
                     |<Request xsi:schemaLocation="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17 http://docs.oasis-open.org/xacml/3.0/xacml-core-v3-schema-wd-17.xsd" ReturnPolicyIdList="false" CombinedDecision="false" xmlns="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                     |  <Attributes Category="urn:oasis:names:tc:xacml:1.0:subject-category:access-subject">
                     |    <Attribute IncludeInResult="false" AttributeId="urn:oasis:names:tc:xacml:1.0:subject:subject-id">
                     |      <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">Julius Hibbert</AttributeValue>
                     |    </Attribute>
                     |    <Attribute IncludeInResult="false" AttributeId="urn:oasis:names:tc:xacml:2.0:conformance-test:age">
                     |      <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#integer">45</AttributeValue>
                     |    </Attribute>
                     |  </Attributes>
                     |  <Attributes Category="urn:oasis:names:tc:xacml:3.0:attribute-category:resource">
                     |    <Attribute IncludeInResult="false" AttributeId="urn:oasis:names:tc:xacml:1.0:resource:resource-id">
                     |      <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#anyURI">http://medico.com/record/patient/BartSimpson</AttributeValue>
                     |    </Attribute>
                     |  </Attributes>
                     |  <Attributes Category="urn:oasis:names:tc:xacml:3.0:attribute-category:action">
                     |    <Attribute IncludeInResult="false" AttributeId="urn:oasis:names:tc:xacml:1.0:action:action-id">
                     |      <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">read</AttributeValue>
                     |    </Attribute>
                     |  </Attributes>
                     |  <Attributes Category="urn:oasis:names:tc:xacml:3.0:attribute-category:environment">
                     |    <Attribute IncludeInResult="false" AttributeId="urn:oasis:names:tc:xacml:2.0:conformance-test:bart-simpson-age">
                     |      <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#integer">10</AttributeValue>
                     |    </Attribute>
                     |  </Attributes>
                     |</Request>""".stripMargin

      val reqCtx = RequestCtxFactory.getFactory.getRequestCtx(reqStr)
      val attributeValues = ContextParser.Parse(reqCtx)

      attributeValues should contain only(
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
        ),
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:action"),
          new URI("urn:oasis:names:tc:xacml:1.0:action:action-id"),
          new URI("http://www.w3.org/2001/XMLSchema#string"),
          "read"
        ),
        FlatAttributeValue(
          new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:environment"),
          new URI("urn:oasis:names:tc:xacml:2.0:conformance-test:bart-simpson-age"),
          new URI("http://www.w3.org/2001/XMLSchema#integer"),
          "10"
        )
      )
    }
  }
}
