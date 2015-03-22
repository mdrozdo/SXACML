package net.drozdowicz.sxacml

import java.net.URI
import scala.collection.JavaConversions._

import org.wso2.balana.ctx.AbstractRequestCtx

/**
 * Created by michal on 2015-03-13.
 */
object ContextParser {
  def Parse(ctx: AbstractRequestCtx): scala.collection.Set[FlatAttributeValue] = {
    ctx.getAttributesSet.flatMap(
      as=>as.getAttributes.flatMap(
        a=>a.getValues.map(
          v=>FlatAttributeValue(as.getCategory, a.getId, v.getType, v.encode()))))
  }
}
