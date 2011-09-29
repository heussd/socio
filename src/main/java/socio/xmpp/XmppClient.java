package socio.xmpp;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Body;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.proxy.ProxyInfo.ProxyType;

import socio.Config;
import socio.semantic.SemanticCore;

public class XmppClient {

	/**
	 * Implementation of a performance critical, synchronized singleton pattern
	 * {@link http://de.wikibooks.org/wiki/Java_Standard:_Muster_Singleton}.
	 */
	private static final class InstanceHolder {
		static final XmppClient INSTANCE = new XmppClient();
	}

	public static XmppClient getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static Logger logger = Logger.getLogger(XmppClient.class);

	/**
	 * This setting seems to be set by each server individually, best practices
	 * could not yet be found. However, this is Google AppEngine's setting (
	 * {@link http
	 * ://code.google.com/intl/de-DE/appengine/docs/java/xmpp/overview
	 * .html#Quotas_and_Limits}).
	 */
	private static final int MAX_MESSAGE_SIZE_IN_KB = 1000;

	/**
	 * SocIOs primary packet listener. Passes all received messages to the
	 * semantic core.
	 */
	private static PacketListener SOCIO_PACKET_LISTENER = new PacketListener() {
		@Override
		public void processPacket(Packet packet) {
			// We only want messages below this point
			if (packet instanceof Message) {
				Message message = (Message) packet;

				if (!message.getFrom().equals(Config.getInstance().getXmppUserId())) {
					for (Body body : message.getBodies()) {
						if (body != null) {
							// Sender-name is suffix'd by his resource. Cut it
							// off.
							String from = "xmpp://" + message.getFrom().replaceAll("/srp", "");

							logger.info("Received message from " + from);
							SemanticCore.getInstance().passXmppMessage(body.getMessage(), from);
						}
					}
				} else {
					logger.warn("Congratulations! You just sent a message to yourself!");
				}
			}
		}
	};

	/**
	 * This listener does nothing with the received message. Because the SRP
	 * solely "broadcasts" its messages, there is not direct peer to peer
	 * conversation. However, the framework requires a MessageListener when
	 * opening a chat.
	 */
	private static MessageListener NULL_MESSAGE_LISTENER = new MessageListener() {
		@Override
		public void processMessage(Chat arg0, Message arg1) {
		}
	};

	private Connection connection;

	private XmppClient() {
		bringUpClient(Config.getInstance().getUserName(), Config.getInstance().getPassword());
	}

	private void bringUpClient(String userName, String password) {
		logger.info("Bringing up XMPP client...");

		try {
			ConnectionConfiguration config = null;

			if (Config.getInstance().useProxy()) {
				ProxyInfo proxy = new ProxyInfo(ProxyType.HTTP, Config.getInstance().getProxyAddress(), Config.getInstance().getProxyPort(), "", "");
				config = new ConnectionConfiguration(Config.getInstance().getServerAddress(), Config.getInstance().getServerPort(), Config.getInstance().getServerAddress(), proxy);
			} else {
				config = new ConnectionConfiguration(Config.getInstance().getServerAddress(), Config.getInstance().getServerPort(), Config.getInstance().getServerAddress());
			}

			connection = new XMPPConnection(config);

			connection.connect();
			connection.login(userName, password, "srp");

			if (connection.getRoster().getEntries().size() == 0)
				logger.warn("There are no entries on the roster");

			logger.debug("Starting XMPP packet listener...");
			connection.addPacketListener(SOCIO_PACKET_LISTENER, new PacketTypeFilter(Message.class));

			logger.debug("XMPP subscription mode = " + connection.getRoster().getSubscriptionMode());

			logger.debug("Starting XMPP roster event listener...");
			connection.getRoster().addRosterListener(new RosterListener() {

				@Override
				public void presenceChanged(Presence arg0) {
					// Ignore this event
				}

				@Override
				public void entriesUpdated(Collection<String> arg0) {
					// Ignore this event
				}

				@Override
				public void entriesDeleted(Collection<String> arg0) {
					for (String deletedEntry : arg0) {
						logger.info("Entry was deleted from the roster: " + deletedEntry);
					}
				}

				@Override
				public void entriesAdded(Collection<String> arg0) {
					for (String addedEntry : arg0) {
						logger.info("Entry was added to the roster: " + addedEntry);
						logger.debug("Sending my statements to user " + addedEntry);

						// Send each resource by its own - because we don't know
						// the maximum XMPP message size, sending the entire RDF
						// store at once is not an option.
						List<String> statements = SemanticCore.getInstance().getAllMyStatements();
						for (int i = 0; i < statements.size(); i++) {
							logger.info("Sending statement " + (1 + i) + "/" + statements.size() + " ...");
							sendQuietly(addedEntry, statements.get(i));
						}

					}
				}

			});

			logger.info("XMPP client is up.");
		} catch (Exception e) {
			logger.error("Could not bring up XMPP client:", e);
		}
	}

	public boolean addUser(String jabberId) {
		jabberId = jabberId.trim().replaceAll("'", "");

		if (!jabberId.startsWith("xmpp://"))
			jabberId = "xmpp://" + jabberId;

		// Do basic checks
		if (!Config.getInstance().isValidXmppId(jabberId)) {
			logger.warn("Invalid XMPP ID: " + jabberId);
			return false;
		}

		RosterEntry existingRosterEntry = connection.getRoster().getEntry(jabberId.replaceAll("xmpp://", ""));
		if (existingRosterEntry != null) {
			logger.debug("User already exists on the roster, trying to delete him first...");
			try {
				connection.getRoster().removeEntry(existingRosterEntry);
			} catch (XMPPException e) {
				logger.warn("Could not delete roster entry \"" + jabberId + "\"", e);
			}
		}

		logger.debug("Adding new user " + jabberId + " ...");
		try {
			// FIXME: Set a group for the new user.
			connection.getRoster().createEntry(jabberId.replaceAll("xmpp://", ""), jabberId, new String[] { "default" });
			return true;
		} catch (Exception e) {
			logger.error("Could not add user to roster:", e);
		}
		return false;

	}

	/**
	 * Broadcasts the message to every user on the roster.
	 */
	public void broadcast(String message) {
		// http://stackoverflow.com/questions/3279057/problem-adding-buddy-with-smack-api-and-openfire-server
		try {
			for (RosterEntry rosterEntry : connection.getRoster().getEntries()) {
				String user = rosterEntry.getUser();
				logger.debug("Broadcasting to user " + user + "...");
				send(user, message);
			}
		} catch (Exception e) {
			logger.error("Could not broadcast message", e);
		}
	}

	/**
	 * Guess what this class does.
	 * 
	 */
	public void send(String user, String body) throws Exception {
		logger.debug("Sending message to user " + user + "...");

		if (body.toCharArray().length * 2 / 1024 > MAX_MESSAGE_SIZE_IN_KB) {
			logger.warn("Message exceeds maximum message size of " + MAX_MESSAGE_SIZE_IN_KB + " KB.");
		}

		// Send the message. The framework requires a MessageListener
		// here, so we add a temporary lister and remove it immediately
		// in the next step.
		Chat chat = connection.getChatManager().createChat(user, NULL_MESSAGE_LISTENER);
		chat.removeMessageListener(NULL_MESSAGE_LISTENER);
		chat.sendMessage(body);

		// Message message = new Message(user, Message.Type.chat);
		// message.setBody(body);
		// connection.sendPacket(message);

	}

	/**
	 * Same as {@code send(user, message)} but catches the possibly thrown
	 * exception.
	 */
	public void sendQuietly(String user, String message) {
		try {
			send(user, message);
		} catch (Exception e) {
			logger.error("Could not send message to user " + user + ":", e);
		}
	}
}
