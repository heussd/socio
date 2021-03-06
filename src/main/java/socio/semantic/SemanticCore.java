package socio.semantic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import socio.Config;
import socio.model.Promotion;
import socio.xmpp.XmppClient;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This class is the entry point for all involved semantic technologies.
 * Currently, SocIO relies on persistence in flat files.
 * 
 * @author th
 * 
 */
public class SemanticCore extends Observable {

	/**
	 * Implementation of a performance critical, synchronized singleton pattern
	 * {@link http://de.wikibooks.org/wiki/Java_Standard:_Muster_Singleton}.
	 */
	private static final class InstanceHolder {
		static final SemanticCore INSTANCE = new SemanticCore();
	}

	public static SemanticCore getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static Logger logger = Logger.getLogger(SemanticCore.class);
	private static Semantics semantics = new Semantics();

	private static File RDF_STORAGE_FILE;
	private boolean storeNeedsWrite = false;

	// private Long rdfStoreLastModification;

	/**
	 * Jena specific settings
	 */
	Model rdfStore;

	private SemanticCore() {
		logger.info("Bringing up semantic core...");

		RDF_STORAGE_FILE = new File(Config.getRdfStoreFile());

		rdfStore = semantics.createDefaultModel();

		logger.debug("Registering shutdown hook...");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				SemanticCore.getInstance().shutdown();
			}
		});

		logger.debug("Reading RDF store from file...");
		if (RDF_STORAGE_FILE.exists()) {
			readStore();
		} else {
			try {
				FileUtils.touch(RDF_STORAGE_FILE);
			} catch (Exception e) {
				logger.error("Cannot touch rdf store file:", e);
			}
		}

		// Make sure the store can be written.
		if (!RDF_STORAGE_FILE.exists()) {
			throw new RuntimeException("Store persitence file " + RDF_STORAGE_FILE.toString() + " not writeable.");
		}

		if (rdfStore.isEmpty()) {
			logger.info("Store is empty. Inserting some demo triples...");

			rdfStore.add(semantics.constructDemoMessageModel());
			persistStore(true);
		}

		logger.info("Semantic core is up.");
	}

	private void persistStore(Boolean force) {
		storeNeedsWrite = true;
		persistStore();
	}

	private void readStore() {
		logger.debug("Reading RDF store from file...");
		// this.rdfStoreLastModification = RDF_STORAGE_FILE.lastModified();
		rdfStore.read(RDF_STORAGE_FILE.toURI().toString(), Semantics.RDF_EXPORT_FORMAT);
	}

	private void persistStore() {
		if (storeNeedsWrite) {
			// Check if there happened a silent update
			// if (RDF_STORAGE_FILE.lastModified() != rdfStoreLastModification)
			// {
			// logger.debug("There happend a silent update on the store.");
			// readStore();
			// }

			if (!Config.isReadonly()) {
				try {
					logger.debug("Persisting store...");

					rdfStore.write(new FileOutputStream(RDF_STORAGE_FILE), Semantics.RDF_EXPORT_FORMAT);
				} catch (Exception e) {
					logger.fatal("Failed to store RDF:", e);
				}
			} else {
				logger.warn("Store is readonly and will therefor not be written.");
			}
		} else {
			logger.info("Store does not need to be persisted.");
		}
	}

	/**
	 * At least try to persist the store on shutdown, when necessary.
	 */
	protected void shutdown() {
		logger.info("Shutdown received.");

		persistStore();
		logger.info("Shutting down...");
	};

	public void testQuery() {
		String query = "SELECT * WHERE { ?subject ?predicate ?object . }";

		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query, rdfStore);

			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();
				RDFNode x = rb.get("subject");
				if (x.isLiteral()) {
					Literal titleStr = (Literal) x;
					System.out.println(" " + titleStr);
				}
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}

	}

	public void passXmppMessage(String body, String from) {
		logger.info("Received message from " + from + ": " + body);

		// Add protocol prefix and remove the resource identifier from userid
		if (!from.startsWith("xmpp://"))
			from = "xmpp://" + from.split("/")[0];

		// Try to parse the received message
		Model receivedMessage = semantics.createDefaultModel();
		StringReader stringReader = new StringReader(body);

		try {
			logger.info("Trying to parse received RDF...");
			RDFReader rdfReader = receivedMessage.getReader(Semantics.RDF_EXPORT_FORMAT);
			rdfReader.read(receivedMessage, stringReader, Semantics.RDF_EXPORT_FORMAT);

			logger.info("Successfully received RDF");

			// Build import model and merge it into the store
			receivedMessage = semantics.constructValidUserModel(receivedMessage, from);
			if (!receivedMessage.isEmpty()) {
				persistStatements(receivedMessage, true);
			}

		} catch (Exception e) {
			logger.warn("Could not parse received RDF " + body, e);
		}

	}

	public void persistStatements(Model model) {
		persistStatements(model, false);
	}

	public void persistStatements(Model model, Boolean silent) {
		logger.debug("Persisting model...");
		if (!model.isEmpty()) {
			rdfStore.add(model);
			persistStore(true);
			setChanged();

			if (!silent) {
				XmppClient.getInstance().broadcast(semantics.constructExportModel(model));
			} else {
				logger.debug("Newly imported model will not be broadcasted!");
			}

			notifyObservers(semantics.extractActivityEntries(model));
		} else {
			logger.debug("Model was empty, nothing to persist.");
		}
	}

	/**
	 * Executes a given ask query on the store.
	 */
	public boolean executeAsk(String query) {
		try {
			return QueryExecutionFactory.create(query, rdfStore).execAsk();

		} catch (Exception e) {
			logger.error("Error while executing ask query:", e);
		}
		return false;
	}

	/**
	 * Executes a given ask query on the store.
	 */
	public boolean executeAsk(Query query) {
		try {
			return QueryExecutionFactory.create(query, rdfStore).execAsk();

		} catch (Exception e) {
			logger.error("Error while executing ask query:", e);
		}
		return false;
	}

	public void executeSelect(Query query) {

		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query, rdfStore);

			ResultSet results = qexec.execSelect();

			// ResultSetFormatter rsf = new ResultSetFormatter();
			// rsf.outputAsJSON(results);
			// rsf.

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();

				System.out.println(rb);
				RDFNode x = rb.get("tag");

				// x.
				if (x.isLiteral()) {
					Literal titleStr = (Literal) x;
					System.out.println(" " + titleStr);
				}
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}
		// ResultSetFormatter fmFormatter =
		// ResultSetFormatter.outputAsJSON(resultSet)

		// RDFNode n = soln.get("x") ; // "x" is a variable in the query
		// // If you need to test the thing returned
		// if ( n.isLiteral() )
		// ((Literal)n).getLexicalForm() ;
		// if ( n.isResource() )
		// {
		// Resource r = (Resource)n ;
		// if ( ! r.isAnon() )
		// {
		// ... r.getURI() ...
		// }
		// }
	}

	public SemanticCore clear() {
		logger.warn("Clearing content store...");
		rdfStore.remove(rdfStore);
		persistStore(true);
		return this;
	}

	public String dumpStore() {
		StringWriter stringWriter = new StringWriter();
		rdfStore.write(stringWriter, Semantics.RDF_EXPORT_FORMAT);

		logger.info("RDF STORE DUMP\n" + stringWriter.toString());
		return stringWriter.toString();
	}

	/**
	 * Ask if the store has statements about given subject. If yes, classify
	 * with the terms "both", "own" or "foreign".
	 */
	public String classifyKnowledgeAbout(URI subject) {
		// Execute an ASK for general knowledge before we do further magic
		logger.debug("Constructing query...");
		String query = "ASK { <" + subject + "> ?predicate ?object . }";

		if (!executeAsk(query)) {
			logger.debug("Resource " + subject + " is unknown the the store.");
			return "none";
		}

		// Make a further distinction - at this point, the store has definitely
		// knowledge about the resource.
		List<String> users = new ArrayList<String>();
		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(semantics.buildTaggingByQuery(subject.toString()), rdfStore);

			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();

				RDFNode x = rb.get("user");

				if (x.isLiteral()) {
					Literal titleStr = (Literal) x;
					users.add(titleStr.toString());
				}
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}

		logger.debug("Users that know " + subject + ": " + users);

		if (!users.contains(Config.getXmppUserId())) {
			return "foreign";
		}

		if (users.size() == 1) {
			return "own";
		}

		return "both";
	}

	/**
	 * Ask if the resource is already tagged with a tag.
	 */
	public boolean hasTag(URI subject, String tag) {

		logger.debug("Constructing query...");
		String query = "ASK { <" + subject + "> ?predicate ?object . }";

		boolean result = executeAsk(query);
		logger.debug("Query " + query + " returned " + result);

		return result;
	}

	/**
	 * Queries the core for all tag names containing the given pattern.
	 */
	public List<String> queryTagsForPattern(String pattern) {
		List<String> result = new ArrayList<String>();

		// ResultSetFormatter resultSetFormatter = new ResultSetFormatter();
		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(semantics.buildTagQuery(pattern), rdfStore);

			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();

				RDFNode x = rb.get("tag");

				if (x.isLiteral()) {
					Literal titleStr = (Literal) x;
					result.add(titleStr.toString());
				}
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}

		// rsf.outputAsJSON(results);
		// rsf.

		return result;
	}

	/**
	 * Queries the core for all tag names associated with the given URI.
	 * 
	 * TODO: make uri param of type URI
	 */
	public List<String> queryTagsForUri(String uri, Boolean ownFlag) {
		List<String> result = new ArrayList<String>();

		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(semantics.buildUriQuery(uri, ownFlag), rdfStore);

			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();

				RDFNode x = rb.get("tagName");

				if (x.isLiteral()) {
					Literal titleStr = (Literal) x;
					result.add(titleStr.toString());
				}
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}

		return result;
	}

	public void addTags(String uri, String... tags) {
		try {
			List<String> candidates = new ArrayList<String>();
			for (String tag : tags) {

				if (executeAsk(semantics.buildAskTaggingQuery(uri, tag))) {
					logger.info(uri + " already tagged with " + tag);
				} else {
					candidates.add(tag);
				}
			}

			if (candidates.size() > 0) {
				Model newTags = semantics.makeTagging(new URI(uri), candidates);
				persistStatements(newTags);
			} else {
				logger.info("No tags remaining in candidate list - no new tagging");
			}
		} catch (Exception e) {
			logger.error("Could not add tags:", e);
		}
	}

	/**
	 * Prepares all own statements in the entire RDF store to be broadcasted via
	 * XMPP
	 * 
	 * @return
	 */
	public List<String> getAllMyStatements() {
		List<String> statements = new ArrayList<String>();

		logger.debug("Collecting all of " + Config.getXmppUserId() + "'s statements...");
		Model model = semantics.constructValidUserModel(rdfStore, Config.getXmppUserId());

		// FIXME: To many warnings here :(
		@SuppressWarnings({ "unchecked", "static-access" })
		Iterator<Resource> iterator = model.listResourcesWithProperty(semantics.tag.tag);

		while (iterator.hasNext()) {

			Resource resource = iterator.next();
			Model statement = semantics.constructValidStatementModel(model, resource);

			StringWriter stringWriter = new StringWriter();
			statement.write(stringWriter, Semantics.RDF_EXPORT_FORMAT);
			statements.add(stringWriter.toString());

		}

		return statements;
	}

	public HashMap<String, Float> queryRelatedUris(URI resource, Boolean ownFlag) {
		HashMap<String, Float> result = new HashMap<String, Float>();

		// 1. Retrieve current tags for this resource
		List<String> tags = queryTagsForUri(resource.toString(), ownFlag);
		Integer numberOfTags = tags.size();

		logger.debug("Tag associated with " + resource + ": " + tags);

		// 2. Count associated urls for each tag
		for (String tag : tags) {

			Query query = semantics.buildTagCountSubquery(tag);

			QueryExecution qexec = null;
			try {
				qexec = QueryExecutionFactory.create(query, rdfStore);

				ResultSet results = qexec.execSelect();

				for (; results.hasNext();) {
					QuerySolution rb = results.nextSolution();
					String url = rb.get("resource").toString();

					// Ignore the original URL, which is of course highly
					// related with itself.
					if (!url.equals(resource.toString())) {
						/**
						 * FIXME This is quite dirty (as the query result is a
						 * string like
						 * "1^^http://www.w3.org/2001/XMLSchema#integer"
						 */
						Float count = new Float(rb.get("count").toString().split("\\^\\^")[0]);

						// Increment the rating
						Float currentCount = result.get(url);
						if (currentCount == null) {
							result.put(url, count);
						} else {
							result.put(url, currentCount + count);
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error while executing query:", e);
			} finally {
				if (qexec != null)
					qexec.close();
			}

		}

		// 3. Make scores relative
		Float threshold = Config.getRelatedThreshold().floatValue() / 100;
		Iterator<Entry<String, Float>> iterator = result.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Float> entry = iterator.next();

			Float rating = entry.getValue() / numberOfTags;

			if (rating < threshold) {
				iterator.remove();
			} else {
				result.put(entry.getKey(), rating);
			}
		}
		return result;
	}

	public List<Promotion> queryTagActivity(String tag) {
		List<Promotion> promotions = new ArrayList<Promotion>();
		Query query = semantics.buildTagActivityQuery(tag);

		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query, rdfStore);
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();

				logger.debug("Query result: " + rb);
				String resource = rb.get("url").toString();
				String date = rb.get("date").toString();
				String user = rb.get("user").toString();

				Promotion promotion = new Promotion(resource, date, user);
				promotions.add(promotion);
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}
		return promotions;
	}

	public List<Promotion> queryUserActivity(String user) {
		List<Promotion> promotions = new ArrayList<Promotion>();
		Query query = semantics.buildUserActivityQuery(user);

		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query, rdfStore);
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();

				logger.debug("Query result: " + rb);
				String resource = rb.get("url").toString();
				String date = rb.get("date").toString();
				String tag = rb.get("tagname").toString();

				Promotion promotion = new Promotion(resource, date, tag);
				promotions.add(promotion);
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}
		return promotions;
	}

	public List<Promotion> queryCommunityActivity() {
		return queryCommunityActivity(Config.getXmppUserId());
	}

	public List<Promotion> queryCommunityActivity(String xmppUserId) {
		List<Promotion> promotions = new ArrayList<Promotion>();
		Query query = semantics.buildCommunityActivityQuery(xmppUserId);

		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query, rdfStore);
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();

				logger.debug("Query result: " + rb);
				String resource = rb.get("url").toString();
				String date = rb.get("date").toString();
				String tag = rb.get("tagname").toString();
				String user = rb.get("user").toString();

				Promotion promotion = new Promotion(resource, date, user, tag);
				promotions.add(promotion);
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}
		return promotions;
	}

	public Suggestions search(String input) {
		Suggestions suggestions = new Suggestions(input);

		Query query = semantics.buildSearchQuery(input);
		logger.debug("Constructed query is " + query);

		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query, rdfStore);
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution rb = results.nextSolution();
				logger.debug("Result: " + rb.toString());
				suggestions.put(rb.get("url").toString(), (rb.get("label") != null ? rb.get("label").toString() : ""), rb.get("score").toString());
			}
		} catch (Exception e) {
			logger.error("Error while executing query:", e);
		} finally {
			if (qexec != null)
				qexec.close();
		}
		return suggestions;
	}

}
