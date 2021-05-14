package org.recap.ils.protocol.rest.model;

import org.junit.Test;
import org.recap.ils.protocol.rest.model.Description;

import static org.junit.Assert.assertNotNull;

public class DescriptionUT {

    @Test
    public void getDescription(){
        Description description = new Description();
        description.setTitle("test");
        description.setAuthor("test");
        description.setCallNumber("AE311245dSD");
        assertNotNull(description.getAuthor());
        assertNotNull(description.getTitle());
        assertNotNull(description.getCallNumber());
    }
}
