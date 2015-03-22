package net.drozdowicz.sxacml

import java.net.URI

/**
 * Created by michal on 2015-03-16.
 */
case class FlatAttributeValue(categoryId:URI, attributeId:URI, valueType: URI, valueString: String) {
}
