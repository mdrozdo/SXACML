package scala.net.drozdowicz.sxacml

import org.wso2.balana.attr.AttributeValue
import org.wso2.balana.attr.BooleanAttribute
import org.wso2.balana.attr.StringAttribute
import org.wso2.balana.cond.Evaluatable
import org.wso2.balana.cond.EvaluationResult
import org.wso2.balana.cond.FunctionBase
import org.wso2.balana.ctx.EvaluationCtx
import java.util

/**
  * Created by drozd on 22.03.2020.
  */
object BoolTextCompare { // the name of the function, which will be used publicly
  val NAME = "bool-text-compare"
  // the parameter types, in order, and whether or not they're bags
  private val params = Array(StringAttribute.identifier, BooleanAttribute.identifier)
  private val bagParams = Array(false, false)

  def getSupportedIdentifiers: util.Set[String] = {
    val set = new util.HashSet[String]
    set.add(NAME)
    set
  }
}

class BoolTextCompare() // use the constructor that handles mixed argument types
  extends FunctionBase(BoolTextCompare.NAME, 0, BoolTextCompare.params, BoolTextCompare.bagParams, BooleanAttribute.identifier, false) {
  override def evaluate(inputs: util.List[Evaluatable], context: EvaluationCtx): EvaluationResult = { // Evaluate the arguments using the helper method...this will
    // catch any errors, and return values that can be compared
    val argValues = new Array[AttributeValue](inputs.size)
    val result = evalArgs(inputs, context, argValues)
    if (result != null) return result
    // cast the resolved values into specific types
    val str = argValues(0).asInstanceOf[StringAttribute]
    val bool = argValues(1).asInstanceOf[BooleanAttribute]
    var evalResult = false
    // now compare the values
    if (bool.getValue) { // see if the string is "true"
      evalResult = str.getValue == "true"
    }
    else { // see if the string is "false"
      evalResult = str.getValue == "false"
    }
    // boolean returns are common, so there's a getInstance() for that
    EvaluationResult.getInstance(evalResult)
  }
}