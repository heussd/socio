package socio.semantic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import socio.Config;
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

/**
 * This class is the entry point for all involved semantic technologies.
 * Currently, SocIO relies on persistence in flat files.
 * 
 * @author th
 * 
 */
public class SemanticCore {

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

	private static File RDF_STORAGE_FILE = new File("store.rdf");
	private boolean storeNeedsWrite = false;

	/**
	 * Jena specific settings
	 */
	Model rdfStore;

	private SemanticCore() {
		logger.info("Bringing up semantic core...");

		rdfStore = semantics.createDefaultModel();

		logger.info("Registering shutdown hook...");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				SemanticCore.getInstance().shutdown();
			}
		});

		logger.info("Reading RDF store from file...");
		if (RDF_STORAGE_FILE.exists()) {
			rdfStore.read(RDF_STORAGE_FILE.toURI().toString(), Semantics.RDF_EXPORT_FORMAT);
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
			storeNeedsWrite = true;
		}

	}

	private void persistStore() {
		try {
			logger.debug("Persisting store...");
			rdfStore.write(new FileOutputStream(RDF_STORAGE_FILE), Semantics.RDF_EXPORT_FORMAT);
		} catch (Exception e) {
			logger.fatal("Failed to store RDF:", e);
		}
	}

	/**
	 * At least try to persist the store on shutdown, when necessary.
	 */
	protected void shutdown() {
		logger.info("Shutdown received.");
		if (storeNeedsWrite) {
			persistStore();
		} else {
			logger.info("Store does not need to be persisted.");
		}
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
			receivedMessage = semantics.constructValidModel(receivedMessage, from);
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
		if (!model.isEmpty()) {
			rdfStore.add(model);
			storeNeedsWrite = true;
			persistStore();

			if (!silent) {
				XmppClient.getInstance().broadcast(semantics.constructExportModel(model));
			} else {
				logger.debug("Newly imported model will not be broadcasted!");
			}
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
		storeNeedsWrite = true;
		return this;
	}

	public String dumpStore() {
		StringWriter stringWriter = new StringWriter();
		rdfStore.write(stringWriter, Semantics.RDF_EXPORT_FORMAT);

		logger.info("RDF STORE DUMP\n" + stringWriter.toString());
		return stringWriter.toString();
	}

	/**
	 * Ask if the store has statements about given subject.
	 */
	public boolean hasKnowledgeAbout(URI subject) {

		logger.debug("Constructing query...");
		String query = "ASK { <" + subject + "> ?predicate ?object . }";

		boolean result = executeAsk(query);
		logger.debug("Query " + query + " returned " + result);

		return result;
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
	public String getAllMyStatements() {
		logger.debug("Collecting all of " + Config.getInstance().getXmppUserId() + "'s statements...");
		Model model = semantics.constructValidModel(rdfStore, Config.getInstance().getXmppUserId());
		StringWriter stringWriter = new StringWriter();
		model.write(stringWriter, Semantics.RDF_EXPORT_FORMAT);

		return stringWriter.toString();
	}

	public HashMap<String, Integer> queryRelatedUris(URI resource, Boolean ownFlag) {
		HashMap<String, Integer> result = new HashMap<String, Integer>();

		// 1. Retrieve current tags for this resource
		List<String> tags = queryTagsForUri(resource.toString(), ownFlag);

		logger.debug("Tag associated with " + resource + ": " + tags);

		// 2. Count associated urls for each tags
		for (String tag : tags) {

			Query query = semantics.buildTagCountSubquery(tag);

			QueryExecution qexec = null;
			try {
				qexec = QueryExecutionFactory.create(query, rdfStore);

				ResultSet results = qexec.execSelect();

				for (; results.hasNext();) {
					QuerySolution rb = results.nextSolution();

					System.out.println("R: " + rb);
					String url = rb.get("resource").toString();

					/**
					 * FIXME This is quite dirty
					 * 
					 * Query result is the string
					 * "1^^http://www.w3.org/2001/XMLSchema#integer"
					 */
					Integer count = new Integer(rb.get("count").toString().split("\\^\\^")[0]);

					// try to get current count
					Integer currentCount = result.get(url);
					if (currentCount == null) {
						result.put(url, count);
					} else {
						result.put(url, currentCount + count);
					}
				}
			} catch (Exception e) {
				logger.error("Error while executing query:", e);
			} finally {
				if (qexec != null)
					qexec.close();
			}

		}
		return result;
	}
}