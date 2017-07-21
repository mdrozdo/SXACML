package net.drozdowicz.sxacml

import java.net.URI

import org.wso2.balana.attr.{AttributeFactory, AttributeValue}

/**
 * Created by michal on 2015-03-16.
 */

sealed trait ContextAttributeValue{
  val categoryId:URI
}

case class FlatAttributeValue(categoryId:URI, attributeId:URI, valueType: URI, valueString: String) extends ContextAttributeValue {
  def createAttributeValue(): AttributeValue = {
    AttributeFactory.getInstance().createValue(valueType, valueString)
  }
}

case class NestedAttributeValue(categoryId:URI, propertyId:Option[URI], namespace: String, localName: String, children: Seq[ContextAttributeValue]) extends ContextAttributeValue{

}
