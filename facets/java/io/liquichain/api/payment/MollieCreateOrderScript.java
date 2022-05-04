package io.liquichain.api.payment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.MoOrderLine;
import org.meveo.model.customEntities.MoAddress;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MollieCreateOrderScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieCreateOrderScript.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private final String BASE_URL = config.getProperty("meveo.admin.baseUrl", "http://localhost:8080/");
    private final String CONTEXT = config.getProperty("meveo.admin.webContext", "meveo");
    private final String MEVEO_BASE_URL = BASE_URL + CONTEXT;


    final ObjectMapper mapper = new ObjectMapper();

    private String result;

    private String orderId;
    private MoOrder order;

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getResult() {
        return result;
    }

    public MoOrder getOrder() {
        return order;
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
        MoAddress result = new MoAddress();
        String city = getStrParam(parameters, "city");
        result.setCity(city);
        String country = getStrParam(parameters, "country");
        result.setCountry(country);
        String postalCode = getStrParam(parameters, "postalCode");
        result.setPostalCode(postalCode);
        String region = getStrParam(parameters, "region");
        result.setRegion(region);
        String streetAdditional = getStrParam(parameters, "streetAdditional");
        result.setStreetAdditional(streetAdditional);
        String streetAndNumber = getStrParam(parameters, "streetAndNumber");
        result.setStreetAndNumber(streetAndNumber);
        String uuid = DigestUtils.sha1Hex(
            (streetAndNumber + streetAdditional + city + postalCode + region + country).replaceAll(
                "[^\\p{IsAlphabetic}\\p{IsDigit}]", ""));
        result.setUuid(uuid);
        try {
            crossStorageApi.createOrUpdate(defaultRepo, result);
        } catch (Exception e) {
            LOG.error("error persisting address:{} [{}]", result, e.getMessage());
            result = null;
        }
        return result;
    }

    private MoOrderLine parseOrderLine(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        MoOrderLine result = new MoOrderLine();
        result.setCategory(getStrParam(parameters, "category"));
        result.setCurrency(getStrParam(parameters, "currency"));
        result.setDiscountAmount(getDoubleParam((Map<String, Object>) parameters.get("value"), "discountAmount"));
        result.setImageUrl(getStrParam(parameters, "imageUrl"));
        result.setName(getStrParam(parameters, "name"));
        result.setProductUrl(getStrParam(parameters, "productUrl"));
        result.setQuantity(getLongParam(parameters, "quantity"));
        result.setSku(getStrParam(parameters, "sku"));
        result.setTotalAmount(getDoubleParam((Map<String, Object>) parameters.get("value"), "totalAmount"));
        result.setType(getStrParam(parameters, "type"));
        result.setUnitPrice(getDoubleParam((Map<String, Object>) parameters.get("value"), "unitPrice"));
        /*try{
            crossStorageApi.createOrUpdate(defaultRepo, result);    
        } catch (Exception e){
          LOG.error("error persisting  orderline:{} [{}]",result,e.getMessage());
          result=null;
        }*/
        return result;
    }

    private List<MoOrderLine> parseOrderLines(List<Map<String, Object>> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return null;
        }
        List<MoOrderLine> result = new ArrayList<>();
        for (Map<String, Object> lineParam : parameters) {
            MoOrderLine line = parseOrderLine(lineParam);
            if (line != null) {
                result.add(line);
            }
        }
        return result;
    }


    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        if (orderId != null) {
            LOG.info("get Order[{}] {}", orderId, parameters);
            try {
                if (orderId.startsWith("ord_")) {
                    orderId = orderId.substring(4);
                }
                order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);
            } catch (Exception e) {
                LOG.error("cannot get order " + orderId, e);
            }
            return;
        }
        LOG.info("CreateOrder {}", parameters);
        Map<String, Object> amountMap = (Map<String, Object>) parameters.get("amount");
        MoOrder order = new MoOrder();
        order.setAmount(Double.parseDouble(getStrParam(amountMap, "value")));
        order.setCurrency(getStrParam(amountMap, "currency"));
        try {
            order.setMetadata(mapper.writeValueAsString(parameters.get("metadata")));
        } catch (Exception e) {
            order.setMetadata(getStrParam(parameters, "metadata"));
        }
        order.setOrderNumber(getStrParam(parameters, "orderNumber"));
        order.setRedirectUrl(getStrParam(parameters, "redirectUrl"));
        if (!"licoin".equals(getStrParam(parameters, "method"))) {
            result = "{\"error\":\"invalid payment method\"";
        }
        order.setLocale(getStrParam(parameters, "locale"));
        order.setRedirectUrl(getStrParam(parameters, "redirectUrl").replace("http:", "https:"));
        order.setWebhookUrl(getStrParam(parameters, "webhookUrl").replace("http:", "https:"));
        order.setBillingAddress(parseAddress((Map<String, Object>) parameters.get("billingAddress")));
        order.setShippingAddress(parseAddress((Map<String, Object>) parameters.get("shippingAddress")));
        order.setLines(parseOrderLines((List<Map<String, Object>>) parameters.get("lines")));
        order.setStatus("created");
        parameters.put("status", "created");
        order.setCreationDate(java.time.Instant.now());
        parameters.put("createdAt", order.getCreationDate().toString());
        order.setExpiresAt(order.getCreationDate().plus(Duration.ofDays(10)));
        parameters.put("expiresAt", order.getExpiresAt().toString());
        try {
            String orderId = crossStorageApi.createOrUpdate(defaultRepo, order);

            parameters.put("id", "ord_" + orderId);
            parameters.put("resource", "order");
            Map<String, Object> links = new HashMap<>();
            Map<String, String> self = new HashMap<>();
            self.put("href", MEVEO_BASE_URL + "/rest/v1/orders/" + orderId);
            self.put("type", "application/hal+json");
            links.put("self", self);
            Map<String, String> checkout = new HashMap<>();
            checkout.put("href", MEVEO_BASE_URL + "/rest/paymentpages/checkout/" + orderId);
            checkout.put("type", "text/html");
            links.put("checkout", checkout);
            Map<String, String> dashboard = new HashMap<>();
            dashboard.put("href", BASE_URL +"dashboard?orderid=" + orderId);
            dashboard.put("type", "text/html");
            links.put("dashboard", dashboard);
            Map<String, String> documentation = new HashMap<>();
            documentation.put("href", "https://docs.liquichain.io/reference/v2/orders-api/get-order");
            // see "https://docs.mollie.com/reference/v2/orders-api/get-order",
            documentation.put("type", "text/html");
            links.put("documentation", documentation);

            parameters.put("_links", links);
            parameters.put("_embed", new ArrayList<>());
            try {
                result = mapper.writeValueAsString(parameters);
            } catch (Exception ex) {
                result = "" + parameters;
            }
        } catch (Exception e) {
            LOG.error("error persisting  order:{} [{}]", result, e.getMessage());
            result = "{\"error\":\"error persisting order\"";
        }
    }

}
