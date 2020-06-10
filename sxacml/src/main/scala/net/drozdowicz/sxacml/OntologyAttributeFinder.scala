package net.drozdowicz.sxacml

import java.net.URI

import org.apache.jena.query.QuerySolution
import onto.sparql.{SparqlReader, ValueSetResultProcessor}
import onto.utils.OntologyUtils
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.model.parameters.Imports
import org.semanticweb.owlapi.search.EntitySearcher

import scala.collection.JavaConverters._
import scala.net.drozdowicz.sxacml.Constants
import scala.compat.java8.OptionConverters._
//import scala.net.drozdowicz.sxacml.JavaOptionals._
import scala.util.matching.Regex

/**
  * Created by michal on 2015-03-18.
  */
object OntologyAttributeFinder {

  val categoryUris = Map(
    "?subject" -> new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
    "?resource" -> new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
    "?action" -> new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:action"),
    "?environment" -> new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:environment"),
    "?request" -> new URI("sxacml:attribute-category:request")
  )

  def queryOntologyWithSparql(query: String, requestOntology: OWLOntology, categoryIndividualIds: Map[URI, String]): Set[String] = {

    val finalQuery = categoryUris.foldLeft(query)((q, c) => q.replaceAllLiterally(c._1, "<" + categoryIndividualIds(c._2) + ">"))

    val sparql = new SparqlReader(requestOntology)

    var result = Set.empty[String]
    sparql.executeQuery(finalQuery, new ValueSetResultProcessor {
      override def processSolution(sol: QuerySolution): Unit = {

        val varNames = sol.varNames().asScala.toList
        if (varNames.size == 1)
          result += sol.get(varNames(0)).toString
        else
          result += varNames.map(v => s"$v:${sol.get(v)}").mkString(";")
      }
    })
    result
  }

  def queryOntologyWithSparqlPath(sparqlPath: String, requestOntology: OWLOntology, categoryIndividualIds: Map[URI, String]): Set[String] = {

    val query = getOntologyPrefixesString(requestOntology) +
      s"""PREFIX sxacml: <https://w3id.org/sxacml/request#>
         |SELECT ?value WHERE
         |{
         | $sparqlPath ?value
         |}""".stripMargin

    val finalQuery = categoryUris.foldLeft(query)((q, c) => q.replaceAllLiterally(c._1, "<" + categoryIndividualIds(c._2) + ">"))

    val sparql = new SparqlReader(requestOntology)

    var result = Set.empty[String]
    sparql.executeQuery(finalQuery, new ValueSetResultProcessor {
      override def processSolution(sol: QuerySolution): Unit = {
        result += sol.get("value").toString
      }
    })
    result
  }


  def getHierarchyDesignator(rootOntology: OWLOntology) = {
    val factory = rootOntology.getOWLOntologyManager.getOWLDataFactory
    //TODO change to non-deprecated
    rootOntology.getObjectPropertiesInSignature(Imports.INCLUDED).asScala
      .filter(p =>
        EntitySearcher.getAnnotations(p, rootOntology, factory.getOWLAnnotationProperty(
          IRI.create("https://w3id.org/sxacml/request#hierarchyDesignator"))
        ).findAny().isPresent)
      .map(p => p.getIRI.toString)
      .headOption
  }

  //THIS DOESN'T WORK while using OWL-API/Pellet for SPARQL processing. Should work if using a real triple store.
  def getHierarchyDesignatorSparql(rootOntology: OWLOntology) = {
    val query =
      """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX sxacml: <https://w3id.org/sxacml/request#>
        |SELECT ?prop WHERE
        |{
        | ?prop sxacml:hierarchyDesignator ?ignored
        |}""".stripMargin

    val sparql = new SparqlReader(rootOntology)
    var result: Option[String] = None
    sparql.executeQuery(query, new ValueSetResultProcessor {
      override def processSolution(sol: QuerySolution): Unit = {
        val solution = sol.getResource("prop")
        if (solution != null) {
          result = Some(solution.getURI)
        }
      }
    })
    result
  }

  def findInstancesOfClass(ontology: OWLOntology, categoryId: String, attributeId: String, classIdOrName: String): Set[FlatAttributeValue] = {

    val classId = if (isIRI(classIdOrName)) {
      classIdOrName
    } else {
      ontology.getOntologyID.getOntologyIRI.asScala
        .map(iri => iri.toString).get + "#" + classIdOrName;
    }

    executeQuery(ontology,
      """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |SELECT ?val WHERE
        |{
        | ?val rdf:type <%s>
        |}""".stripMargin.format(classId),
      (sol: QuerySolution) => {
        val solution = sol.getResource("val")
        FlatAttributeValue(URI.create(categoryId), URI.create(attributeId), URI.create("http://www.w3.org/2001/XMLSchema#anyURI"), solution.getURI)
      }
    )
  }

  def findInstancesFromHierarchy(ontology: OWLOntology, categoryId: String, attributeId: String, rootIdOrName: String, hierarchyDesignatorId: String): Set[FlatAttributeValue] = {
    executeQuery(ontology,
      """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |SELECT ?val WHERE
        |{
        | <%s> <%s> ?val
        |}""".stripMargin.format(rootIdOrName, hierarchyDesignatorId),
      (sol: QuerySolution) => {
        val solution = sol.getResource("val")
        FlatAttributeValue(URI.create(categoryId), URI.create(attributeId), URI.create("http://www.w3.org/2001/XMLSchema#anyURI"), solution.getURI)
      }
    )
  }

  def findAttributeValues(ontology: OWLOntology, individualId: String, categoryId: String, attributeId: String): Set[FlatAttributeValue] = {
    executeQuery(ontology,
      """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |SELECT ?val WHERE
        |{
        | <%s> <%s> ?val
        |}""".stripMargin.format(individualId, attributeId),
      (sol: QuerySolution) => {
        val literal = sol.getLiteral("val")
        FlatAttributeValue(URI.create(categoryId), URI.create(attributeId), URI.create(literal.getDatatypeURI), literal.getString)
      }
    )

  }

  def findClassAttributeValues(ontology: OWLOntology, individualId: String, categoryId: String, attributeId: String): Set[FlatAttributeValue] = {
    executeQuery(ontology,
      """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |SELECT ?val WHERE
        |{
        | <%s> rdf:type ?val
        |}""".stripMargin.format(individualId),
      (sol: QuerySolution) => {
        val solution = sol.getResource("val")
        FlatAttributeValue(URI.create(categoryId), URI.create(attributeId), URI.create("http://www.w3.org/2001/XMLSchema#anyURI"), solution.getURI)
      }
    )
  }


  private def isIRI(value: String): Boolean = {
    return "" != IRI.create(value).getNamespace
  }

  private def executeQuery(ontology: OWLOntology, sparqlQuery: String, valueGetter: (QuerySolution) => FlatAttributeValue): Set[FlatAttributeValue] = {
    val sparql = new SparqlReader(ontology)

    var result = Set.empty[FlatAttributeValue]
    sparql.executeQuery(sparqlQuery, new ValueSetResultProcessor {
      override def processSolution(sol: QuerySolution): Unit = {
        result += valueGetter(sol)
      }
    })
    result
  }

  def getAllSupportedAttributes(ontology: OWLOntology): Set[String] = {
    var model = OntologyUtils.createJenaModel(ontology)
    ontology.getDataPropertiesInSignature().asScala.map(dp => dp.getIRI.toString).toSet ++
      Constants.classIdForCategory.values.map(uri => uri.toString)
  }

  private def getOntologyPrefixesString(ontology: OWLOntology): String = {
    val ontologyManager = ontology.getOWLOntologyManager
    val prefixes = ontology.importsClosure().iterator().asScala
      .map(o => ontologyManager.getOntologyFormat(o).asPrefixOWLDocumentFormat().getPrefixName2PrefixMap.asScala.toMap)
      .reduce(_ ++ _) //convert list of maps to a single map

    prefixes.map {
      case (key, value) => {
        val prefixStr = s"PREFIX $key <$value>"
        prefixStr
      }
    }
      .toList
      .distinct
      .mkString("", scala.compat.Platform.EOL, scala.compat.Platform.EOL)

  }
}
