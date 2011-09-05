package socio.xmpp;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
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
							logger.info("Received message from " + message.getFrom());
							SemanticCore.getInstance().passXmppMessage(body.getMessage(), "xmpp://" + message.getFrom());
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
		// bringUpClient("socio", "TC_MwC_-x7");
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
					// Ignore this event
				}

				@Override
				public void entriesAdded(Collection<String> arg0) {
					for (String addedEntry : arg0) {
						logger.info("Entry was added to the roster: " + addedEntry);
						logger.debug("Sending my statements to user " + addedEntry);
						sendQuietly(addedEntry, SemanticCore.getInstance().getAllMyStatements());
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

		// Do basic checks
		if (!Config.getInstance().isValidXmppId(jabberId)) {
			logger.warn("Invalid XMPP ID: " + jabberId);
			return false;
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
	 */
	public void send(String user, String message) throws Exception {
		logger.debug("Sending message to user " + user + "...");

		// Send the message. The framework requires a MessageListener
		// here, so we add a temporary lister and remove it immediately
		// in the next step.
		Chat chat = connection.getChatManager().createChat(user, NULL_MESSAGE_LISTENER);
		chat.removeMessageListener(NULL_MESSAGE_LISTENER);
		chat.sendMessage(message);
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
