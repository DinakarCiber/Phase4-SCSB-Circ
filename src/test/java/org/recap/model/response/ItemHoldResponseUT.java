package org.recap.model.response;

import org.junit.Test;
import org.recap.BaseTestCaseUT;
import org.recap.model.response.ItemHoldResponse;

import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * Created by hemalathas on 3/4/17.
 */
public class ItemHoldResponseUT extends BaseTestCaseUT {

    @Test
    public void testItemHoldResponse() {
        ItemHoldResponse itemHoldResponse = new ItemHoldResponse();

        itemHoldResponse.setAvailable(true);
        itemHoldResponse.setTransactionDate(new Date().toString());
        itemHoldResponse.setInstitutionID("1");
        itemHoldResponse.setPatronIdentifier("43256835645");
        itemHoldResponse.setTitleIdentifier("test");
        itemHoldResponse.setExpirationDate(new Date().toString());
        itemHoldResponse.setPickupLocation("PB");
        itemHoldResponse.setQueuePosition("1");
        itemHoldResponse.setBibId("1");
        itemHoldResponse.setIsbn("1");
        itemHoldResponse.setLccn("1");
        itemHoldResponse.setTrackingId("1");
        itemHoldResponse.setJobId("1");
        itemHoldResponse.setUpdatedDate(new Date().toString());
        itemHoldResponse.setCreatedDate(new Date().toString());

        assertNotNull(itemHoldResponse.getTransactionDate());
        assertNotNull(itemHoldResponse.getInstitutionID());
        assertNotNull(itemHoldResponse.getPatronIdentifier());
        assertNotNull(itemHoldResponse.getTitleIdentifier());
        assertNotNull(itemHoldResponse.getExpirationDate());
        assertNotNull(itemHoldResponse.getPickupLocation());
        assertNotNull(itemHoldResponse.getQueuePosition());
        assertNotNull(itemHoldResponse.getBibId());
        assertNotNull(itemHoldResponse.getIsbn());
        assertNotNull(itemHoldResponse.getLccn());
        assertNotNull(itemHoldResponse.getTrackingId());
        assertNotNull(itemHoldResponse.getJobId());
        assertNotNull(itemHoldResponse.getUpdatedDate());
        assertNotNull(itemHoldResponse.getCreatedDate());

    }

}