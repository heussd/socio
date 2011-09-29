package socio.tray;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This log appender logs messages as tray notifications.
 * 
 * @author th
 * 
 */
public class TrayLogger extends AppenderSkeleton {

	@Override
	public void close() {

	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

	@Override
	protected void append(LoggingEvent loggingEvent) {
		String message = this.layout.format(loggingEvent);

		switch (loggingEvent.getLevel().toInt()) {
		case Level.WARN_INT:
			Tray.getInstance().warn(message);
			break;
		case Level.ERROR_INT:
		case Level.FATAL_INT:
			// ERROR == FATAL notifications
			Tray.getInstance().error(message);
			break;
		default:
			break;
		}
	}
}
