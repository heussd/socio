package socio.semantic;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import socio.Config;
import socio.rss.ActivityEntry;
import socio.rss.ActivityFeed;

import com.hp.hpl.jena.rdf.model.Model;

public class SemanticCoreTest {
	private static URI knownSubject;
	private static Semantics semantics;
	private static SemanticCore core;

	@Before
	public void setUp() throws Exception {
		Config.getTestInstance();

		knownSubject = new URI("https://www.fbi.h-da.de/");
		semantics = new Semantics();

		core = SemanticCore.getInstance();
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);
	}

	@Test
	public void testActivity() throws Exception {
		SemanticCore core = SemanticCore.getInstance();
		Semantics semantics = new Semantics();

		String tag = "News";

		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);

		List<ActivityEntry> activityEntries = core.queryTagActivity(tag);

		assertEquals(1, activityEntries.size());
		assertEquals(knownSubject.toString(), activityEntries.get(0).getResource());

		SemanticCore.getInstance().persistStatements(semantics.makeTagging("xmpp://anotheruser@example.com", knownSubject, tag), true);
		activityEntries = core.queryTagActivity(tag);

		assertEquals(2, activityEntries.size());

		ActivityFeed activityFeed = new ActivityFeed(tag);
		activityFeed.addEntries(activityEntries);
		System.out.println(activityFeed.toString());

	}

	@Test
	public void testKnowledge() throws Exception {
		SemanticCore core = SemanticCore.getInstance();
		Semantics semantics = new Semantics();

		// Build default environment
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);
		URI knownSubject = new URI("https://www.fbi.h-da.de/");

		assertEquals("own", core.classifyKnowledgeAbout(knownSubject));
		assertEquals("none", core.classifyKnowledgeAbout(new URI("http://www.google.de")));

		URI newUrl = new URI("http://a.totally.random-uri.com");

		// Add foreign tagging
		Model model = semantics.makeTagging("xmpp://foreignxmppuser@example.com", newUrl, "tag1");
		core.persistStatements(model, true);

		core.dumpStore();
		assertEquals("foreign", core.classifyKnowledgeAbout(newUrl));

		// Now make the tagging known by the own user, too
		model = semantics.makeTagging(Config.getTestInstance().getXmppUserId(), new URI("http://a.totally.random-uri.com"), "tag1");
		core.persistStatements(model, true);

		assertEquals("both", core.classifyKnowledgeAbout(newUrl));

	}

	@Test
	public void testAllMyStatements() throws Exception {
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);

		List<String> allMyStatements = core.getAllMyStatements();

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

		Model model = semantics.makeTagging("xmpp://foreignxmppuser@example.com", knownSubject, newTags);
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
		core.persistStatements(semantics.constructDemoMessageModel(), true);

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
		SemanticCore.getInstance().passXmppMessage("", Config.getInstance().getXmppUserId());
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