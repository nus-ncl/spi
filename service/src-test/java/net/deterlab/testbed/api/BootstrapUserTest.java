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
public class BootstrapUserTest {

    @Test
    public void getUid() {
        final BootstrapUser o = new BootstrapUser("demo", "password");
        assertThat(o.getUid(), is("demo"));
    }
    
    @Test
    public void setUid() {
        final BootstrapUser o = new BootstrapUser();
        o.setUid("demo");
        assertThat(o.getUid(), is("demo"));
    }
    
    @Test
    public void getPassword() {
        final BootstrapUser o = new BootstrapUser("demo", "password");
        assertThat(o.getPassword(), is("password"));
    }
    
    @Test
    public void setPassword() {
        final BootstrapUser o = new BootstrapUser();
        o.setPassword("password");
        assertThat(o.getPassword(), is("password"));
    }

}
