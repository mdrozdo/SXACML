package onto.utils;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Created by michal on 2015-03-28.
 */
public class OntologyUtils {

    public static InfModel createJenaModel(OWLOntology ontology) {
        PelletReasoner reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
        KnowledgeBase kb = reasoner.getKB();
        PelletInfGraph graph = new org.mindswap.pellet.jena.PelletReasoner().bind(kb);
        return ModelFactory.createInfModel(graph);
    }

    public static OWLOntology loadOntology(IRI source){
        OWLOntologyManager ontoMgr = OWLManager.createOWLOntologyManager();
        try {
            return ontoMgr.loadOntology(source);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return null;
        }
    }
}
