package org.recap.ils;

import com.pkrete.jsip2.connection.SIP2SocketConnection;
import com.pkrete.jsip2.exceptions.InvalidSIP2ResponseException;
import com.pkrete.jsip2.exceptions.InvalidSIP2ResponseValueException;
import com.pkrete.jsip2.messages.request.SIP2CreateBibRequest;
import com.pkrete.jsip2.messages.request.SIP2RecallRequest;
import com.pkrete.jsip2.messages.requests.*;
import com.pkrete.jsip2.messages.response.SIP2CreateBibResponse;
import com.pkrete.jsip2.messages.response.SIP2RecallResponse;
import com.pkrete.jsip2.messages.responses.*;
import com.pkrete.jsip2.util.MessageUtil;
import com.pkrete.jsip2.variables.HoldMode;
import org.recap.ReCAPConstants;
import org.recap.ils.model.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;

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

    public boolean jSIPLogin(SIP2SocketConnection connection, String patronIdentifier) throws InvalidSIP2ResponseException, InvalidSIP2ResponseValueException {
        SIP2LoginRequest login = null;
        boolean loginPatronStatus = false;
        try {
            if (connection == null) {
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
            logger.error("InvalidSIP2Response " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("InvalidSIP2ResponseValue " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception " + e.getMessage());
        }

        return loginPatronStatus;
    }

    public boolean patronValidation(String institutionId, String patronIdentifier) {
        boolean loginPatronStatus = false;
        SIP2SocketConnection connection = getSocketConnection();
        try {
            SIP2LoginRequest login = new SIP2LoginRequest(getOperatorUserId(), getOperatorPassword(), getOperatorLocation());
            SIP2LoginResponse loginResponse = (SIP2LoginResponse) connection.send(login);
            SIP2PatronInformationRequest request = new SIP2PatronInformationRequest(institutionId, patronIdentifier, getOperatorPassword());
            SIP2PatronInformationResponse response = (SIP2PatronInformationResponse) connection.send(request);
            loginPatronStatus = false;
            if (loginResponse.isOk() && response.isValidPatron() && response.isValidPatronPassword()) {
                loginPatronStatus = true;
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        } finally {
            connection.close();
        }

        return loginPatronStatus;
    }

    public abstract String getHost();

    public abstract String getOperatorUserId();

    public abstract String getOperatorPassword();

    public abstract String getOperatorLocation();

    public AbstractResponseItem lookupItem(String itemIdentifier, String source) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2ItemInformationResponse sip2ItemInformationResponse = null;
        ItemInformationResponse itemInformationResponse = new ItemInformationResponse();
        try {
            SIP2ItemInformationRequest itemRequest = new SIP2ItemInformationRequest(itemIdentifier);
            logger.info(itemRequest.getData());
            itemInformationResponse.setEsipDataIn(itemRequest.getData());
            sip2ItemInformationResponse = (SIP2ItemInformationResponse) connection.send(itemRequest);
            itemInformationResponse.setEsipDataOut(sip2ItemInformationResponse.getData());
            itemInformationResponse.setItemBarcode(sip2ItemInformationResponse.getItemIdentifier());
            if (sip2ItemInformationResponse.getScreenMessage().size() > 0) {
                itemInformationResponse.setScreenMessage(sip2ItemInformationResponse.getScreenMessage().get(0));
            }
            itemInformationResponse.setSuccess(sip2ItemInformationResponse.isOk());
            itemInformationResponse.setTitleIdentifier(sip2ItemInformationResponse.getTitleIdentifier());

            itemInformationResponse.setDueDate(formatFromSipDate(sip2ItemInformationResponse.getDueDate()));
            itemInformationResponse.setRecallDate(formatFromSipDate(sip2ItemInformationResponse.getRecallDate()));
            itemInformationResponse.setHoldPickupDate(formatFromSipDate(sip2ItemInformationResponse.getHoldPickupDate()));
            itemInformationResponse.setTransactionDate(formatFromSipDate(sip2ItemInformationResponse.getTransactionDate()));
            itemInformationResponse.setExpirationDate(formatFromSipDate(sip2ItemInformationResponse.getExpirationDate()));

            itemInformationResponse.setCirculationStatus(sip2ItemInformationResponse.getCirculationStatus().name());
            itemInformationResponse.setCurrentLocation(sip2ItemInformationResponse.getCurrentLocation());
            itemInformationResponse.setPermanentLocation(sip2ItemInformationResponse.getPermanentLocation());
            itemInformationResponse.setFeeType(sip2ItemInformationResponse.getFeeType().name());
            itemInformationResponse.setHoldQueueLength(sip2ItemInformationResponse.getHoldQueueLength());
            itemInformationResponse.setOwner(sip2ItemInformationResponse.getOwner());
            itemInformationResponse.setSecurityMarker(sip2ItemInformationResponse.getSecurityMarker().name());
            itemInformationResponse.setCurrencyType((sip2ItemInformationResponse.getCurrencyType() != null) ? sip2ItemInformationResponse.getCurrencyType().name() : "");
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Invalid SIP2 Value = ", e);

        } catch (Exception e) {
            logger.error("Exception = ", e);
        } finally {
            connection.close();
        }
        return itemInformationResponse;
    }

    public SIP2ItemInformationResponse lookupItemStatus(String itemIdentifier, String itemProperties, String patronIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2ItemInformationResponse itemResponse = null;
        try {
            if (connection.connect() && jSIPLogin(connection, patronIdentifier)) {
                SIP2ItemInformationRequest itemRequest = new SIP2ItemInformationRequest(itemIdentifier);
                logger.info(itemRequest.getData());
                itemResponse = (SIP2ItemInformationResponse) connection.send(itemRequest);

                SIP2ItemStatusUpdateRequest sip2ItemStatusUpdateRequest = new SIP2ItemStatusUpdateRequest(itemIdentifier, "");
                SIP2ItemStatusUpdateResponse sip2ItemStatusUpdateResponse = (SIP2ItemStatusUpdateResponse) connection.send(sip2ItemStatusUpdateRequest);
                logger.info(sip2ItemStatusUpdateResponse.getData());
            } else {
                logger.info("Item Status Request Failed");
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Invalid SIP2 Value = " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception = " + e.getMessage());

        } finally {
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
            } else {
                logger.info("Item Request Failed");
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception = " + e.getMessage());
        } finally {
            connection.close();
        }
        return patronStatusResponse;
    }

    public AbstractResponseItem checkOutItem(String itemIdentifier, String patronIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2CheckoutResponse checkoutResponse = null;
        ItemCheckoutResponse itemCheckoutResponse = new ItemCheckoutResponse();
        try {
            if (connection.connect()) {
                if (jSIPLogin(connection, patronIdentifier)) {
                    SIP2SCStatusRequest status = new SIP2SCStatusRequest();
                    SIP2ACSStatusResponse statusResponse = (SIP2ACSStatusResponse) connection.send(status);
                    if (statusResponse.getSupportedMessages().isCheckout()) {
                        SIP2CheckoutRequest checkoutRequest = new SIP2CheckoutRequest(patronIdentifier, itemIdentifier);
                        checkoutRequest.setCurrentLocation("");

                        itemCheckoutResponse.setEsipDataIn(checkoutRequest.getData());
                        checkoutResponse = (SIP2CheckoutResponse) connection.send(checkoutRequest);
                        itemCheckoutResponse.setEsipDataOut(itemCheckoutResponse.getEsipDataOut());

                        itemCheckoutResponse.setItemBarcode(checkoutResponse.getItemIdentifier());
                        itemCheckoutResponse.setPatronIdentifier(checkoutResponse.getPatronIdentifier());
                        itemCheckoutResponse.setTitleIdentifier(checkoutResponse.getTitleIdentifier());
                        itemCheckoutResponse.setDesensitize(checkoutResponse.isDesensitizeSupported());
                        itemCheckoutResponse.setRenewal(checkoutResponse.isRenewalOk());
                        itemCheckoutResponse.setMagneticMedia(checkoutResponse.isMagneticMedia());
                        itemCheckoutResponse.setDueDate(formatFromSipDate(checkoutResponse.getDueDate()));
                        itemCheckoutResponse.setTransactionDate(formatFromSipDate(checkoutResponse.getTransactionDate()));
                        itemCheckoutResponse.setInstitutionID(checkoutResponse.getInstitutionId());
                        itemCheckoutResponse.setItemOwningInstitution(checkoutResponse.getInstitutionId());
                        itemCheckoutResponse.setPatronIdentifier(checkoutResponse.getPatronIdentifier());
                        itemCheckoutResponse.setMediaType((checkoutResponse.getMediaType() != null) ? checkoutResponse.getMediaType().name() : "");
                        itemCheckoutResponse.setBibId(checkoutResponse.getBibId());
                        itemCheckoutResponse.setScreenMessage((checkoutResponse.getScreenMessage().size() > 0) ? checkoutResponse.getScreenMessage().get(0) : "");
                        itemCheckoutResponse.setSuccess(checkoutResponse.isOk());

                    }
                } else {
                    logger.info("Login Failed");
                    itemCheckoutResponse.setScreenMessage("Login to ILS failed");
                    itemCheckoutResponse.setSuccess(false);
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
            itemCheckoutResponse.setScreenMessage(e.getMessage());
            itemCheckoutResponse.setSuccess(false);
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
            itemCheckoutResponse.setScreenMessage(e.getMessage());
            itemCheckoutResponse.setSuccess(false);
        } catch (Exception e) {
            logger.error("Exception = " + e.getMessage());
            itemCheckoutResponse.setScreenMessage(e.getMessage());
            itemCheckoutResponse.setSuccess(false);
        } finally {
            connection.close();
        }
        return itemCheckoutResponse;
    }

    public AbstractResponseItem checkInItem(String itemIdentifier, String patronIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2CheckinResponse checkinResponse = null;
        ItemCheckinResponse itemCheckinResponse = new ItemCheckinResponse();
        try {
            if (connection.connect()) { // Connect to the SIP Server - Princton, Voyager, ILS
                if (jSIPLogin(connection, patronIdentifier)) {
                    SIP2SCStatusRequest status = new SIP2SCStatusRequest();
                    SIP2ACSStatusResponse statusResponse = (SIP2ACSStatusResponse) connection.send(status);
                    if (statusResponse.getSupportedMessages().isCheckin()) {
                        SIP2CheckinRequest checkinRequest = new SIP2CheckinRequest(itemIdentifier);

                        itemCheckinResponse.setEsipDataIn(checkinRequest.getData());
                        checkinResponse = (SIP2CheckinResponse) connection.send(checkinRequest);
                        itemCheckinResponse.setEsipDataOut(checkinResponse.getData());

                        if (checkinResponse.isOk()) {
                            logger.info("Check In Request Successful");
                            itemCheckinResponse.setItemBarcode(checkinResponse.getItemIdentifier());
                            itemCheckinResponse.setTitleIdentifier(checkinResponse.getTitleIdentifier());
                            itemCheckinResponse.setDueDate(formatFromSipDate(checkinResponse.getDueDate()));
                            itemCheckinResponse.setResensitize(checkinResponse.isResensitize());
                            itemCheckinResponse.setAlert(checkinResponse.isAlert());
                            itemCheckinResponse.setMagneticMedia(checkinResponse.isMagneticMedia());
                            itemCheckinResponse.setTransactionDate(formatFromSipDate(checkinResponse.getTransactionDate()));
                            itemCheckinResponse.setInstitutionID(checkinResponse.getInstitutionId());
                            itemCheckinResponse.setItemOwningInstitution(checkinResponse.getInstitutionId());
                            itemCheckinResponse.setPatronIdentifier(checkinResponse.getPatronIdentifier());
                            itemCheckinResponse.setMediaType((checkinResponse.getMediaType() != null) ? checkinResponse.getMediaType().name() : "");
                            itemCheckinResponse.setBibId(checkinResponse.getBibId());
                            itemCheckinResponse.setPermanentLocation(checkinResponse.getPermanentLocation());
                            itemCheckinResponse.setCollectionCode(checkinResponse.getCollectionCode());
                            itemCheckinResponse.setSortBin(checkinResponse.getSortBin());
                            itemCheckinResponse.setCallNumber(checkinResponse.getCallNumber());
                            itemCheckinResponse.setDestinationLocation(checkinResponse.getDestinationLocation());
                            itemCheckinResponse.setAlertType((checkinResponse.getAlertType() != null) ? checkinResponse.getAlertType().name() : "");
                            itemCheckinResponse.setHoldPatronId(checkinResponse.getHoldPatronId());
                            itemCheckinResponse.setHoldPatronName(checkinResponse.getHoldPatronName());
                        } else {
                            logger.info("Check In Request Failed");
                            logger.info("Response -> " + checkinResponse.getData());
                        }
                        itemCheckinResponse.setScreenMessage((checkinResponse.getScreenMessage().size() > 0) ? checkinResponse.getScreenMessage().get(0) : "");
                        itemCheckinResponse.setSuccess(checkinResponse.isOk());
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
        return itemCheckinResponse;
    }

    public Object placeHold(String itemIdentifier, String patronIdentifier, String callInstitutionId, String itemInstitutionId, String expirationDate, String bibId, String pickupLocation, String trackingId, String title, String author, String callNumber) {
        return hold(HoldMode.ADD, itemIdentifier, patronIdentifier, callInstitutionId, expirationDate, bibId, pickupLocation);
    }

    public Object cancelHold(String itemIdentifier, String patronIdentifier, String institutionId, String expirationDate, String bibId, String pickupLocation, String trackingId) {
        return hold(HoldMode.DELETE, itemIdentifier, patronIdentifier, institutionId, expirationDate, bibId, pickupLocation);
    }

    private AbstractResponseItem hold(HoldMode holdMode, String itemIdentifier, String patronIdentifier, String institutionId, String expirationDate, String bibId, String pickupLocation) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2HoldResponse holdResponse = null;
        ItemHoldResponse itemHoldResponse = new ItemHoldResponse();
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
                        holdRequest.setExpirationDate(MessageUtil.createFutureDate(ReCAPConstants.ESIPEXPIRATION_DATE_DAY, ReCAPConstants.ESIPEXPIRATION_DATE_MONTH));
                        holdRequest.setBibId(bibId);
                        holdRequest.setPickupLocation(pickupLocation);

                        logger.info("Request Hold -> " + holdRequest.getData());
                        itemHoldResponse.setEsipDataIn(holdRequest.getData());
                        holdResponse = (SIP2HoldResponse) connection.send(holdRequest);
                        itemHoldResponse.setEsipDataOut(holdResponse.getData());

                        itemHoldResponse.setItemBarcode(holdResponse.getItemIdentifier());
                        itemHoldResponse.setScreenMessage((holdResponse.getScreenMessage().size() > 0) ? holdResponse.getScreenMessage().get(0) : "");
                        itemHoldResponse.setSuccess(holdResponse.isOk());
                        itemHoldResponse.setTitleIdentifier(holdResponse.getTitleIdentifier());
                        itemHoldResponse.setExpirationDate(formatFromSipDate(holdResponse.getExpirationDate()));
                        itemHoldResponse.setTransactionDate(formatFromSipDate(holdResponse.getTransactionDate()));
                        itemHoldResponse.setInstitutionID(holdResponse.getInstitutionId());
                        itemHoldResponse.setPatronIdentifier(holdResponse.getPatronIdentifier());
                        itemHoldResponse.setBibId(holdResponse.getBibId());
                        itemHoldResponse.setQueuePosition(holdResponse.getQueuePosition());
                        itemHoldResponse.setLCCN(holdResponse.getLccn());
                        itemHoldResponse.setISBN(holdResponse.getIsbn());
                        itemHoldResponse.setAvailable(holdResponse.isAvailable());
                    } else {
                        itemHoldResponse.setSuccess(false);
                        itemHoldResponse.setScreenMessage("Patron Validation Failed");
                    }
                } else {
                    logger.info("Login Failed");
                    itemHoldResponse.setSuccess(false);
                    itemHoldResponse.setScreenMessage("Login Failed");
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
            holdResponse = new SIP2HoldResponse("");
            holdResponse.setScreenMessage(java.util.Arrays.asList("Invaild Response from ILS"));
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
            holdResponse.setScreenMessage(java.util.Arrays.asList("Invaild Response Values from ILS"));

        } finally {
            connection.close();
        }

        return itemHoldResponse;
    }

    public ItemCreateBibResponse createBib(String itemIdentifier, String patronIdentifier, String institutionId, String titleIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2CreateBibResponse createBibResponse = null;
        ItemCreateBibResponse itemCreateBibResponse = new ItemCreateBibResponse();
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
                        itemCreateBibResponse.setEsipDataIn(createBibRequest.getData());
                        createBibResponse = (SIP2CreateBibResponse) connection.send(createBibRequest);
                        itemCreateBibResponse.setEsipDataOut(createBibResponse.getData());

                        itemCreateBibResponse.setItemBarcode(createBibResponse.getItemIdentifier());
                        itemCreateBibResponse.setScreenMessage((createBibResponse.getScreenMessage().size() > 0) ? createBibResponse.getScreenMessage().get(0) : "");
                        itemCreateBibResponse.setSuccess(createBibResponse.isOk());
                        itemCreateBibResponse.setBibId(createBibResponse.getBibId());
                        itemCreateBibResponse.setItemId(createBibResponse.getItemIdentifier());
                    } else {
                        itemCreateBibResponse.setSuccess(false);
                        itemCreateBibResponse.setScreenMessage("Patron Validation Failed: " + ((response.getScreenMessage().size() > 0) ? response.getScreenMessage().get(0) : ""));
                    }
                } else {
                    logger.info("Login Failed");
                    itemCreateBibResponse.setSuccess(false);
                    itemCreateBibResponse.setScreenMessage("Login Failed");
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = " + e.getMessage());
            itemCreateBibResponse.setSuccess(false);
            itemCreateBibResponse.setScreenMessage(e.getMessage());
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
            itemCreateBibResponse.setSuccess(false);
            itemCreateBibResponse.setScreenMessage(e.getMessage());
        } catch (Exception e) {
            logger.error("Connection Invalid SIP2 Value = " + e.getMessage());
            itemCreateBibResponse.setSuccess(false);
            itemCreateBibResponse.setScreenMessage(e.getMessage());
        } finally {
            connection.close();
        }
        return itemCreateBibResponse;

    }

    public AbstractResponseItem lookupPatron(String patronIdentifier) {
        SIP2SocketConnection connection = getSocketConnection();
        SIP2PatronInformationRequest sip2PatronInformationRequest = null;
        SIP2PatronInformationResponse sip2PatronInformationResponse = null;
        PatronInformationResponse patronInformationResponse = new PatronInformationResponse();
        try {
            SIP2LoginRequest login = new SIP2LoginRequest(getOperatorUserId(), getOperatorPassword(), getOperatorLocation());
            SIP2LoginResponse loginResponse = (SIP2LoginResponse) connection.send(login);

            sip2PatronInformationRequest = new SIP2PatronInformationRequest(patronIdentifier);

            patronInformationResponse.setEsipDataIn(sip2PatronInformationRequest.getData());
            sip2PatronInformationResponse = (SIP2PatronInformationResponse) connection.send(sip2PatronInformationRequest);
            patronInformationResponse.setEsipDataOut(sip2PatronInformationResponse.getData());

            patronInformationResponse.setSuccess(true);
            patronInformationResponse.setScreenMessage((sip2PatronInformationResponse.getScreenMessage() != null) ? sip2PatronInformationResponse.getScreenMessage().get(0) : "");
            patronInformationResponse.setPatronName(sip2PatronInformationResponse.getPersonalName());
            patronInformationResponse.setPatronIdentifier(sip2PatronInformationResponse.getPatronIdentifier());
            patronInformationResponse.setEmail(sip2PatronInformationResponse.getEmail());
            patronInformationResponse.setBirthDate(sip2PatronInformationResponse.getBirthDate());
            patronInformationResponse.setPhone(sip2PatronInformationResponse.getPhone());
            patronInformationResponse.setPermanentLocation(sip2PatronInformationResponse.getPermanentLocation());
            patronInformationResponse.setPickupLocation(sip2PatronInformationResponse.getPickupLocation());

            patronInformationResponse.setChargedItemsCount(sip2PatronInformationResponse.getChargedItemsCount());
            patronInformationResponse.setChargedItemsLimit(sip2PatronInformationResponse.getChargedItemsLimit());

            patronInformationResponse.setFeeLimit(sip2PatronInformationResponse.getFeeLimit());
            patronInformationResponse.setFeeType((sip2PatronInformationResponse.getFeeType() != null) ? sip2PatronInformationResponse.getFeeType().name() : "");

            patronInformationResponse.setHoldItemsCount(sip2PatronInformationResponse.getHoldItemsCount());
            patronInformationResponse.setHoldItemsLimit(sip2PatronInformationResponse.getHoldItemsLimit());
            patronInformationResponse.setUnavailableHoldsCount(sip2PatronInformationResponse.getUnavailableHoldsCount());

            patronInformationResponse.setFineItemsCount(sip2PatronInformationResponse.getFineItemsCount());
            patronInformationResponse.setFeeAmount(sip2PatronInformationResponse.getFeeAmount());
            patronInformationResponse.setHomeAddress(sip2PatronInformationResponse.getHomeAddress());
            patronInformationResponse.setItems(sip2PatronInformationResponse.getItems());
            patronInformationResponse.setItemType((sip2PatronInformationResponse.getItemType() != null) ? sip2PatronInformationResponse.getItemType().name() : "");

            patronInformationResponse.setOverdueItemsCount(sip2PatronInformationResponse.getOverdueItemsCount());
            patronInformationResponse.setOverdueItemsLimit(sip2PatronInformationResponse.getOverdueItemsLimit());
            patronInformationResponse.setPacAccessType(sip2PatronInformationResponse.getPacAccessType());
            patronInformationResponse.setPatronGroup(sip2PatronInformationResponse.getPatronGroup());
            patronInformationResponse.setPatronType(sip2PatronInformationResponse.getPatronType());
            patronInformationResponse.setDueDate(sip2PatronInformationResponse.getDueDate());
            patronInformationResponse.setExpirationDate(sip2PatronInformationResponse.getExpirationDate());
            patronInformationResponse.setStatus(sip2PatronInformationResponse.getStatus().toString());

        } catch (Exception ex) {
            logger.error("", ex);
        } finally {
            connection.close();
        }
        return patronInformationResponse;
    }

    public ItemRecallResponse recallItem(String itemIdentifier, String patronIdentifier, String institutionId, String expirationDate, String bibId, String pickupLocation) {
        SIP2RecallResponse sip2RecallResponse = null;
        SIP2SocketConnection connection = getSocketConnection();
        ItemRecallResponse itemRecallResponse = new ItemRecallResponse();
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
                        recallRequest.setExpirationDate(MessageUtil.createFutureDate(ReCAPConstants.ESIPEXPIRATION_DATE_DAY, ReCAPConstants.ESIPEXPIRATION_DATE_MONTH));
                        recallRequest.setBibId(bibId);
                        recallRequest.setPickupLocation(pickupLocation);

                        logger.info("Request Recall -> " + recallRequest.getData());
                        itemRecallResponse.setEsipDataIn(recallRequest.getData());
                        sip2RecallResponse = (SIP2RecallResponse) connection.send(recallRequest);
                        itemRecallResponse.setEsipDataOut(sip2RecallResponse.getData());

                        itemRecallResponse.setItemBarcode(sip2RecallResponse.getItemIdentifier());
                        itemRecallResponse.setScreenMessage((sip2RecallResponse.getScreenMessage().size() > 0) ? sip2RecallResponse.getScreenMessage().get(0) : "");
                        itemRecallResponse.setSuccess(sip2RecallResponse.isOk());
                        itemRecallResponse.setTitleIdentifier(sip2RecallResponse.getTitleIdentifier());
                        itemRecallResponse.setTransactionDate(formatFromSipDate(sip2RecallResponse.getDueDate()));
                        itemRecallResponse.setExpirationDate(formatFromSipDate(sip2RecallResponse.getExpirationDate()));
                        itemRecallResponse.setInstitutionID(sip2RecallResponse.getInstitutionId());
                        itemRecallResponse.setPickupLocation(sip2RecallResponse.getPickupLocation());
                        itemRecallResponse.setPatronIdentifier(sip2RecallResponse.getPatronIdentifier());
                    } else {
                        logger.info("Invalid Patron");
                        itemRecallResponse.setScreenMessage("Invalid Patron");
                        itemRecallResponse.setSuccess(true);
                    }
                } else {
                    logger.info("Login Failed");
                    itemRecallResponse.setScreenMessage("Login failed");
                    itemRecallResponse.setSuccess(true);
                }
            }
        } catch (InvalidSIP2ResponseException e) {
            logger.error("Connection Invalid SIP2 Response = ", e);
        } catch (InvalidSIP2ResponseValueException e) {
            logger.error("Connection Invalid SIP2 Value = ", e);
        } finally {
            connection.close();
        }
        return itemRecallResponse;
    }

    private String formatFromSipDate(String sipDate) {
        SimpleDateFormat sipFormat = new SimpleDateFormat("yyyyMMdd    HHmmss");
        SimpleDateFormat requiredFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String reformattedStr = "";
        try {
            if (sipDate != null && sipDate.trim().length() > 0) {
                reformattedStr = requiredFormat.format(sipFormat.parse(sipDate));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return reformattedStr;
    }
}
