package org.recap.ils;

import com.pkrete.jsip2.connection.SIP2SocketConnection;
import com.pkrete.jsip2.exceptions.InvalidSIP2ResponseException;
import com.pkrete.jsip2.exceptions.InvalidSIP2ResponseValueException;
import com.pkrete.jsip2.messages.requests.*;
import com.pkrete.jsip2.messages.responses.*;
import com.pkrete.jsip2.variables.HoldMode;
import org.recap.ils.jsipmessages.SIP2CreateBibRequest;
import org.recap.ils.jsipmessages.SIP2CreateBibResponse;
import org.recap.ils.jsipmessages.SIP2RecallRequest;
import org.recap.ils.jsipmessages.SIP2RecallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sudhishk on 9/11/16.
 */
public abstract class JSIPConnector implements IJSIPConnector {
    private Logger logger = LoggerFactory.getLogger(JSIPConnector.class);

    private SIP2SocketConnection getSocketConnection() {
        SIP2SocketConnection connection = new SIP2SocketConnection(getHost(), 7031);
        try {
            connection.connect();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return connection;
    }

    public boolean jSIPLogin(SIP2SocketConnection connection, String patronIdentifier) throws InvalidSIP2ResponseException, InvalidSIP2ResponseValueException{
        SIP2LoginRequest login =null;
        boolean loginPatronStatus= false;
        try {
            if(connection == null) {
                connection = getSocketConnection();
            }
            if (connection.connect()) {
                login = new SIP2LoginRequest(getOperatorUserId(), getOperatorPassword(), getOperatorLocation());
                SIP2LoginResponse loginResponse = (SIP2LoginResponse) connection.send(login);
                SIP2PatronInformationRequest request = new SIP2PatronInformationRequest(patronIdentifier);
                SIP2PatronInformationResponse response = (SIP2PatronInformationResponse) connection.send(request);
                loginPatronStatus = false;
                if (loginResponse.isOk() && response.isValidPatron() && response.isValidPatronPassword()) {
                    loginPatronStatus = true;
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("InvalidSIP2Response "+e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("InvalidSIP2ResponseValue "+e.getMessage());
        } catch (Exception e) {
            logger.error("Exception "+ e.getMessage());
        }

        return loginPatronStatus;
    }

    public boolean patronValidation(String institutionId, String patronIdentifier){
        boolean loginPatronStatus=false;
        SIP2SocketConnection connection = getSocketConnection();
        try {
            SIP2LoginRequest login = new SIP2LoginRequest(getOperatorUserId(), getOperatorPassword(), getOperatorLocation());
            SIP2LoginResponse loginResponse = (SIP2LoginResponse) connection.send(login);
            SIP2PatronInformationRequest request = new SIP2PatronInformationRequest(institutionId, patronIdentifier, getOperatorPassword());
            SIP2PatronInformationResponse response = (SIP2PatronInformationResponse) connection.send(request);
            loginPatronStatus = false;
            if(loginResponse.isOk() && response.isValidPatron() && response.isValidPatronPassword()){
                loginPatronStatus=true;
            }
        }catch(Exception ex){
            logger.error(ex.getMessage());
        } finally{
            connection.close();
        }

        return loginPatronStatus;
    }

    public abstract String getHost();

    public abstract String getOperatorUserId();

    public abstract String getOperatorPassword();

    public abstract String getOperatorLocation();

    public SIP2ItemInformationResponse lookupItem(String itemIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2ItemInformationResponse itemResponse = null;
        try {
            SIP2ItemInformationRequest itemRequest = new SIP2ItemInformationRequest(itemIdentifier);
            logger.info(itemRequest.getData());
            itemResponse = (SIP2ItemInformationResponse) connection.send(itemRequest);
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Invalid SIP2 Value = ", e);
        } catch (Exception e) {
            logger.error("Exception = " ,e);

        }finally {
            connection.close();
        }
        return itemResponse;
    }

    public SIP2ItemInformationResponse lookupItemStatus(String itemIdentifier, String itemProperties, String patronIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2ItemInformationResponse itemResponse = null;
        try {
            if (connection.connect() && jSIPLogin(connection, patronIdentifier)) {
                SIP2ItemInformationRequest itemRequest = new SIP2ItemInformationRequest(itemIdentifier);
                logger.info(itemRequest.getData());
                itemResponse = (SIP2ItemInformationResponse) connection.send(itemRequest);

                SIP2ItemStatusUpdateRequest sip2ItemStatusUpdateRequest = new SIP2ItemStatusUpdateRequest(itemIdentifier,"");
                SIP2ItemStatusUpdateResponse sip2ItemStatusUpdateResponse = (SIP2ItemStatusUpdateResponse)connection.send(sip2ItemStatusUpdateRequest);
                logger.info(sip2ItemStatusUpdateResponse.getData());
            }else{
                logger.info("Item Status Request Failed");
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Invalid SIP2 Value = " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception = " + e.getMessage());

        }finally {
            connection.close();
        }
        return itemResponse;
    }

    public SIP2PatronStatusResponse lookupUser(String institutionId, String patronIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2PatronStatusResponse patronStatusResponse = null;
        try {
            if (connection.connect()) {
                SIP2PatronStatusRequest patronStatusRequest = new SIP2PatronStatusRequest(institutionId, patronIdentifier);
                logger.info(patronStatusRequest.getData());
                patronStatusResponse = (SIP2PatronStatusResponse) connection.send(patronStatusRequest);
            }else{
                logger.info("Item Request Failed");
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception = " + e.getMessage());
        }finally {
            connection.close();
        }
        return patronStatusResponse;
    }

    public SIP2CheckoutResponse checkOutItem(String itemIdentifier, String patronIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2CheckoutResponse checkoutResponse = null;
        try {
            if (connection.connect()) {
                if (jSIPLogin(connection, patronIdentifier)) {
                    SIP2SCStatusRequest status = new SIP2SCStatusRequest();
                    SIP2ACSStatusResponse statusResponse = (SIP2ACSStatusResponse) connection.send(status);
                    if(statusResponse.getSupportedMessages().isCheckout()) {
                        SIP2CheckoutRequest checkoutRequest = new SIP2CheckoutRequest(patronIdentifier, itemIdentifier);
                        checkoutRequest.setCurrentLocation("");
                        checkoutResponse = (SIP2CheckoutResponse) connection.send(checkoutRequest);
                        if (checkoutResponse.isOk()) {
                            logger.info("checkout Request Successful");
                        } else {
                            logger.info("checkout Request Failed");
                            logger.info("Response -> " + checkoutResponse.getData());
                            logger.info(checkoutResponse.getScreenMessage().get(0));
                        }
                    }
                } else {
                    logger.info("Login Failed");
                }

            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
        } finally {
            connection.close();
        }
        return checkoutResponse;
    }

    public SIP2CheckinResponse checkInItem(String itemIdentifier, String patronIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2CheckinResponse checkinResponse = null;
        try {
            if (connection.connect()) { // Connect to the SIP Server - Princton, Voyager, ILS
                if (jSIPLogin(connection, patronIdentifier)) {
                    SIP2SCStatusRequest status = new SIP2SCStatusRequest();
                    SIP2ACSStatusResponse statusResponse = (SIP2ACSStatusResponse) connection.send(status);
                    if(statusResponse.getSupportedMessages().isCheckin()) {
                        SIP2CheckinRequest checkinRequest = new SIP2CheckinRequest(itemIdentifier);
                        checkinResponse = (SIP2CheckinResponse) connection.send(checkinRequest);
                        if (checkinResponse.isOk()) {
                            logger.info("Check In Request Successful");
                        } else {
                            logger.info("Check In Request Failed");
                            logger.info("Response -> " + checkinResponse.getData());
                            logger.info(checkinResponse.getScreenMessage().get(0));
                        }
                    }
                } else {
                    logger.info("Login Failed");
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
        } finally {
            connection.close();
        }
        return checkinResponse;
    }

    public SIP2HoldResponse placeHold(String itemIdentifier, String patronIdentifier, String institutionId, String expirationDate, String bibId, String pickupLocation) {
        return hold(HoldMode.ADD, itemIdentifier, patronIdentifier, institutionId, expirationDate, bibId, pickupLocation);
    }

    public SIP2HoldResponse cancelHold(String itemIdentifier, String patronIdentifier, String institutionId, String expirationDate, String bibId, String pickupLocation) {
        return hold(HoldMode.DELETE, itemIdentifier, patronIdentifier, institutionId, expirationDate, bibId, pickupLocation);
    }

    private SIP2HoldResponse hold(HoldMode holdMode, String itemIdentifier, String patronIdentifier, String institutionId, String expirationDate, String bibId, String pickupLocation) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2HoldResponse holdResponse = null;
        try {
            if (connection.connect()) { // Connect to the SIP Server - Princton, Voyager, ILS
                /* Login to the ILS */
                /* Create a login request */
                SIP2LoginRequest login = new SIP2LoginRequest(getOperatorUserId(), getOperatorPassword(), getOperatorLocation());
                /* Send the request */
                SIP2LoginResponse loginResponse = (SIP2LoginResponse) connection.send(login);

                /* Check the response*/
                if (loginResponse.isOk()) {
                    /* Send SCStatusRequest */
                    SIP2SCStatusRequest status = new SIP2SCStatusRequest();
                    SIP2ACSStatusResponse statusResponse = (SIP2ACSStatusResponse) connection.send(status);

                    /* The patron must be validated before placing a hold */
                    SIP2PatronInformationRequest request = new SIP2PatronInformationRequest(institutionId, patronIdentifier, getOperatorPassword());
                    SIP2PatronInformationResponse response = (SIP2PatronInformationResponse) connection.send(request);

                    /* Check if the patron and patron password are valid */
                    if (response.isValidPatron() && response.isValidPatronPassword()) {
                        SIP2HoldRequest holdRequest = new SIP2HoldRequest(patronIdentifier, itemIdentifier);
                        holdRequest.setHoldMode(holdMode);
                        holdRequest.setExpirationDate(expirationDate);
                        holdRequest.setBibId(bibId);
                        holdRequest.setPickupLocation(pickupLocation);
                        holdRequest.setErrorDetectionEnabled(true);
                        logger.info("Request Hold -> " + holdRequest.getData());
                        holdResponse = (SIP2HoldResponse) connection.send(holdRequest);

                        /* Check that the hold was placed succesfully */
                        if (holdResponse.isOk()) {
                            logger.info("Hold Request Successful");
                        } else {
                            logger.info("Hold Failed");
                            logger.info("Response Hold -> " + holdResponse.getData());
                            logger.info(holdResponse.getScreenMessage().get(0));
                        }
                    }
                } else {
                    logger.info("Login Failed");
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
        } finally {
            connection.close();
        }

        return holdResponse;
    }

    public SIP2CreateBibResponse createBib(String itemIdentifier, String patronIdentifier, String institutionId, String titleIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2CreateBibResponse createBibResponse = null;
        try {
            if (connection.connect()) { // Connect to the SIP Server - Princton, Voyager, ILS
                /* Login to the ILS */
                /* Create a login request */
                SIP2LoginRequest login = new SIP2LoginRequest(getOperatorUserId(), getOperatorPassword(), getOperatorLocation());
                /* Send the request */
                SIP2LoginResponse loginResponse = (SIP2LoginResponse) connection.send(login);

                /* Check the response*/
                if (loginResponse.isOk()) {
                    /* Send SCStatusRequest */
                    SIP2SCStatusRequest status = new SIP2SCStatusRequest();
                    SIP2ACSStatusResponse statusResponse = (SIP2ACSStatusResponse) connection.send(status);

                    /* The patron must be validated before placing a hold */
                    SIP2PatronInformationRequest request = new SIP2PatronInformationRequest(institutionId, patronIdentifier, getOperatorPassword());
                    SIP2PatronInformationResponse response = (SIP2PatronInformationResponse) connection.send(request);

                    /* Check if the patron and patron password are valid */
                    if (response.isValidPatron() && response.isValidPatronPassword()) {
                        SIP2CreateBibRequest createBibRequest = new SIP2CreateBibRequest(patronIdentifier, titleIdentifier, itemIdentifier);


                        logger.info("Request Create -> " + createBibRequest.getData());
                        createBibResponse = (SIP2CreateBibResponse) connection.send(createBibRequest);

                        /* Check that the hold was placed succesfully */
                        if (createBibResponse.isOk()) {
                            logger.info("Create Request Successful");
                        } else {
                            logger.info("Create Failed");
                            logger.info("Response Hold -> " + createBibResponse.getData());
                            logger.info(createBibResponse.getScreenMessage().get(0));
                        }
                    }
                } else {
                    logger.info("Login Failed");
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
        } finally {
            connection.close();
        }

        return createBibResponse;

    }

    public SIP2PatronInformationResponse lookupPatron(String patronIdentifier){
        SIP2SocketConnection connection = getSocketConnection();
        SIP2PatronInformationRequest sip2PatronInformationRequest =null;
        SIP2PatronInformationResponse sip2PatronInformationResponse=null;
        try {
            SIP2LoginRequest login = new SIP2LoginRequest(getOperatorUserId(), getOperatorPassword(), getOperatorLocation());
            SIP2LoginResponse loginResponse = (SIP2LoginResponse) connection.send(login);
            sip2PatronInformationRequest = new SIP2PatronInformationRequest(patronIdentifier);
            sip2PatronInformationResponse = (SIP2PatronInformationResponse) connection.send(sip2PatronInformationRequest);
        }catch(Exception ex){
            logger.error(ex.getMessage());
        } finally{
            connection.close();
        }
        return sip2PatronInformationResponse;
    }

    public SIP2RecallResponse recallItem(String  itemIdentifier, String patronIdentifier, String institutionId, String expirationDate, String pickupLocation,String bibId){
        SIP2RecallResponse sip2RecallResponse =null;
        SIP2SocketConnection connection = getSocketConnection();
        try {
            if (connection.connect()) { // Connect to the SIP Server - Princton, Voyager, ILS
                /* Login to the ILS */
                /* Create a login request */
                SIP2LoginRequest login = new SIP2LoginRequest(getOperatorUserId(), getOperatorPassword(), getOperatorLocation());
                /* Send the request */
                SIP2LoginResponse loginResponse = (SIP2LoginResponse) connection.send(login);

                /* Check the response*/
                if (loginResponse.isOk()) {
                    /* Send SCStatusRequest */
                    SIP2SCStatusRequest status = new SIP2SCStatusRequest();
                    SIP2ACSStatusResponse statusResponse = (SIP2ACSStatusResponse) connection.send(status);

                    /* The patron must be validated before placing a hold */
                    SIP2PatronInformationRequest request = new SIP2PatronInformationRequest(institutionId, patronIdentifier, getOperatorPassword());
                    SIP2PatronInformationResponse response = (SIP2PatronInformationResponse) connection.send(request);

                    /* Check if the patron and patron password are valid */
                    if (response.isValidPatron() && response.isValidPatronPassword()) {
                        SIP2RecallRequest recallRequest = new SIP2RecallRequest(patronIdentifier, itemIdentifier);
                        recallRequest.setHoldMode(HoldMode.ADD);
                        recallRequest.setInstitutionId(institutionId);
                        recallRequest.setExpirationDate(expirationDate);
                        recallRequest.setPickupLocation(pickupLocation);
                        recallRequest.setBibId(bibId);
                        recallRequest.setErrorDetectionEnabled(true);
                        logger.info("Request Recall -> " + recallRequest.getData());
                        sip2RecallResponse = (SIP2RecallResponse) connection.send(recallRequest);

                        /* Check that the hold was placed succesfully */
                        if (sip2RecallResponse.isOk()) {
                            logger.info("Recall Request Successful");
                        } else {
                            logger.info("Recall Failed");
                            logger.info("Response Recall -> " + sip2RecallResponse.getData());
                            logger.info(sip2RecallResponse.getScreenMessage().get(0));
                        }
                    }else{
                        logger.info("Invalid Patron");
                    }
                } else {
                    logger.info("Login Failed");
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
        } finally {
            connection.close();
        }
        return sip2RecallResponse;
    }
}
