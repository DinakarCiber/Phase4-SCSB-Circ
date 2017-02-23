package org.recap.request;

import org.apache.camel.Exchange;
import org.recap.ReCAPConstants;
import org.recap.controller.RequestItemValidatorController;
import org.recap.ils.model.response.ItemInformationResponse;
import org.recap.model.ItemEntity;
import org.recap.model.ItemRequestInformation;
import org.recap.model.RequestTypeEntity;
import org.recap.repository.ItemDetailsRepository;
import org.recap.repository.RequestTypeDetailsRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Created by sudhishk on 1/12/16.
 */
@Component
public class ItemEDDRequestService {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private RequestTypeDetailsRepository requestTypeDetailsRepository;

    @Autowired
    private RequestItemValidatorController requestItemValidatorController;

    @Autowired
    private ItemRequestService itemRequestService;

    public ItemInformationResponse eddRequestItem(ItemRequestInformation itemRequestInfo, Exchange exchange) {

        String messagePublish;
        boolean bsuccess;
        List<ItemEntity> itemEntities;
        ItemEntity itemEntity;
        RequestTypeEntity requestTypeEntity;
        ItemInformationResponse itemResponseInformation = new ItemInformationResponse();
        ResponseEntity res;

        try {
            itemEntities = itemDetailsRepository.findByBarcodeIn(itemRequestInfo.getItemBarcodes());

            if (itemEntities != null && !itemEntities.isEmpty()) {
                logger.info("Item Exists in SCSB Database");
                itemEntity = itemEntities.get(0);
                if (itemEntity.getBibliographicEntities().get(0).getOwningInstitutionBibId().trim().length() <= 0) {
                    itemRequestInfo.setBibId(itemEntity.getBibliographicEntities().get(0).getOwningInstitutionBibId());
                }
                itemRequestInfo.setItemOwningInstitution(itemEntity.getInstitutionEntity().getInstitutionCode());
                itemRequestInfo.setTitleIdentifier(itemRequestService.getTitle(itemRequestInfo.getTitleIdentifier(), itemEntity));
                itemRequestInfo.setCustomerCode(itemEntity.getCustomerCode());
                // Validate Patron
                res = requestItemValidatorController.validateItemRequestInformations(itemRequestInfo);
                if (res.getStatusCode() == HttpStatus.OK) {
                    logger.info("Request Validation Successful");
                    requestTypeEntity = requestTypeDetailsRepository.findByrequestTypeCode(itemRequestInfo.getRequestType());
                    itemResponseInformation = itemRequestService.updateGFA(itemRequestInfo, itemResponseInformation);
                    if (itemResponseInformation.isSuccess()) {
                        itemRequestService.updateRecapRequestItem(itemRequestInfo, itemEntity, requestTypeEntity, ReCAPConstants.REQUEST_STATUS_EDD);
                        bsuccess = true;
                        messagePublish = "EDD requests is successfull";
                    } else {
                        bsuccess = false;
                        messagePublish = itemResponseInformation.getScreenMessage();
                    }
                } else {
                    logger.warn("Validate Request Errors : " + res.getBody().toString());
                    messagePublish = res.getBody().toString();
                    bsuccess = false;
                }
                itemResponseInformation.setItemId(itemEntity.getItemId());
            } else {
                messagePublish = ReCAPConstants.WRONG_ITEM_BARCODE;
                bsuccess = false;
            }
            logger.info("Finish Processing");
            itemResponseInformation.setScreenMessage(messagePublish);
            itemResponseInformation.setSuccess(bsuccess);
            itemResponseInformation.setItemOwningInstitution(itemRequestInfo.getItemOwningInstitution());
            itemResponseInformation.setDueDate(itemRequestInfo.getExpirationDate());
            itemResponseInformation.setRequestingInstitution(itemRequestInfo.getRequestingInstitution());
            itemResponseInformation.setTitleIdentifier(itemRequestInfo.getTitleIdentifier());
            itemResponseInformation.setPatronBarcode(itemRequestInfo.getPatronBarcode());
            itemResponseInformation.setBibID(itemRequestInfo.getBibId());
            itemResponseInformation.setItemBarcode(itemRequestInfo.getItemBarcodes().get(0));
            itemResponseInformation.setRequestType(itemRequestInfo.getRequestType());
            itemResponseInformation.setEmailAddress(itemRequestInfo.getEmailAddress());
            itemResponseInformation.setDeliveryLocation(itemRequestInfo.getDeliveryLocation());
            itemResponseInformation.setRequestNotes(itemRequestInfo.getRequestNotes());
            // Update Topics
            itemRequestService.sendMessageToTopic(itemRequestInfo.getRequestingInstitution(), itemRequestInfo.getRequestType(), itemResponseInformation, exchange);
        } catch (RestClientException ex) {
            logger.error(ReCAPConstants.REQUEST_EXCEPTION_REST,ex);
        } catch (Exception ex) {
            logger.error(ReCAPConstants.REQUEST_EXCEPTION,ex);
        }
        return itemResponseInformation;
    }
}
