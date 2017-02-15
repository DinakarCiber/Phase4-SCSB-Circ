package org.recap.controller;

import org.recap.ReCAPConstants;
import org.recap.service.submitcollection.SubmitCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Created by premkb on 21/12/16.
 */
@RestController
@RequestMapping("/sharedCollection")
public class SharedCollectionRestController {

    Logger logger = LoggerFactory.getLogger(SharedCollectionRestController.class);

    @Autowired
    private SubmitCollectionService submitCollectionService;

    @RequestMapping(value = "/submitCollection", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity submitCollection(@RequestBody String inputRecords){
        ResponseEntity responseEntity;
        String response;
        List<Integer> processedBibIdList = new ArrayList<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        try {
            response = submitCollectionService.process(inputRecords,processedBibIdList,idMapToRemoveIndex);
            if (response.contains(ReCAPConstants.SUMBIT_COLLECTION_UPDATE_MESSAGE)) {
                String indexResponse = submitCollectionService.indexData(processedBibIdList);
                if (idMapToRemoveIndex.size()>0) {//remove the incomplete record from solr index
                    response  = submitCollectionService.removeSolrIndex(idMapToRemoveIndex);
                }
                if(!indexResponse.equalsIgnoreCase(ReCAPConstants.SUCCESS)){
                    response = indexResponse;
                }
            }
            responseEntity = new ResponseEntity(response,getHttpHeaders(), HttpStatus.OK);
        } catch (Exception e) {
            logger.error(ReCAPConstants.LOG_ERROR,e);
            response = ReCAPConstants.FAILURE;
            responseEntity = new ResponseEntity(response,getHttpHeaders(), HttpStatus.OK);
        }
        return responseEntity;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(ReCAPConstants.RESPONSE_DATE, new Date().toString());
        return responseHeaders;
    }
}
