package net.deterlab.testbed.api;

/**
 * A notification from the system to a user about an event that requires their
 * action.  Requests to join or be added to circles and projects generate
 * notifications as well as some system administrative events.  The
 * notification system is used to communicate with DETER users in prefernce to
 * e-mail or instant messages in order to avoid the various spam and other
 * protections that might block operations.
 * <p>
 * A notification includes a unique identifier for the message, the body of the
 * message, the time it was sent and a flags indicating whether the ntification
 * has been read or is urgent.
 *
 * @author DETER Team
 * @version 1.0
 */
public class UserNotification extends ApiObject {
    /** unique id for this notification (NB a notification may be delivered to
     * many users. */
    protected long id;
    /** The date the notification was created (as a string) */
    protected String sent;
    /** Flags (see constants ) */
    protected NotificationFlag[] flags;
    /** the text of the notification */
    protected String body;

    /**
     * Create an empty notification.
     */
    public UserNotification() {
	id = -1;
	sent = null;
	flags = null;
    }

    /**
     * Get the ID
     * @return the ID
     */
    public long getId() { return id; }
    /**
     * Set the ID
     * @param i the new ID
     */
    public void setId(long i) { id = i; }
    /**
     * Get the sending time
     * @return the sending time
     */
    public String getSent() { return sent; }
    /**
     * Set the sending time
     * @param d the new sending time
     */
    public void setSent(String d) { sent = d; }
    /**
     * Get the flags.  These return value is an array of
     * <a href="NotificationFlag.html">NotificationFlag</a> objects.
     * @return the flags
     * @see NotificationFlag
     */
    public NotificationFlag[] getFlags() { return flags; }
    /**
     * Set the flags.  Note that the flags are
     * <a href="NotificationFlag.html">NotificationFlag</a> objects.
     * @param f the new flags
     * @see NotificationFlag
     */
    public void setFlags(NotificationFlag[] f) { flags = f; }
    /**
     * Get the body
     * @return the body
     */
    public String getBody() { return body; }
    /**
     * Set the body
     * @param b the new body
     */
    public void setBody(String b) { body = b; }
};
