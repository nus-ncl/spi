package net.deterlab.testbed.api;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mocked;
import mockit.integration.junit4.JMockit;

/**
 * @author Tran
 *
 */

@RunWith(JMockit.class)
public class RealizationDescriptionTest {
	
	@Test
	public void defaultConstructorTest(){
		RealizationDescription rd= new RealizationDescription();
		
		String actual1=rd.getName();
		String actual2=rd.getCircle();
		String actual3=rd.getExperiment();
		String actual4=rd.getStatus();
		int actual5=rd.getContainment().length ;
		int actual6=rd.getMapping().length;
		int actual7=rd.getACL().length;
		int actual8=rd.getPerms().length;
		
		assertEquals(null,actual1);
		assertEquals(null,actual2);
		assertEquals(null,actual3);
		assertEquals("Empty",actual4);
		assertEquals(0,actual5);
		assertEquals(0,actual6);
		assertEquals(0,actual7);
		assertEquals(0,actual8);
	}
	
	@Test
	public void getandsetNameTest(){	
		RealizationDescription rd= new RealizationDescription();
		rd.setName("expected");
		
		String actual=rd.getName();
		assertEquals("expected",actual);
	}
	
	@Test
	public void getandsetCircleTest(){	
		RealizationDescription rd= new RealizationDescription();
		rd.setCircle("expected");
		
		String actual=rd.getCircle();
		assertEquals("expected",actual);
	}
	
	@Test
	public void getandsetExperiementTest(){	
		RealizationDescription rd= new RealizationDescription();
		rd.setExperiment("expected");
		
		String actual=rd.getExperiment();
		assertEquals("expected",actual);
	}
	
	@Test
	public void getandsetStatusTest(){	
		RealizationDescription rd= new RealizationDescription();
		
		rd.setStatus("expected");
		String actual=rd.getStatus();
		
		assertEquals("expected",actual);
	}
	
	@Test
	public void getandsetContainmentTest1(@Mocked RealizationContainment[] expected,@Mocked final RealizationContainment rc1,@Mocked final RealizationContainment rc2){	
		RealizationDescription rd= new RealizationDescription();
		expected=new RealizationContainment[]{rc1,rc2};
		
		rd.setContainment(expected);
		RealizationContainment[] actual=rd.getContainment();
		
		assertArrayEquals(expected,actual);
	}
	
	@Test
	public void getandsetContainmentTest2(@Mocked List<RealizationContainment>rc,@Mocked final RealizationContainment rc1,@Mocked final RealizationContainment rc2){	
		RealizationDescription rd= new RealizationDescription();
		rc.add(rc1);
		rc.add(rc2);
		
		Object[] expected=rc.toArray();
		RealizationContainment[] actual=rd.getContainment();
		
		assertArrayEquals(expected,actual);
	}
	
	@Test
	public void getandsetContainmentTest3(){	
		RealizationDescription rd= new RealizationDescription();
		Map<String, Set<String>> m=new HashMap<String, Set<String>>();
		Set<String> s1=new HashSet<String>();
		s1.add("expected1b");
		m.put("expected1a", s1);
		Set<String> s2=new HashSet<String>();
		s2.add("expected2b");
		m.put("expected2a", s2);
		
		rd.setContainment(m);
		RealizationContainment[] actual=rd.getContainment();
		

		RealizationContainment myR1 = new RealizationContainment("expected1a", "expected1b");
		RealizationContainment myR2 = new RealizationContainment("expected2a", "expected2b");
		RealizationContainment[] expected =new RealizationContainment[]{myR1, myR2};
		
		assertArrayEquals(expected,actual);
	}
	
	@Test
	public void getandsetMappingTest1(@Mocked RealizationMap[] rm,@Mocked final RealizationMap rm1,@Mocked final RealizationMap rm2){	
		RealizationDescription rd= new RealizationDescription();
		rm=new RealizationMap[]{rm1,rm2};

		rd.setMapping(rm);
		RealizationMap[] actual=rd.getMapping();
		
		assertArrayEquals(rm,actual);
	}
	
	@Test
	public void getandsetMappingTest2(@Mocked List<RealizationMap> rm,@Mocked final RealizationMap rm1, @Mocked final RealizationMap rm2){	
		RealizationDescription rd= new RealizationDescription();
		rm=new ArrayList<RealizationMap>();
		rm.add(rm1);
		rm.add(rm2);
		
		Object[] expected=rm.toArray();
		rd.setMapping(rm);
		RealizationMap[] actual=rd.getMapping();
		
		assertArrayEquals(expected,actual);
	}
	
	@Test
	public void getandsetMappingTest3(){	
		RealizationDescription rd= new RealizationDescription();
		
		Map<String, Set<String>> m=new HashMap<String, Set<String>>();
		Set<String> s1=new HashSet<String>();
		s1.add("expected1b");
		m.put("expected1a", s1);
		Set<String> s2=new HashSet<String>();
		s2.add("expected2b");
		m.put("expected2a", s2 );
		
		rd.setMapping(m);	
		RealizationMap[] actual=rd.getMapping();
		
		RealizationMap myR1 = new RealizationMap("expected1a", "expected1b");
		RealizationMap myR2 = new RealizationMap("expected2a", "expected2b");
		RealizationMap[] expected =new RealizationMap[]{myR1, myR2};
	
		assertArrayEquals(expected,actual);
	}
	
	@Test
	public void getandsetACLTest1(@Mocked List<AccessMember> am,@Mocked final AccessMember am1,@Mocked final AccessMember am2){	
		RealizationDescription rd= new RealizationDescription();
		am.add(am1);
		am.add(am2);
		
		rd.setACL(am);
		Object[] expected=am.toArray();
		AccessMember[] actual=rd.getACL();
		
		assertArrayEquals(expected,actual);
	}

	@Test
	public void getandsetACLTest2(@Mocked AccessMember[] am, @Mocked final AccessMember am1,@Mocked final AccessMember am2){	
		RealizationDescription rd= new RealizationDescription();
		am=new AccessMember[]{am1,am2};
		
		rd.setACL(am);
		AccessMember[] actual=rd.getACL();
		
		assertArrayEquals(am,actual);
	}
	
	@Test
	public void addAccessTest(@Mocked final AccessMember am){
		RealizationDescription rd= new RealizationDescription();
		rd.addAccess(am);
		
		AccessMember[] expected=new AccessMember[]{am};
		AccessMember[] actual=rd.getACL();
		
		assertArrayEquals(expected,actual);		
	}
	
	@Test
	public void getterandsetterPermsTest1(){
		RealizationDescription rd= new RealizationDescription();
		String[] p=new String[]{"expected1","expected2"};
		
		rd.setPerms(p);
		String[] actual=rd.getPerms();
		
		assertArrayEquals(p,actual);
	}
	
	@Test
	public void getterandsetterPermsTest2(){
		RealizationDescription rd= new RealizationDescription();
		List<String> p=new ArrayList<String>();
		p.add("expected1");
		p.add("expected2");
		
		rd.setPerms(p);
		
		Object[] expected=p.toArray();
		String[] actual=rd.getPerms();
		
		assertArrayEquals(expected,actual);
	}
}
