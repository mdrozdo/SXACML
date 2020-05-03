package onto.sparql;

import org.apache.jena.query.ResultSet;

public interface IResultProcessor {
	void processResult(ResultSet rs);
}