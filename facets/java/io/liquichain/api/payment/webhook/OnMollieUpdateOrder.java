package io.liquichain.api.payment.webhook;

import static io.liquichain.api.payment.PaymentService.*;
import static org.apache.commons.lang3.StringUtils.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnMollieUpdateOrder extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(OnMollieUpdateOrder.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private final List<String> VALID_WEBHOOK_STATUS = Arrays.asList("paid", "canceled", "expired", "failed");

    private String orderId;
    private Map<String, String> result = new LinkedHashMap<>();

    public Map<String, String> getResult() {
        return result;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        LOG.info("OnMollieUpdateOrder order: {}, parameters: {}", orderId, parameters);
        try {
            if (orderId == null) {
                throw new RuntimeException("Order ID is null. Will not invoke webhook.");
            }
            String uuid = removeStart(orderId, "ord_");
            String normalizedId = prependIfMissing(orderId, "ord_");

            MoOrder order = crossStorageApi.find(defaultRepo, uuid, MoOrder.class);
            LOG.info("OnMollieUpdateOrder order: {}", toJsonString(order));
            String status = order.getStatus();

            if (VALID_WEBHOOK_STATUS.contains(status)) {
                LOG.info("Searching for payment for order: " + normalizedId);
                Transaction payment = crossStorageApi.find(defaultRepo, Transaction.class)
                                                     .by("orderId", normalizedId)
                                                     .getResult();
                if (payment == null) {
                    throw new RuntimeException("Payment does not exist for order: " + normalizedId);
                }
                LOG.info("OnMollieUpdateOrder transaction: {}", toJsonString(payment));
                callWebhook(order, payment);
                result.put("status", "success");
                result.put("result", status);
            }
        } catch (Exception e) {
            LOG.error("Encountered errors while trying to invoke webhook.", e);
            result.put("status", "fail");
            result.put("result", e.getMessage());
        }
    }

}
