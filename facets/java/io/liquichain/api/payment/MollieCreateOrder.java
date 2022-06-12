package io.liquichain.api.payment;

import static io.liquichain.api.payment.PaymentUtils.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.MoOrderLine;
import org.meveo.model.customEntities.MoAddress;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.web3j.crypto.Hash;

import javax.inject.Inject;

public class MollieCreateOrder extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieCreateOrder.class);

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
        return result;
    }

    private void init() {
        this.defaultRepo = repositoryService.findDefaultRepository();
        ParamBean config = paramBeanFactory.getInstance();

        BASE_URL = config.getProperty("meveo.admin.baseUrl", "http://localhost:8080/");
        String CONTEXT = config.getProperty("meveo.admin.webContext", "meveo");
        MEVEO_BASE_URL = BASE_URL + CONTEXT;
    }

    private MoAddress getSavedAddress(Map<String, Object> parameters){
        MoAddress address = parseAddress(parameters);
        try {
            crossStorageApi.createOrUpdate(defaultRepo, address);
        } catch (Exception e) {
            LOG.error("error persisting address:{} [{}]", address, e.getMessage());
            address = null;
        }
        return address;
    }

    private MoOrder createOrder(Map<String, Object> parameters, List<MoOrderLine> orderLines) throws BusinessException {
        String orderId = getString(parameters, "id");
        MoOrder order;
        String uuid = null;
        if (orderId != null) {
            LOG.info("get Order[{}] {}", orderId, parameters);
            try {
                if (orderId.startsWith("ord_")) {
                    orderId = orderId.substring(4);
                }
                order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);
                uuid = order.getUuid();
            } catch (Exception e) {
                String error = "Cannot retrieve order: " + orderId;
                LOG.error(error);
                throw new BusinessException(error, e);
            }
        } else {
            order = new MoOrder();
        }

        LOG.info("CreateOrder {}", parameters);
        Map<String, Object> amountMap = getMap(parameters,"amount");
        double amountValue = Double.parseDouble(getString(amountMap, "value"));
        String amountCurrency = getString(amountMap, "currency");
        order.setMethod(getString(parameters, "method"));
        order.setAmount(amountValue);
        order.setCurrency(amountCurrency);
        order.setMetadata(convertJsonToString(parameters.get("metadata")));
        order.setOrderNumber(getString(parameters, "orderNumber"));
        order.setRedirectUrl(getString(parameters, "redirectUrl"));
        order.setLocale(getString(parameters, "locale"));
        order.setRedirectUrl(getString(parameters, "redirectUrl").replace("http:", "https:"));
        order.setWebhookUrl(getString(parameters, "webhookUrl").replace("http:", "https:"));
        order.setBillingAddress(getSavedAddress(getMap(parameters,"billingAddress")));
        order.setShippingAddress(getSavedAddress(getMap(parameters,"shippingAddress")));

        order.setLines(orderLines);
        order.setStatus("created");
        order.setCreationDate(Instant.now());
        order.setExpiresAt(order.getCreationDate().plus(Duration.ofDays(10)));

        if(uuid == null){
            order.setUuid(generateUUID(order));
        }

        try {
            uuid = crossStorageApi.createOrUpdate(defaultRepo, order);
        } catch (Exception e) {
            String error = String.format("Error saving order:%s [%s]", order.getOrderNumber(), e.getMessage());
            LOG.error(error);
            throw new BusinessException(error, e);
        }

        try {
            order = crossStorageApi.find(defaultRepo, uuid, MoOrder.class);
        } catch (Exception e) {
            String error = String.format("Error retrieving order:%s [%s]", uuid, e.getMessage());
            LOG.error(error);
            throw new BusinessException(error, e);
        }
        return order;
    }

    private Transaction createPayment(MoOrder order) throws BusinessException {
        String amountValue = "" + order.getAmount();
        String amountCurrency = order.getCurrency();
        String orderId = "ord_" + order.getUuid();
        String description = "Payment for Order #" + order.getOrderNumber();
        String redirectUrl = order.getRedirectUrl();
        String webhookUrl = order.getWebhookUrl();
        String metadata = "{\"order_id\": " + order.getOrderNumber() + "}";
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(Duration.ofDays(10));

        String data = Hash.sha3(amountValue + amountCurrency + orderId +
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
        transaction.setUuid(generateUUID(transaction));

        String uuid;
        try {
            uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
        } catch (Exception e) {
            String error = String.format("Error persisting  payment transaction: %s [%s]", result, e.getMessage());
            LOG.error(error, e);
            throw new BusinessException(error, e);
        }

        try {
            transaction = crossStorageApi.find(defaultRepo, uuid, Transaction.class);
        } catch (Exception e) {
            String error = String.format("Error retrieving payment transaction: %s [%s]", uuid, e.getMessage());
            LOG.error(error);
            throw new BusinessException(error, e);
        }

        return transaction;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();

        MoOrder order;
        Transaction payment;
        List<MoOrderLine> orderLines;
        try {
            orderLines = parseOrderLines((List<Map<String, Object>>) parameters.get("lines"));
            order = this.createOrder(parameters, orderLines);
            payment = this.createPayment(order);
        } catch (BusinessException e) {
            LOG.error(e.getMessage(), e);
            result = createErrorResponse("500", "Internal Server Error", e.getMessage());
            return;
        }

        String id = "ord_" + order.getUuid();
        result = "{"
            + "\"resource\": \"order\","
            + "\"id\": \"" + id + "\","
            + "\"profileId\": \"pfl_" + order.getUuid() + "\","
            + "\"method\": \"" + order.getMethod() + "\","
            + "\"amount\": " + order.getAmount() + ","
            + "\"status\": \"created\","
            + "\"isCancelable\": false,"
            + "\"metadata\": " + order.getMetadata() + ","
            + "\"createdAt\": \"" + order.getCreationDate().toString() + "\","
            + "\"expiresAt\": \"" + order.getExpiresAt().toString() + "\","
            + "\"mode\": \"test\","
            + "\"locale\": \"" + order.getLocale() + "\","
            + "\"billingAddress\": " + convertJsonToString(parameters.get("billingAddress")) + ","
            + "\"shoppercountrymustmatchbillingcountry\": false,"
            + "\"ordernumber\": \"" + order.getOrderNumber() + "\","
            + "\"redirecturl\": \"" + order.getRedirectUrl() + "\","
            + "\"webhookurl\": \"" + order.getWebhookUrl() + "\",";

        String lines = orderLines
            .stream()
            .map(
                line -> "{\n" +
                    "      \"resource\": \"orderline\",\n" +
                    "      \"id\": \"odl_" + line.getUuid() + "\",\n" +
                    "      \"orderId\": \"" + id + "\",\n" +
                    "      \"name\": \"" + line.getName() + "\",\n" +
                    "      \"sku\": \"" + line.getSku() + "\",\n" +
                    "      \"type\": \"physical\",\n" +
                    "      \"status\": \"created\",\n" +
                    "      \"metadata\": " + line.getMetadata() + ",\n" +
                    "      \"isCancelable\": false,\n" +
                    "      \"quantity\": " + line.getQuantity() + ",\n" +
                    "      \"quantityShipped\": 0,\n" +
                    "      \"amountShipped\": {\n" +
                    "        \"value\": \"0.00\",\n" +
                    "        \"currency\": \"USD\"\n" +
                    "      },\n" +
                    "      \"quantityRefunded\": 0,\n" +
                    "      \"amountRefunded\": {\n" +
                    "        \"value\": \"0.00\",\n" +
                    "        \"currency\": \"USD\"\n" +
                    "      },\n" +
                    "      \"quantityCanceled\": 0,\n" +
                    "      \"amountCanceled\": {\n" +
                    "        \"value\": \"0.00\",\n" +
                    "        \"currency\": \"USD\"\n" +
                    "      },\n" +
                    "      \"shippableQuantity\": 0,\n" +
                    "      \"refundableQuantity\": 0,\n" +
                    "      \"cancelableQuantity\": 0,\n" +
                    "      \"unitPrice\": {\n" +
                    "        \"value\": \"" + line.getUnitPrice() + "\",\n" +
                    "        \"currency\": \"" + line.getCurrency() + "\"\n" +
                    "      },\n" +
                    "      \"vatRate\": \"0.00\",\n" +
                    "      \"vatAmount\": {\n" +
                    "        \"value\": \"" + line.getVatAmount() + "\",\n" +
                    "        \"currency\": \"" + line.getCurrency() + "\"\n" +
                    "      },\n" +
                    "      \"totalAmount\": {\n" +
                    "        \"value\": \"" + line.getTotalAmount() + "\",\n" +
                    "        \"currency\": \"" + line.getCurrency() + "\"\n" +
                    "      },\n" +
                    "      \"createdAt\": \"" + line.getCreationDate().toString() + "\"\n" +
                    "    }")
            .collect(Collectors.joining(",\n"));

        result += String.format("\"lines\": [%s],", lines);

        String paymentId = "tr_" + payment.getUuid();
        result += "\"_embedded\": {\"payments\": [{\n" +
            "    \"resource\": \"payment\",\n" +
            "    \"id\": \"" + paymentId + "\",\n" +
            "    \"mode\": \"test\",\n" +
            "    \"createdAt\": \"" + payment.getCreationDate() + "\",\n" +
            "    \"amount\": {\n" +
            "        \"value\": \"" + payment.getValue() + "\",\n" +
            "        \"currency\": \"" + payment.getCurrency() + "\"\n" +
            "    },\n" +
            "    \"description\": \"" + payment.getDescription() + "\",\n" +
            "    \"method\": \"" + order.getMethod() + "\",\n" +
            "    \"metadata\": " + payment.getMetadata() + ",\n" +
            "    \"status\": \"open\",\n" +
            "    \"isCancelable\": false,\n" +
            "    \"expiresAt\": \"" + payment.getExpirationDate() + "\",\n" +
            "    \"details\": null,\n" +
            "    \"profileId\": \"pfl_" + payment.getUuid() + "\",\n" +
            "    \"orderId\": \"" + payment.getOrderId() + "\",\n" +
            "    \"sequenceType\": \"oneoff\",\n" +
            "    \"redirectUrl\": \"" + payment.getRedirectUrl() + "\",\n" +
            "    \"webhookUrl\": \"" + payment.getWebhookUrl() + "\",\n" +
            "    \"_links\": {\n" +
            "        \"self\": {\n" +
            "            \"href\": \"" + MEVEO_BASE_URL + "/rest/pg/v1/payments/" + paymentId + "\",\n" +
            "            \"type\": \"application/json\"\n" +
            "        },\n" +
            "        \"checkout\": {\n" +
            "            \"href\": \"" + MEVEO_BASE_URL + "/rest/paymentpages/checkout/" + id + "\",\n" +
            "            \"type\": \"text/html\"\n" +
            "        },\n" +
            "        \"dashboard\": {\n" +
            "            \"href\": \"" + BASE_URL + "dashboard?orderid=" + id + "\",\n" +
            "            \"type\": \"application/json\"\n" +
            "        },\n" +
            "        \"documentation\": {\n" +
            "            \"href\": \"https://docs.liquichain.io/reference/v2/payments-api/get-payment\",\n" +
            "            \"type\": \"text/html\"\n" +
            "        }\n" +
            "    }\n" +
            "}]},";

        result += "\"_links\": {\n" +
            "    \"self\": {\n" +
            "      \"href\": \"" + MEVEO_BASE_URL + "/rest/pg/v1/orders/" + id + "\",\n" +
            "      \"type\": \"application/hal+json\"\n" +
            "    },\n" +
            "    \"dashboard\": {\n" +
            "      \"href\": \"" + BASE_URL + "dashboard?orderid=" + id + "\",\n" +
            "      \"type\": \"text/html\"\n" +
            "    },\n" +
            "    \"checkout\": {\n" +
            "      \"href\": \"" + MEVEO_BASE_URL + "/rest/paymentpages/checkout/" + id + "\",\n" +
            "      \"type\": \"text/html\"\n" +
            "    },\n" +
            "    \"documentation\": {\n" +
            "      \"href\": \"https://docs.liquichain.io/reference/v2/orders-api/get-order\",\n" +
            "      \"type\": \"text/html\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    }

}
