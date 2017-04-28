package org.recap.request;

import org.apache.commons.lang3.StringUtils;
import org.recap.ReCAPConstants;
import org.recap.ils.model.response.ItemInformationResponse;
import org.recap.model.*;
import org.recap.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by sudhishk on 1/12/16.
 */
@Component
public class ItemRequestDBService {

    private static final Logger logger = LoggerFactory.getLogger(ItemRequestDBService.class);

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private RequestItemDetailsRepository requestItemDetailsRepository;

    /**
     * The Item change log details repository.
     */
    @Autowired
    ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;

    @Autowired
    private RequestItemStatusDetailsRepository requestItemStatusDetailsRepository;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private RequestTypeDetailsRepository requestTypeDetailsRepository;


    /**
     * Update recap request item integer.
     *
     * @param itemRequestInformation the item request information
     * @param itemEntity             the item entity
     * @param requestStatusCode      the request status code
     * @return the integer
     */
    public Integer updateRecapRequestItem(ItemRequestInformation itemRequestInformation, ItemEntity itemEntity, String requestStatusCode) {

        RequestItemEntity requestItemEntity = new RequestItemEntity();
        RequestItemEntity savedItemRequest;
        Integer requestId = 0;
        try {
            RequestStatusEntity requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(requestStatusCode);
            InstitutionEntity institutionEntity = institutionDetailsRepository.findByInstitutionCode(itemRequestInformation.getRequestingInstitution());
            RequestTypeEntity requestTypeEntity = requestTypeDetailsRepository.findByrequestTypeCode(itemRequestInformation.getRequestType());
            //Request Item
            if (itemRequestInformation.getRequestId() != null && itemRequestInformation.getRequestId() > 0) {
                requestItemEntity = requestItemDetailsRepository.findByRequestId(itemRequestInformation.getRequestId());
                requestItemEntity.setRequestExpirationDate(getExpirationDate(itemRequestInformation.getExpirationDate(), itemRequestInformation.getRequestingInstitution()));
                requestItemEntity.setLastUpdatedDate(new Date());
                requestItemEntity.setRequestStatusId(requestStatusEntity.getRequestStatusId());
                requestItemEntity.setNotes(itemRequestInformation.getRequestNotes());
            } else {
                requestItemEntity.setItemId(itemEntity.getItemId());
                requestItemEntity.setRequestingInstitutionId(institutionEntity.getInstitutionId());
                requestItemEntity.setRequestTypeId(requestTypeEntity.getRequestTypeId());
                requestItemEntity.setRequestExpirationDate(getExpirationDate(itemRequestInformation.getExpirationDate(), itemRequestInformation.getRequestingInstitution()));
                requestItemEntity.setCreatedBy(getUser(itemRequestInformation.getUsername()));
                requestItemEntity.setCreatedDate(new Date());
                requestItemEntity.setLastUpdatedDate(new Date());
                requestItemEntity.setPatronId(itemRequestInformation.getPatronBarcode());
                requestItemEntity.setStopCode(itemRequestInformation.getDeliveryLocation());
                requestItemEntity.setRequestStatusId(requestStatusEntity.getRequestStatusId());
                requestItemEntity.setEmailId(itemRequestInformation.getEmailAddress());
                requestItemEntity.setNotes(itemRequestInformation.getRequestNotes());
            }
            savedItemRequest = requestItemDetailsRepository.save(requestItemEntity);
            if (savedItemRequest != null) {
                requestId = savedItemRequest.getRequestId();
                saveItemChangeLogEntity(savedItemRequest.getRequestId(), getUser(itemRequestInformation.getUsername()), ReCAPConstants.REQUEST_ITEM_INSERT, savedItemRequest.getItemId() + " - " + savedItemRequest.getPatronId());
            }
            logger.info("SCSB DB Update Successful");
        } catch (ParseException e) {
            logger.error(ReCAPConstants.REQUEST_PARSE_EXCEPTION, e);
        } catch (Exception e) {
            logger.error(ReCAPConstants.REQUEST_EXCEPTION, e);
        }
        return requestId;
    }

    /**
     * Update recap request item item information response.
     *
     * @param itemInformationResponse the item information response
     * @return the item information response
     */
    public ItemInformationResponse updateRecapRequestItem(ItemInformationResponse itemInformationResponse) {

        RequestItemEntity requestItemEntity;
        RequestItemEntity savedItemRequest;
        RequestStatusEntity requestStatusEntity=null;
        Integer requestId = 0;
        try {
            if (!itemInformationResponse.isSuccess()) {
                requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(ReCAPConstants.REQUEST_STATUS_EXCEPTION);
            }else {
                if (itemInformationResponse.getRequestType().equalsIgnoreCase(ReCAPConstants.REQUEST_TYPE_RETRIEVAL)){
                    requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(ReCAPConstants.REQUEST_STATUS_RETRIEVAL_ORDER_PLACED);
                }else if (itemInformationResponse.getRequestType().equalsIgnoreCase(ReCAPConstants.REQUEST_TYPE_EDD)){
                    requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(ReCAPConstants.REQUEST_STATUS_EDD);
                }else if (itemInformationResponse.getRequestType().equalsIgnoreCase(ReCAPConstants.REQUEST_TYPE_RECALL)){
                    requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(ReCAPConstants.REQUEST_STATUS_RECALLED);
                }
            }

            if (itemInformationResponse.getRequestId() != null && itemInformationResponse.getRequestId() > 0) {
                requestItemEntity = requestItemDetailsRepository.findByRequestId(itemInformationResponse.getRequestId());
                requestItemEntity.setRequestStatusId(requestStatusEntity.getRequestStatusId());
                requestItemEntity.setRequestExpirationDate(getExpirationDate(itemInformationResponse.getExpirationDate(), itemInformationResponse.getRequestingInstitution()));
                requestItemEntity.setNotes(itemInformationResponse.getRequestNotes());
                requestItemEntity.setLastUpdatedDate(new Date());
            } else {
                requestItemEntity = new RequestItemEntity();
                RequestTypeEntity requestTypeEntity = requestTypeDetailsRepository.findByrequestTypeCode(itemInformationResponse.getRequestType());
                InstitutionEntity institutionEntity = institutionDetailsRepository.findByInstitutionCode(itemInformationResponse.getRequestingInstitution());

                //Request Item
                requestItemEntity.setItemId(itemInformationResponse.getItemId());
                requestItemEntity.setRequestingInstitutionId(institutionEntity.getInstitutionId());
                requestItemEntity.setRequestTypeId(requestTypeEntity.getRequestTypeId());
                requestItemEntity.setRequestExpirationDate(getExpirationDate(itemInformationResponse.getExpirationDate(), itemInformationResponse.getRequestingInstitution()));
                requestItemEntity.setCreatedBy(getUser(itemInformationResponse.getUsername()));
                requestItemEntity.setCreatedDate(new Date());
                requestItemEntity.setLastUpdatedDate(new Date());
                requestItemEntity.setPatronId(itemInformationResponse.getPatronBarcode());
                requestItemEntity.setStopCode(itemInformationResponse.getDeliveryLocation());
                requestItemEntity.setRequestStatusId(requestStatusEntity.getRequestStatusId());
                requestItemEntity.setEmailId(itemInformationResponse.getEmailAddress());
                requestItemEntity.setNotes(itemInformationResponse.getRequestNotes());
            }
            savedItemRequest = requestItemDetailsRepository.save(requestItemEntity);
            if (savedItemRequest != null) {
                requestId = savedItemRequest.getRequestId();
                saveItemChangeLogEntity(savedItemRequest.getRequestId(), getUser(itemInformationResponse.getUsername()), ReCAPConstants.REQUEST_ITEM_INSERT, savedItemRequest.getItemId() + " - " + savedItemRequest.getPatronId());
            }
            itemInformationResponse.setRequestId(requestId);
            logger.info("SCSB DB Update Successful");
        } catch (ParseException e) {
            logger.error(ReCAPConstants.REQUEST_PARSE_EXCEPTION, e);
        } catch (Exception e) {
            logger.error(ReCAPConstants.REQUEST_EXCEPTION, e);
        }
        return itemInformationResponse;
    }

    /**
     * Update recap request status item information response.
     *
     * @param itemInformationResponse the item information response
     * @return the item information response
     */
    public ItemInformationResponse updateRecapRequestStatus(ItemInformationResponse itemInformationResponse) {
        RequestStatusEntity requestStatusEntity;
        RequestItemEntity requestItemEntity = requestItemDetailsRepository.findByRequestId(itemInformationResponse.getRequestId());
        if (itemInformationResponse.isSuccess()) {
            requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(ReCAPConstants.REQUEST_STATUS_PENDING);
        } else {
            requestStatusEntity = requestItemStatusDetailsRepository.findByRequestStatusCode(ReCAPConstants.REQUEST_STATUS_EXCEPTION);
        }
        requestItemEntity.setRequestStatusId(requestStatusEntity.getRequestStatusId());
        requestItemDetailsRepository.save(requestItemEntity);
        return itemInformationResponse;
    }

    /**
     * Update item availabiluty status.
     *
     * @param itemEntities the item entities
     * @param userName     the user name
     */
    public void updateItemAvailabilutyStatus(List<ItemEntity> itemEntities, String userName) {
        for (ItemEntity itemEntity : itemEntities) {
            itemEntity.setItemAvailabilityStatusId(2); // Not Available
            itemEntity.setLastUpdatedBy(getUser(userName));
            itemEntity.setLastUpdatedDate(new Date());

            saveItemChangeLogEntity(itemEntity.getItemId(), getUser(userName), ReCAPConstants.REQUEST_ITEM_AVAILABILITY_STATUS_UPDATE, ReCAPConstants.REQUEST_ITEM_AVAILABILITY_STATUS_DATA_UPDATE);
        }
        // Not Available
        itemDetailsRepository.save(itemEntities);

    }

    /**
     * Rollback update item availabiluty status.
     *
     * @param itemEntity the item entity
     * @param userName   the user name
     */
    public void rollbackUpdateItemAvailabilutyStatus(ItemEntity itemEntity, String userName) {
        itemEntity.setItemAvailabilityStatusId(1); // Available
        itemEntity.setLastUpdatedBy(getUser(userName));
        itemEntity.setLastUpdatedDate(new Date());
        itemDetailsRepository.save(itemEntity);
        saveItemChangeLogEntity(itemEntity.getItemId(), getUser(userName), ReCAPConstants.REQUEST_ITEM_AVAILABILITY_STATUS_UPDATE, ReCAPConstants.REQUEST_ITEM_AVAILABILITY_STATUS_DATA_ROLLBACK);
    }

    /**
     * Save item change log entity.
     *
     * @param recordId      the record id
     * @param userName      the user name
     * @param operationType the operation type
     * @param notes         the notes
     */
    public void saveItemChangeLogEntity(Integer recordId, String userName, String operationType, String notes) {
        ItemChangeLogEntity itemChangeLogEntity = new ItemChangeLogEntity();
        itemChangeLogEntity.setUpdatedBy(userName);
        itemChangeLogEntity.setUpdatedDate(new Date());
        itemChangeLogEntity.setOperationType(operationType);
        itemChangeLogEntity.setRecordId(recordId);
        itemChangeLogEntity.setNotes(notes);
        itemChangeLogDetailsRepository.save(itemChangeLogEntity);
    }

    /**
     * Gets user.
     *
     * @param userId the user id
     * @return the user
     */
    public String getUser(String userId) {
        if (StringUtils.isBlank(userId)) {
            return "Discovery";
        } else {
            return userId;
        }
    }

    private Date getExpirationDate(String expirationDate, String requestingInstitutionId) throws ParseException {
        if (StringUtils.isNotBlank(expirationDate)) {
            if (ReCAPConstants.NYPL.equalsIgnoreCase(requestingInstitutionId)) {
                DateFormat dateFormatter = new SimpleDateFormat(ReCAPConstants.NYPL_HOLD_DATE_FORMAT);
                return dateFormatter.parse(expirationDate);
            } else {
                return simpleDateFormat.parse(expirationDate);
            }
        }
        return null;
    }

    /**
     * Rollback after gfa item request information.
     *
     * @param itemInformationResponse the item information response
     * @return the item request information
     */
    public ItemRequestInformation rollbackAfterGFA(ItemInformationResponse itemInformationResponse) {
        ItemRequestInformation itemRequestInformation = new ItemRequestInformation();
        RequestItemEntity requestItemEntity = requestItemDetailsRepository.findByRequestId(itemInformationResponse.getRequestId());
        rollbackUpdateItemAvailabilutyStatus(requestItemEntity.getItemEntity(), ReCAPConstants.GUEST_USER);
        saveItemChangeLogEntity(itemInformationResponse.getRequestId(), ReCAPConstants.GUEST_USER, ReCAPConstants.REQUEST_ITEM_GFA_FAILURE, ReCAPConstants.GFA_ITEM_STATUS_CHECK_FAILED);
        itemRequestInformation.setBibId(requestItemEntity.getItemEntity().getBibliographicEntities().get(0).getOwningInstitutionBibId());
        itemRequestInformation.setPatronBarcode(requestItemEntity.getPatronId());
        itemRequestInformation.setItemBarcodes(Arrays.asList(requestItemEntity.getItemEntity().getBarcode()));


        return itemRequestInformation;
    }

}
