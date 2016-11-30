package org.recap;

/**
 * Created by hemalathas on 10/11/16.
 */
public class ReCAPConstants {

    public static final String COLUMBIA = "CUL";
    public static final String PRINCETON = "PUL";
    public static final String NYPL = "NYPL";

    public static final String RESPONSE_DATE = "Date";

    public static final String REGEX_FOR_EMAIL_ADDRESS = "^[A-Za-z0-9+_.-]+@(.+)$";
    public static final String INVALID_REQUEST_INSTITUTION = "Please enter valid Institution PUL/CUL/NYPL for requestingInstitution";
    public static final String INVALID_EMAIL_ADDRESS = "Please enter valid emailAddress";
    public static final String START_PAGE_AND_END_PAGE_REQUIRED = "Startpage and endpage information is required for the request type EDD";
    public static final String INVALID_PAGE_NUMBER = "Page number should starts with 1";
    public static final String INVALID_END_PAGE = "End page should not be 0 and less than or equal to start page";
    public static final String DELIVERY_LOCATION_REQUIRED = "Delivery Location is required for request type Recall/hold/retrieval";
    public static final String EMPTY_PATRON_BARCODE = "Patron barcode should not be null or empty.Please enter the valid patron barcode";
    public static final String INVALID_REQUEST_TYPE = "Please enter the valid request type";
    public static final String RETRIEVAL = "Retrieval";
    public static final String HOLD = "Hold";
    public static final String RECALL = "Recall";
    public static final String EDD_REQUEST = "EDD";
    public static final String BORROW_DIRECT = "Borrow Direct";
    public static final String PHYSICAL_REQUEST = "Physical";
    public static final String VALID_REQUEST = "All request parameters are valid.Patron is eligible to raise a request";
    public static final String INVALID_PATRON = "Patron is not available";
    public static final String VALID_PATRON = "Patron validated successfully.";
    public static final String AVAILABLE = "Available";
}
