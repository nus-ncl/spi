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
public class RealizationContainmentTest {
	
	@Test
	public void defaultConstructorTest(){
		RealizationContainment rc=new RealizationContainment();
		
		String actual1=rc.getOuter();
		String actual2=rc.getInner();
			
		assertEquals(null,actual1);
		assertEquals(null,actual2);
	}
	
	@Test
	public void parameterizedConstructorTest(){
		RealizationContainment rc=new RealizationContainment("expected1","expected2");
		
		String actual1=rc.getOuter();
		String actual2=rc.getInner();
		
		assertEquals("expected1",actual1);
		assertEquals("expected2",actual2);
	}

	@Test
	public void setterTest(){
		RealizationContainment rc=new RealizationContainment();
		rc.setOuter("expected1");
		rc.setInner("expected2");
		
		String actual1=rc.getOuter();
		String actual2=rc.getInner();
		
		assertEquals("expected1",actual1);
		assertEquals("expected2",actual2);
	}
}