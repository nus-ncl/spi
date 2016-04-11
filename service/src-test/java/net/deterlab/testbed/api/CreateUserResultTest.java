package net.deterlab.testbed.api;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;


import static org.hamcrest.CoreMatchers.*;

/**
 * @author yeoteye
 *
 */

@RunWith(JMockit.class)
public class CreateUserResultTest {

    @Test
    public void getUid(@Mocked final byte[] identity) {
        final CreateUserResult o = new CreateUserResult("demo", identity);
        assertThat(o.getUid(), is("demo"));
    }
    
    @Test
    public void setUid() {
        final CreateUserResult o = new CreateUserResult();
        o.setUid("demo");
        assertThat(o.getUid(), is("demo"));
    }
    
    @Test
    public void getIdentity(@Mocked final byte[] identity) {
        final CreateUserResult o = new CreateUserResult("demo", identity);
        assertThat(o.getIdentity(), is(sameInstance(identity)));
    }

    @Test
    public void setIdentity(@Mocked final byte[] identity) {
        final CreateUserResult o = new CreateUserResult();
        o.setIdentity(identity);
        assertThat(o.getIdentity(), is(sameInstance(identity)));
    }

}
