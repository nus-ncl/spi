package net.deterlab.testbed.user;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.db.SharedConnection;

/**
 * @author Tran
 *
 */


@RunWith(JMockit.class)
public class UserChallengeDBTest {
	
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @Test
    public void defaultConstructorTest1a(@Mocked SharedConnection sc) throws Exception{
    	UserChallengeDB u= new UserChallengeDB();
    	
    	String user=u.getUid();
    	String type=u.getType();
    	byte[] data=u.getData();
    	String validity=u.getValidity();
    	long challenge  =u.getChallengeID();
    	
    	assertEquals(null,user);
    	assertEquals(null,type);
    	assertEquals(null,data);
    	assertEquals(null,validity);
    	assertEquals(-1,challenge);
    }

    @Test
    public void defaultConstructorTest1b() throws Exception{
    final SharedConnection sc = new MockUp<SharedConnection>() {
        @Mock
        void open() throws DeterFault {
        }

        @Mock
        void close() throws DeterFault {
            throw new DeterFault(1, "");
        }
    }.getMockInstance();
    
    exception.expect(DeterFault.class);
    sc.close(); 
    UserChallengeDB u= new UserChallengeDB();  
    }
    
        @Test
        public void defaultConstructorTest2a(@Mocked final SharedConnection sc) throws Exception{
        	UserChallengeDB u= new UserChallengeDB(sc);
        	
        	String user=u.getUid();
        	String type=u.getType();
        	byte[] data=u.getData();
        	String validity=u.getValidity();
        	long challenge  =u.getChallengeID();
        	
        	assertEquals(null,user);
        	assertEquals(null,type);
        	assertEquals(null,data);
        	assertEquals(null,validity);
        	assertEquals(-1,challenge);
        }
        
        @Test
        public void defaultConstructorTest2b() throws Exception{
            final SharedConnection sc = new MockUp<SharedConnection>() {
                @Mock
                void open() throws DeterFault {
                }

                @Mock
                void close() throws DeterFault {
                    throw new DeterFault(1, "");
                }
            }.getMockInstance();
            
            exception.expect(DeterFault.class);
            sc.close(); 
            UserChallengeDB u= new UserChallengeDB(sc);  
        }
        
        @Test
        public void parameterizedConstructorTest1a(@Mocked final SharedConnection sc) throws Exception{
        	byte[] expected= new byte[]{1,2};
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,"expected3",12);
        	
        	String user=u.getUid();
        	String type=u.getType();
        	byte[] data=u.getData();
        	String validity=u.getValidity();
        	long challenge  =u.getChallengeID();
        	
        	assertEquals("expected1",user);
        	assertEquals("expected2",type);
        	assertEquals(expected,data);
        	assertEquals("expected3",validity);
        	assertEquals(12,challenge);
        }
        
        public void parameterizedConstructorTest1b() throws Exception{
        	final SharedConnection sc = new MockUp<SharedConnection>() {
                @Mock
                void open() throws DeterFault {
                }

                @Mock
                void close() throws DeterFault {
                    throw new DeterFault(1, "");
                }
            }.getMockInstance();
            
            exception.expect(DeterFault.class);
            sc.close(); 
        	byte[] expected= new byte[]{1,2};
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,"expected3",12);
        }
        
        @Test
        public void parameterizedConstructorTest2a(@Mocked final SharedConnection sc) throws Exception{
        	byte[] expected= new byte[]{1,2};
        	
        	SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        	dateformat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
        	Date date = dateformat.parse("20160411T170000Z");
        			    
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,date,12);  	
        	
        	String user=u.getUid();
        	String type=u.getType();
        	byte[] data=u.getData();
        	String validity=u.getValidity();
        	long challenge  =u.getChallengeID();
        	
        	String dateexpected=dateformat.format(date);
        	
        	assertEquals("expected1",user);
        	assertEquals("expected2",type);
        	assertEquals(expected,data);
        	assertEquals(dateexpected,validity);
        	assertEquals(12,challenge);
        }
        
        @Test
        public void parameterizedConstructorTest2b() throws Exception{
        	final SharedConnection sc = new MockUp<SharedConnection>() {
                @Mock
                void open() throws DeterFault {
                }

                @Mock
                void close() throws DeterFault {
                    throw new DeterFault(1, "");
                }
            }.getMockInstance();
            
            exception.expect(DeterFault.class);
            sc.close(); 
            
        	byte[] expected= new byte[]{1,2};
        	SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        	Date date = dateformat.parse("20160411T170000Z");
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,date,12);
        }
        
        @Test
        public void parameterizedConstructorTest3a(@Mocked final SharedConnection sc ) throws Exception{
        	byte[] expected= new byte[]{1,2};
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,"expected3",12,sc);
 
        	String user=u.getUid();
        	String type=u.getType();
        	byte[] data=u.getData();
        	String validity=u.getValidity();
        	long challenge  =u.getChallengeID();
        	
        	assertEquals("expected1",user);
        	assertEquals("expected2",type);
        	assertEquals(expected,data);
        	assertEquals("expected3",validity);
        	assertEquals(12,challenge);
        }
        
        @Test
        public void parameterizedConstructorTest3b() throws Exception{
        	final SharedConnection sc = new MockUp<SharedConnection>() {
                @Mock
                void open() throws DeterFault {
                }

                @Mock
                void close() throws DeterFault {
                    throw new DeterFault(1, "");
                }
            }.getMockInstance();
            
            exception.expect(DeterFault.class);
            sc.close(); 
            byte[] expected= new byte[]{1,2};
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,"expected3",12,sc);
        }
        
        @Test
        public void parameterizedConstructorTest4a(@Mocked final SharedConnection sc ) throws Exception{
        	byte[] expected= new byte[]{1,2};
        	SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        	dateformat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
        	Date date = dateformat.parse("20160411T170000Z");
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,date,12,sc);
 
        	String user=u.getUid();
        	String type=u.getType();
        	byte[] data=u.getData();
        	String validity=u.getValidity();
        	long challenge  =u.getChallengeID();
        	
        	String dateexpected=dateformat.format(date);
        	
        	assertEquals("expected1",user);
        	assertEquals("expected2",type);
        	assertEquals(expected,data);
        	assertEquals(dateexpected,validity);
        	assertEquals(12,challenge);
        }
        
        @Test
        public void parameterizedConstructorTest4b() throws Exception{
        	final SharedConnection sc = new MockUp<SharedConnection>() {
                @Mock
                void open() throws DeterFault {
                }

                @Mock
                void close() throws DeterFault {
                    throw new DeterFault(1, "");
                }
            }.getMockInstance();
            
            exception.expect(DeterFault.class);
            sc.close();
        	
        	byte[] expected= new byte[]{1,2};
        	SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        	Date date = dateformat.parse("20160411T170000Z");
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,date,12,sc);
        }
              
        @Test
        public void parameterizedConstructorTest5a(@Mocked final SharedConnection sc ) throws Exception{
        	byte[] expected= new byte[]{1,2};
        	SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        	dateformat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
        	Date date = dateformat.parse("20160411T170000Z");
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,date,sc);
 
        	String user=u.getUid();
        	String type=u.getType();
        	byte[] data=u.getData();
        	String validity=u.getValidity();
        	long challenge  =u.getChallengeID();
        	
        	String dateexpected=dateformat.format(date);
        	
        	assertEquals("expected1",user);
        	assertEquals("expected2",type);
        	assertEquals(expected,data);
        	assertEquals(dateexpected,validity);
        	assertEquals(-1,challenge);
        }
        
        @Test
        public void parameterizedConstructorTest5b() throws Exception{
        	final SharedConnection sc = new MockUp<SharedConnection>() {
                @Mock
                void open() throws DeterFault {
                }

                @Mock
                void close() throws DeterFault {
                    throw new DeterFault(1, "");
                }
            }.getMockInstance();
            
            exception.expect(DeterFault.class);
            sc.close();
        	
        	byte[] expected= new byte[]{1,2};
        	SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        	Date date = dateformat.parse("20160411T170000Z");
        	UserChallengeDB u= new UserChallengeDB("expected1","expected2",expected,date,sc);
        }
        
        @Test
        public void setTypeTest(@Mocked final SharedConnection sc) throws Exception{
        	UserChallengeDB u= new UserChallengeDB(sc);
        	u.setType("expected");
        	
        	String expected=u.getType();
        	
        	assertEquals("expected",expected);
        }
        
        @Test
        public void setDataTest(@Mocked final SharedConnection sc) throws Exception{
        	UserChallengeDB u= new UserChallengeDB(sc);
        	byte[] expected= new byte[]{1,2};
        	u.setData(expected);
        	
        	byte[] actual=u.getData();
        	
        	assertArrayEquals(expected,actual);
        }

        @Test
        public void setValidityTest(@Mocked final SharedConnection sc) throws Exception{
        	UserChallengeDB u= new UserChallengeDB(sc);
        	u.setValidity("expected");
        	
        	String actual=u.getValidity();
        	
        	assertEquals("expected",actual);
        }
        
        @Test
        public void setChallengeIDTest(@Mocked final SharedConnection sc) throws Exception{
        	UserChallengeDB u= new UserChallengeDB(sc);
        	u.setChallengeID(12);
        	
        	long actual=u.getChallengeID();
        	
        	assertEquals(12,actual);
        }
        
        @Test
        public void setUidTest(@Mocked final SharedConnection sc) throws Exception{
        	UserChallengeDB u= new UserChallengeDB(sc);
        	u.setUid("expected");
        	
        	String actual=u.getUid();
        	
        	assertEquals("expected",actual);
        }
        
        @Test
        public void saveTest1(@Mocked final SharedConnection sc)throws Exception{
        	UserChallengeDB u= new UserChallengeDB(sc);
        	u.save();
        	String actual=u.getValidity();
        	assertNotNull(actual);  	   	
        }
        
        @Test
        public void saveTest2()throws Exception{
        	
        	final SharedConnection sc = new MockUp<SharedConnection>() {
                @Mock
                void open() throws DeterFault {
                }

                @Mock
                void close() throws DeterFault {
                    throw new DeterFault(1, "");
                }
            }.getMockInstance();
            
            exception.expect(DeterFault.class);
            sc.close();
            UserChallengeDB u= new UserChallengeDB(sc);
            u.save();
        }
        
        @Test
        public void loadTest1(@Mocked final SharedConnection sc)throws Exception{
        	UserChallengeDB u= new UserChallengeDB(sc);
        	exception.expect(DeterFault.class);
        	long id=u.getChallengeID();
        	u.load(id); 		
        }
        
        
}
