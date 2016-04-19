package net.deterlab.testbed.user;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.NotificationFlag;
import net.deterlab.testbed.api.UserNotification;
import net.deterlab.testbed.db.SharedConnection;

/**
 * Created by Tran on 04/14/2016.
 */


@RunWith(JMockit.class)
public class NotificationStoreDBTest {
	@Rule
	public ExpectedException exception=ExpectedException.none();
	
	@Test
	public void defaultConstructorTest1() throws Exception{
		final SharedConnection sc = new MockUp<SharedConnection>() {
	        @Mock
	        public void open() throws DeterFault {
	        }

	        @Mock
	        public void close() throws DeterFault {
	            throw new DeterFault(1, "");
	        }
	    }.getMockInstance();
	    exception.expect(DeterFault.class);
		sc.close();
		NotificationStoreDB n=new NotificationStoreDB();
	}
	
	@Test
	public void defaultConstructorTest2() throws Exception{
		final SharedConnection sc = new MockUp<SharedConnection>() {
	        @Mock
	        public void open() throws DeterFault {
	        }

	        @Mock
	        public void close() throws DeterFault {
	            throw new DeterFault(1, "");
	        }
	    }.getMockInstance();
	    exception.expect(DeterFault.class);
		sc.close();
		NotificationStoreDB n=new NotificationStoreDB(sc);
	}
	
	@Test
	public void validateFlagTest1(@Mocked NotificationFlag flag) throws Exception{
		flag=null;
		exception.expect(DeterFault.class);	
		int actual=NotificationStoreDB.validateFlag(flag);				
	}
	
	@Test
	public void validateFlagTest2()throws Exception{
		final NotificationFlag flag = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return null;
	        }

	    }.getMockInstance();
		exception.expect(DeterFault.class);	
		int actual=NotificationStoreDB.validateFlag(flag);	
	}
	
	@Test
	public void validateFlagTest3() throws Exception{
		final NotificationFlag flag = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "read";
	        }
	        @Mock
	        public boolean getIsSet(){
	        	return true;
	       
	        }
	    }.getMockInstance();
		
		int actual=NotificationStoreDB.validateFlag(flag);
		assertEquals(1,actual);
	}
	
	@Test
	public void validateFlagTest4() throws Exception{
			final NotificationFlag flag = new MockUp<NotificationFlag>() {
		        @Mock
		        public String getTag() throws DeterFault {
		        	return "read";
		        }
		        @Mock
		        public boolean getIsSet(){
		        	return false;
		       
		        }
		    }.getMockInstance();
			
			int actual=NotificationStoreDB.validateFlag(flag);
			assertEquals(0,actual);
	}
	
	@Test
	public void validateFlagTest5() throws Exception{
		final NotificationFlag flag = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "urgent";
	        }
	        @Mock
	        public boolean getIsSet(){
	        	return true;
	       
	        }
	    }.getMockInstance();
		
		int actual=NotificationStoreDB.validateFlag(flag);
		assertEquals(2,actual);
	}
	
	@Test
	public void validateFlagTest6() throws Exception{
		final NotificationFlag flag = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "urgent";
	        }
	        @Mock
	        public boolean getIsSet(){
	        	return false;
	       
	        }
	    }.getMockInstance();
		
		int actual=NotificationStoreDB.validateFlag(flag);
		assertEquals(0,actual);
	}
	
	@Test
	public void validateFlagTest7() throws Exception{
		final NotificationFlag flag = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "other";
	        }
	       
	      
	    }.getMockInstance();
	    exception.expect(DeterFault.class);	
		
		int actual=NotificationStoreDB.validateFlag(flag);
		
	}
	
	@Test
	public void validateFlagsTest1(@Mocked NotificationFlag[] flags) throws Exception{
		final NotificationFlag flag = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "other";
	        }
		}.getMockInstance();
		
		flags=new NotificationFlag[]{flag};
		exception.expect(DeterFault.class);
		int actual=NotificationStoreDB.validateFlags(flags);
	}
	
	@Test
	public void validateFlagsTest2(@Mocked NotificationFlag[] flags) throws Exception{
		final NotificationFlag flag1 = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "read";
	        }
	        @Mock
	        public boolean getIsSet(){
	        	return true;
	       
	        }
	    }.getMockInstance();
	    
	    final NotificationFlag flag2 = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "urgent";
	        }
	        @Mock
	        public boolean getIsSet(){
	        	return true;
	       
	        }
	    }.getMockInstance();
	    
	    flags=new NotificationFlag[]{flag1,flag2};    
	    int actual=NotificationStoreDB.validateFlags(flags);   
	    assertEquals(3,actual);   
	}
	
	@Test
	public void intToFlagsTest()throws Exception{
		NotificationFlag flag1=new NotificationFlag("URGENT",false);
		NotificationFlag flag2=new NotificationFlag("READ",true);
		NotificationFlag[] expected=new NotificationFlag[2];
		expected[0]=flag1;
		expected[1]=flag2;
		
		NotificationFlag[] actual=NotificationStoreDB.intToFlags(1);	
		assertArrayEquals(expected,actual);
	}
	
	@Test
	public void makeMaskTest1() throws Exception{
		NotificationFlag flag=new NotificationFlag("other",true);
		exception.expect(DeterFault.class);
		int actual=NotificationStoreDB.makeMask(flag);
	}
	
	@Test
	public void makeMaskTest2() throws Exception{
		final NotificationFlag flag = new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "read";
	        }

	    }.getMockInstance();

		int actual=NotificationStoreDB.makeMask(flag);
		assertEquals(1,actual);
	}
	
	@Test
	public void makeMaskTest3() throws Exception{
		final NotificationFlag flag1= new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "other";
	        }
	    }.getMockInstance();
	    
	    NotificationFlag[] flags=new NotificationFlag[]{flag1};
	    exception.expect(DeterFault.class);
		int actual=NotificationStoreDB.makeMask(flags);
	}
	
	@Test
	public void makeMaskTest4() throws Exception{
		final NotificationFlag flag1= new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "URGENT";
	        }
	    }.getMockInstance();
		
	    final NotificationFlag flag2= new MockUp<NotificationFlag>() {
	        @Mock
	        public String getTag() throws DeterFault {
	        	return "READ";
	        }
	    }.getMockInstance();
	    
	    NotificationFlag[] flags=new NotificationFlag[]{flag1,flag2};
		int actual=NotificationStoreDB.makeMask(flags);
		assertEquals(3,actual);
	}
	
	@Test
	public void createTest1(@Mocked final SharedConnection sc)throws Exception{
		new MockUp<ResultSet>(){
			@Mock
			public boolean next(){
				return false;
			}
		};
		exception.expect(DeterFault.class);
		NotificationStoreDB n= new NotificationStoreDB(sc);
		long actual=n.create("test");
	}
	
	@Test
	public void createTest2(@Mocked final SharedConnection sc)throws Exception{
		new MockUp<ResultSet>(){
			@Mock
			public boolean next(){
				return true;
			}
		};
		exception.expect(DeterFault.class);
		NotificationStoreDB n= new NotificationStoreDB(sc);
		long actual=n.create("test");
	}
	
	//to do
	public void createTest3(@Mocked final SharedConnection sc)throws Exception{
		
		new MockUp<ResultSet>(){
			boolean current;
			@Mock
			public long getLong(int para){
				return 1;
			}

		};
		new MockUp<PreparedStatement>(){
			@Mock
			public ResultSet getGeneratedKeys(@Mocked ResultSet r){ 
				r=null;
				return r;
			}
		};
		
		NotificationStoreDB n= new NotificationStoreDB(sc);
		long actual1=n.create("test1");
		
		assertEquals(1,actual1);

	}
	
	@Test
	public void deliverTest1(@Mocked final SharedConnection sc)throws Exception{
		final Set<String> users = new HashSet<String>();
		users.add("user1");
		int f=1;
		long id=-1;
		NotificationStoreDB n= new NotificationStoreDB(sc);
		exception.expect(DeterFault.class);
		n.deliver(users,f,id);
	}
	
	@Test
	public void getTest(@Mocked final SharedConnection sc) throws Exception{
		NotificationStoreDB n= new NotificationStoreDB(sc);
		String uid=null;
		String from="test1";
		String to="test2";
		int flags=1;
		int mask=1;
		List<UserNotification> actual=n.get(uid, from, to, flags, mask);
		assertEquals(0,actual.size());
	}
	
	
}

	