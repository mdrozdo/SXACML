package net.drozdowicz.sxacml

import java.net.URI

import org.apache.jena.query.QuerySolution
import onto.sparql.{SparqlReader, ValueSetResultProcessor}
import onto.utils.OntologyUtils
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.model.parameters.Imports
import org.semanticweb.owlapi.search.EntitySearcher

import scala.collection.JavaConversions._
import scala.net.drozdowicz.sxacml.Constants
import scala.compat.java8.OptionConverters._
//import scala.net.drozdowicz.sxacml.JavaOptionals._
import scala.util.matching.Regex

/**
  * Created by michal on 2015-03-18.
  */
object OntologyAttributeFinder {

  def getHierarchyDesignator(rootOntology: OWLOntology) = {
    val factory = rootOntology.getOWLOntologyManager.getOWLDataFactory
    //TODO change to non-deprecated
    rootOntology.getObjectPropertiesInSignature(Imports.INCLUDED)
      .filter(p =>
        EntitySearcher.getAnnotations(p, rootOntology, factory.getOWLAnnotationProperty(
          IRI.create("http://drozdowicz.net/sxacml/request#hierarchyDesignator"))
        ).findAny().isPresent)
      .map(p => p.getIRI.toString)
      .headOption
  }

  //THIS DOESN'T WORK while using OWL-API/Pellet for SPARQL processing. Should work if using a real triple store.
  def getHierarchyDesignatorSparql(rootOntology: OWLOntology) = {
    val query =
      """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX sxacml: <http://drozdowicz.net/sxacml/request#>
        |SELECT ?prop WHERE
        |{
        | ?prop sxacml:hierarchyDesignator ?ignored
        |}""".stripMargin

    val sparql = new SparqlReader(rootOntology)
    var result : Option[String] = None
    sparql.executeQuery(query, new ValueSetResultProcessor {
      override def processSolution(sol: QuerySolution): Unit = {
        val solution = sol.getResource("prop")
        if(solution != null){
          result = Some(solution.getURI)
        }
      }
    })
    result
  }

  def findInstancesOfClass(ontology: OWLOntology, categoryId: String, attributeId: String, classIdOrName: String): Set[FlatAttributeValue] = {

    val classId = if(isIRI(classIdOrName)){
        classIdOrName
      } else {
      ontology.getOntologyID.getOntologyIRI.asScala
        .map(iri=>iri.toString).get + "#" + classIdOrName;
    }

    queryOntology(ontology,
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
    queryOntology(ontology,
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
    if (attributeId.equalsIgnoreCase(Constants.TYPE_PROPERTY_URI)) {
      queryOntology(ontology,
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
    } else {
      queryOntology(ontology,
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
  }

  private def isIRI(value: String): Boolean = {
    return "" != IRI.create(value).getNamespace
  }

  private def queryOntology(ontology: OWLOntology, sparqlQuery: String, valueGetter: (QuerySolution) => FlatAttributeValue): Set[FlatAttributeValue] = {
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
    ontology.getDataPropertiesInSignature().map(dp => dp.getIRI.toString).toSet + Constants.TYPE_PROPERTY_URI
  }
}
