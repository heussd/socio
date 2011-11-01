package socio.rss;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class ActivityEntry {

	private final static Logger LOGGER = Logger.getLogger(ActivityEntry.class);
	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");

	private String resource;
	private Date date;
	private String user;
	private String tag;

	public ActivityEntry(String resource, String date, String user) {
		setResource(resource);
		setDate(date);
		setUser(user);
	}

	public ActivityEntry(String resource, String date, String user, String tag) {
		setResource(resource);
		setDate(date);
		setUser(user);
		setTag(tag);
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(String date) {
		try {
			this.date = DATE_FORMAT.parse(date);
		} catch (Exception e) {
			LOGGER.warn("Could not parse " + date + " as date: ", e);
			this.date = new Date(System.currentTimeMillis());
		}
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String toString() {
		return (getUser() != null ? "User " + getUser() + " tagged " : "Tagging for ") + (getResource() != null ? " resource " + getResource() + " " : " ")
				+ (getTag() != null ? " with tag \"" + getTag() + "\"" : "");

	}
}
