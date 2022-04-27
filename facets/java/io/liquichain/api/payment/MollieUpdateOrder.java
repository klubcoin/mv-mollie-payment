package io.liquichain.api.payment;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import io.liquichain.core.BlockForgerScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MollieUpdateOrder extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieUpdateOrder.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String result = "ok";
        LOG.info(" parameters: {}", parameters);
        MoOrder order = (MoOrder) parameters.get("order");
        Transaction transaction = (Transaction) parameters.get("transac");
        try {
            crossStorageApi.createOrUpdate(defaultRepo, transaction);
            //FIXME: you should get the BlockForgerScript from scriptService
            BlockForgerScript.addTransaction(transaction);
            order.setStatus("paid");
            order.setAmountCaptured(order.getAmount());
            crossStorageApi.createOrUpdate(defaultRepo, order);
        } catch (Exception e) {
            LOG.error("transaction ko", e);
            result = "ko";
        }
        parameters.put("RESULT", result);
    }
}
