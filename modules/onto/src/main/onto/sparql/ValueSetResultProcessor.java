package onto.sparql;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public abstract class ValueSetResultProcessor implements IResultProcessor {

	public abstract void processSolution(QuerySolution sol);
	
	public void processResult(ResultSet rs) {
		if (!rs.hasNext()) {
			System.out.println("Found none.");
		}
		while (rs.hasNext()) {
			QuerySolution sol = rs.nextSolution();
			processSolution(sol);
		}
	}
}
