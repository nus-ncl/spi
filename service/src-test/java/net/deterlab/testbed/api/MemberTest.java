package net.deterlab.testbed.api;

import net.deterlab.testbed.api.Member;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.integration.junit4.JMockit;

/**
 * @author Tran
 *
 */

@RunWith(JMockit.class)
public class MemberTest {
	
	
	@Test
	public void defaultConstructorTest(){
		Member member=new Member();
		
		String actual1=member.getUid();
		String[] actual2=member.getPermissions();
		
		assertEquals(null,actual1);
		assertArrayEquals(null,actual2);	
	}
	
	@Test
	public void parameterizedConstructorTest(){
		String[] perms={"test1","test2","test3"};
		Member member =new Member("test",perms);
		
		String actual1=member.getUid();
		String[] actual2=member.getPermissions();
		
		assertArrayEquals(perms,actual2);
		assertEquals("test",actual1);
	}

	@Test
	public void setterTest() {
		Member member=new Member();
		String[] perms={"test1","test2","test3"};
		
		
		member.setUid("test");
		member.setPermissions(perms);
		
		String actual1=member.getUid();
		String[] actual2=member.getPermissions();
		
		assertEquals("test",actual1);
		assertArrayEquals(perms,actual2);
	}	
}
