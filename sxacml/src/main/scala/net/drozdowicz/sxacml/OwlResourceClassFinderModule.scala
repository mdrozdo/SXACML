package scala.net.drozdowicz.sxacml

import org.wso2.balana.attr.AttributeValue
import org.wso2.balana.ctx.EvaluationCtx
import org.wso2.balana.finder.{ResourceFinderResult, ResourceFinderModule}

/**
  * Created by michal on 19.03.2016.
  */
class OwlResourceClassFinderModule extends ResourceFinderModule() {

  override def isChildSupported() = false

  override def isDescendantSupported() = true

  override def findChildResources( parentResourceId: AttributeValue, context: EvaluationCtx ) =
    new ResourceFinderResult()

  override def findDescendantResources(parentResourceId: AttributeValue , context: EvaluationCtx ) =
    new ResourceFinderResult()

}
