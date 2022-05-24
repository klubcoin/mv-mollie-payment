package io.liquichain.api.payment;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.Hash;

import javax.inject.Inject;

public class MollieCreatePaymentScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieCreateOrderScript.class);

    @Inject
    private CrossStorageApi crossStorageApi;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private ParamBeanFactory paramBeanFactory;

    private Repository defaultRepo = null;

    private String BASE_URL = null;
    private String MEVEO_BASE_URL = null;
    private final ObjectMapper mapper = new ObjectMapper();

    private String result;

    public String getResult() {
        return this.result;
    }

    private void init() {
        this.defaultRepo = repositoryService.findDefaultRepository();
        ParamBean config = paramBeanFactory.getInstance();

        BASE_URL = config.getProperty("meveo.admin.baseUrl", "http://localhost:8080/");
        String CONTEXT = config.getProperty("meveo.admin.webContext", "meveo");
        MEVEO_BASE_URL = BASE_URL + CONTEXT;
    }

    private String convertJsonToString(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to map result to json string.", e);
        }
        return null;
    }

    public static String normalizeHash(String hash) {
        if (hash.startsWith("0x")) {
            return hash.substring(2);
        }
        return hash.toLowerCase();
    }

    public String createErrorResponse(String status, String title, String detail) {
        String response = "{\n" +
            "  \"status\": " + status + ",\n" +
            "  \"title\": \"" + title + "\",\n" +
            "  \"detail\": \"" + detail + "\",\n" +
            "  \"_links\": {\n" +
            "    \"documentation\": {\n" +
            "      \"href\": \"https://docs.mollie.com/errors\",\n" +
            "      \"type\": \"text/html\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
        LOG.debug("error response: {}", response);
        return response;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        this.init();

        Map<String, Object> amountMap = (Map<String, Object>) parameters.get("amount");
        String amountValue = (String) amountMap.get("value");
        String amountCurrency = (String) amountMap.get("currency");
        Map<String, Object> metadataMap = (Map<String, Object>) parameters.get("metadata");
        String orderId = (String) metadataMap.get("order_id");
        String amount = convertJsonToString(parameters.get("amount"));
        String description = (String) parameters.get("description");
        String redirectUrl = (String) parameters.get("redirectUrl");
        String webhookUrl = (String) parameters.get("webhookUrl");
        String metadata = convertJsonToString(parameters.get("metadata"));
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(Duration.ofDays(10));

        String data = Hash
            .sha3(amountValue + amountCurrency + orderId +
                description + redirectUrl + webhookUrl + metadata + createdAt);

        Transaction transaction = new Transaction();
        transaction.setHexHash(normalizeHash(data));
        transaction.setSignedHash(data);
        transaction.setValue(amountValue);
        transaction.setCurrency(amountCurrency);
        transaction.setDescription(description);
        transaction.setRedirectUrl(redirectUrl);
        transaction.setWebhookUrl(webhookUrl);
        transaction.setMetadata(metadata);
        transaction.setCreationDate(createdAt);
        transaction.setExpirationDate(expiresAt);
        transaction.setOrderId(orderId);
        transaction.setData("{\"type\":\"mollie\",\"description\":\"Mollie Payment\"}");
        transaction.setType("mollie");

        String uuid;
        try {
            uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
        } catch (Exception e) {
            String error = "Failed to save payment transaction.";
            LOG.error(error, e);
            result = createErrorResponse("500", "Internal Server Error", error);
            return;
        }

        MoOrder order;
        try {
            order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);
        } catch (Exception e) {
            String error = "Cannot retrieve order: " + orderId;
            LOG.error(error, e);
            result = createErrorResponse("404", "Not found", error);
            return;
        }

        String id = "tr_" + uuid;
        result = "{\n" +
            "    \"resource\": \"payment\",\n" +
            "    \"id\": \"" + id + "\",\n" +
            "    \"mode\": \"test\",\n" +
            "    \"createdAt\": \"" + createdAt + "\",\n" +
            "    \"amount\": " + amount + ",\n" +
            "    \"description\": \"" + description + "\",\n" +
            "    \"method\": \"" + order.getMethod() + "\",\n" +
            "    \"metadata\": " + metadata + ",\n" +
            "    \"status\": \"open\",\n" +
            "    \"isCancelable\": false,\n" +
            "    \"expiresAt\": \"" + expiresAt + "\",\n" +
            "    \"details\": null,\n" +
            "    \"profileId\": \"pfl_QkEhN94Ba\",\n" +
            "    \"sequenceType\": \"oneoff\",\n" +
            "    \"redirectUrl\": \"" + redirectUrl + "\",\n" +
            "    \"webhookUrl\": \"" + webhookUrl + "\",\n" +
            "    \"_links\": {\n" +
            "        \"self\": {\n" +
            "            \"href\": \"" + MEVEO_BASE_URL + "/rest/pg/v1/payments/" + id + "\",\n" +
            "            \"type\": \"application/json\"\n" +
            "        },\n" +
            "        \"checkout\": {\n" +
            "            \"href\": \"" + MEVEO_BASE_URL + "/rest/paymentpages/checkout/" + orderId + "\",\n" +
            "            \"type\": \"text/html\"\n" +
            "        },\n" +
            "        \"dashboard\": {\n" +
            "            \"href\": \"" + BASE_URL + "dashboard?orderid=" + orderId + "\",\n" +
            "            \"type\": \"application/json\"\n" +
            "        },\n" +
            "        \"documentation\": {\n" +
            "            \"href\": \"https://docs.liquichain.io/reference/v2/payments-api/create-payment\",\n" +
            "            \"type\": \"text/html\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
    }

}
