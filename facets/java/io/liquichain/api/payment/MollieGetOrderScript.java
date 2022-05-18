package io.liquichain.api.payment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.MoOrderLine;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class MollieGetOrderScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieGetOrderScript.class);

    @Inject
    private CrossStorageApi crossStorageApi;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private ParamBeanFactory paramBeanFactory;

    private Repository defaultRepo = null;

    private final ObjectMapper mapper = new ObjectMapper();
    private String BASE_URL = null;
    private String MEVEO_BASE_URL = null;

    private String orderId;
    private String result;

    public String getResult() {
        return this.result;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
        return data.toString();
    }

    private MoOrderLine getOrderLine(String uuid) {
        try {
            return crossStorageApi.find(defaultRepo, uuid, MoOrderLine.class);
        } catch (EntityDoesNotExistsException e) {
            LOG.error("Failed to fetch order line: {}", uuid);
        }
        return null;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();
        LOG.info("get Order[{}] {}", orderId, parameters);
        try {
            if (orderId.startsWith("ord_")) {
                orderId = orderId.substring(4);
            }
            MoOrder order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);

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

            List<MoOrderLine> orderLines = order.getLines();
            LOG.info("orderLines: {}", orderLines);

            String lines = orderLines
                .stream()
                .map(
                    orderLine -> {
                        MoOrderLine line = getOrderLine(orderLine.getUuid());
                        if (line != null) {
                            return "{\n" +
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
                                "      \"createdAt\": \"" + line.getCreationDate() + "\"\n" +
                                "    }";
                        }
                        return "";
                    })
                .collect(Collectors.joining(",\n"));

            result += String.format("\"lines\": [%s],", lines);

            String embedQuery = (String) parameters.get("embed");
            if ("payments---GET".equals(embedQuery)) {
                List<Transaction> transactions = crossStorageApi
                    .find(defaultRepo, Transaction.class)
                    .by("orderId", id)
                    .getResults();
                result += "{\"_embedded\": {\"payments\": [" +
                    transactions.stream().map(
                        transaction ->
                            "{\n" +
                                "    \"resource\": \"payment\",\n" +
                                "    \"id\": \"" + id + "\",\n" +
                                "    \"mode\": \"test\",\n" +
                                "    \"createdAt\": \"" + transaction.getCreationDate() + "\",\n" +
                                "    \"amount\": {\n" +
                                "        \"value\": \"" + transaction.getValue() + "\",\n" +
                                "        \"currency\": \"" + transaction.getCurrency() + "\"\n" +
                                "    },\n" +
                                "    \"description\": \"" + transaction.getDescription() + "\",\n" +
                                "    \"method\": null,\n" +
                                "    \"metadata\": " + transaction.getMetadata() + ",\n" +
                                "    \"status\": \"open\",\n" +
                                "    \"isCancelable\": false,\n" +
                                "    \"expiresAt\": \"" + transaction.getExpirationDate() + "\",\n" +
                                "    \"details\": null,\n" +
                                "    \"profileId\": \"pfl_" + transaction.getUuid() + "\",\n" +
                                "    \"orderId\": \"" + transaction.getOrderId() + "\",\n" +
                                "    \"sequenceType\": \"oneoff\",\n" +
                                "    \"redirectUrl\": \"" + transaction.getRedirectUrl() + "\",\n" +
                                "    \"webhookUrl\": \"" + transaction.getWebhookUrl() + "\",\n" +
                                "    \"_links\": {\n" +
                                "        \"self\": {\n" +
                                "            \"href\": \"" + MEVEO_BASE_URL + "/rest/v1/payments/" + id + "\",\n" +
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
                                "            \"href\": \"https://docs.liquichain.io/reference/v2/payments-api/get-payment\",\n" +
                                "            \"type\": \"text/html\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}").collect(Collectors.joining(",\n"))
                    + "]}}";
            }

            result += "\"_links\": {\n" +
                "    \"self\": {\n" +
                "      \"href\": \"" + MEVEO_BASE_URL + "/rest/v1/orders/" + id + "\",\n" +
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
        } catch (Exception e) {
            LOG.error("Cannot retrieve order: " + orderId, e);
            result = "{\"error\": \"Cannot retrieve order: " + orderId + "\"}";
        }
        super.execute(parameters);
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


}
