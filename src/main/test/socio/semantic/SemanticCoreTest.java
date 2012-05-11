package socio.semantic;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import socio.Config;
import socio.model.Promotion;
import socio.rest.URIFactory;
import socio.rss.ActivityFeed;

import com.hp.hpl.jena.rdf.model.Model;

public class SemanticCoreTest {
	private static URI knownSubject;
	private static Semantics semantics;
	private static SemanticCore core;

	@Before
	public void setUp() throws Exception {
		Config.testmode();

		knownSubject = new URI("https://www.fbi.h-da.de/");
		semantics = new Semantics();

		core = SemanticCore.getInstance();
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);
	}

	// @Test
	// public void testLastMod() throws Exception {
	// core = SemanticCore.getInstance();
	// core.clear();
	//
	// core.persistStatements(semantics.constructDemoMessageModel());
	//
	// FileUtils.touch(new File(Config.getRdfStoreFile()));
	//
	// core.clear();
	//
	// // Not yet tested!
	// // assertFalse("none".equals(core.classifyKnowledgeAbout(knownSubject)));
	//
	// core.dumpStore();
	//
	// // System.exit(0);
	// }

	@Test
	public void testSearch() throws Exception {
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);

		assertEquals(1, core.search("h-da.").size());
		// assertEquals(1, core.search("bob-socio").size());
		assertEquals(0, core.search("asdfgjklqwertzuiopyxcvbnmalqwezuiop").size());

		core.persistStatements(semantics.makeTagging(Config.getXmppUserId(), new URI("http://www.google.de"), "Search", "Cool", "Computer"), false);

		assertEquals(2, core.search("Computer").size());
		assertEquals(1, core.search("Search").size());
		assertEquals(2, core.search("http").size());

		System.out.println(core.search("Computer Dep").toJson());
		// assertEquals(1, core.search("Computer Dep").size());
		// assertEquals(1, core.search("Comp earc").size());
		assertEquals(2, core.search("http comp de").size());

	}

	@Test
	public void testActivity() throws Exception {
		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);

		List<Promotion> promotions = core.queryCommunityActivity();

		assertEquals(0, promotions.size());

		promotions = core.queryCommunityActivity("ANUNKNOWNUSER");

		assertEquals(4, promotions.size());

	}

	@Test
	public void testStrangeUris() throws Exception {
		// This is an invalid URI, according to
		// http://stackoverflow.com/questions/40568/square-brackets-in-urls
		URI uri = URIFactory.getUri("http://soundcloud.com/search?q[fulltext]=balkan");
		assertEquals("none", core.classifyKnowledgeAbout(uri));

		core.persistStatements(semantics.makeTagging(Config.getXmppUserId(), uri, "test"));

		assertEquals("own", core.classifyKnowledgeAbout(uri));
	}

	@Test
	public void testUserActivity() throws Exception {
		SemanticCore core = SemanticCore.getInstance();
		Semantics semantics = new Semantics();

		String user = Config.getXmppUserId();

		core.clear();
		core.dumpStore();
		core.persistStatements(semantics.constructDemoMessageModel(), true);

		List<Promotion> promotions = core.queryUserActivity(user);

		assertEquals(4, promotions.size());

		core.persistStatements(semantics.makeTagging(Config.getXmppUserId(), knownSubject, "arandomtag"), true);
		core.dumpStore();

		promotions = core.queryUserActivity(user);

		assertEquals(5, promotions.size());

		SemanticCore.getInstance().persistStatements(semantics.makeTagging("xmpp://anotheruser@example.com", knownSubject, "arandomtag"), true);
		promotions = core.queryUserActivity("xmpp://anotheruser@example.com");

		assertEquals(1, promotions.size());

		ActivityFeed activityFeed = new ActivityFeed("TAG");
		activityFeed.addEntries(promotions);
		System.out.println(activityFeed.toString());

	}

	@Test
	public void testTagActivity() throws Exception {
		SemanticCore core = SemanticCore.getInstance();
		Semantics semantics = new Semantics();

		String tag = "News";

		core.clear();
		core.persistStatements(semantics.constructDemoMessageModel(), true);

		List<Promotion> promotions = core.queryTagActivity(tag);

		assertEquals(0, promotions.size());

		SemanticCore.getInstance().persistStatements(semantics.makeTagging("xmpp://anotheruser@example.com", knownSubject, tag), true);
		promotions = core.queryTagActivity(tag);

		assertEquals(1, promotions.size());

		ActivityFeed activityFeed = new ActivityFeed(tag);
		activityFeed.addEntries(promotions);
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
		model = semantics.makeTagging(Config.getXmppUserId(), new URI("http://a.totally.random-uri.com"), "tag1");
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
		SemanticCore.getInstance().passXmppMessage("", Config.getXmppUserId());
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