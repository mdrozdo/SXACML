package onto.sparql;

import com.hp.hpl.jena.query.ResultSet;

public interface IResultProcessor {
	void processResult(ResultSet rs);
}