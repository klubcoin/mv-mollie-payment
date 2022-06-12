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

public class MollieUpdateOrder extends Script {
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

    private <T> T normalize(T value, T currentValue) {
        if (value != null) {
            return value;
        }
        return currentValue;
    }

    private String toHttps(String url) {
        return url != null ? url.replace("http:", "https:") : null;
    }

    private MoAddress getSavedAddress(Map<String, Object> parameters, String name) throws BusinessException {
        Map<String, Object> newAddressMap = getMap(parameters, name);
        MoAddress address = null;
        String uuid = getString(newAddressMap, "id");
        if (uuid != null) {
            try {
                address = crossStorageApi.find(defaultRepo, uuid, MoAddress.class);

                String streetAndNumber =
                    normalize(getString(parameters, "streetAndNumber"), address.getStreetAndNumber());
                String streetAdditional =
                    normalize(getString(parameters, "streetAdditional"), address.getStreetAdditional());
                String city = normalize(getString(parameters, "city"), address.getCity());
                String region = normalize(getString(parameters, "region"), address.getRegion());
                String country = normalize(getString(parameters, "country"), address.getCountry());
                String postalCode = normalize(getString(parameters, "postalCode"), address.getPostalCode());

                address.setStreetAndNumber(streetAndNumber);
                address.setStreetAdditional(streetAdditional);
                address.setCity(city);
                address.setRegion(region);
                address.setCountry(country);
                address.setPostalCode(postalCode);

            } catch (Exception e) {
                String errorMessage = "Failed to retrieve address: " + mapValues(newAddressMap);
                LOG.error(errorMessage, e);
                throw new BusinessException(errorMessage, e);
            }
        } else {
            address = parseAddress(parameters);
        }
        try {
            crossStorageApi.createOrUpdate(defaultRepo, address);
        } catch (Exception e) {
            String errorMessage = "Error updating address: " + mapValues(newAddressMap);
            LOG.error(errorMessage, e);
            throw new BusinessException(errorMessage, e);
        }
        return address;
    }

    private MoOrder updateOrder(Map<String, Object> parameters, List<MoOrderLine> orderLines) throws BusinessException {
        String orderId = getString(parameters, "id");
        MoOrder order;
        String uuid;
        LOG.info("Retrieve Order[{}]", orderId);
        try {
            uuid = orderId.startsWith("ord_") ? orderId.substring(4) : orderId;
            order = crossStorageApi.find(defaultRepo, uuid, MoOrder.class);
        } catch (Exception e) {
            String error = "Cannot retrieve order: " + orderId;
            LOG.error(error);
            throw new BusinessException(error, e);
        }

        LOG.info("Update Order: {}", parameters);
        Map<String, Object> amountMap = getMap(parameters, "amount");
        if (amountMap != null) {
            double amountValue = Double.parseDouble(getString(amountMap, "value"));
            String amountCurrency = getString(amountMap, "currency");
            order.setAmount(amountValue);
            order.setCurrency(amountCurrency);
        }
        order.setMethod(normalize(getString(parameters, "method"), order.getMethod()));
        order.setMetadata(normalize(convertJsonToString(parameters.get("metadata")), order.getMetadata()));
        order.setOrderNumber(normalize(getString(parameters, "orderNumber"), order.getOrderNumber()));
        order.setRedirectUrl(normalize(getString(parameters, "redirectUrl"), order.getRedirectUrl()));
        order.setLocale(normalize(getString(parameters, "locale"), order.getLocale()));
        order.setRedirectUrl(normalize(toHttps(getString(parameters, "redirectUrl")), order.getRedirectUrl()));
        order.setWebhookUrl(normalize(toHttps(getString(parameters, "webhookUrl")), order.getWebhookUrl()));
        order.setBillingAddress(getSavedAddress(parameters, "billingAddress"));
        order.setShippingAddress(getSavedAddress(parameters, "shippingAddress"));
        order.setLines(orderLines);
        String status = normalize(getString(parameters, "status"), order.getStatus());
        order.setStatus(status);

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

    private Transaction updatePayment(MoOrder order) throws BusinessException {
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
            order = this.updateOrder(parameters, orderLines);
            payment = this.updatePayment(order);
        } catch (BusinessException e) {
            LOG.error(e.getMessage(), e);
            result = "{\"error\":\"" + e.getMessage() + "\"}";
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
