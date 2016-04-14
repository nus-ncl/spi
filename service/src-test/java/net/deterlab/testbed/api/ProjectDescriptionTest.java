package net.deterlab.testbed.api;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mocked;
import mockit.integration.junit4.JMockit;

/**
 * @author Tran
 *
 */


@RunWith(JMockit.class)
public class ProjectDescriptionTest {
	
	@Test
	public void defaultConstructorTest(){
		ProjectDescription pd=new ProjectDescription();
		
		String actual1=pd.getProjectId();
		String actual2=pd.getOwner();
		boolean actual3=pd.getApproved();
		int actual4=pd.getMembers().length;
		
		assertEquals(null,actual1);
		assertEquals(null,actual2);
		assertFalse(actual3);
		assertEquals(0,actual4);
	}
	
	@Test
	public void parameterizedConstructorTest(){
		ProjectDescription pd=new ProjectDescription("expected1","expected2",true);
		
		String actual1=pd.getProjectId();
		String actual2=pd.getOwner();
		boolean actual3=pd.getApproved();
		int actual4=pd.getMembers().length;
		
		assertEquals("expected1",actual1);
		assertEquals("expected2",actual2);
		assertTrue(actual3);
		assertEquals(0,actual4);
	}
	
	@Test
	public void setterTest(){
		ProjectDescription pd=new ProjectDescription();
		
		pd.setProjectId("expected1");
		pd.setOwner("expected2");
		pd.setApproved(true);
		
		String actual1=pd.getProjectId();
		String actual2=pd.getOwner();
		boolean actual3=pd.getApproved();
	
		assertEquals("expected1",actual1);
		assertEquals("expected2",actual2);
		assertTrue(actual3);
	}
	
	
	@Test	
	public void setMemberTest1(@Mocked List<Member> m, @Mocked final Member m1,@Mocked final Member m2 ){
		ProjectDescription pd=new ProjectDescription();
		m.add(m1);
		m.add(m2);
		
		pd.setMembers(m);
		Member[] actual=pd.getMembers();
		Object[] expected=m.toArray();
		
		assertArrayEquals(expected,actual);
	}
	
	@Test	
	public void setMemberTest2(@Mocked Member[] expected,@Mocked final Member m1,@Mocked final Member m2){
		ProjectDescription pd=new ProjectDescription();
		expected=new Member[]{m1,m2};
		
		pd.setMembers(expected);
		Member[] actual=pd.getMembers();
		
		assertArrayEquals(expected,actual);
	}
	
	@Test
	public void addMemberTest(@Mocked final Member m){
		ProjectDescription pd=new ProjectDescription();
		pd.addMember(m);
		
		Member[] actual=pd.getMembers();
		Member[] expected=new Member[]{m};
		
		assertArrayEquals(expected,actual);
	}
}
