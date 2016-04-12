package net.deterlab.testbed.api;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import net.deterlab.testbed.db.SharedConnection;

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

import org.junit.Test;

public class ExperimentAspectTest {

    @Test
    public void setType() {
        String type = "layout";
        final ExperimentAspect o = new ExperimentAspect();
        o.setType(type);
        assertThat(o.getType(), is(type));
    }
    
    @Test
    public void setSubType() {
        String subType = "subtype";
        final ExperimentAspect o = new ExperimentAspect();
        o.setSubType(subType);
        assertThat(o.getSubType(), is(subType));
    }
    
    @Test
    public void setName() {
        String name = "name";
        final ExperimentAspect o = new ExperimentAspect();
        o.setName(name);
        assertThat(o.getName(), is(name));
    }
    
    @Test
    public void setData(@Mocked final byte[] data) {
        final ExperimentAspect o = new ExperimentAspect();
        o.setData(data);
        assertThat(o.getData(), is(data));
    }
    
    @Test
    public void setDataReference() {
        String dataRef = "data reference";
        final ExperimentAspect o = new ExperimentAspect();
        o.setDataReference(dataRef);
        assertThat(o.getDataReference(), is(dataRef));
    }
    
}
