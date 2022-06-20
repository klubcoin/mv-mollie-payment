package io.liquichain.api.payment;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoOrder;
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
  
  	public void setPaymentId(String paymentId){
      	this.paymentId = paymentId;
    }
  
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
      	String uuid = paymentId.startsWith("tr_") ? paymentId = paymentId.substring(3) : paymentId;
	}
	
}