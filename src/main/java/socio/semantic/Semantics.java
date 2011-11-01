package socio.semantic;

import java.io.StringWriter;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.ISO8601DateFormat;

import socio.Config;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * This class does the mapping between several ontologies and Java, like
 * constructing ontology-based {@link Query} objects for Jena. In a perfect
 * world, customizing this class would be sufficient for enabling more
 * ontologies in SocIO...
 * 
 * @author th
 * 
 */
public class Semantics {

	public class Tag {

		private OntModel base;
		public static final String NameSpace = "http://www.holygoat.co.uk/owl/redwood/0.1/tags/";

		public Tag() {
			base = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

			// This is a packaged copy of the ontology.
			base.read(Semantics.class.getClassLoader().getResource("tags.n3").toString(), "RDF/XML");

			Tagging = base.createOntResource(NameSpace + "Tagging");
			Tag = base.createOntResource(NameSpace + "Tag");
			associatedTag = base.createObjectProperty(NameSpace + "associatedTag");
			taggedBy = base.createObjectProperty(NameSpace + "taggedBy");
			taggedOn = base.createObjectProperty(NameSpace + "taggedOn");
			name = base.createObjectProperty(NameSpace + "name");
			tag = base.createObjectProperty(NameSpace + "tag");
		}

		public final Resource Tagging;
		public final Resource Tag;
		public final Property taggedBy;
		public final Property associatedTag;
		public final Property taggedOn;
		public final Property name;
		public final Property tag;

		public Individual createIndividual(String uri, Resource resource) {
			return base.createIndividual(uri, resource);
		}

		public Individual createIndividual(Resource resource) {
			return base.createIndividual(resource);
		}

	}

	public class Sioc {

		private OntModel base;
		public static final String NameSpace = "http://rdfs.org/sioc/spec/";

		public Sioc() {
			base = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

			// This is a packaged copy of the ontology.
			base.read(Semantics.class.getClassLoader().getResource("sioc.rdf").toString(), "RDF/XML");

			userAccount = base.getOntClass(NameSpace + "UserAccount");
			email = base.createObjectProperty(NameSpace + "email");
		}

		public final Resource userAccount;
		public final Property email;

		public Individual createIndividual(String uri, Resource resource) {
			return base.createIndividual(uri, resource);
		}

		public Individual createIndividual(Resource resource) {
			return base.createIndividual(resource);
		}

	}

	private static Logger logger = Logger.getLogger(Semantics.class);
	private PrefixMapping prefixMapping;
	private Properties queries;

	protected static Tag tag;
	protected static Sioc sioc;
	private static DateFormat dateFormat = new ISO8601DateFormat();

	public static Selector TAGGING_SELECTOR;

	/**
	 * Selected most (human) readable RDF format. Documentation warns about
	 * performance issues when writing "very large Models" {@link http
	 * ://jena.sourceforge.net/tutorial/RDF_API/index.html}
	 * 
	 * FIXME: This section is performance critical
	 */
	public static String RDF_EXPORT_FORMAT = "N3";

	public Semantics() {
		tag = new Tag();
		sioc = new Sioc();

		prefixMapping = PrefixMapping.Factory.create();
		prefixMapping.setNsPrefix("rdf", RDF.getURI());
		prefixMapping.setNsPrefix("tag", Tag.NameSpace);
		prefixMapping.setNsPrefix("rdfs", RDFS.getURI());
		prefixMapping.setNsPrefix("foaf", FOAF.getURI());

		TAGGING_SELECTOR = new SimpleSelector(RDFS.Resource, RDF.type, tag.Tagging);

		queries = new Properties();
		try {
			queries.load(Semantics.class.getClassLoader().getResourceAsStream("sparql.properties"));
		} catch (Exception e) {
			logger.error("Could not load query property file: ", e);
		}
	}

	/**
	 * Generates a usual default RDF model with basic namespace definitions.
	 */
	public Model createDefaultModel() {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(prefixMapping);
		return model;
	}

	/**
	 * Writes a given model in a unified notation format.
	 */
	private String writeModel(Model model, Individual... individuals) {
		for (Individual individual : individuals) {
			model.add(individual.listProperties());
		}

		StringWriter stringWriter = new StringWriter();
		model.write(stringWriter, Semantics.RDF_EXPORT_FORMAT);

		return stringWriter.toString();
	}

	public String constructDemoMessage() {
		return writeModel(constructDemoMessageModel());
	}

	/**
	 * Creates a default model with an example tagging.
	 */
	public Model constructDemoMessageModel() {
		try {
			Model model = makeTagging(Config.getXmppUserId(), new URI("https://www.fbi.h-da.de/"), "Department page", "Computer", "University", "News");
			return model;
		} catch (Exception e) {
			logger.error("Could not construct demo model:", e);
		}
		return null;
	}

	// FIXME: All makeTagging methods require some kind of clean up
	public Model makeTagging(String user, URI resource, String... strings) {
		// TODO Make this better
		List<String> list = new ArrayList<String>();
		for (String string : strings) {
			list.add(string);
		}
		return makeTagging(user, resource, list);
	}

	public Model makeTagging(URI resource, List<String> strings) {
		return makeTagging(Config.getXmppUserId(), resource, strings);
	}

	/**
	 * Assigns one or more tags to a given resource.
	 */
	public Model makeTagging(String user, URI resource, List<String> tags) {
		Model model = createDefaultModel();

		Individual source = tag.createIndividual(resource.toASCIIString(), RDFS.Resource);
		Individual tagging = tag.createIndividual(tag.Tagging);

		Individual me = tag.createIndividual(FOAF.Agent);
		me.addProperty(FOAF.jabberID, user);
		model.add(me.listProperties());

		for (String stringedTag : tags) {
			// Create anonymous tag and add tag as name
			Individual individualTag = tag.createIndividual(tag.Tag);
			individualTag.addProperty(tag.name, stringedTag);

			// Add tag to the tagging
			tagging.addProperty(tag.associatedTag, individualTag);

			model.add(individualTag.listProperties());
		}

		// Timestamp & "Userstamp"
		tagging.addProperty(tag.taggedOn, dateFormat.format(Calendar.getInstance().getTime()));
		tagging.addProperty(tag.taggedBy, me);

		// Add the tagging
		source.addProperty(tag.tag, tagging);

		// Export properties to model
		model.add(tagging.listProperties());
		model.add(source.listProperties());

		return model;
	}

	/**
	 * This method constructs a graph, fitting in SocIOs semantic storage model.
	 * Basically, it makes sure that all taggings are named with the proper user
	 * and have timestamps. Invalid models will be consumed silently.
	 */
	public Model constructValidUserModel(Model receivedModel, String user) {
		logger.debug("Constructing import model for user " + user + " ...");
		Model result = createDefaultModel();

		if (!receivedModel.isEmpty()) {
			Query query = QueryFactory.make();
			query.setPrefixMapping(prefixMapping);

			// This CONSTRUCT builds a graph following SocIOs semantic storage
			// model.
			query = QueryFactory.parse(query, queries.getProperty("query.constructusermodel").replaceAll("###user###", user), null, Syntax.syntaxSPARQL);

			logger.debug("Constructed import query: " + query);

			try {
				QueryExecution qcexec = QueryExecutionFactory.create(query, receivedModel);
				result = qcexec.execConstruct();

				if (result.isEmpty()) {
					logger.warn("Constructed model was empty.");
				}
			} catch (Exception e) {
				logger.error("Error while constructing import graph:", e);
			}
		} else {
			logger.warn("Model to be imported was received empty.");
		}

		return result;
	}

	public Model constructValidStatementModel(Model model, Resource resource) {
		logger.debug("Constructing statement model for resource " + resource.toString() + " ...");
		Model result = createDefaultModel();

		if (!model.isEmpty()) {
			Query query = QueryFactory.make();
			query.setPrefixMapping(prefixMapping);

			// This CONSTRUCT builds a graph following SocIOs semantic storage
			// model.
			query = QueryFactory.parse(query, queries.getProperty("query.constructstatementmodel").replaceAll("###resource###", resource.toString()), null, Syntax.syntaxSPARQL);

			logger.debug("Constructed import query: " + query);

			try {
				QueryExecution qcexec = QueryExecutionFactory.create(query, model);
				result = qcexec.execConstruct();

				if (result.isEmpty()) {
					logger.warn("Constructed model was empty.");
				}
			} catch (Exception e) {
				logger.error("Error while constructing import graph:", e);
			}
		} else {
			logger.warn("Model to be imported was received empty.");
		}

		return result;
	}

	/**
	 * Case-insensitive tag name search containing the given pattern
	 */
	public Query buildTagQuery(String pattern) {
		Query query = QueryFactory.make();
		query.setPrefixMapping(prefixMapping);

		String queryString = queries.getProperty("query.tagpattern").replaceAll("###pattern###", pattern);

		return QueryFactory.parse(query, queryString, null, Syntax.syntaxSPARQL);
	}

	/**
	 * Retrieve tags to the given URI.
	 * 
	 * @param ownTagsOnly
	 *            users tags only / foreign tags only
	 */
	public Query buildUriQuery(String uri, Boolean ownTagsOnly) {
		Query query = QueryFactory.make();
		query.setPrefixMapping(prefixMapping);

		String queryString = ownTagsOnly ? queries.getProperty("query.alltagsby") : queries.getProperty("query.alltagsnotby");
		queryString = queryString.replaceAll("###uri###", uri).replaceAll("###user###", Config.getXmppUserId());

		logger.debug("Constructed query: " + queryString);
		return QueryFactory.parse(query, queryString, null, Syntax.syntaxSPARQL);
	}

	public Query buildAskTaggingQuery(String uri, String tag) {
		Query query = QueryFactory.make();
		query.setPrefixMapping(prefixMapping);

		String queryString = queries.getProperty("query.askfortaggingfor").replaceAll("###uri###", uri).replaceAll("###tag###", tag)
				.replaceAll("###user###", Config.getXmppUserId());

		return QueryFactory.parse(query, queryString, null, Syntax.syntaxSPARQL);
	}

	public Query buildTaggingByQuery(String uri) {
		Query query = QueryFactory.make();
		query.setPrefixMapping(prefixMapping);

		String queryString = queries.getProperty("query.taggingby").replaceAll("###uri###", uri);
		return QueryFactory.parse(query, queryString, null, Syntax.syntaxSPARQL);
	}

	public Query buildTagCountSubquery(String tag) {
		Query query = QueryFactory.make();
		query.setPrefixMapping(prefixMapping);

		String queryString = queries.getProperty("query.tagcount").replaceAll("###tag###", tag);
		logger.debug("Query is " + queryString);
		return QueryFactory.parse(query, queryString, null, Syntax.syntaxARQ);
	}

	public String constructExportModel(Model model) {
		Model export = createDefaultModel();
		export.add(constructValidUserModel(model, Config.getXmppUserId()));

		StringWriter stringWriter = new StringWriter();
		export.write(stringWriter, Semantics.RDF_EXPORT_FORMAT);

		return stringWriter.toString();
	}

	public Query buildTagActivityQuery(String tagName) {
		Query query = QueryFactory.make();
		query.setPrefixMapping(prefixMapping);

		String queryString = queries.getProperty("query.tagactivity").replaceAll("###tag###", tagName);
		logger.debug("Query is " + queryString);
		return QueryFactory.parse(query, queryString, null, Syntax.syntaxSPARQL);
	}

	public Query buildUserActivityQuery(String user) {
		Query query = QueryFactory.make();
		query.setPrefixMapping(prefixMapping);

		String queryString = queries.getProperty("query.useractivity").replaceAll("###user###", user);
		logger.debug("Query is " + queryString);
		return QueryFactory.parse(query, queryString, null, Syntax.syntaxSPARQL);
	}

	public Query buildCommunityActivityQuery(String user) {
		Query query = QueryFactory.make();
		query.setPrefixMapping(prefixMapping);

		String queryString = queries.getProperty("query.activity").replaceAll("###user###", user);
		logger.debug("Query is " + queryString);
		return QueryFactory.parse(query, queryString, null, Syntax.syntaxSPARQL);
	}
}
