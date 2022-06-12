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
import org.meveo.model.customEntities.MoAddress;
import org.meveo.model.customEntities.MoOrderLine;
import org.meveo.service.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentUtils extends Script {
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

    private static String getFieldValue(Object object, Field field) {
        field.setAccessible(true);
        try {
            return field.getType() == String.class ? "" + field.get(object) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String generateUUID(Object object) {
        return DigestUtils.sha1Hex(Arrays.stream(object.getClass().getDeclaredFields())
                                         .map(field -> getFieldValue(object, field))
                                         .collect(Collectors.joining())
                                         .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", ""));
    }

    public static MoAddress parseAddress(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        MoAddress address = new MoAddress();

        String streetAndNumber = getString(parameters, "streetAndNumber");
        String streetAdditional = getString(parameters, "streetAdditional");
        String city = getString(parameters, "city");
        String region = getString(parameters, "region");
        String country = getString(parameters, "country");
        String postalCode = getString(parameters, "postalCode");

        address.setCity(city);
        address.setCountry(country);
        address.setPostalCode(postalCode);
        address.setRegion(region);
        address.setStreetAdditional(streetAdditional);
        address.setStreetAndNumber(streetAndNumber);
        address.setUuid(generateUUID(address));

        return address;
    }

    private static MoOrderLine parseOrderLine(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        MoOrderLine orderLine = new MoOrderLine();

        Map<String, Object> discountAmount = getMap(parameters, "discountAmount");
        Map<String, Object> totalAmount = getMap(parameters, "totalAmount");
        Map<String, Object> vatAmount = getMap(parameters, "vatAmount");
        Map<String, Object> unitPrice = getMap(parameters, "unitPrice");

        orderLine.setSku(getString(parameters, "sku"));
        orderLine.setName(getString(parameters, "name"));
        orderLine.setQuantity(getLong(parameters, "quantity"));
        orderLine.setVatRate(getDouble(parameters, "vatRate"));
        orderLine.setUnitPrice(getDouble(unitPrice, "value"));
        orderLine.setTotalAmount(getDouble(totalAmount, "value"));
        orderLine.setVatAmount(getDouble(vatAmount, "value"));
        orderLine.setDiscountAmount(getDouble(discountAmount, "value"));
        orderLine.setCurrency(getString(totalAmount, "currency"));
        orderLine.setCategory(getString(parameters, "category"));
        orderLine.setImageUrl(getString(parameters, "imageUrl"));
        orderLine.setProductUrl(getString(parameters, "productUrl"));
        orderLine.setType(getString(parameters, "type"));
        orderLine.setMetadata(convertJsonToString(parameters.get("metadata")));
        orderLine.setCreationDate(Instant.now());
        orderLine.setUuid(generateUUID(orderLine));

        return orderLine;
    }

    public static List<MoOrderLine> parseOrderLines(List<Map<String, Object>> parameters) {
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
}
