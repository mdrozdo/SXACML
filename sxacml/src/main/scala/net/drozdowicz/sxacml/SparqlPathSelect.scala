package scala.net.drozdowicz.sxacml

import java.util

import org.wso2.balana.attr.{AttributeValue, BooleanAttribute, StringAttribute}
import org.wso2.balana.cond.{Evaluatable, EvaluationResult, FunctionBase}
import org.wso2.balana.ctx.EvaluationCtx

/**
  * Created by drozd on 22.03.2020.
  */
object SparqlPathSelect { // the name of the function, which will be used publicly
  val NAME = "sparql-path-select"
  // the parameter types, in order, and whether or not they're bags
  private val params = Array(StringAttribute.identifier)
  private val bagParams = Array(false)

  def getSupportedIdentifiers: util.Set[String] = {
    val set = new util.HashSet[String]
    set.add(NAME)
    set
  }
}

class SparqlPathSelect() // use the constructor that handles mixed argument types
  extends FunctionBase(SparqlPathSelect.NAME, 0, SparqlPathSelect.params, SparqlPathSelect.bagParams, StringAttribute.identifier, true) {
  override def evaluate(inputs: util.List[Evaluatable], context: EvaluationCtx): EvaluationResult = { // Evaluate the arguments using the helper method...this will
    // catch any errors, and return values that can be compared
    val argValues = new Array[AttributeValue](inputs.size)
    val result = evalArgs(inputs, context, argValues)
    if (result != null) return result
    // cast the resolved values into specific types
    val str = argValues(0).asInstanceOf[StringAttribute]

    // boolean returns are common, so there's a getInstance() for that
    EvaluationResult.getInstance(true)
  }
}