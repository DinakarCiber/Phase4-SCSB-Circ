package org.recap.controller;

import io.swagger.annotations.ApiParam;
import org.recap.ReCAPConstants;
import org.recap.ils.model.response.ItemHoldResponse;
import org.recap.ils.model.response.ItemInformationResponse;
import org.recap.model.*;
import org.recap.repository.RequestItemDetailsRepository;
import org.recap.repository.RequestItemStatusDetailsRepository;
import org.recap.request.ItemRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Created by sudhishk on 31/01/17.
 */
@RestController
@RequestMapping("/cancelRequest")
public class CancelItemController {

    public static final String REQUEST_CANCELLATION_NOT_ACTIVE = "RequestId is not active status to be canceled";
    public static final String REQUEST_CANCELLATION_DOES_NOT_EXIST = "RequestId does not exist";
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RequestItemController requestItemController;

    @Autowired
    private RequestItemDetailsRepository requestItemDetailsRepository;

    @Autowired
    private RequestItemStatusDetailsRepository requestItemStatusDetailsRepository;

    @Autowired
    private ItemRequestService itemRequestService;

    @RequestMapping(value = "/cancel", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CancelRequestResponse cancelRequest(@ApiParam(value = "Parameters for cancelling", required = true, name = "requestId") @RequestParam Integer requestId) {
        CancelRequestResponse cancelRequestResponse = new CancelRequestResponse();
        ItemHoldResponse itemCanceHoldResponse;
        boolean bSuccess = false;
        String screenMessage = "";
        try {
            RequestItemEntity requestItemEntity = requestItemDetailsRepository.findByRequestId(requestId);
            if (requestItemEntity != null) {
                String requestStatus = requestItemEntity.getRequestStatusEntity().getRequestStatusCode();
                if (requestStatus.equalsIgnoreCase(ReCAPConstants.REQUEST_STATUS_RETRIEVAL_ORDER_PLACED) || requestStatus.equalsIgnoreCase(ReCAPConstants.REQUEST_STATUS_RECALLED)) {
                    ItemEntity itemEntity = requestItemEntity.getItemEntity();

                    ItemRequestInformation itemRequestInformation = new ItemRequestInformation();
                    itemRequestInformation.setItemBarcodes(Arrays.asList(itemEntity.getBarcode()));
                    itemRequestInformation.setItemOwningInstitution(itemEntity.getInstitutionEntity().getInstitutionCode());
                    itemRequestInformation.setBibId(itemEntity.getBibliographicEntities().get(0).getOwningInstitutionBibId());

                    itemRequestInformation.setRequestingInstitution(requestItemEntity.getInstitutionEntity().getInstitutionCode());
                    itemRequestInformation.setPatronBarcode(requestItemEntity.getPatronEntity().getInstitutionIdentifier());
                    itemRequestInformation.setDeliveryLocation(requestItemEntity.getStopCode());

                    ItemInformationResponse itemInformationResponse = (ItemInformationResponse) requestItemController.itemInformation(itemRequestInformation, itemRequestInformation.getRequestingInstitution());
                    int iholdQueue = 0;
                    if (itemInformationResponse.getHoldQueueLength().trim().length() > 0) {
                        iholdQueue = Integer.parseInt(itemInformationResponse.getHoldQueueLength());
                    }
                    if (iholdQueue > 0 && (itemInformationResponse.getCirculationStatus().equalsIgnoreCase(ReCAPConstants.CIRCULATION_STATUS_OTHER) || itemInformationResponse.getCirculationStatus().equalsIgnoreCase(ReCAPConstants.CIRCULATION_STATUS_IN_TRANSIT))) {
                        ItemInformationResponse itemInformation = (ItemInformationResponse) requestItemController.itemInformation(itemRequestInformation, itemRequestInformation.getRequestingInstitution());
                        itemRequestInformation.setBibId(itemInformation.getBibID());
                        itemCanceHoldResponse = (ItemHoldResponse) requestItemController.cancelHoldItem(itemRequestInformation, itemRequestInformation.getRequestingInstitution());
                        if (itemCanceHoldResponse.isSuccess()) {
                            if (!itemRequestInformation.getItemOwningInstitution().equalsIgnoreCase(ReCAPConstants.COLUMBIA)) {
                                requestItemController.checkinItem(itemRequestInformation, itemRequestInformation.getItemOwningInstitution());
                            }
                            RequestStatusEntity requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(ReCAPConstants.REQUEST_STATUS_CANCELED);
                            requestItemEntity.setRequestStatusId(requestStatusEntity.getRequestStatusId());
                            if (requestItemEntity.getRequestTypeEntity().getRequestTypeCode().equalsIgnoreCase(ReCAPConstants.REQUEST_TYPE_RETRIEVAL)) {
                                requestItemEntity.getItemEntity().setItemAvailabilityStatusId(1);
                            }
                            RequestItemEntity savedRequestItemEntity = requestItemDetailsRepository.save(requestItemEntity);
                            itemRequestService.saveItemChangeLogEntity(savedRequestItemEntity.getRequestId(), ReCAPConstants.GUEST_USER, ReCAPConstants.REQUEST_ITEM_CANCEL_ITEM_AVAILABILITY_STATUS, ReCAPConstants.REQUEST_STATUS_CANCELED + savedRequestItemEntity.getItemId());
                            itemRequestService.updateSolrIndex(savedRequestItemEntity.getItemEntity());
                            bSuccess = true;
                            screenMessage = ReCAPConstants.REQUEST_CANCELLATION_SUCCCESS;
                        } else {
                            bSuccess = false;
                            screenMessage = itemCanceHoldResponse.getScreenMessage();
                        }
                    } else {
                        bSuccess = false;
                        screenMessage = ReCAPConstants.REQUEST_CANCELLATION_NOT_ON_HOLD_IN_ILS;
                    }
                } else if (requestStatus.equalsIgnoreCase(ReCAPConstants.REQUEST_STATUS_EDD)){
                    RequestStatusEntity requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(ReCAPConstants.REQUEST_STATUS_CANCELED);
                    requestItemEntity.setRequestStatusId(requestStatusEntity.getRequestStatusId());
                    RequestItemEntity savedRequestItemEntity = requestItemDetailsRepository.save(requestItemEntity);
                    itemRequestService.saveItemChangeLogEntity(savedRequestItemEntity.getRequestId(), ReCAPConstants.GUEST_USER, ReCAPConstants.REQUEST_ITEM_CANCEL_ITEM_AVAILABILITY_STATUS, ReCAPConstants.REQUEST_STATUS_CANCELED + savedRequestItemEntity.getItemId());
                    bSuccess = true;
                    screenMessage = ReCAPConstants.REQUEST_CANCELLATION_EDD_SUCCCESS;
                } else {
                    bSuccess = false;
                    screenMessage = REQUEST_CANCELLATION_NOT_ACTIVE;
                }
            } else {
                bSuccess = false;
                screenMessage = REQUEST_CANCELLATION_DOES_NOT_EXIST;
            }
        } catch (Exception e) {
            bSuccess = false;
            screenMessage = e.getMessage();
            logger.error(ReCAPConstants.REQUEST_EXCEPTION, e);
        } finally {
            cancelRequestResponse.setSuccess(bSuccess);
            cancelRequestResponse.setScreenMessage(screenMessage);
        }
        return cancelRequestResponse;
    }
}
