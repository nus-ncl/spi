package net.deterlab.testbed.api;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import net.deterlab.testbed.api.DeterFault;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author yeoteye
 *
 */

@RunWith(JMockit.class)
public class ApiObjectTest {
    
    @Test
    public void dateToStringNull() {
        assertThat(ApiObject.dateToString(null), is(equalTo(null)));
    }
    
    @Test
    public void stringToDateNull() {
        assertThat(ApiObject.stringToDate(null), is(equalTo(null)));
    }
    
    @Test
    public void stringToDateValid() throws Exception {
        // Convert Apr 01 2016 17:00 GMT+0000 to SGT
        Date myDateResult = ApiObject.stringToDate("20160401T170000Z");
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        isoFormat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
        Date myDateExpected = new Date();
        myDateExpected = isoFormat.parse("20160401T170000Z");
        assertThat(myDateResult, is(equalTo(myDateExpected)));
    }
    
    @Test
    public void dateToStringValid() {
        Date myDateResult = ApiObject.stringToDate("20160401T170000Z");
        String myDateStrResult = ApiObject.dateToString(myDateResult);
        String myDateStrExpected = "20160401T170000Z";
        assertThat(myDateStrResult, is(myDateStrExpected));
    }
}