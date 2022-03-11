package io.liquichain.api.payment;

import com.google.gson.Gson;
import io.liquichain.core.BlockForgerScript;


import java.util.*;
import java.time.Instant;
import java.math.BigInteger;
import java.io.IOException;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.CustomEntityInstance;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.persistence.CrossStorageService;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.custom.NativeCustomEntityInstanceService;
import org.meveo.api.rest.technicalservice.EndpointScript;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.meveo.service.script.Script;
import java.math.BigInteger;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.api.exception.EntityDoesNotExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.web3j.crypto.*;

import javax.servlet.http.HttpServletRequest;

public class MolliePaymentScript extends Script {

    private static final Logger log = LoggerFactory.getLogger(MolliePaymentScript.class);

    private long chainId = 76;

    private String result;

    private String method = null;

    private String originWallet = "212dFDD1Eb4ee053b2f5910808B7F53e3D49AD2f";

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();
    private CustomEntityTemplateService customEntityTemplateService = getCDIBean(CustomEntityTemplateService.class);
    private CrossStorageService crossStorageService = getCDIBean(CrossStorageService.class);

    public String getResult() {
        return result;
    }

    public void setMethod(String method) {
      	log.info("method setter {}", method);
        this.method = method;
    }
  
  	public String getMethod() {
    	return method;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
      	log.info("method from path={}", getMethod());
      result="{\r\n"
    +"    \"count\": 2,\r\n"
    +"    \"_embedded\": {\r\n"
    +"        \"methods\": [\r\n"
    +"            {\r\n"
    +"                 \"resource\": \"method\",\r\n"
    +"                 \"id\": \"licoin\",\r\n"
    +"                 \"description\": \"Licoin\",\r\n"
    +"                 \"minimumAmount\": {\r\n"
    +"                     \"value\": \"0.01\",\r\n"
    +"                     \"currency\": \"EUR\"\r\n"
    +"                 },\r\n"
    +"                 \"maximumAmount\": {\r\n"
    +"                     \"value\": \"50000.00\",\r\n"
    +"                     \"currency\": \"EUR\"\r\n"
    +"                 },\r\n"
    +"                 \"image\": {\r\n"
    +"                     \"size1x\": \"https://docs.liquichain.io/media/payments/licoin.png\",\r\n"
    +"                     \"size2x\": \"https://docs.liquichain.io/media/payments/licoin%402x.png\",\r\n"
    +"                     \"svg\": \"https://docs.liquichain.io/media/payments/licoin.svg\"\r\n"
    +"                 },\r\n"
    +"                 \"status\": \"activated\",\r\n"
    +"                 \"pricing\": [\r\n"
    +"                     {\r\n"
    +"                         \"description\": \"Netherlands\",\r\n"
    +"                         \"fixed\": {\r\n"
    +"                             \"value\": \"0.29\",\r\n"
    +"                             \"currency\": \"EUR\"\r\n"
    +"                         },\r\n"
    +"                         \"variable\": \"0\"\r\n"
    +"                     }\r\n"
    +"                 ],\r\n"
    +"                 \"_links\": {\r\n"
    +"                     \"self\": {\r\n"
    +"                         \"href\": \"https://liquichain.io/meveo/rest/gp/v1/methods/licoin\",\r\n"
    +"                         \"type\": \"application/hal+json\"\r\n"
    +"                     }\r\n"
    +"                 }\r\n"
    +"            },\r\n"
    +"            {\r\n"
    +"                 \"resource\": \"method\",\r\n"
    +"                 \"id\": \"creditcard2\",\r\n"
    +"                 \"description\": \"Credit card\",\r\n"
    +"                 \"minimumAmount\": {\r\n"
    +"                     \"value\": \"0.01\",\r\n"
    +"                     \"currency\": \"EUR\"\r\n"
    +"                 },\r\n"
    +"                 \"maximumAmount\": {\r\n"
    +"                     \"value\": \"2000.00\",\r\n"
    +"                     \"currency\": \"EUR\"\r\n"
    +"                 },\r\n"
    +"                 \"image\": {\r\n"
    +"                     \"size1x\": \"https://docs.liquichain.io/media/payments/creditcard.png\",\r\n"
    +"                     \"size2x\": \"https://docs.liquichain.io/media/payments/creditcard%402x.png\",\r\n"
    +"                     \"svg\": \"https://docs.liquichain.io/media/payments/creditcard.svg\"\r\n"
    +"                 },\r\n"
    +"                 \"status\": \"activated\",\r\n"
    +"                 \"pricing\": [\r\n"
    +"                     {\r\n"
    +"                         \"description\": \"Commercial & non-European cards\",\r\n"
    +"                         \"fixed\": {\r\n"
    +"                             \"value\": \"0.25\",\r\n"
    +"                             \"currency\": \"EUR\"\r\n"
    +"                         },\r\n"
    +"                         \"variable\": \"2.8\",\r\n"
    +"                         \"feeRegion\": \"other\"\r\n"
    +"                     },\r\n"
    +"                     {\r\n"
    +"                         \"description\": \"European cards\",\r\n"
    +"                         \"fixed\": {\r\n"
    +"                             \"value\": \"0.25\",\r\n"
    +"                             \"currency\": \"EUR\"\r\n"
    +"                         },\r\n"
    +"                         \"variable\": \"1.8\",\r\n"
    +"                         \"feeRegion\": \"eu-cards\"\r\n"
    +"                     },\r\n"
    +"                     {\r\n"
    +"                         \"description\": \"American Express\",\r\n"
    +"                         \"fixed\": {\r\n"
    +"                             \"value\": \"0.25\",\r\n"
    +"                             \"currency\": \"EUR\"\r\n"
    +"                         },\r\n"
    +"                         \"variable\": \"2.8\",\r\n"
    +"                         \"feeRegion\": \"amex\"\r\n"
    +"                     }\r\n"
    +"                 ],\r\n"
    +"                 \"_links\": {\r\n"
    +"                     \"self\": {\r\n"
    +"                         \"href\": \"https://liquichain.io/meveo/rest/gp/v1/methods/creditcard\",\r\n"
    +"                         \"type\": \"application/hal+json\"\r\n"
    +"                     }\r\n"
    +"                 }\r\n"
    +"            },\r\n"
    +"            {\r\n"
    +"                 \"resource\": \"method\",\r\n"
    +"                 \"id\": \"playCoin\",\r\n"
    +"                 \"description\": \"Play coin\",\r\n"
    +"                 \"minimumAmount\": {\r\n"
    +"                     \"value\": \"0.01\",\r\n"
    +"                     \"currency\": \"EUR\"\r\n"
    +"                 },\r\n"
    +"                 \"maximumAmount\": {\r\n"
    +"                     \"value\": \"2000.00\",\r\n"
    +"                     \"currency\": \"EUR\"\r\n"
    +"                 },\r\n"
    +"                 \"image\": {\r\n"
    +"                     \"size1x\": \"https://docs.liquichain.io/media/payments/creditcard.png\",\r\n"
    +"                     \"size2x\": \"https://docs.liquichain.io/media/payments/creditcard%402x.png\",\r\n"
    +"                     \"svg\": \"https://docs.liquichain.io/media/payments/creditcard.svg\"\r\n"
    +"                 },\r\n"
    +"                 \"status\": \"activated\",\r\n"
    +"                 \"pricing\": [\r\n"
    +"                     {\r\n"
    +"                         \"description\": \"Commercial & non-European cards\",\r\n"
    +"                         \"fixed\": {\r\n"
    +"                             \"value\": \"0.25\",\r\n"
    +"                             \"currency\": \"EUR\"\r\n"
    +"                         },\r\n"
    +"                         \"variable\": \"2.8\",\r\n"
    +"                         \"feeRegion\": \"other\"\r\n"
    +"                     },\r\n"
    +"                     {\r\n"
    +"                         \"description\": \"European cards\",\r\n"
    +"                         \"fixed\": {\r\n"
    +"                             \"value\": \"0.25\",\r\n"
    +"                             \"currency\": \"EUR\"\r\n"
    +"                         },\r\n"
    +"                         \"variable\": \"1.8\",\r\n"
    +"                         \"feeRegion\": \"eu-cards\"\r\n"
    +"                     },\r\n"
    +"                     {\r\n"
    +"                         \"description\": \"American Express\",\r\n"
    +"                         \"fixed\": {\r\n"
    +"                             \"value\": \"0.25\",\r\n"
    +"                             \"currency\": \"EUR\"\r\n"
    +"                         },\r\n"
    +"                         \"variable\": \"2.8\",\r\n"
    +"                         \"feeRegion\": \"amex\"\r\n"
    +"                     }\r\n"
    +"                 ],\r\n"
    +"                 \"_links\": {\r\n"
    +"                     \"self\": {\r\n"
    +"                         \"href\": \"https://liquichain.io/meveo/rest/gp/v1/methods/creditcard\",\r\n"
    +"                         \"type\": \"application/hal+json\"\r\n"
    +"                     }\r\n"
    +"                 }\r\n"
    +"            }\r\n"
    +"        ]\r\n"
    +"    },\r\n"
    +"    \"_links\": {\r\n"
    +"        \"self\": {\r\n"
    +"            \"href\": \"https://liquichain.io/meveo/rest/gp/v1/methods\",\r\n"
    +"            \"type\": \"application/hal+json\"\r\n"
    +"        },\r\n        \"documentation\": {\r\n"
    +"            \"href\": \"https://docs.liquichain.com/reference/v2/methods-api/list-methods\",\r\n"
    +"            \"type\": \"text/html\"\r\n"
    +"        }\r\n"
    +"    }\r\n}";
    }

}