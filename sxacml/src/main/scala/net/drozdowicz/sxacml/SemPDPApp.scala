package net.drozdowicz.sxacml

import scala.compat.Platform
import scala.net.drozdowicz.sxacml.SemanticPDP

object SemPDPApp {

  val usage =
    """
    Usage: sempdp -p policyPath -o ontologyFolderPath -u ontologyUri
    After running, the process will accept XACML requests on the StdIn.
  """

  def matchesRequestEnd(line: String) = {
    !("(?i)</\\s*request\\s*>".r.findFirstIn(line).isEmpty)
  }

  def readFromStdIn: Option[String] = {
    val lines = io.Source.stdin.getLines

    (lines.span(l => !matchesRequestEnd(l)) match {
      case (h, t) => (h.toList, t.take(1).toList)
    })
    match {
      case (_, List()) => None
      case (h, t) => Some((h ::: t).mkString(Platform.EOL))
    }
  }

  def main(args: Array[String]) {

    if (args.length == 0){
      println(usage)
      sys.exit(1)
    }
    val arglist = args.toList
    type OptionMap = Map[Symbol, String]

    def parseOptions(map: OptionMap, list: List[String]): OptionMap = {
      list match {
        case Nil => map
        case "-p" :: value :: tail =>
          parseOptions(map ++ Map('p -> value), tail)
        case "-o" :: value :: tail =>
          parseOptions(map ++ Map('o -> value), tail)
        case "-u" :: value :: tail =>
          parseOptions(map ++ Map('u -> value), tail)
        //        case string :: opt2 :: tail if isSwitch(opt2) =>
        //          parseOptions(map ++ Map('infile -> string), list.tail)
        //        case string :: Nil =>  parseOptions(map ++ Map('infile -> string), list.tail)
        case option :: tail => println("Unknown option " + option)
          sys.exit(1)
      }
    }

    val options = parseOptions(Map(), arglist)
    println(options)
    val policyLocation = options.get('p).getOrElse("")
    val ontologyPath = options.get('o).getOrElse("")
    val ontologyUri = options.get('u).getOrElse("")

    val pdp = new SemanticPDP(policyLocation, ontologyPath, ontologyUri)

    Iterator.continually(readFromStdIn).takeWhile(p => p.nonEmpty).flatMap(o => o).foreach(request => {
      val response = pdp.evaluate(request)
      println(response)
    })
  }
}
