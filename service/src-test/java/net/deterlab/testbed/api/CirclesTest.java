package net.deterlab.testbed.api;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import net.deterlab.testbed.db.SharedConnection;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * @author yeoteye
 *
 */

@RunWith(JMockit.class)
public class CirclesTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void setLogger() {
        // TBD
    }
    
    @Test
    public void createCircleAttribute() {
        final Circles o = new Circles();
    }
    
    @Test
    public void modifyCircleAttribute() {
    }
    
    @Test
    public void removeCircleAttribute() {
    }
    
    @Test
    public void getValidPermissions() {
    }
    
    @Test
    public void createCircleNullCircleID(@Mocked final Attribute[] attributeArray) throws Exception {
        final Circles o = new Circles();
        // circleid is required
        exception.expect(DeterFault.class);
        o.createCircle(null, null, attributeArray);
    }
    
    @Test
    public void createCircleProfileDBDeterFault() {
    }
    
    @Test
    public void createCircleThrowFaultSameCircleName() {
        
    }

}
