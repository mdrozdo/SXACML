package scala.net.drozdowicz.sxacml

import java.net.URI
import java.util

import org.apache.commons.logging.LogFactory
import org.wso2.balana.attr._
import org.wso2.balana.cond.{Evaluatable, EvaluationResult, FunctionBase}
import org.wso2.balana.ctx.EvaluationCtx

import scala.collection.JavaConverters._
import scala.util.control.ControlThrowable

/**
  * Created by drozd on 22.03.2020.
  */
object SparqlSelect { // the name of the function, which will be used publicly
  val NAME = "sparql-select"
  // the parameter types, in order, and whether or not they're bags
  private val params = Array(StringAttribute.identifier)
  private val bagParams = Array(false)

  def getSupportedIdentifiers: util.Set[String] = {
    val set = new util.HashSet[String]
    set.add(NAME)
    set
  }
}

class SparqlSelect(owlAttributeStore: OwlAttributeStore) // use the constructor that handles mixed argument types
  extends FunctionBase(SparqlSelect.NAME, 0, SparqlSelect.params, SparqlSelect.bagParams, StringAttribute.identifier, true) {

  private val log = LogFactory.getLog(classOf[SparqlSelect])

  override def evaluate(inputs: util.List[Evaluatable], context: EvaluationCtx): EvaluationResult = { // Evaluate the arguments using the helper method...this will
    // catch any errors, and return values that can be compared
    val argValues = new Array[AttributeValue](inputs.size)
    val result = evalArgs(inputs, context, argValues)
    if (result != null) return result
    // cast the resolved values into specific types
    val sparql = argValues(0).asInstanceOf[StringAttribute].getValue

    try {
      val results = owlAttributeStore.queryOntologyWithSparql(sparql, context)

      val values = results.map(value => AttributeFactory.getInstance().createValue(new URI(StringAttribute.identifier), value))

      new EvaluationResult(new BagAttribute(new URI(StringAttribute.identifier), values.toList.asJava))
    } catch safely {
      case e: Throwable => {
        log.error("Error while processing the function: SparqlSelect. " + e.getClass.getName + e.getMessage, e)
        throw e
      }
    }
  }

  def safely[T](handler: PartialFunction[Throwable, T]): PartialFunction[Throwable, T] = {
    case ex: ControlThrowable => throw ex
    // case ex: OutOfMemoryError (Assorted other nasty exceptions you don't want to catch)

    //If it's an exception they handle, pass it on
    case ex: Throwable if handler.isDefinedAt(ex) => handler(ex)

    // If they didn't handle it, rethrow. This line isn't necessary, just for clarity
    case ex: Throwable => throw ex
  }
}