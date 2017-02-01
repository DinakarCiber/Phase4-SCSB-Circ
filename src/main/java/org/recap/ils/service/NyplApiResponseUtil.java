package org.recap.ils.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.recap.ReCAPConstants;
import org.recap.ils.model.nypl.*;
import org.recap.ils.model.nypl.response.*;
import org.recap.ils.model.response.ItemCheckinResponse;
import org.recap.ils.model.response.ItemCheckoutResponse;
import org.recap.ils.model.response.ItemHoldResponse;
import org.recap.ils.model.response.ItemInformationResponse;
import org.recap.model.ItemEntity;
import org.recap.repository.ItemDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by rajeshbabuk on 20/12/16.
 */

@Service
public class NyplApiResponseUtil {

    @Autowired
    ItemDetailsRepository itemDetailsRepository;

    public ItemInformationResponse buildItemInformationResponse(ItemResponse itemResponse) {
        ItemInformationResponse itemInformationResponse = new ItemInformationResponse();
        ItemData itemData = itemResponse.getItemData();
        itemInformationResponse.setItemBarcode((String) itemData.getBarcode());
        itemInformationResponse.setBibID(itemData.getBibIds().get(0));
        itemInformationResponse.setBibIds(itemData.getBibIds());
        itemInformationResponse.setCallNumber((String) itemData.getCallNumber());
        itemInformationResponse.setItemType((String) itemData.getItemType());
        itemInformationResponse.setSource(itemData.getNyplSource());
        itemInformationResponse.setUpdatedDate(itemData.getUpdatedDate());
        itemInformationResponse.setCreatedDate(itemData.getCreatedDate());
        itemInformationResponse.setDeletedDate((String) itemData.getDeletedDate());
        itemInformationResponse.setDeleted(itemData.getDeleted() != null ? (Boolean) itemData.getDeleted() : false);
        if (null != itemData.getStatus()) {
            itemInformationResponse.setDueDate((String) ((LinkedHashMap) itemData.getStatus()).get("dueDate"));
            itemInformationResponse.setCirculationStatus((String) ((LinkedHashMap) itemData.getStatus()).get("display"));
        }
        if (null != itemData.getLocation()) {
            itemInformationResponse.setCurrentLocation((String) ((LinkedHashMap) itemData.getLocation()).get("name"));
        }
        itemInformationResponse.setSuccess(true);
        return itemInformationResponse;
    }

    public ItemCheckoutResponse buildItemCheckoutResponse(CheckoutResponse checkoutResponse) {
        ItemCheckoutResponse itemCheckoutResponse = new ItemCheckoutResponse();
        CheckoutData checkoutData = checkoutResponse.getData();
        itemCheckoutResponse.setItemBarcode(checkoutData.getItemBarcode());
        itemCheckoutResponse.setPatronIdentifier(checkoutData.getPatronBarcode());
        itemCheckoutResponse.setCreatedDate(checkoutData.getCreatedDate());
        itemCheckoutResponse.setUpdatedDate((String) checkoutData.getUpdatedDate());
        itemCheckoutResponse.setDueDate(checkoutData.getDesiredDateDue());
        itemCheckoutResponse.setProcessed(checkoutData.getProcessed());
        itemCheckoutResponse.setJobId(checkoutData.getJobId());
        itemCheckoutResponse.setSuccess(checkoutData.getSuccess());
        return itemCheckoutResponse;
    }

    public ItemCheckinResponse buildItemCheckinResponse(CheckinResponse checkinResponse) {
        ItemCheckinResponse itemCheckinResponse = new ItemCheckinResponse();
        CheckinData checkinData = checkinResponse.getData();
        itemCheckinResponse.setItemBarcode(checkinData.getItemBarcode());
        itemCheckinResponse.setCreatedDate(checkinData.getCreatedDate());
        itemCheckinResponse.setUpdatedDate((String) checkinData.getUpdatedDate());
        itemCheckinResponse.setProcessed(checkinData.getProcessed());
        itemCheckinResponse.setJobId(checkinData.getJobId());
        itemCheckinResponse.setSuccess(checkinData.getSuccess());
        return itemCheckinResponse;
    }

    public ItemHoldResponse buildItemHoldResponse(CreateHoldResponse createHoldResponse) throws Exception {
        ItemHoldResponse itemHoldResponse = new ItemHoldResponse();
        CreateHoldData holdData = createHoldResponse.getData();
        itemHoldResponse.setItemOwningInstitution(holdData.getOwningInstitutionId());
        itemHoldResponse.setItemBarcode(holdData.getItemBarcode());
        itemHoldResponse.setPatronIdentifier(holdData.getPatronBarcode());
        itemHoldResponse.setTrackingId(holdData.getTrackingId());
        itemHoldResponse.setCreatedDate(holdData.getCreatedDate());
        itemHoldResponse.setUpdatedDate((String) holdData.getUpdatedDate());
        itemHoldResponse.setExpirationDate(getExpirationDateForNypl());
        return itemHoldResponse;
    }

    public ItemHoldResponse buildItemCancelHoldResponse(CancelHoldResponse cancelHoldResponse) {
        ItemHoldResponse itemHoldResponse = new ItemHoldResponse();
        CancelHoldData holdData = cancelHoldResponse.getData();
        itemHoldResponse.setItemOwningInstitution(holdData.getOwningInstitutionId());
        itemHoldResponse.setItemBarcode(holdData.getItemBarcode());
        itemHoldResponse.setPatronIdentifier(holdData.getPatronBarcode());
        itemHoldResponse.setTrackingId(holdData.getTrackingId());
        itemHoldResponse.setCreatedDate(holdData.getCreatedDate());
        itemHoldResponse.setUpdatedDate((String) holdData.getUpdatedDate());
        return itemHoldResponse;
    }

    public String getJobStatusMessage(JobData jobData) throws Exception {
        String jobStatus = null;
        List<Notice> notices = jobData.getNotices();
        if (CollectionUtils.isNotEmpty(notices)) {
            Collections.reverse(notices);
            Notice notice = notices.get(0);
            jobStatus = notice.getText();
        }
        return jobStatus;
    }

    public String getNyplSource(String institutionId) {
        String nyplSource = null;
        if (StringUtils.isNotBlank(institutionId)) {
            if (institutionId.equalsIgnoreCase(ReCAPConstants.NYPL)) {
                nyplSource = ReCAPConstants.NYPL_SOURCE_NYPL;
            } else if (institutionId.equalsIgnoreCase(ReCAPConstants.PRINCETON)) {
                nyplSource = ReCAPConstants.NYPL_SOURCE_PUL;
            } else if (institutionId.equalsIgnoreCase(ReCAPConstants.COLUMBIA)) {
                nyplSource = ReCAPConstants.NYPL_SOURCE_CUL;
            }
        }
        return nyplSource;
    }

    public String getNormalizedItemIdForNypl(String itemBarcode) throws Exception {
        String itemId = null;
        List<ItemEntity> itemEntities = itemDetailsRepository.findByBarcode(itemBarcode);
        if (CollectionUtils.isNotEmpty(itemEntities)) {
            ItemEntity itemEntity = itemEntities.get(0);
            if (null != itemEntity.getInstitutionEntity()) {
                String institutionCode = itemEntity.getInstitutionEntity().getInstitutionCode();
                itemId = itemEntity.getOwningInstitutionItemId();
                if (ReCAPConstants.NYPL.equalsIgnoreCase(institutionCode)) {
                    itemId = itemId.replace(".i", ""); // Remove prefix .i
                    itemId = StringUtils.chop(itemId); // Remove last check digit or char
                }
            }
        }
        return itemId;
    }

    public String getExpirationDateForNypl() throws Exception {
        Date expirationDate = DateUtils.addYears(new Date(), 1);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ReCAPConstants.NYPL_HOLD_DATE_FORMAT);
        return simpleDateFormat.format(expirationDate);
    }

    public String getItemOwningInstitutionByItemBarcode(String itemBarcode) throws Exception {
        String institutionCode = null;
        List<ItemEntity> itemEntities = itemDetailsRepository.findByBarcode(itemBarcode);
        if (CollectionUtils.isNotEmpty(itemEntities)) {
            ItemEntity itemEntity = itemEntities.get(0);
            if (null != itemEntity.getInstitutionEntity()) {
                institutionCode = itemEntity.getInstitutionEntity().getInstitutionCode();
            }
        }
        return institutionCode;
    }

}
