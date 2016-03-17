package net.drozdowicz.sxacml

import java.net.URI

import com.hp.hpl.jena.query.QuerySolution
import onto.sparql.{SparqlReader, ValueSetResultProcessor}
import onto.utils.OntologyUtils
import org.semanticweb.owlapi.model._

import scala.collection.JavaConversions._

/**
  * Created by michal on 2015-03-18.
  */
object OntologyAttributeFinder {

  private val typePropertyURI = "urn:sxacml:attributes:type"

  def findAttributeValues(ontology: OWLOntology, individualId: String, categoryId: String, attributeId: String): Set[FlatAttributeValue] = {
    val queryFunction = queryForAttribute(ontology, individualId, categoryId, attributeId, _: String, _: (QuerySolution) => FlatAttributeValue)

    if (attributeId.equalsIgnoreCase(typePropertyURI)) {
      queryFunction(
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
      queryFunction(
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

  private def queryForAttribute(ontology: OWLOntology, individualId: String, categoryId: String, attributeId: String, sparqlQuery: String, valueGetter: (QuerySolution) => FlatAttributeValue): Set[FlatAttributeValue] = {
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
    ontology.getDataPropertiesInSignature().map(dp => dp.getIRI.toString).toSet + typePropertyURI
  }
}
