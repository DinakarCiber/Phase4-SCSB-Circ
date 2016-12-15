package org.recap.ils;

import com.pkrete.jsip2.messages.responses.*;
import com.pkrete.jsip2.util.MessageUtil;
import org.junit.Test;
import org.recap.BaseTestCase;
import com.pkrete.jsip2.messages.response.SIP2CreateBibResponse;
import com.pkrete.jsip2.messages.response.SIP2RecallResponse;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by saravanakumarp on 28/9/16.
 */
public class ColumbiaJSIPConnectorUT extends BaseTestCase {

    @Autowired
    private ColumbiaJSIPConnector columbiaJSIPConnector;

    private String[] itemIdentifier = {"CU55724132", "1002534398","MR68799284","1000534323"," CU65897706"};
    private String patronIdentifier = "RECAPTST01";
    private String institutionId = "";

    @Test
    public void login() throws Exception {
        boolean sip2LoginRequest = columbiaJSIPConnector.jSIPLogin(null,patronIdentifier);
        assertTrue(sip2LoginRequest);
    }

    @Test
    public void lookupItem() throws Exception {

        SIP2ItemInformationResponse itemInformationResponse = columbiaJSIPConnector.lookupItem(itemIdentifier[3]);

//        assertEquals(itemIdentifier,itemInformationResponse.getItemIdentifier());
//        assertEquals("Bolshevism, by an eye-witness from Wisconsin, by Lieutenant A. W. Kliefoth ...",itemInformationResponse.getTitleIdentifier());
    }

    @Test
    public void lookupUser() throws Exception {
        String patronIdentifier = "RECAPTST01";
        String institutionId = "htccul";
        SIP2PatronStatusResponse patronInformationResponse = columbiaJSIPConnector.lookupUser(institutionId, patronIdentifier);
        assertNotNull(patronInformationResponse);
//        assertTrue(patronInformationResponse.isValid());
    }

    @Test
    public void checkout() throws Exception { // CULTST11345 , CULTST13345 ,
        String itemIdentifier = "CULTST52345";
        String patronIdentifier = "RECAPPUL01";
        String institutionId = "";
        SIP2CheckoutResponse checkOutResponse = columbiaJSIPConnector.checkOutItem(itemIdentifier, patronIdentifier);
        assertNotNull(checkOutResponse);
        assertTrue(checkOutResponse.isOk());
    }

    @Test
    public void checkIn() throws Exception {
        String itemIdentifier = "CULTST92345";
        String patronIdentifier = "RECAPPUL01";
        String institutionId = "";
        SIP2CheckinResponse checkInResponse = columbiaJSIPConnector.checkInItem(itemIdentifier,patronIdentifier);
        assertNotNull(checkInResponse);
        assertTrue(checkInResponse.isOk());
    }

    @Test
    public void check_out_In() throws Exception {
        String itemIdentifier = "";
        String patronIdentifier = "";
        String institutionId = "";
        SIP2CheckoutResponse checkOutResponse = columbiaJSIPConnector.checkOutItem(itemIdentifier, patronIdentifier);
        assertNotNull(checkOutResponse);
        assertTrue(checkOutResponse.isOk());
        SIP2CheckinResponse checkInResponse = columbiaJSIPConnector.checkInItem(itemIdentifier,patronIdentifier);
        assertNotNull(checkInResponse);
        assertTrue(checkInResponse.isOk());
    }

    @Test
    public void cancelHold() throws Exception {
        String itemIdentifier = "555555555";;
        String patronIdentifier = "RECAPTST01";
        String institutionId = "";
        String expirationDate =MessageUtil.createFutureDate(20,2);
        String bibId="12040033";
        String pickupLocation="CIRCrecap";

        SIP2HoldResponse holdResponse = columbiaJSIPConnector.cancelHold(itemIdentifier, patronIdentifier,institutionId ,expirationDate,bibId,pickupLocation);

        assertNotNull(holdResponse);
        assertTrue(holdResponse.isOk());
    }

    @Test
    public void placeHold() throws Exception {
        String itemIdentifier = "555555555";;
        String patronIdentifier = "RECAPTST01";
        String institutionId = "";
        String expirationDate =MessageUtil.createFutureDate(20,2);
        String bibId="12040033";
        String pickupLocation="CIRCrecap";

        SIP2HoldResponse holdResponse = columbiaJSIPConnector.placeHold(itemIdentifier, patronIdentifier,institutionId ,expirationDate,bibId,pickupLocation);

        assertNotNull(holdResponse);
        assertTrue(holdResponse.isOk());
    }

    @Test
    public void bothHold() throws Exception {
//        String itemIdentifier = "32101057972166";
        String itemIdentifier = "32101095533293";
        String patronIdentifier = "198572368";
        String institutionId = "htccul";
        String expirationDate =MessageUtil.getSipDateTime(); // Date Format YYYYMMDDZZZZHHMMSS
        String bibId="100001";
        String pickupLocation="htcsc";
        SIP2HoldResponse holdResponse;

        holdResponse = columbiaJSIPConnector.placeHold(itemIdentifier, patronIdentifier,institutionId ,expirationDate,bibId,pickupLocation);
        holdResponse = columbiaJSIPConnector.cancelHold(itemIdentifier, patronIdentifier,institutionId ,expirationDate,bibId,pickupLocation);

        assertNotNull(holdResponse);
        assertTrue(holdResponse.isOk());
    }

    @Test
    public void createBib() throws Exception {
        String itemIdentifier = "555555555";
        String patronIdentifier = "RECAPTST01";
        String institutionId = "";
        String titleIdentifier ="Recap Testing 1002";
        String bibId ="";
        SIP2CreateBibResponse createBibResponse;

        createBibResponse = columbiaJSIPConnector.createBib(itemIdentifier,patronIdentifier,institutionId ,titleIdentifier);

        assertNotNull(createBibResponse);
        assertTrue(createBibResponse.isOk());
    }

    @Test
    public void testRecall() throws Exception {
        String itemIdentifier = "CULTST52345";
        String patronIdentifier = "RECAPTST01";
        String institutionId = "htccul";
        String expirationDate = MessageUtil.createFutureDate(20,2);
        String pickupLocation="htcsc";
        String bibId="9959053";

        SIP2RecallResponse recallResponse = columbiaJSIPConnector.recallItem(itemIdentifier, patronIdentifier,institutionId ,expirationDate,pickupLocation,bibId);

        assertNotNull(recallResponse);
        assertTrue(recallResponse.isOk());
    }
}
