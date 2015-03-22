package onto.sparql;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public abstract class SingleValueResultProcessor implements IResultProcessor {

	public abstract void processSolution(QuerySolution sol);

	public void processResult(ResultSet rs) {
		if (!rs.hasNext()) {
			System.out.println("Found none.");
			return;
		}

		QuerySolution sol = rs.nextSolution();
		processSolution(sol);
	}

}
