package io.liquichain.api.payment;

import java.util.Map;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;
import io.liquichain.core.BlockForgerScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MollieUpdateOrder extends Script {
  
    private static final Logger log = LoggerFactory.getLogger(MollieUpdateOrder.class);

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();


    @Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
       String result ="ok";
       log.info(" parameters: {}",parameters);
       MoOrder order = (MoOrder)parameters.get("order");
       Transaction transac = (Transaction)parameters.get("transac");
       try {
        crossStorageApi.createOrUpdate(defaultRepo, transac);
        //FIXME: you should get the BlockForgerScript from scriptService
        BlockForgerScript.addTransaction(transac);
        order.setStatus("paid");
        order.setAmountCaptured(order.getAmount());
        crossStorageApi.createOrUpdate(defaultRepo, order);
       } catch(Exception e){
            log.error("transaction ko",e);
            result = "ko";
       }
       parameters.put("RESULT",result);
	}

}