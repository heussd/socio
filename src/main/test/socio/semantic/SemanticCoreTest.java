package socio.semantic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;

public class SemanticCoreTest {
	private static URI knownSubject;
	private static Semantics semantics;
	private static SemanticCore core;

	@Before
	public void setUp() throws Exception {
		knownSubject = new URI("https://www.fbi.h-da.de/");
		semantics = new Semantics();

		core = SemanticCore.getInstance();
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);
	}

	@Test
	public void testAllMyStatements() throws Exception {
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);

		String allMyStatements = core.getAllMyStatements();
		System.out.println(allMyStatements);

		List<String> newTags = new ArrayList<String>();
		newTags.add("new");
		newTags.add("test");
		Model model = semantics.makeTagging("testuser", new URI("https://www.fbi.h-da.de/"), newTags);
		core.persistStatements(model, true);

		core.dumpStore();

		System.out.println(core.getAllMyStatements());

		assertEquals(allMyStatements, core.getAllMyStatements());
	}

	@Test
	public void testTagQueryForURI() throws Exception {
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);

		List<String> newTags = new ArrayList<String>();
		newTags.add("new");
		newTags.add("test");

		Model model = semantics.makeTagging("testuser", new URI("https://www.fbi.h-da.de/"), newTags);
		core.persistStatements(model, true);

		core.dumpStore();

		List<String> tags = core.queryTagsForUri(knownSubject.toString(), true);
		System.out.println(tags);
		assertEquals("Own tags", 4, tags.size());

		tags = core.queryTagsForUri(knownSubject.toString(), false);
		System.out.println(tags);
		assertEquals("Foreign tags", 2, tags.size());
	}

	@Test
	public void testTagQuery() throws Exception {
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel());

		List<String> tags = core.queryTagsForPattern("totatllyrandomstringwhichcannotbefound123192839129309182380");
		System.out.println(tags);
		assertEquals(0, tags.size());

		tags = core.queryTagsForPattern("dep");
		System.out.println(tags);
		assertEquals(1, tags.size());

		tags = core.queryTagsForPattern("");
		System.out.println(tags);
		assertEquals(4, tags.size());

	}

	@Test
	public void testFrom() throws Exception {
		SemanticCore.getInstance().passXmppMessage("", "spellman@jabber.ccc.de/Psi");
	}

	@Test
	public void testKnowledge() throws Exception {
		SemanticCore core = SemanticCore.getInstance();
		Semantics semantics = new Semantics();

		// Build default environment
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel());
		URI knownSubject = new URI("https://www.fbi.h-da.de/");

		assertTrue(core.hasKnowledgeAbout(knownSubject));
		assertFalse(core.hasKnowledgeAbout(new URI("http://www.google.de")));
	}

	@Test
	public void testPassXmppMessage() {
		Semantics semanticHelper = new Semantics();

		// Send valid rdf
		SemanticCore.getInstance().passXmppMessage(semanticHelper.constructDemoMessage(), "xmpp://sttiheus@h-da.de");

		// Send invalid text
		SemanticCore.getInstance().passXmppMessage("INVALID", "xmpp://sttiheus@h-da.de");
	}

}

// http://w3studi.informatik.uni-stuttgart.de/~bischowg/jena/jena.html
//
// odel model = ModelFactory.createDefaultModel();
//
// model.register(new LogModelChangedListener());
//
// // create the resource
// Resource johnSmith = model.createResource(personURI);
//
// // add the property
// johnSmith.addProperty(VCARD.FN, fullName);
// Der Change Listener wird über das hinzufügen der Resource und der Property
// informiert.
//
// Das Model Interface ist Vaterinterface des InfModel Interface, welches
// wiederum Vaterinterface vom OntModel Interface ist.
//
// Parallel zu den Interfaces gibt es eine Hierarchie von Implementierungen.
// ModelCo
