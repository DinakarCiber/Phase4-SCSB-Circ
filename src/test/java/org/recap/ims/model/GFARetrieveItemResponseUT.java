package org.recap.ims.model;

import org.junit.Test;
import org.recap.BaseTestCaseUT;

import static org.junit.Assert.assertNotNull;

public class GFARetrieveItemResponseUT extends BaseTestCaseUT {

    @Test
    public void getGFARetrieveItemResponse(){
        GFARetrieveItemResponse gfaRetrieveItemResponse = new GFARetrieveItemResponse();
        GFARetrieveItemResponse gfaRetrieveItemResponse1 = new GFARetrieveItemResponse();
        gfaRetrieveItemResponse.setScreenMessage("success");
        gfaRetrieveItemResponse.setDsitem(new RetrieveItem());
        gfaRetrieveItemResponse.setSuccess(true);
        gfaRetrieveItemResponse.equals(gfaRetrieveItemResponse);
        gfaRetrieveItemResponse.equals(gfaRetrieveItemResponse1);
        gfaRetrieveItemResponse1.equals(gfaRetrieveItemResponse);
        gfaRetrieveItemResponse.hashCode();
        gfaRetrieveItemResponse1.hashCode();
        gfaRetrieveItemResponse.toString();

        assertNotNull(gfaRetrieveItemResponse.getScreenMessage());
        assertNotNull(gfaRetrieveItemResponse.getDsitem());
        assertNotNull(gfaRetrieveItemResponse.isSuccess());
    }
}
