package io.liquichain.api.payment.webhook;

import static io.liquichain.api.payment.PaymentService.*;

import java.util.Arrays;
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

    private MoOrder order;

    public void setOrder(MoOrder order) {
        this.order = order;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        LOG.info("OnMollieUpdateOrder order: {}, parameters: {}", order, parameters);
        try {
            if (order == null) {
                throw new RuntimeException("Order is null. Will not invoke webhook");
            }
            String status = order.getStatus();
            if (VALID_WEBHOOK_STATUS.contains(status)) {
                String normalizedId = "ord_" + order.getUuid();
                LOG.info("Searching for payment for order: " + normalizedId);
                Transaction payment = crossStorageApi.find(defaultRepo, Transaction.class)
                                                     .by("orderId", normalizedId)
                                                     .getResult();
                if (payment == null) {
                    throw new RuntimeException("Payment does not exist for order: " + normalizedId);
                }
                callWebhook(order, payment);
            }
        } catch (Exception e) {
            LOG.error("Encountered errors while trying to invoke webhook.", e);
        }
    }

}
