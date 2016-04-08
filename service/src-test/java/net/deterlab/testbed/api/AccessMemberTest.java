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
public class AccessMemberTest {
    
    @Test
    public void setCircleId() {
        final AccessMember o = new AccessMember();
        o.setCircleId("demo");
        String result = o.getCircleId();
        String expected = "demo";
        assertThat(result, is(expected));
    }
    
    @Test
    public void setPermissions() {
        final AccessMember o = new AccessMember();
        String[] expected = {"read", "write"};
        o.setPermissions(expected);
        String[] result = o.getPermissions();
        assertThat(result, is(expected));
    }
    
    @Test
    public void getCircleId() {
        String[] perms = {"read", "write"};
        final AccessMember o = new AccessMember("demoCircleId", perms);
        assertThat(o.getCircleId(), is("demoCircleId"));
    }
    
    @Test
    public void getPermissions() {
        String[] perms = {"read", "write"};
        final AccessMember o = new AccessMember("demoCircleId", perms);
        assertThat(o.getPermissions(), is(perms));
    }
}