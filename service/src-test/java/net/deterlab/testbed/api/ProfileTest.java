package net.deterlab.testbed.api;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mocked;
import mockit.integration.junit4.JMockit;

/**
 * @author Tran
 *
 */


@RunWith(JMockit.class)
public class ProfileTest {
	
	@Test
	public void defaultConstructorTest(){
		Profile profile=new Profile();
		
		String actual1=profile.getId();
		Attribute[] actual2=profile.getAttributes();
		
		assertEquals(null,actual1);
		assertArrayEquals(null,actual2);
	}
	
	@Test
	public void parameterizedConstructorTest1(@Mocked Attribute[] expected,@Mocked final Attribute a1,@Mocked final Attribute a2){
		expected=new Attribute[]{a1,a2};
		Profile profile=new Profile("expected",expected);
		
		String actual1=profile.getId();
		Attribute[] actual2=profile.getAttributes();
		
		assertEquals("expected",actual1);
		assertArrayEquals(expected,actual2);
	}
	
	@Test
	public void parameterizedConstructorTest2(){
		Profile profile=new Profile("expected");
		
		String actual1=profile.getId();
		Attribute[] actual2=profile.getAttributes();
		
		assertEquals("expected",actual1);
		assertArrayEquals(null,actual2);
	}
	
	
	@Test
	public void setterTest(@Mocked Attribute[] expected,@Mocked final Attribute a1,@Mocked final Attribute a2){
		expected=new Attribute[]{a1,a2};
		Profile profile=new Profile();
		
		profile.setId("expected");
		profile.setAttributes(expected);
		
		String actual1=profile.getId();
		Attribute[] actual2=profile.getAttributes();
		
		assertEquals("expected",actual1);
		assertArrayEquals(expected,actual2);
	}
}	
