package net.drozdowicz.sxacml

import java.net.URI

import com.hp.hpl.jena.query.QuerySolution
import onto.sparql.{SparqlReader, ValueSetResultProcessor}
import onto.utils.OntologyUtils
import org.semanticweb.owlapi.model._

import scala.collection.JavaConversions._
import scala.net.drozdowicz.sxacml.Constants
import scala.net.drozdowicz.sxacml.JavaOptionals._
import scala.util.matching.Regex

/**
  * Created by michal on 2015-03-18.
  */
object OntologyAttributeFinder {

  def isIRI(value: String): Boolean = {
    
    return pattern.pattern.matcher(value).matches()
  }

  def findInstancesOfClass(ontology: OWLOntology, categoryId: String, attributeId: String, classIdOrName: String): Set[FlatAttributeValue] = {

    val classId = if(isIRI(classIdOrName)){
        classIdOrName
      } else {
      ontology.getOntologyID.getOntologyIRI.toOption
        .map(iri=>iri.toString).get + "#" + classIdOrName;
    }

    //val ontologyId = ontology.getOntologyID.getOntologyIRI.toOption.map(iri=>iri.toString).getOrElse(requestOntologyId)

//    val classId = ontology.getOntologyID.getOntologyIRI.toOption
//      .map(iri=>iri.toString).getOrElse(requestOntologyId)

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
