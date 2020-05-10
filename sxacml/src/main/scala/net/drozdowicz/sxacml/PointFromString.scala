package scala.net.drozdowicz.sxacml

import org.wso2.balana.attr.AttributeValue
import org.wso2.balana.attr.BooleanAttribute
import org.wso2.balana.attr.StringAttribute
import org.wso2.balana.cond.Evaluatable
import org.wso2.balana.cond.EvaluationResult
import org.wso2.balana.cond.FunctionBase
import org.wso2.balana.ctx.{EvaluationCtx, Status}
import java.util
import scala.collection.JavaConverters._

import org.geotools.xacml.geoxacml.attr.{GML3Support, GMLVersion, GeometryAttribute}
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory, PrecisionModel}

/**
  * Created by drozd on 22.03.2020.
  */
object PointFromString { // the name of the function, which will be used publicly
  val NAME = "net:drozdowicz:sxacml:point-from-string"
  // the parameter types, in order, and whether or not they're bags
  private val params = Array(StringAttribute.identifier)
  private val bagParams = Array(false)

  def getSupportedIdentifiers: util.Set[String] = {
    val set = new util.HashSet[String]
    set.add(NAME)
    set
  }
}

class PointFromString() // use the constructor that handles mixed argument types
  extends FunctionBase(PointFromString.NAME, 0, PointFromString.params, PointFromString.bagParams, GeometryAttribute.identifier, false) {

  override def evaluate(inputs: util.List[Evaluatable], context: EvaluationCtx): EvaluationResult = { // Evaluate the arguments using the helper method...this will
    // catch any errors, and return values that can be compared
    val argValues = new Array[AttributeValue](inputs.size)
    val result = evalArgs(inputs, context, argValues)
    if (result != null) return result
    val latLongArr = argValues(0).asInstanceOf[StringAttribute].getValue.split(',')
    if (latLongArr.size != 2) {
      new EvaluationResult(new Status(List(Status.STATUS_PROCESSING_ERROR).asJava, "Expected two double values separated wtih a comma."))
    } else {
      val cLat = latLongArr(0).toDouble
      val cLong = latLongArr(1).toDouble
      val gf = new GeometryFactory(new PrecisionModel(), 4326)
      val evalResult = gf.createPoint(new Coordinate(cLat, cLong))
      val resultAttribute = new GeometryAttribute(evalResult, "EPSG:4326", null, GMLVersion.Version3, GML3Support.GML_POINT)

      new EvaluationResult(resultAttribute)
    }
  }
}