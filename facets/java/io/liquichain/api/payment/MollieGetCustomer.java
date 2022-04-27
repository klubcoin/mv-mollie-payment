package io.liquichain.api.payment;

import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

public class MollieGetCustomer extends Script {
  private String result;
  private String customerId;
  
  public String getResult(){
    return this.result;
  }
  
  public void setCustomerId(String customerId){
    this.customerId = customerId;
  }
	
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
	}
	
}