package net.deterlab.testbed.api;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.integration.junit4.JMockit;

/**
 * @author Tran
 *
 */

@RunWith(JMockit.class)
public class NotificationFlagTest {
	
	@Test
	public void defaultConstructorTest(){
		NotificationFlag nf=new NotificationFlag();
		
		String actual1=nf.getTag();
		boolean actual2=nf.getIsSet();
		
		assertEquals(null,actual1);
		assertFalse(actual2);
	}

	@Test
	public void parameterizedConstructorTest(){
		NotificationFlag nf=new NotificationFlag("expected1",true);
			
		String actual1=nf.getTag();
		boolean actual2=nf.getIsSet();
		
		assertEquals("expected1",actual1);
		assertTrue(actual2);
	}
	
	@Test
	public void setTagTest(){
		NotificationFlag nf=new NotificationFlag();	
		nf.setTag("expected1");
		
		String actual=nf.getTag();
		
		assertEquals("expected1",actual);
	}
	
	@Test
	public void setIsSetTest(){
		NotificationFlag nf=new NotificationFlag();	
		nf.setIsSet(true);
		
		boolean actual=nf.getIsSet();
		
		assertTrue(actual);
	}
}
