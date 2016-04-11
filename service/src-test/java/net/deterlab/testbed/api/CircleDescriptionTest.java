package net.deterlab.testbed.api;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;

@RunWith(JMockit.class)
public class CircleDescriptionTest {

    @Test
    public void getCircleId() {
        final CircleDescription o = new CircleDescription("demo", "owner_name");
        assertThat(o.getCircleId(), is("demo"));
    }
    
    @Test
    public void setCircleId() {
        final CircleDescription o = new CircleDescription();
        o.setCircleId("demo");
        assertThat(o.getCircleId(), is("demo"));
    }
    
    @Test
    public void getOwner() {
        final CircleDescription o = new CircleDescription("demo", "owner_name");
        assertThat(o.getOwner(), is("owner_name"));
    }
    
    @Test
    public void setOwner() {
        final CircleDescription o = new CircleDescription();
        o.setOwner("owner_name");
        assertThat(o.getOwner(), is("owner_name"));
    }
    
    @Test
    public void getMembersEmpty() {
        final CircleDescription o = new CircleDescription();
        Member[] mArray = {};
        assertThat(o.getMembers(), is(mArray));
    }
    
    @Test
    public void getMembersValid() {
        final CircleDescription o = new CircleDescription();
        String[] perms = {"READ_WRITE"};
        Member m = new Member("demo", perms);
        Member[] mArray = {m};
        o.addMember(m);
        assertThat(o.getMembers(), is(equalTo(mArray)));
    }
    
    @Test
    public void setMembersArrayEmpty(@Mocked final Member[] m) {
        final CircleDescription o = new CircleDescription();
        Member[] expected = {};
        o.setMembers(expected);
        assertThat(o.getMembers(), is(expected));
    }
    
    @Test
    public void setMembersArrayValid() {
        final CircleDescription o = new CircleDescription();
        String[] perms = {"READ_WRITE"};
        Member m = new Member("demo", perms);
        Member[] mArray = {m};
        o.setMembers(mArray);
        assertThat(o.getMembers(), is(mArray));
        
    }
    
    @Test
    public void setMembersList(@Mocked final List<Member> m) {
        final CircleDescription o = new CircleDescription();
        o.setMembers(m);
        assertThat(o.getMembers(), is(m.toArray()));
    }
    
    @Test
    public void addMember() {
        final CircleDescription o = new CircleDescription();
        String[] perms = {"READ_WRITE"};
        Member m = new Member("demo", perms);
        Member[] mArray = {m};
        o.addMember(m);
        assertThat(o.getMembers(), is(equalTo(mArray)));
    }

}
