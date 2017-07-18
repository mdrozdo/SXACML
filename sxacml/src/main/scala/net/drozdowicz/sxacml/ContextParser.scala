package net.drozdowicz.sxacml

import java.net.URI
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXResult

import scala.collection.JavaConversions._
import org.wso2.balana.ctx.AbstractRequestCtx
import org.wso2.balana.xacml3.Attributes

import scala.xml.{Elem, Node, Text}

/**
 * Created by michal on 2015-03-13.
 */
object ContextParser {

  def asXml(dom: _root_.org.w3c.dom.Node): Node = {

    var el = dom
    while(el != null && el.getNodeType != org.w3c.dom.Node.ELEMENT_NODE){
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

  def Parse(ctx: AbstractRequestCtx): Seq[ContextAttributeValue] = {
    def parseAttributes(as: Attributes) = {
      as.getAttributes.flatMap(
          a => a.getValues.map(
            v => FlatAttributeValue(as.getCategory, a.getId, v.getType, v.encode())))
    }

    def parseContent(category: URI, contentRoot: Node): Seq[ContextAttributeValue] = {
      if(contentRoot != null) {
        contentRoot.nonEmptyChildren.flatMap(node => {
          node match {
            case el: Elem => {
              val name = new URI(el.scope.uri + ":" + el.label)
              val xsdType = new URI("http://www.w3.org/2001/XMLSchema#string")

              el.nonEmptyChildren.flatMap(c=> {
                c match {
                  case text: Text => {
                    if(text.data.trim() != "") Some(FlatAttributeValue(category, name, xsdType, text.data))
                    else None
                  }
                  case child: Elem => Some(NestedAttributeValue(category, name, parseContent(category, el)))
                  case _ => None
                }
              })
            }
            case _ => None
          }
        })
      } else {
        Seq.empty[ContextAttributeValue]
      }
    }

    ctx.getAttributesSet.flatMap(
      as => {
        val attrs = parseAttributes(as)
        val content = parseContent(as.getCategory, asXml(as.getContent))
        attrs ++ content
      }
    ).toSeq
  }
}
