package io.liquichain.api.payment;

import static io.liquichain.api.payment.PaymentService.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

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

public class MollieCreatePayment extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieCreatePayment.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);

    private Repository defaultRepo = null;

    private String BASE_URL = null;
    private String MEVEO_BASE_URL = null;

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

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        this.init();
        LOG.info("MollieCreatePayment parameters: {}", parameters);

        Map<String, Object> amountMap = getMap(parameters, "amount");
        String amountValue = getString(amountMap, "value");
        String amountCurrency = getString(amountMap, "currency");
        Map<String, Object> metadataMap = getMap(parameters, "metadata");
        String orderId = getString(metadataMap, "order_id");
        String amount = toJsonString(parameters.get("amount"));
        String description = getString(parameters, "description");
        String redirectUrl = getString(parameters, "redirectUrl");
        String webhookUrl = getString(parameters, "webhookUrl");
        String metadata = toJsonString(parameters.get("metadata"));
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(Duration.ofDays(10));

        String data = Hash
            .sha3(amountValue + amountCurrency + orderId +
                description + redirectUrl + webhookUrl + metadata + createdAt);

        MoOrder order;
        String orderUuid;
        String normalizedId;
        try {
            boolean isUuid = orderId.startsWith("ord_");
            if (isUuid) {
                orderUuid = orderId.substring(4);
                order = crossStorageApi.find(defaultRepo, orderUuid, MoOrder.class);
            } else {
                order = crossStorageApi.find(defaultRepo, MoOrder.class)
                                       .by("orderNumber", orderId)
                                       .getResult();
                orderUuid = order.getUuid();
            }
            normalizedId = "ord_" + orderUuid;
        } catch (Exception e) {
            String error = "Cannot retrieve order: " + orderId;
            LOG.error(error, e);
            result = createErrorResponse("404", "Not found", error);
            return;
        }

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
        transaction.setOrderId(normalizedId);
        transaction.setData("{\"type\":\"payonline\",\"description\":\"Pay online payment\"}");
        transaction.setType("payonline");
        transaction.setUuid(generateUUID(transaction));

        String uuid;
        try {
            uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
        } catch (Exception e) {
            String error = "Failed to save payment transaction.";
            LOG.error(error, e);
            result = createErrorResponse("500", "Internal Server Error", error);
            return;
        }

        String id = "tr_" + uuid;
        String paymentStatus = parseStatus(order);
        result = "{\n" +
            "    \"resource\": \"payment\",\n" +
            "    \"id\": \"" + id + "\",\n" +
            "    \"mode\": \"test\",\n" +
            "    \"createdAt\": \"" + createdAt + "\",\n" +
            "    \"amount\": " + amount + ",\n" +
            "    \"description\": \"" + description + "\",\n" +
            "    \"method\": \"" + order.getMethod() + "\",\n" +
            "    \"metadata\": " + metadata + ",\n" +
            "    \"status\": \"" + paymentStatus + "\",\n" +
            "    \"isCancelable\": false,\n" +
            "    \"expiresAt\": \"" + expiresAt + "\",\n" +
            "    \"details\": null,\n" +
            "    \"profileId\": \"pfl_" + transaction.getUuid() + "\",\n" +
            "    \"sequenceType\": \"oneoff\",\n" +
            "    \"redirectUrl\": \"" + redirectUrl + "\",\n" +
            "    \"webhookUrl\": \"" + webhookUrl + "\",\n" +
            "    \"_links\": {\n" +
            "        \"self\": {\n" +
            "            \"href\": \"" + MEVEO_BASE_URL + "/rest/pg/v1/payments/" + id + "\",\n" +
            "            \"type\": \"application/json\"\n" +
            "        },\n" +
            "        \"checkout\": {\n" +
            "            \"href\": \"" + MEVEO_BASE_URL + "/rest/paymentpages/checkout/" + normalizedId + "\",\n" +
            "            \"type\": \"text/html\"\n" +
            "        },\n" +
            "        \"dashboard\": {\n" +
            "            \"href\": \"" + BASE_URL + "dashboard?orderid=" + normalizedId + "\",\n" +
            "            \"type\": \"application/json\"\n" +
            "        },\n" +
            "        \"documentation\": {\n" +
            "            \"href\": \"https://docs.liquichain.io/reference/v2/payments-api/create-payment\",\n" +
            "            \"type\": \"text/html\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
    }

    private String parseStatus(MoOrder order) {
        String status = order.getStatus();
        Instant now = Instant.now();
        if (now.isAfter(order.getExpiresAt())) {
            return "expired";
        } else if ("created".equals(status)) {
            return "open";
        }
        return status;
    }

}
