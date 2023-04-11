package io.liquichain.api.payment;

import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentStatus extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatus.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);

    private Repository defaultRepo;

    private String result;
    private String orderId;

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getResult() {
        return result;
    }

    private void init() {
        defaultRepo = repositoryService.findDefaultRepository();
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();
        LOG.info("PaymentStatus orderId: {}, parameters: {}", orderId, parameters);

        String orderUuid = orderId.startsWith("ord_") ? orderId.substring(4) : orderId;
        try {
            MoOrder order = crossStorageApi.find(defaultRepo, orderUuid, MoOrder.class);
            String status = "created".equals(order.getStatus()) ? "open" : order.getStatus();
            result = "{\"status\": \"" + status + "\"}";
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            result = "{\"error\": \"Failed to payment status.\"}";
        }

        super.execute(parameters);
    }
}
