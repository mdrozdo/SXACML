package net.drozdowicz.sxacml

import java.net.URI

import org.wso2.balana.attr.{AttributeFactory, AttributeValue}

/**
 * Created by michal on 2015-03-16.
 */
case class FlatAttributeValue(categoryId:URI, attributeId:URI, valueType: URI, valueString: String) {
  def createAttributeValue(): AttributeValue = {
    AttributeFactory.getInstance().createValue(valueType, valueString)
  }
}
