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


  def classIdForCategory(category: URI): URI = {
    category.toString match{
      case "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject" => new URI("sxacml:subject:subject-class-id")
      case "urn:oasis:names:tc:xacml:3.0:attribute-category:resource" => new URI("sxacml:resource:resource-class-id")
      case "urn:oasis:names:tc:xacml:3.0:attribute-category:action" => new URI("sxacml:action:action-class-id")
      case "urn:oasis:names:tc:xacml:3.0:attribute-category:environment" => new URI("sxacml:environment:environment-class-id")
    }
  }

  def Parse(ctx: AbstractRequestCtx): Seq[ContextAttributeValue] = {
    def parseAttributes(as: Attributes) = {
      as.getAttributes.flatMap(
          a => a.getValues.map(
            v => FlatAttributeValue(as.getCategory, a.getId, v.getType, v.encode())))
    }

    def parseContent(category: URI, contentRoot: Node): Seq[ContextAttributeValue] = {
      (parseRootClassId(category, contentRoot) ++ parseContentAttributes(category, contentRoot)).toSeq
    }

    def parseRootClassId(category: URI, contentRoot: Node) : Option[FlatAttributeValue] = {
      if(contentRoot != null) {
        Some(FlatAttributeValue(
          category,
          classIdForCategory(category),
          new URI("http://www.w3.org/2001/XMLSchema#anyURI"),
          contentRoot.scope.uri + ":" + contentRoot.label
        ))
      } else {
        None
      }
    }

    def parseContentAttributes(category: URI, contentRoot: Node): Seq[ContextAttributeValue] = {
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
                  case child: Elem => Some(NestedAttributeValue(category, name, parseContentAttributes(category, el)))
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
        parseAttributes(as) ++ parseContent(as.getCategory, asXml(as.getContent))
      }
    ).toSeq
  }
}
