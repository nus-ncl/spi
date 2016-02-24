package net.deterlab.testbed.api;

import java.util.Date;

/**
 * A generic challenge to establish user identity.  It includes the type and
 * contents of the challenge.  The user will respond based on that type and the
 * outcome of a calculation on the data.  A unique identifier is provided so
 * that the system can match challenges and responses.  The challenge can only
 * be responsed to before the Validity field.
 * <p>
 * Currently only "clear" is supported as a type.  There is no data for that
 * type and the response is to send the password back to the system through an
 * encrypted channel.
 *
 * @author the DETER Team
 * @version 1.0
 */
public class UserChallenge extends ApiObject {
    /**  The kind of challege being issued */
    protected String type;
    /** Bytes in the challenge*/
    protected byte[] data;
    /** Valid until */
    protected String validity;
    /** challenge identifier */
    protected long challengeID;

    /**
     * Construct an empty UserChallenge
     */
    public UserChallenge() { }
    /**
     * Construct a full UserChallenge
     * @param t the challenge type
     * @param d the data to carry out the challenge
     * @param v the validity time, as a string
     * @param i the challenge ID
     */
    public UserChallenge(String t, byte[] d, String v, long i) {
	type = t;
	data = d;
	validity = v;
	challengeID = i;
    }
    /**
     * Construct a full UserChallenge
     * @param t the challenge type
     * @param d the data to carry out the challenge
     * @param v the validity time, as a Date
     * @param i the challenge ID
     */
    public UserChallenge(String t, byte[] d, Date v, long i) {
	this(t, d, (String) null, i);
	validity = dateToString(v);
    }
    /**
     * Get the challenge type
     * @return the challenge type
     */
    public String getType() { return type; }
    /**
     * Set the challenge type
     * @param t the new challenge type
     */
    public void setType(String t) { type = t; }
    /**
     * Get the challenge data
     * @return the challenge data
     */
    public byte[] getData() { return data; }
    /**
     * Set the challenge data
     * @param d the new challenge data
     */
    public void setData(byte[] d) { data = d; }
    /**
     * Get the time the challenge is valid until.  It is a string of 4-digits
     * of year, 2 of month, 2 of day, a constant T then 2 digits of hour (24
     * hour), 2 of minute and 2 of second followed by a 'Z', in GMT.  In java a
     * format string of yyyyMMdd'T'HHmmss'Z will format it.
     * @return the valid time
     */
    public String getValidity() { return validity; }
    /**
     * Set the time the challenge is valid until.  It is a string of 4-digits
     * of year, 2 of month, 2 of day, a constant T then 2 digits of hour (24
     * hour), 2 of minute and 2 of second followed by a Z, in GMT.  In java a
     * format string of yyyyMMdd'T'HHmmss'Z' will format it.
     * @param v the new validity
     */
    public void setValidity(String v) { validity=v; }
    /**
     * Get the challenge ID
     * @return the challenge ID
     */
    public long getChallengeID() { return challengeID; }
    /**
     * Set the challenge data
     * @param i the new challenge data
     */
    public void setChallengeID(long i) { challengeID = i; }
}
