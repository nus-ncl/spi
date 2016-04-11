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
public class ChangeAttributeTest {
    
    @Test
    public void setName() {
        String attributeName = "email";
        final ChangeAttribute o = new ChangeAttribute();
        o.setName(attributeName);
        assertThat(o.getName(), is(attributeName));
    }
    
    
    @Test
    public void setValue() {
        String attributeValue = "abc@example.com";
        final ChangeAttribute o = new ChangeAttribute();
        o.setValue(attributeValue);
        assertThat(o.getValue(), is(attributeValue));
    }
    
    @Test
    public void setDelete() {
        boolean deleteFlag = false;
        final ChangeAttribute o = new ChangeAttribute();
        o.setDelete(deleteFlag);
        assertThat(o.getDelete(), is(deleteFlag));
    }

}
