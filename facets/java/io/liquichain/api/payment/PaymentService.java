package io.liquichain.api.payment;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.MoAddress;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.MoOrderLine;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Hash;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class PaymentService extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Long getLong(Map<String, Object> parameters, String name) {
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

    public static Double getDouble(Map<String, Object> parameters, String name) {
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

    public static String getString(Map<String, Object> parameters, String name) {
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

    public static <K, V> Map<K, V> getMap(Map<String, Object> parameters, String name) {
        if (parameters == null) {
            return null;
        }
        Map<K, V> value = null;
        if (parameters.containsKey(name)) {
            Object objResult = parameters.get(name);
            if (objResult != null) {
                value = (Map<K, V>) objResult;
            }
        }
        return value;
    }

    public static String toJsonString(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to map result to json string.", e);
        }
        return null;
    }

    public static String normalizeHash(String hash) {
        if (hash.startsWith("0x")) {
            return hash.substring(2).toLowerCase();
        }
        return hash.toLowerCase();
    }

    public static String toHex(byte[] bytes) {
        StringBuilder hexValue = new StringBuilder();
        for (byte aByte : bytes) {
            hexValue.append(String.format("%02x", aByte));
        }
        return hexValue.toString().toLowerCase();
    }

    public static String createResponse(String result) {
        String resultFormat = result.startsWith("{") ? "%s" : "\"%s\"";
        String response = "{\n" +
            "  \"result\": " + String.format(resultFormat, result) + "\n" +
            "}";
        LOG.debug("response: {}", response);
        return response;
    }

    public static String createErrorResponse(String status, String title, String detail) {
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

    public static <T> T normalize(T value, T currentValue) {
        if (value != null) {
            return value;
        }
        return currentValue;
    }

    private static String printMapValues(Map<String, Object> map) {
        return "{" +
            map.keySet().stream()
               .map(key -> key + ": " + getObjectValue(map.get(key)))
               .collect(Collectors.joining(", ")) +
            "}";
    }

    private static String getObjectValue(Object object) {
        if (object == null) {
            return "";
        }
        if (object.getClass() == String.class) {
            return "" + object;
        } else {
            return getFieldValues(object);
        }
    }

    public static void callWebhook(MoOrder order, Transaction transaction) {
        Client client = ClientBuilder.newClient();
        Response response = null;
        String webhookUrl = order.getWebhookUrl();
        try {
            Form form = new Form().param("id", "tr_" + transaction.getUuid());
            WebTarget target = client.target(webhookUrl);
            LOG.info("target: {}", target);
            Invocation.Builder builder = target.request(MediaType.APPLICATION_FORM_URLENCODED);
            LOG.info("builder: {}", builder);
            response = builder.post(Entity.form(form));
        } catch (Exception e) {
            String error = String.format("Failed invoking webhook: %s", webhookUrl);
            LOG.error(error, e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private static String getFieldValue(Object object, Field field) {
        field.setAccessible(true);
        try {
            if (field.getType() == Map.class) {
                Map<String, Object> objectMap = (Map<String, Object>) field.get(object);
                return objectMap.keySet().stream()
                                .map(key -> getObjectValue(objectMap.get(key)))
                                .collect(Collectors.joining());

            } else if (field.getType() == List.class) {
                List<Object> objectList = (List) field.get(object);
                return objectList.stream().map(PaymentService::getObjectValue).collect(Collectors.joining());
            } else {
                Object value = field.get(object);
                return value != null ? "" + field.get(object) : "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String getFieldValues(Object object) {
        return Arrays.stream(object.getClass().getDeclaredFields())
                     .map(field -> getFieldValue(object, field))
                     .collect(Collectors.joining())
                     .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
    }

    private static String toHttps(String url) {
        return url != null ? url.replace("http:", "https:") : null;
    }

    public static String generateUUID(Object object) {
        String fieldValues = getFieldValues(object);
        return DigestUtils.sha1Hex(fieldValues);
    }

    public static MoAddress parseAddress(CrossStorageApi crossStorageApi, Repository defaultRepo,
        MoAddress existingAddress, Map<String, Object> parameters) {
        if (parameters == null && existingAddress == null) {
            return null;
        }
        String id = getString(parameters, "id");
        MoAddress address;
        if (id != null) {
            try {
                address = crossStorageApi.find(defaultRepo, id, MoAddress.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve address: " + printMapValues(parameters), e);
            }
        } else {
            if (existingAddress != null) {
                try {
                    existingAddress = crossStorageApi.find(defaultRepo, existingAddress.getUuid(), MoAddress.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to retrieve address: " + toJsonString(existingAddress), e);
                }
            }
            address = existingAddress != null ? existingAddress : new MoAddress();
        }


        String streetAndNumber = getString(parameters, "streetAndNumber");
        String streetAdditional = getString(parameters, "streetAdditional");
        String city = getString(parameters, "city");
        String region = getString(parameters, "region");
        String country = getString(parameters, "country");
        String postalCode = getString(parameters, "postalCode");

        address.setStreetAndNumber(normalize(streetAndNumber, address.getStreetAndNumber()));
        address.setStreetAdditional(normalize(streetAdditional, address.getStreetAdditional()));
        address.setCity(normalize(city, address.getCity()));
        address.setRegion(normalize(region, address.getRegion()));
        address.setCountry(normalize(country, address.getCountry()));
        address.setPostalCode(normalize(postalCode, address.getPostalCode()));
        if (address.getUuid() == null) {
            address.setUuid(generateUUID(address));
        }

        return address;
    }

    private static MoOrderLine parseOrderLine(CrossStorageApi crossStorageApi, Repository defaultRepo,
        Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        String id = getString(parameters, "id");
        MoOrderLine orderLine;
        if (id != null) {
            try {
                orderLine = crossStorageApi.find(defaultRepo, id, MoOrderLine.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve order line with id: " + id, e);
            }
        } else {
            orderLine = new MoOrderLine();
        }

        Map<String, Object> discountAmountMap = getMap(parameters, "discountAmount");
        Map<String, Object> totalAmountMap = getMap(parameters, "totalAmount");
        Map<String, Object> vatAmountMap = getMap(parameters, "vatAmount");
        Map<String, Object> unitPriceMap = getMap(parameters, "unitPrice");
        String sku = getString(parameters, "sku");
        String name = getString(parameters, "name");
        long quantity = getLong(parameters, "quantity");
        double vatRate = getDouble(parameters, "vatRate");
        double unitPrice = getDouble(unitPriceMap, "value");
        double totalAmount = getDouble(totalAmountMap, "value");
        String currency = getString(totalAmountMap, "currency");
        double vatAmount = getDouble(vatAmountMap, "value");
        double discountAmount = getDouble(discountAmountMap, "value");
        String category = getString(parameters, "category");
        String imageUrl = getString(parameters, "imageUrl");
        String productUrl = getString(parameters, "productUrl");
        String type = getString(parameters, "type");
        String metadata = toJsonString(parameters.get("metadata"));

        orderLine.setSku(normalize(sku, orderLine.getSku()));
        orderLine.setName(normalize(name, orderLine.getName()));
        orderLine.setQuantity(normalize(quantity, orderLine.getQuantity()));
        orderLine.setVatRate(normalize(vatRate, orderLine.getVatRate()));
        orderLine.setUnitPrice(normalize(unitPrice, orderLine.getUnitPrice()));
        orderLine.setTotalAmount(normalize(totalAmount, orderLine.getTotalAmount()));
        orderLine.setCurrency(normalize(currency, orderLine.getCurrency()));
        orderLine.setVatAmount(normalize(vatAmount, orderLine.getVatAmount()));
        orderLine.setDiscountAmount(normalize(discountAmount, orderLine.getDiscountAmount()));
        orderLine.setCategory(normalize(category, orderLine.getCategory()));
        orderLine.setImageUrl(normalize(imageUrl, orderLine.getImageUrl()));
        orderLine.setProductUrl(normalize(productUrl, orderLine.getProductUrl()));
        orderLine.setType(normalize(type, orderLine.getType()));
        orderLine.setMetadata(normalize(metadata, orderLine.getMetadata()));
        if (id == null) {
            orderLine.setCreationDate(Instant.now());
            orderLine.setUuid(generateUUID(orderLine));
        }

        return orderLine;
    }

    public static MoOrder parseOrder(CrossStorageApi crossStorageApi, Repository defaultRepo,
        Map<String, Object> parameters) {
        String orderId = getString(parameters, "id");
        MoOrder order;
        String uuid = null;
        LOG.info("Retrieve order: {}", orderId);
        if (orderId != null) {
            try {
                orderId = orderId.startsWith("ord_") ? orderId.substring(4) : orderId;
                order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);
                uuid = order.getUuid();
            } catch (Exception e) {
                String error = "Cannot retrieve order: " + (parameters);
                throw new RuntimeException(error, e);
            }
        } else {
            order = new MoOrder();
        }

        Map<String, Object> amountMap = getMap(parameters, "amount");
        if (amountMap != null) {
            double amountValue = Double.parseDouble(getString(amountMap, "value"));
            String amountCurrency = getString(amountMap, "currency");
            order.setAmount(amountValue);
            order.setCurrency(amountCurrency);
        }
        String method = getString(parameters, "method");
        String metadata = toJsonString(parameters.get("metadata"));
        String orderNumber = getString(parameters, "orderNumber");
        String locale = getString(parameters, "locale");
        String redirectUrl = toHttps(getString(parameters, "redirectUrl"));
        String webhookUrl = toHttps(getString(parameters, "webhookUrl"));

        order.setMethod(normalize(method, order.getMethod()));
        order.setMetadata(normalize(metadata, order.getMetadata()));
        order.setOrderNumber(normalize(orderNumber, order.getOrderNumber()));
        order.setLocale(normalize(locale, order.getLocale()));
        order.setRedirectUrl(normalize(redirectUrl, order.getRedirectUrl()));
        order.setWebhookUrl(normalize(webhookUrl, order.getWebhookUrl()));

        String status = "created";
        if (uuid == null) {
            order.setUuid(generateUUID(order));
            order.setCreationDate(Instant.now());
            order.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        } else {
            status = normalize(getString(parameters, "status"), order.getStatus());
            if ("canceled".equals(status)) {
                order.setCanceledAt(Instant.now());
            }
            if ("expired".equals(status) || Instant.now().isAfter(order.getExpiresAt())) {
                status = "expired";
                order.setExpiredAt(Instant.now());
            }
        }
        order.setStatus(status);

        return order;
    }

    public static Transaction parsePayment(MoOrder order) {
        if (order == null) {
            return null;
        }
        String orderId = "ord_" + order.getUuid();
        String amountValue = "" + order.getAmount();
        String amountCurrency = order.getCurrency();
        String description = "Payment for Order #" + order.getOrderNumber();
        String redirectUrl = order.getRedirectUrl();
        String webhookUrl = order.getWebhookUrl();
        String metadata = "{\"order_id\": " + order.getOrderNumber() + "}";
        Instant createdAt = order.getCreationDate();
        Instant expiresAt = order.getExpiresAt();

        String signedHash = Hash.sha3(orderId + amountValue + amountCurrency +
            description + redirectUrl + webhookUrl + metadata + createdAt);

        Transaction payment = new Transaction();
        payment.setHexHash(normalizeHash(signedHash));
        payment.setSignedHash(signedHash);
        payment.setValue(amountValue);
        payment.setCurrency(amountCurrency);
        payment.setDescription(description);
        payment.setRedirectUrl(redirectUrl);
        payment.setWebhookUrl(webhookUrl);
        payment.setMetadata(metadata);
        payment.setCreationDate(createdAt);
        payment.setExpirationDate(expiresAt);
        payment.setOrderId(orderId);
        payment.setData("{\"type\":\"payonline\",\"description\":\"Pay online payment\"}");
        payment.setType("payonline");
        payment.setUuid(generateUUID(payment));

        return payment;
    }

    public static MoAddress getSavedAddress(CrossStorageApi crossStorageApi, Repository defaultRepo,
        MoAddress existingAddress, Map<String, Object> newAddressMap) {
        if (newAddressMap == null && existingAddress == null) {
            return null;
        }
        MoAddress address = parseAddress(crossStorageApi, defaultRepo, existingAddress, newAddressMap);

        if (address != null) {
            try {
                crossStorageApi.createOrUpdate(defaultRepo, address);
            } catch (Exception e) {
                String errorMessage = "Failed to save address: " + toJsonString(address);
                throw new RuntimeException(errorMessage, e);
            }
        }

        return address;
    }

    public static List<MoOrderLine> getSavedOrderLines(CrossStorageApi crossStorageApi, Repository defaultRepo,
        List<MoOrderLine> existingLines, Map<String, Object> parameters) {
        List<Map<String, Object>> lines = (List<Map<String, Object>>) parameters.get("lines");
        boolean hasNewLines = lines != null && lines.size() > 0;
        boolean hasExistingLines = existingLines != null && existingLines.size() > 0;
        if (!hasNewLines && !hasExistingLines) {
            return null;
        }
        List<MoOrderLine> orderLines = new ArrayList<>();
        if (hasNewLines) {
            if (hasExistingLines) {
                List<String> idsToUpdate = lines
                    .stream()
                    .filter(line -> StringUtils.isNotBlank(getString(line, "id")))
                    .map(line -> getString(line, "id"))
                    .collect(Collectors.toList());
                List<MoOrderLine> linesToRemove = existingLines
                    .stream()
                    .filter(line -> !idsToUpdate.contains(line.getUuid()))
                    .collect(Collectors.toList());

                for (MoOrderLine orderLine : linesToRemove) {
                    try {
                        crossStorageApi.remove(defaultRepo, orderLine.getUuid(), MoOrderLine.class);
                    } catch (Exception e) {
                        String errorMessage = "Failed to remove order line: " + toJsonString(orderLine);
                        throw new RuntimeException(errorMessage, e);
                    }
                }
            }
            for (Map<String, Object> line : lines) {
                MoOrderLine orderLine = parseOrderLine(crossStorageApi, defaultRepo, line);
                if (orderLine != null) {
                    try {
                        crossStorageApi.createOrUpdate(defaultRepo, orderLine);
                    } catch (Exception e) {
                        String errorMessage = "Failed to save order line: " + printMapValues(line);
                        throw new RuntimeException(errorMessage, e);
                    }
                    orderLines.add(orderLine);
                }
            }
        } else {
            // no new lines but has existing lines so fetch existing lines
            for (MoOrderLine existingLine : existingLines) {
                try {
                    existingLine = crossStorageApi.find(defaultRepo, existingLine.getUuid(), MoOrderLine.class);
                } catch (Exception e) {
                    String errorMessage = "Failed to retrieve order line: " + toJsonString(existingLine);
                    throw new RuntimeException(errorMessage, e);
                }
                orderLines.add(existingLine);
            }
        }
        return orderLines;
    }

    public static MoOrder getSavedOrder(CrossStorageApi crossStorageApi, Repository defaultRepo,
        Map<String, Object> parameters) {
        MoOrder order = parseOrder(crossStorageApi, defaultRepo, parameters);
        Map<String, Object> newBillingAddress = getMap(parameters, "billingAddress");
        Map<String, Object> newShippingAddress = getMap(parameters, "shippingAddress");
        MoAddress billingAddress =
            getSavedAddress(crossStorageApi, defaultRepo, order.getBillingAddress(), newBillingAddress);
        MoAddress shippingAddress =
            getSavedAddress(crossStorageApi, defaultRepo, order.getShippingAddress(), newShippingAddress);
        List<MoOrderLine> orderLines = getSavedOrderLines(crossStorageApi, defaultRepo, order.getLines(), parameters);

        order.setBillingAddress(billingAddress);
        order.setShippingAddress(shippingAddress);
        order.setLines(orderLines);

        try {
            crossStorageApi.createOrUpdate(defaultRepo, order);
        } catch (Exception e) {
            String error = "Failed to save order: " + printMapValues(parameters);
            throw new RuntimeException(error, e);
        }

        LOG.info("getSavedOrder - order: {}", toJsonString(order));
        return order;
    }

    public static Transaction getSavedPayment(CrossStorageApi crossStorageApi, Repository defaultRepo, MoOrder order) {
        Transaction payment = parsePayment(order);
        if (payment != null) {
            try {
                crossStorageApi.createOrUpdate(defaultRepo, payment);
            } catch (Exception e) {
                String error = "Failed to save payment transaction: " + toJsonString(payment);
                throw new RuntimeException(error, e);
            }
        }

        LOG.info("getSavedPayment - payment: {}", toJsonString(payment));
        return payment;
    }

    public static Transaction getSavedPayment(CrossStorageApi crossStorageApi, Repository defaultRepo,
        Map<String, Object> parameters) {
        String paymentId = getString(parameters, "id");

        Transaction payment;
        String uuid = null;
        try {
            uuid = paymentId.startsWith("tr_") ? paymentId = paymentId.substring(3) : paymentId;
            payment = crossStorageApi.find(defaultRepo, paymentId, Transaction.class);
        } catch (Exception e) {
            String error = "Failed to retrieve payment: " + uuid;
            throw new RuntimeException(error, e);
        }

        String orderId = payment.getOrderId();

        MoOrder order;
        try {
            String orderUuid = orderId.startsWith("ord_") ? orderId.substring(4) : orderId;
            order = crossStorageApi.find(defaultRepo, orderUuid, MoOrder.class);
        } catch (Exception e) {
            String error = "Cannot retrieve order: " + orderId;
            throw new RuntimeException(error, e);
        }
        orderId = "ord_" + order.getUuid();

        String status = normalize(getString(parameters, "status"), order.getStatus());
        if ("canceled".equals(status)) {
            order.setCanceledAt(Instant.now());
        }
        if ("expired".equals(status) || Instant.now().isAfter(order.getExpiresAt())) {
            status = "expired";
            order.setExpiredAt(Instant.now());
        }
        order.setStatus(status);

        try {
            crossStorageApi.createOrUpdate(defaultRepo, order);
        } catch (Exception e) {
            String error = "Failed to save order for payment: " + toJsonString(payment);
            throw new RuntimeException(error, e);
        }

        return payment;
    }
}
