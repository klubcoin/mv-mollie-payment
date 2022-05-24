package io.liquichain.api.payment;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.digest.DigestUtils;
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

public class MollieCreateOrderScript extends Script {
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
        return result;
    }

    private void init() {
        this.defaultRepo = repositoryService.findDefaultRepository();
        ParamBean config = paramBeanFactory.getInstance();

        BASE_URL = config.getProperty("meveo.admin.baseUrl", "http://localhost:8080/");
        String CONTEXT = config.getProperty("meveo.admin.webContext", "meveo");
        MEVEO_BASE_URL = BASE_URL + CONTEXT;
    }

    private Long getLongParam(Map<String, Object> parameters, String name) {
        if (parameters == null) {
            return null;
        }
        Long value = null;
        if (parameters.containsKey(name)) {
            Object objResult = parameters.get(name);
            if (objResult != null) {
                try {
                    value = Long.parseLong(objResult.toString());
                } catch (Exception e) {
                    LOG.error("error while parsing " + name, e);
                }
            }
        }
        return value;
    }

    private Double getDoubleParam(Map<String, Object> parameters, String name) {
        if (parameters == null) {
            return null;
        }
        Double value = null;
        if (parameters.containsKey(name)) {
            Object objResult = parameters.get(name);
            if (objResult != null) {
                try {
                    value = Double.parseDouble(objResult.toString());
                } catch (Exception e) {
                    LOG.error("error while parsing " + name, e);
                }
            }
        }
        return value;
    }

    private String getStrParam(Map<String, Object> parameters, String name) {
        if (parameters == null) {
            return null;
        }
        String value = null;
        if (parameters.containsKey(name)) {
            Object objResult = parameters.get(name);
            if (objResult != null) {
                value = objResult.toString();
            }
        }
        return value;
    }

    private MoAddress parseAddress(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        MoAddress address = new MoAddress();
        String city = getStrParam(parameters, "city");
        address.setCity(city);
        String country = getStrParam(parameters, "country");
        address.setCountry(country);
        String postalCode = getStrParam(parameters, "postalCode");
        address.setPostalCode(postalCode);
        String region = getStrParam(parameters, "region");
        address.setRegion(region);
        String streetAdditional = getStrParam(parameters, "streetAdditional");
        address.setStreetAdditional(streetAdditional);
        String streetAndNumber = getStrParam(parameters, "streetAndNumber");
        address.setStreetAndNumber(streetAndNumber);
        String uuid = DigestUtils.sha1Hex(
            (streetAndNumber + streetAdditional + city + postalCode + region + country).replaceAll(
                "[^\\p{IsAlphabetic}\\p{IsDigit}]", ""));
        address.setUuid(uuid);
        try {
            crossStorageApi.createOrUpdate(defaultRepo, address);
        } catch (Exception e) {
            LOG.error("error persisting address:{} [{}]", address, e.getMessage());
            address = null;
        }
        return address;
    }

    private MoOrderLine parseOrderLine(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        MoOrderLine orderLine = new MoOrderLine();
        Map<String, Object> discountAmount = (Map<String, Object>) parameters.get("discountAmount");
        Map<String, Object> totalAmount = (Map<String, Object>) parameters.get("totalAmount");
        Map<String, Object> vatAmount = (Map<String, Object>) parameters.get("vatAmount");
        Map<String, Object> unitPrice = (Map<String, Object>) parameters.get("unitPrice");
        orderLine.setSku(getStrParam(parameters, "sku"));
        orderLine.setName(getStrParam(parameters, "name"));
        orderLine.setQuantity(getLongParam(parameters, "quantity"));
        orderLine.setVatRate(getDoubleParam(parameters, "vatRate"));
        orderLine.setUnitPrice(getDoubleParam(unitPrice, "value"));
        orderLine.setTotalAmount(getDoubleParam(totalAmount, "value"));
        orderLine.setVatAmount(getDoubleParam(vatAmount, "value"));
        orderLine.setDiscountAmount(getDoubleParam(discountAmount, "value"));
        orderLine.setCurrency(getStrParam(totalAmount, "currency"));
        orderLine.setCategory(getStrParam(parameters, "category"));
        orderLine.setImageUrl(getStrParam(parameters, "imageUrl"));
        orderLine.setProductUrl(getStrParam(parameters, "productUrl"));
        orderLine.setType(getStrParam(parameters, "type"));
        orderLine.setMetadata(convertJsonToString(parameters.get("metadata")));
        orderLine.setCreationDate(Instant.now());
        return orderLine;
    }

    private List<MoOrderLine> parseOrderLines(List<Map<String, Object>> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return null;
        }
        List<MoOrderLine> orderLines = new ArrayList<>();
        for (Map<String, Object> lineParam : parameters) {
            MoOrderLine line = parseOrderLine(lineParam);
            if (line != null) {
                orderLines.add(line);
            }
        }
        return orderLines;
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

    private MoOrder createOrder(Map<String, Object> parameters, List<MoOrderLine> orderLines) throws BusinessException {
        String orderId = getStrParam(parameters, "id");
        MoOrder order;
        if (orderId != null) {
            LOG.info("get Order[{}] {}", orderId, parameters);
            try {
                if (orderId.startsWith("ord_")) {
                    orderId = orderId.substring(4);
                }
                order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);
            } catch (Exception e) {
                String error = "Cannot retrieve order: " + orderId;
                LOG.error(error);
                throw new BusinessException(error, e);
            }
        } else {
            order = new MoOrder();
        }

        LOG.info("CreateOrder {}", parameters);
        Map<String, Object> amountMap = (Map<String, Object>) parameters.get("amount");
        double amountValue = Double.parseDouble(getStrParam(amountMap, "value"));
        String amountCurrency = getStrParam(amountMap, "currency");
        order.setMethod(getStrParam(parameters, "method"));
        order.setAmount(amountValue);
        order.setCurrency(amountCurrency);
        order.setMetadata(convertJsonToString(parameters.get("metadata")));
        order.setOrderNumber(getStrParam(parameters, "orderNumber"));
        order.setRedirectUrl(getStrParam(parameters, "redirectUrl"));
        order.setLocale(getStrParam(parameters, "locale"));
        order.setRedirectUrl(getStrParam(parameters, "redirectUrl").replace("http:", "https:"));
        order.setWebhookUrl(getStrParam(parameters, "webhookUrl").replace("http:", "https:"));
        order.setBillingAddress(parseAddress((Map<String, Object>) parameters.get("billingAddress")));
        order.setShippingAddress(parseAddress((Map<String, Object>) parameters.get("shippingAddress")));

        order.setLines(orderLines);
        order.setStatus("created");
        order.setCreationDate(Instant.now());
        order.setExpiresAt(order.getCreationDate().plus(Duration.ofDays(10)));

        String uuid;
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
            result = "{\"error\":\"" + e.getMessage() + "\"}";
            return;
        }

        String id = "ord_" + order.getUuid();
        result = "{"
            + "\"resource\": \"order\","
            + "\"id\": \"" + id + "\","
            + "\"profileId\": \"pfl_" + order.getUuid() + "\","
            + "\"method\": \"" + parameters.get("method") + "\","
            + "\"amount\": " + convertJsonToString(parameters.get("amount")) + ","
            + "\"status\": \"created\","
            + "\"isCancelable\": false,"
            + "\"metadata\": " + convertJsonToString(parameters.get("metadata")) + ","
            + "\"createdAt\": \"" + order.getCreationDate().toString() + "\","
            + "\"expiresAt\": \"" + order.getExpiresAt().toString() + "\","
            + "\"mode\": \"test\","
            + "\"locale\": \"" + parameters.get("locale") + "\","
            + "\"billingAddress\": " + convertJsonToString(parameters.get("billingAddress")) + ","
            + "\"shoppercountrymustmatchbillingcountry\": false,"
            + "\"ordernumber\": \"" + parameters.get("orderNumber") + "\","
            + "\"redirecturl\": \"" + parameters.get("redirectUrl") + "\","
            + "\"webhookurl\": \"" + parameters.get("webhookUrl") + "\",";

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
            "    \"method\": null,\n" +
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
