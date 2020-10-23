package net.drozdowicz.sxacml

import java.net.URI

import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXResult
import org.wso2.balana.attr.{DateAttribute, DateTimeAttribute, TimeAttribute}

import scala.collection.JavaConverters._
import org.wso2.balana.ctx.{AbstractRequestCtx, EvaluationCtx}
import org.wso2.balana.xacml3.Attributes

import scala.collection.mutable
import scala.net.drozdowicz.sxacml.Constants
import scala.xml.{Elem, Node, Text}

/**
  * Created by michal on 2015-03-13.
  */
object ContextParser {

  def Parse(ctx: AbstractRequestCtx): Seq[ContextAttributeValue] = {

    def asXml(dom: _root_.org.w3c.dom.Node): Node = {

      var el = dom
      while (el != null && el.getNodeType != org.w3c.dom.Node.ELEMENT_NODE) {
        el = el.getNextSibling
      }

      val source = new DOMSource(el)
      val adapter = new scala.xml.parsing.NoBindingFactoryAdapter
      val saxResult = new SAXResult(adapter)
      val transformerFactory = javax.xml.transform.TransformerFactory.newInstance()
      val transformer = transformerFactory.newTransformer()
      transformer.transform(source, saxResult)
      adapter.rootElem
    }

    def parseAttributes(as: Attributes) = {
      as.getAttributes.asScala.flatMap(
        a => a.getValues.asScala.map(
          v => FlatAttributeValue(as.getCategory, a.getId, v.getType, v.encode())))
    }

    def parseContent(category: URI, contentRoot: Node): Seq[ContextAttributeValue] = {
      (parseRootClassId(category, contentRoot) ++ parseContentAttributes(category, contentRoot)).toSeq
    }

    def parseRootClassId(category: URI, contentRoot: Node): Option[FlatAttributeValue] = {
      if (contentRoot != null) {
        Some(FlatAttributeValue(
          category,
          Constants.classIdForCategory(category.toString),
          new URI("http://www.w3.org/2001/XMLSchema#anyURI"),
          contentRoot.namespace + contentRoot.label
        ))
      } else {
        None
      }
    }

    def parseContentAttributes(category: URI, contentRoot: Node): Seq[ContextAttributeValue] = {
      if (contentRoot != null) {
        contentRoot.nonEmptyChildren.flatMap(node => {
          node match {
            case el: Elem =>
              val name = new URI(el.namespace + el.label)
              //Always grab first datatype attribute value
              val xsdType = el.attribute("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "datatype").map(attributes=>attributes.text).getOrElse("http://www.w3.org/2001/XMLSchema#string")
              val propertyId = el.attribute("https://w3id.org/sxacml/request#", "property").map(a => new URI(a.text))

              el.nonEmptyChildren.flatMap(c => {
                c match {
                  case text: Text =>
                    if (text.data.trim() != "") Some(FlatAttributeValue(category, name, new URI(xsdType), text.data))
                    else None
                  case child: Elem =>
                    Some(NestedAttributeValue(category, propertyId, el.namespace, el.label, parseContentAttributes(category, el)))
                  case _ => None
                }
              })
            case _ => None
          }
        })
      } else {
        Seq.empty[ContextAttributeValue]
      }
    }

    ctx.getAttributesSet.asScala.flatMap(
      as => {
        parseAttributes(as) ++ parseContent(as.getCategory, asXml(as.getContent))
      }
    ).toSeq
  }
}
