package io.liquichain.api.payment;

import static io.liquichain.api.payment.PaymentService.*;

import java.util.Map;

import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class MollieGetPayment extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieGetPayment.class);

    @Inject
    private CrossStorageApi crossStorageApi;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private ParamBeanFactory paramBeanFactory;

    private Repository defaultRepo = null;

    private String BASE_URL = null;
    private String MEVEO_BASE_URL = null;

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

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        this.init();
        LOG.info("MollieGetPayment paymentId: {}, parameters: {}", paymentId, parameters);

        Transaction transaction;
        String uuid = null;
        try {
            uuid = paymentId.startsWith("tr_") ? paymentId = paymentId.substring(3) : paymentId;
            transaction = crossStorageApi.find(defaultRepo, paymentId, Transaction.class);
            LOG.info("MollieGetPayment - transaction: {}", toJsonString(transaction));
        } catch (EntityDoesNotExistsException e) {
            String error = "Failed to retrieve payment transaction: " + uuid;
            LOG.error(error, e);
            result = createErrorResponse("404", "Not found", error);
            return;
        }
        String id = "tr_" + transaction.getUuid();
        String orderId = transaction.getOrderId();

        MoOrder order;
        try {
            boolean isUuid = orderId.startsWith("ord_");
            if(isUuid){
                String orderUuid = orderId.substring(4);
                order = crossStorageApi.find(defaultRepo, orderUuid, MoOrder.class);
            } else {
                order = crossStorageApi.find(defaultRepo, MoOrder.class)
                                       .by("orderNumber", orderId)
                                       .getResult();
            }
            LOG.info("MollieGetPayment - order: {}", toJsonString(order));
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
            "    \"isCancelable\": true,\n" +
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
