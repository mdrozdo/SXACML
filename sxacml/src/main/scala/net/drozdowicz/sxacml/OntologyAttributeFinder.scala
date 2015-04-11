package net.drozdowicz.sxacml

import java.net.URI

import com.hp.hpl.jena.query.{QuerySolution, ResultSet}
import onto.sparql.{ValueSetResultProcessor, IResultProcessor, SparqlReader}
import org.semanticweb.owlapi.model._
import scala.collection.JavaConversions._

/**
 * Created by michal on 2015-03-18.
 */
object OntologyAttributeFinder {
  def findAttributeValues(ontology: OWLOntology, requestId: String, categoryId: String, attributeId: String): Set[FlatAttributeValue] = {
    val sparql = new SparqlReader(ontology)
    val individualId = categoryId + ":request_" + requestId
    val qry =
      """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |SELECT ?val WHERE
        |{
        | <%s> <%s> ?val
        |}""".stripMargin.format(individualId, attributeId) //TODO refactor creating individual id or pass it around
    var result = Set.empty[FlatAttributeValue]
    sparql.executeQuery(qry, new ValueSetResultProcessor {
      override def processSolution(sol: QuerySolution): Unit = {
        val literal = sol.getLiteral("val")
        result += FlatAttributeValue(URI.create(categoryId), URI.create(attributeId), URI.create(literal.getDatatypeURI), literal.getString)
      }
    })
    result
  }
}
