package io.liquichain.api.payment;

import static io.liquichain.api.payment.PaymentService.*;

import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();
        MoOrder order;
        Transaction payment;
        try {
            order = getSavedOrder(crossStorageApi, defaultRepo, parameters);
            payment = getSavedPayment(crossStorageApi, defaultRepo, order);
        } catch (Exception e) {
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
            + "\"billingAddress\": " + toJsonString(parameters.get("billingAddress")) + ","
            + "\"shoppercountrymustmatchbillingcountry\": false,"
            + "\"ordernumber\": \"" + order.getOrderNumber() + "\","
            + "\"redirecturl\": \"" + order.getRedirectUrl() + "\","
            + "\"webhookurl\": \"" + order.getWebhookUrl() + "\",";

        String lines = order.getLines()
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
