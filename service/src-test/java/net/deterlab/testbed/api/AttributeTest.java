package net.deterlab.testbed.api;

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

@RunWith(JMockit.class)
public class AttributeTest {
    
    @Test
    public void setName() {
        final Attribute o = new Attribute();
        String attributeName = "email";
        o.setName(attributeName);
        assertThat(o.getName(), is(attributeName));
    }
    
    @Test
    public void setValue() {
        final Attribute o = new Attribute();
        String value = "demo@example.com";
        o.setValue(value);
        assertThat(o.getValue(), is(value));
    }
    
    @Test
    public void setDataType() {
        final Attribute o = new Attribute();
        String dataType = "STRING";
        o.setDataType(dataType);
        assertThat(o.getDataType(), is(dataType));
    }
    
    @Test
    public void setOptional() {
        final Attribute o = new Attribute();
        boolean b = false;
        o.setOptional(b);
        assertThat(o.getOptional(), is(b));
    }
    
    @Test
    public void setAccess() {
        final Attribute o = new Attribute();
        String access = "READ_WRITE";
        o.setAccess(access);
        assertThat(o.getAccess(), is(access));
    }
    
    @Test
    public void setDescription() {
        final Attribute o = new Attribute();
        String description = "a simple description";
        o.setDescription(description);
        assertThat(o.getDescription(), is(description));
    }
    
    @Test
    public void setFormat() {
        final Attribute o = new Attribute();
        String regexFormat = "//s+";
        o.setFormat(regexFormat);
        assertThat(o.getFormat(), is(regexFormat));
    }
    
    @Test
    public void setFormatDescription() {
        final Attribute o = new Attribute();
        String formatDescription = "a format description";
        o.setFormatDescription(formatDescription);
        assertThat(o.getFormatDescription(), is(formatDescription));
    }
    
    @Test
    public void setOrderingHintZero() {
        final Attribute o = new Attribute();
        int orderingHint = 0;
        o.setOrderingHint(orderingHint);
        assertThat(o.getOrderingHint(), is(orderingHint));
    }
    
    @Test
    public void setOrderingHintPositive() {
        final Attribute o = new Attribute();
        int orderingHint = 1;
        o.setOrderingHint(orderingHint);
        assertThat(o.getOrderingHint(), is(orderingHint));
    }
    
    @Test
    public void setOrderingHintNegative() {
        final Attribute o = new Attribute();
        int orderingHint = -1;
        o.setOrderingHint(orderingHint);
        assertThat(o.getOrderingHint(), is(orderingHint));
    }
    
    @Test
    public void setLengthHintZero() {
        final Attribute o = new Attribute();
        int lengthHint = 0;
        o.setLengthHint(lengthHint);
        assertThat(o.getLengthHint(), is(lengthHint));
    }
    
    @Test
    public void setLengthHintPositive() {
        final Attribute o = new Attribute();
        int lengthHint = 1;
        o.setLengthHint(lengthHint);
        assertThat(o.getLengthHint(), is(lengthHint));
    }
    
    @Test
    public void setLengthHintNegative() {
        final Attribute o = new Attribute();
        int lengthHint = -1;
        o.setLengthHint(lengthHint);
        assertThat(o.getLengthHint(), is(lengthHint));
    }
    
    @Test
    public void validateAccessReadWrite() {
        final Attribute o = new Attribute();
        assertThat(Attribute.validateAccess("READ_WRITE"), is(true));
    }
    
    @Test
    public void validateAccessReadOnly() {
        final Attribute o = new Attribute();
        assertThat(Attribute.validateAccess("READ_ONLY"), is(true));
    }
    
    @Test
    public void validateAccessWriteOnly() {
        final Attribute o = new Attribute();
        assertThat(Attribute.validateAccess("WRITE_ONLY"), is(true));
    }
    
    @Test
    public void validateAccessNoAccess() {
        final Attribute o = new Attribute();
        assertThat(Attribute.validateAccess("NO_ACCESS"), is(true));
    }
    
    @Test
    public void validateAccessInvalid() {
        final Attribute o = new Attribute();
        assertThat(Attribute.validateAccess("ABCDEF"), is(false));
    }
    
    @Test
    public void toStringTest() {
        final Attribute o = new Attribute();
        o.setName("email");
        o.setValue("demo@example.com");
        String expected = "Name: email Value: demo@example.com";
        assertThat(o.toString(), is(expected));
    }
    
    @Test
    public void export() {
        // TBD
    }
    
    @Test
    public void getName() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getName(), is("email"));
    }
    
    @Test
    public void getValue() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getValue(), is("demo@example.com"));
    }
    
    @Test
    public void getDataType() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getDataType(), is("STRING"));
    }
    
    @Test
    public void getOptional() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getOptional(), is(true));
    }
    
    @Test
    public void getAccess() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getAccess(), is("READ_WRITE"));
    }
    
    @Test
    public void getDescription() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getDescription(), is("a simple description"));
    }
    
    @Test
    public void getFormat() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getFormat(), is("//s+"));
    }
    
    @Test
    public void getFormatNull() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", null, "a format description", 0, 0);
        assertThat(o.getFormat(), is(equalTo(null)));
    }
    
    @Test
    public void getFormatDescription() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getFormatDescription(), is("a format description"));
    }
    
    @Test
    public void getFormatDescriptionNull() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", null, 0, 0);
        assertThat(o.getFormatDescription(), is(equalTo(null)));
    }
    
    @Test
    public void getOrderingHint() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getOrderingHint(), is(0));
    }
    
    @Test
    public void getLengthHint() {
        final Attribute o = new Attribute("email", "demo@example.com", "STRING", true, "READ_WRITE", "a simple description", "//s+", "a format description", 0, 0);
        assertThat(o.getLengthHint(), is(0));
    }
    
    @Test
    public void cloneTest() {
        // TBD
    }
}