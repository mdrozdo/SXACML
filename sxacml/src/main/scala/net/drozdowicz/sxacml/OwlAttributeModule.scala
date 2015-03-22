package net.drozdowicz.sxacml

import java.net.URI

import org.wso2.balana.cond.EvaluationResult
import org.wso2.balana.ctx.{Status, EvaluationCtx}
import org.wso2.balana.finder.AttributeFinderModule
import org.wso2.balana.xacml3.Attributes
import collection.JavaConversions._
import collection.mutable.Set

class OwlAttributeModule extends AttributeFinderModule {
  override def isDesignatorSupported() = true

  override def getSupportedCategories(): java.util.Set[String] = {
    Set("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject")
  }

  /*
   DONE Convert the attributes from the EvaluationCtx to an ontology
    DONE Extract the attribute values from the EvaluationCtx
    DONE Create ontology values from attribute values - category should become an individual, attribute id should become property, value should become a literal
      DONE RequestOntologyGenerator.convertToOntology(Set[FlatAttributeValue])

   TODO Make the context ontology import the required other ontologies
   TODO Run the reasoner
   TODO Run a SPARQL query for the value of the attribute to find
   TODO Convert the SPARQL result to the EvaluationResult
   TODO Should there be links added between diff categories? Like "subject1" requests "resourceA"?? Later
   */
  override def findAttribute(attributeType: URI, attributeId: URI, issuer: String, category: URI, context: EvaluationCtx): EvaluationResult = {
    val requestAttributes:Set[Attributes] = context.getRequestCtx.getAttributesSet

    requestAttributes.foreach(attributes => {
      attributes.getAttributes.foreach(attribute => {
        val attrId = attribute.getId

      })
    })

    new EvaluationResult(new Status(Seq(Status.STATUS_MISSING_ATTRIBUTE)))
  }
}