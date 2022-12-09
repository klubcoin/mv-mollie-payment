package io.liquichain.api.payment;

import static io.liquichain.api.payment.PaymentService.*;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MollieUpdatePayment extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieUpdatePayment.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private final String BASE_URL = config.getProperty("meveo.admin.baseUrl", "http://localhost:8080/");
    private final String CONTEXT = config.getProperty("meveo.admin.webContext", "meveo");
    private final String MEVEO_BASE_URL = BASE_URL + CONTEXT;

    private String paymentId;
    private String result;

    public String getResult() {
        return result;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        LOG.info("MollieUpdatePayment parameters: {}", parameters);

        if (StringUtils.isBlank(paymentId)) {
            String error = "Payment id is required.";
            LOG.error(error);
            result = createErrorResponse("404", "Not found", error);
            return;
        }

        Transaction transaction;
        try {
            transaction = getSavedPayment(crossStorageApi, defaultRepo, parameters);
        } catch (Exception e) {
            String error = "Cannot retrieve payment: " + paymentId;
            LOG.error(error, e);
            result = createErrorResponse("404", "Not found", error);
            return;
        }
        String id = "tr_" + transaction.getUuid();
        String orderId = transaction.getOrderId();

        MoOrder order;
        try {
            String orderUuid = orderId.startsWith("ord_") ? orderId.substring(4) : orderId;
            order = crossStorageApi.find(defaultRepo, orderUuid, MoOrder.class);
        } catch (Exception e) {
            String error = "Cannot retrieve order: " + orderId;
            LOG.error(error, e);
            result = createErrorResponse("404", "Not found", error);
            return;
        }

        String status = "created".equals(order.getStatus()) ? "open" : order.getStatus();
        result = "{\n" +
            "    \"resource\": \"payment\",\n" +
            "    \"id\": \"" + id + "\",\n" +
            "    \"mode\": \"test\",\n" +
            "    \"status\": \"" + status + "\",\n" +
            "    \"createdAt\": \"" + transaction.getCreationDate() + "\",\n" +
            "    \"paidAt\": \"" + order.getPaidAt() + "\",\n" +
            "    \"canceledAt\": \"" + order.getCanceledAt() + "\",\n" +
            "    \"expiredAt\": \"" + order.getExpiredAt() + "\",\n" +
            "    \"amount\": {\n" +
            "        \"value\": \"" + transaction.getValue() + "\",\n" +
            "        \"currency\": \"" + transaction.getCurrency() + "\"\n" +
            "    },\n" +
            "    \"description\": \"" + transaction.getDescription() + "\",\n" +
            "    \"method\": \"" + order.getMethod() + "\",\n" +
            "    \"metadata\": " + transaction.getMetadata() + ",\n" +
            "    \"isCancelable\": false,\n" +
            "    \"expiresAt\": \"" + transaction.getExpirationDate() + "\",\n" +
            "    \"details\": null,\n" +
            "    \"profileId\": \"pfl_" + transaction.getUuid() + "\",\n" +
            "    \"sequenceType\": \"oneoff\",\n" +
            "    \"redirectUrl\": \"" + transaction.getRedirectUrl() + "\",\n" +
            "    \"webhookUrl\": \"" + transaction.getWebhookUrl() + "\",\n" +
            "    \"_links\": {\n" +
            "        \"self\": {\n" +
            "            \"href\": \"" + MEVEO_BASE_URL + "/rest/pg/v1/payments/" + id + "\",\n" +
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
