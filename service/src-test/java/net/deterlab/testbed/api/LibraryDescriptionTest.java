package net.deterlab.testbed.api;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mocked;
import mockit.integration.junit4.JMockit;

/**
 * @author Tran
 *
 */

@RunWith(JMockit.class)
public class LibraryDescriptionTest {
	
	@Test
	public void defaultConstructorTest(){
		LibraryDescription ld=new LibraryDescription();
		
		String actual1=ld.getLibraryId();
		String actual2=ld.getOwner();
		int actual3=ld.getACL().length;
		int actual4=ld.getExperiments().length;
		int actual5=ld.getPerms() .length;
		
		assertEquals(null,actual1);
		assertEquals(null,actual2);
		assertEquals(0,actual3);
		assertEquals(0,actual4);
		assertEquals(0,actual5);
	}
	
	@Test
	public void parameterizedConstructorTest(){
		LibraryDescription ld=new LibraryDescription("expected1","expected2");
		
		String actual1=ld.getLibraryId();
		String actual2=ld.getOwner();
		int actual3=ld.getACL().length;
		int actual4=ld.getExperiments().length;
		int actual5=ld.getPerms() .length;
		
		assertEquals("expected1",actual1);
		assertEquals("expected2",actual2);
		assertEquals(0,actual3);
		assertEquals(0,actual4);
		assertEquals(0,actual5);
	}

	@Test
	public void setLibraryIdTest(){
		LibraryDescription ld=new LibraryDescription();
		ld.setLibraryId("expected1");
		
		String actual=ld.getLibraryId();
		
		assertEquals("expected1",actual);	
	}
	
	@Test
	public void setOwnerTest(){
		LibraryDescription ld=new LibraryDescription();
		ld.setOwner("expected1");
		
		String actual=ld.getOwner();
		
		assertEquals("expected1",actual);		
	}
	
	@Test
	public void setACLTest1(@Mocked List<AccessMember> am, @Mocked final AccessMember am1, @Mocked final AccessMember am2){
		LibraryDescription ld=new LibraryDescription();
		am.add(am1);
		am.add(am2);
		ld.setACL(am);
		
		AccessMember[] actual=ld.getACL();
		Object[] expected=am.toArray();
		
		assertArrayEquals(expected,actual);		
	}
	
	@Test
	public void setACLTest2(@Mocked AccessMember[] expected, @Mocked final AccessMember am1, @Mocked final AccessMember am2){
		LibraryDescription ld=new LibraryDescription();
		expected= new AccessMember[]{am1,am2};
		ld.setACL(expected);
		
		AccessMember[] actual=ld.getACL();
		
		assertArrayEquals(expected,actual);		
	}
	
	@Test
	public void addAccessTest(@Mocked final AccessMember am){
		LibraryDescription ld=new LibraryDescription();
		ld.addAccess(am);
		
		AccessMember[] actual=ld.getACL();
		AccessMember[] expected=new AccessMember[]{am};
		
		assertArrayEquals(expected,actual);	
	}
	
	@Test
	public void setExperimentsTest1(){
		LibraryDescription ld=new LibraryDescription();
		List<String> e=new ArrayList<String>();
		e.add("expected1");
		e.add("expected2");
		ld.setExperiments(e);
		
		String[] actual=ld.getExperiments();
		Object[] expected=e.toArray();
		
		assertArrayEquals(expected,actual);	
	}
	
	@Test
	public void setExperimentsTest2(){
		LibraryDescription ld=new LibraryDescription();
		String[] expected=new String[]{"expected1","expected2"};
		ld.setExperiments(expected);
		
		String[] actual=ld.getExperiments();
		
		assertArrayEquals(expected,actual);	
	}
	
	@Test
	public void addExperimentTest(){
		LibraryDescription ld=new LibraryDescription();
		String e="expected";
		ld.addExperiment(e);
		
		String[] actual=ld.getExperiments();
		String[] expected=new String[]{e};
		
		assertArrayEquals(expected,actual);	
	}
	
	@Test
	public void setPermsTest1(){
		LibraryDescription ld=new LibraryDescription();
		
		String[] expected=new String[]{"expected1","Test2"};
		ld.setPerms(expected);
		String[] actual=ld.getPerms();
		
		assertArrayEquals(expected,actual);	
	}
	
	@Test
	public void setPermsTest2(){
		LibraryDescription ld=new LibraryDescription();
		List<String> p=new ArrayList<String>();
		p.add("expected1");
		p.add("expected2");
		ld.setPerms(p);

		String[] tempactual=ld.getPerms();
		Set<String> expected = new HashSet<String>(p);
		Set<String> actual = new HashSet<String>(Arrays.asList(tempactual));
		
		assertEquals(expected,actual);	
	}
}
