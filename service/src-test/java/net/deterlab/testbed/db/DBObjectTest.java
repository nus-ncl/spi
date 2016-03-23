package net.deterlab.testbed.db;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import net.deterlab.testbed.api.DeterFault;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Created by chris on 3/22/2016.
 */
@RunWith(JMockit.class)
public class DBObjectTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void checkScopedName1(@Mocked final SharedConnection sharedConnection) throws Exception {
        final DBObject o = new DBObject(sharedConnection);

        DBObject.checkScopedName("a:a");
    }

    @Test
    public void checkScopedName2(@Mocked final SharedConnection sharedConnection) throws Exception {
        final DBObject o = new DBObject(sharedConnection);

        exception.expect(DeterFault.class);

        DBObject.checkScopedName(null);
    }

    @Test
    public void checkScopedName3(@Mocked final SharedConnection sharedConnection) throws Exception {
        final DBObject o = new DBObject(sharedConnection);

        exception.expect(DeterFault.class);

        DBObject.checkScopedName("");
    }

    @Test
    public void close1(@Mocked final SharedConnection sharedConnection) throws Exception {
        final DBObject o = new DBObject(sharedConnection);

        o.close();
    }

    @Test
    public void close2() throws Exception {
        final SharedConnection sharedConnection = new MockUp<SharedConnection>() {
            @Mock
            void open() throws DeterFault {
            }

            @Mock
            void close() throws DeterFault {
                throw new DeterFault(1, "");
            }
        }.getMockInstance();
        final DBObject o = new DBObject(sharedConnection);

        exception.expect(DeterFault.class);

        o.close();
    }

    @Test
    public void close3(@Mocked final SharedConnection sharedConnection) throws Exception {
        final DBObject o = new DBObject(sharedConnection);

        o.close();
        o.close();
    }

    @Test
    public void forceClose1(@Mocked final SharedConnection sharedConnection) throws Exception {
        final DBObject o = new DBObject(sharedConnection);

        o.forceClose();
    }

    @Test
    public void forceClose2() throws Exception {
        final SharedConnection sharedConnection = new MockUp<SharedConnection>() {
            @Mock
            void open() throws DeterFault {
            }

            @Mock
            void close() throws DeterFault {
                throw new DeterFault(1, "");
            }
        }.getMockInstance();
        final DBObject o = new DBObject(sharedConnection);

        o.forceClose();
    }

    @Test
    public void getPreparedStatement() throws Exception {
        // TODO
    }

    @Test
    public void getPreparedStatement1() throws Exception {
        // TODO 
    }

    @Test
    public void getSharedConnection(@Mocked final SharedConnection sharedConnection) throws Exception {
        final DBObject o = new DBObject(sharedConnection);

        assertThat(o.getSharedConnection(), is(sameInstance(sharedConnection)));
    }

}