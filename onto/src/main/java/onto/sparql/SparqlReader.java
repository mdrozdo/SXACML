package onto.sparql;

import onto.utils.Box;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;


import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.semanticweb.owlapi.model.OWLOntology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;


//TODO enable declaration of prefixes when instantiating the reader, to reuse between queries
public class SparqlReader implements Closeable{

	private InfModel model;
	private PelletInfGraph graph;

	public SparqlReader(OWLOntology ontology) {
		PelletReasoner reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
		KnowledgeBase kb = reasoner.getKB();
		graph = new org.mindswap.pellet.jena.PelletReasoner().bind(kb);
		model = ModelFactory.createInfModel(graph);
	}

	public void executeQuery(String qry, IResultProcessor resultProcessor) {
		QueryExecution qe = null;
		ResultSet rs;
		try {
			qe = QueryExecutionFactory.create(qry, model);
			rs = qe.execSelect();
			resultProcessor.processResult(rs);
		} finally {
			if(qe != null)
				qe.close();
		}		
	}
	
	public String readSingleString(String query, final String returnName){
		final Box<String> result = new Box<String>();
		executeQuery(query, new SingleValueResultProcessor() {
			
			@Override
			public void processSolution(QuerySolution sol) {
				result.set(sol.get(returnName).asLiteral().getString());				
			}
		});
		
		return result.get();
	}

    public Set<String> readAllValuesAsString(String query, final String returnName){
        final Set<String> result = new HashSet<String>();
        executeQuery(query, new ValueSetResultProcessor() {

            @Override
            public void processSolution(QuerySolution sol) {
                result.add(sol.get(returnName).asLiteral().getString());
            }
        });

        return result;
    }

	public void close() {
		model.close();
		graph.close();	
	}

}
