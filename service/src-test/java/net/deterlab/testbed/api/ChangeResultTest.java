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

import static org.hamcrest.CoreMatchers.*;

@RunWith(JMockit.class)
public class ChangeResultTest {
    
    @Test
    public void setName() {
        final ChangeResult o = new ChangeResult();
        String expected = "full name";
        o.setName(expected);
        assertThat(o.getName(), is(expected));
    }
    
    @Test
    public void setReason() {
        final ChangeResult o = new ChangeResult();
        String expected = "fail due to wrong attribute name";
        o.setReason(expected);
        assertThat(o.getReason(), is(expected));
    }
    
    @Test
    public void setSuccess() {
        final ChangeResult o = new ChangeResult();
        boolean expected = false;
        o.setSuccess(expected);
        assertThat(o.getSuccess(), is(expected));
    }
    
    @Test
    public void getName() {
        final ChangeResult o = new ChangeResult("full name", "fail due to wrong attribute name", true);
        assertThat(o.getName(), is("full name"));
    }
    
    @Test
    public void getReason() {
        final ChangeResult o = new ChangeResult("full name", "fail due to wrong attribute name", true);
        assertThat(o.getReason(), is("fail due to wrong attribute name"));
    }
    
    @Test
    public void getSuccess() {
        final ChangeResult o = new ChangeResult("full name", "fail due to wrong attribute name", true);
        assertThat(o.getSuccess(), is(true));
    }
}