package io.liquichain.api.payment;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class MollieGetPayment extends Script {
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

    private String paymentId;
    private String result;

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

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

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        this.init();

        Transaction transaction;
        try {
            if (paymentId.startsWith("tr_")) {
                paymentId = paymentId.substring(3);
            }
            transaction = crossStorageApi.find(defaultRepo, paymentId, Transaction.class);
        } catch (EntityDoesNotExistsException e) {
            String error = "Failed to retrieve payment transaction: " + paymentId;
            LOG.error(error, e);
            result = createErrorResponse("500", "Internal Server Error", error);
            return;
        }
        String id = "tr_" + transaction.getUuid();
        String orderId = transaction.getOrderId();
        
        result = "{\n" +
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
            "}";
    }
}
