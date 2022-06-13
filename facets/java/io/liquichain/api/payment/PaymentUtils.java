package io.liquichain.api.payment;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoAddress;
import org.meveo.model.customEntities.MoOrderLine;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentUtils {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentUtils.class);
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

    public static String mapValues(Map<String, Object> map) {
        return "[ " +
            map.keySet().stream()
               .map(key -> key + ": " + map.get(key))
               .collect(Collectors.joining(", ")) +
            " ]";
    }

    public static String convertJsonToString(Object data) {
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

    private static String getObjectValue(Object object) {
        if (object.getClass() == String.class) {
            return "" + object;
        } else {
            return getFieldValues(object);
        }
    }

    private static String getFieldValue(Object object, Field field) {
        field.setAccessible(true);
        try {
            if (field.getType() == String.class) {
                return "" + field.get(object);
            } else if (field.getType() == Map.class) {
                Map<String, Object> objectMap = (Map<String, Object>) field.get(object);
                return objectMap.keySet().stream()
                                .map(key -> key + ":" + getObjectValue(objectMap.get(key)))
                                .collect(Collectors.joining(","));

            } else if (field.getType() == List.class) {
                List<Object> objectList = (List) field.get(object);
                return objectList.stream().map(PaymentUtils::getObjectValue).collect(Collectors.joining(","));
            } else {
                return "";
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

    public static String generateUUID(Object object) {
        String fieldValues = getFieldValues(object);
        String uuid = DigestUtils.sha1Hex(fieldValues);
        LOG.info("generateUUID - fieldValues: {}", fieldValues);
        LOG.info("generateUUID - uuid: {}", uuid);
        return uuid;
    }

    public static MoAddress parseAddress(CrossStorageApi crossStorageApi, Repository defaultRepo,
        Map<String, Object> parameters) throws BusinessException {
        if (parameters == null) {
            return null;
        }
        String id = getString(parameters, "id");
        MoAddress address;
        if (id != null) {
            try {
                address = crossStorageApi.find(defaultRepo, id, MoAddress.class);
            } catch (Exception e) {
                throw new BusinessException("Failed to retrieve address with id: " + id, e);
            }
        } else {
            address = new MoAddress();
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
        Map<String, Object> parameters) throws BusinessException {
        if (parameters == null) {
            return null;
        }

        String id = getString(parameters, "id");
        MoOrderLine orderLine;
        if (id != null) {
            try {
                orderLine = crossStorageApi.find(defaultRepo, id, MoOrderLine.class);
            } catch (Exception e) {
                throw new BusinessException("Failed to retrieve order line with id: " + id, e);
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
        String metadata = convertJsonToString(parameters.get("metadata"));

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

    public static List<MoOrderLine> parseOrderLines(CrossStorageApi crossStorageApi, Repository defaultRepo,
        List<Map<String, Object>> parameters) throws BusinessException {
        if (parameters == null || parameters.size() == 0) {
            return null;
        }
        List<MoOrderLine> orderLines = new ArrayList<>();
        for (Map<String, Object> lineParam : parameters) {
            MoOrderLine line = parseOrderLine(crossStorageApi, defaultRepo, lineParam);
            if (line != null) {
                orderLines.add(line);
            }
        }
        return orderLines;
    }
}
