package net.drozdowicz.sxacml

import java.io.File
import java.net.URLDecoder
import java.util.jar.JarFile

import scala.collection.JavaConversions._

/**
 * Created by michal on 2015-05-03.
 */
object ResourceFiles {
  def getFilesFromResourceDirectory(someClass: Class[_], path: String): Set[String] = {
    var dirUrl = someClass.getResource(path);

    if (dirUrl != null && dirUrl.getProtocol().equals("file")) {
      //File path
      val endedPath = if(path.endsWith(File.separator)) path else (path + File.separator)
      new File(dirUrl.toURI()).list().map(fn => endedPath + fn).toSet
    } else if (dirUrl == null) {
      //Jar file - assuming jar file containing someClass
      val classFilePath = someClass.getName().replace(".", File.separator) + ".class"
      dirUrl = someClass.getClassLoader().getResource(classFilePath)

      if (dirUrl.getProtocol().equals("jar")) {
        /* A JAR path */
        val jarPath = dirUrl.getPath().substring(5, dirUrl.getPath().indexOf("!"));
        //strip out only the JAR file
        val jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
        val entries = jar.entries(); //gives ALL entries in jar
        entries.map(je => je.getName()).filter(n => n.startsWith("path")).toSet
      } else Set.empty[String]

    } else Set.empty[String]
  }
}
