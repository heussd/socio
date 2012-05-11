package socio.rss;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import socio.Config;
import socio.model.Promotion;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * This class helps to generate a newsfeed for specific tags. It can be used to
 * follow the activity of the SocIO community.
 * 
 * @author th
 * 
 */
public class ActivityFeed {

	private final static String FEED_TYPE = "rss_2.0";
	// FIXME: This setting should be retrieved from the respective REST-Launcher
	private final static String LINK = "http://localhost:8080/socio/rest/activity";

	private final static Logger LOGGER = Logger.getLogger(ActivityFeed.class);

	private SyndFeed syndFeed;
	private List<SyndEntry> syndEntries;
	private StringWriter stringWriter;

	/**
	 * Creates a new activity feed for a specific tag.
	 */
	public ActivityFeed(String tag) {
		syndFeed = new SyndFeedImpl();
		syndFeed.setFeedType(FEED_TYPE);
		syndFeed.setTitle(tag);
		syndFeed.setLink(LINK + "?tag=\"" + tag + "\"");
		syndFeed.setDescription("Activity feed to the tag \"" + tag + "\"");

		syndEntries = new ArrayList<SyndEntry>();
	}

	public ActivityFeed() {
		syndFeed = new SyndFeedImpl();
		syndFeed.setFeedType(FEED_TYPE);
		syndFeed.setTitle(Config.getXmppUserId() + "'s Community Activity");
		syndFeed.setLink(LINK);
		syndFeed.setDescription("Community activity feed of " + Config.getXmppUserId());

		syndEntries = new ArrayList<SyndEntry>();
	}

	public void addEntry(Promotion promotion) {
		SyndEntry entry = new SyndEntryImpl();

		entry.setTitle(promotion.getResource());
		entry.setLink(promotion.getResource());
		entry.setPublishedDate(promotion.getDate());

		if (!promotion.getUser().equals("")) {
			SyndContent description = new SyndContentImpl();
			description = new SyndContentImpl();
			description.setType("text/plain");
			description.setValue(promotion.toString());
			entry.setDescription(description);
		}

		syndEntries.add(entry);
	}

	public void addEntries(List<Promotion> promotions) {
		for (Promotion promotion : promotions) {
			addEntry(promotion);
		}
	}

	public String toString() {
		String feed = "<rss/>";

		try {
			syndFeed.setEntries(syndEntries);
			stringWriter = new StringWriter();

			// FIXME: Output & StringWriter might be static final
			SyndFeedOutput syndFeedOutput = new SyndFeedOutput();
			syndFeedOutput.output(syndFeed, stringWriter);
			feed = stringWriter.toString();
		} catch (Exception e) {
			LOGGER.error("Could not generate feed:", e);
		}

		return feed;
	}
}
