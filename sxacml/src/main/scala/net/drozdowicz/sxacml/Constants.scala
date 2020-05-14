package scala.net.drozdowicz.sxacml

import java.io.File
import java.net.{URI, URLDecoder}
import java.util.jar.JarFile

import scala.collection.JavaConversions._

/**
 * Created by michal on 2015-05-03.
 */
object Constants {
  val REQUEST_CLASS_ID = "sxacml:attribute-category:request"

  val classIdForCategory = Map(
    "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject" -> new URI("sxacml:subject:subject-class-id"),
    "urn:oasis:names:tc:xacml:3.0:attribute-category:resource" -> new URI("sxacml:resource:resource-class-id"),
    "urn:oasis:names:tc:xacml:3.0:attribute-category:action" -> new URI("sxacml:action:action-class-id"),
    "urn:oasis:names:tc:xacml:3.0:attribute-category:environment" -> new URI("sxacml:environment:environment-class-id"),
    REQUEST_CLASS_ID -> new URI("sxacml:request:request-class-id")
  )
}
