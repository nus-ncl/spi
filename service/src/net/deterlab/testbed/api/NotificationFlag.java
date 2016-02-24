package net.deterlab.testbed.api;

/**
 * The flags that modify notifications, used by getNotifications and
 * markNotifications in the Users service.  Currenlty notifications can be
 * flagged as urgent and read.  The system generally sets the URGENT flag,
 * though applications may as well.  The read flag is conventionally set once a
 * user has been shown a notification, but this is under application control.
 */

public class NotificationFlag {
    /** Notification has been read */
    static public final String READ_TAG = "READ";
    /** Notification is urgent */
    static public final String URGENT_TAG = "URGENT";

    /** The tag */
    private String tag;
    /** The state of the field */
    private boolean isSet;

    /**
     * Create a NotificationFlag.
     */
    public NotificationFlag() {
	tag = null;
	isSet = false;
    }

    /**
     * Make a NotificationFlag
     * @param t the tag
     * @param s true if the flag is or should be set
     */
    public NotificationFlag(String t, boolean s) {
	tag = t;
	isSet = s;
    }

    /**
     * Return the tag
     * @return the tag
     */
    public String getTag() { return tag; }
    /**
     * Set the tag
     * @param t the new tag value
     */
    public void setTag(String t) { tag = t; }

    /**
     * Return true if the flag is set
     * @return true if the flag is set
     */
    public boolean getIsSet() { return isSet; }
    /**
     * Set or unset the flag
     * @param f the new flag value
     */
    public void setIsSet(boolean f) { isSet = f; }

}
