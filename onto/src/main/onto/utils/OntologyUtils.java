package onto.utils;

import openllet.core.KnowledgeBase;
import openllet.jena.PelletInfGraph;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
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
        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
        KnowledgeBase kb = reasoner.getKB();
        PelletInfGraph graph = new openllet.jena.PelletReasoner().bind(kb);
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
