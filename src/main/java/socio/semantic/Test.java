package socio.semantic;

import java.util.Properties;

import socio.semantic.Semantics.Tag;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Properties queries = new Properties();
		try {
			queries.load(Semantics.class.getClassLoader().getResourceAsStream("sparql.properties"));
		} catch (Exception e) {

		}
		Query query = QueryFactory.make();
		// query.setPrefixMapping(prefixMapping);
		
		PrefixMapping prefixMapping;
		prefixMapping = PrefixMapping.Factory.create();
		prefixMapping.setNsPrefix("rdf", RDF.getURI());
		prefixMapping.setNsPrefix("tag", Tag.NameSpace);
		prefixMapping.setNsPrefix("rdfs", RDFS.getURI());
		prefixMapping.setNsPrefix("foaf", FOAF.getURI());
		
		
		String user = "xmpp://trallalal@example.com";
		query.setPrefixMapping(prefixMapping);
		query = QueryFactory.parse(query, queries.getProperty("query.constructmodel").replaceAll("###user###", user), null, Syntax.syntaxSPARQL);
		
		
		System.out.println(query.toString());
	}

}
