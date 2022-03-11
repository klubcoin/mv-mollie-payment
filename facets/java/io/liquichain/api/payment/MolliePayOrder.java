package io.liquichain.api.payment;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.client.Entity;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.Transaction;
import io.liquichain.core.BlockForgerScript;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolliePayOrder extends Script {
  
    private static final Logger log = LoggerFactory.getLogger(MolliePayOrder.class);

    private class LoggingFilter implements ClientRequestFilter {
      @Override
      public void filter(ClientRequestContext requestContext) throws IOException {
          log.info(requestContext.getEntity().toString());
      }
    }
  
    private ScriptInstanceService scriptInstanceService  = getCDIBean(ScriptInstanceService.class);
    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();

    private final Client client = ClientBuilder.newClient();
    {
      client.register(new LoggingFilter());
    }
  
    private String destWallet = "212dfdd1eb4ee053b2f5910808b7f53e3d49ad2f";

    private String result;

    public String getResult() {
        return result;
    }

    @Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
        log.info(" parameters: {}",parameters);
        MoOrder order =null;
        String orderId = ""+parameters.get("orderId");
        String from = (""+parameters.get("from")).substring(2).toLowerCase();
        String sig = ""+parameters.get("sig");
        try{
            order = crossStorageApi.find(defaultRepo,orderId, MoOrder.class);
            Wallet originWallet = crossStorageApi.find(defaultRepo, Wallet.class).by("hexHash", from).getResult();
            log.info(" originWallet[{}] {}",from,originWallet);
            Wallet toWallet = crossStorageApi.find(defaultRepo, Wallet.class).by("hexHash", destWallet).getResult();
            BigInteger amount = new BigDecimal(order.getAmount()+"").multiply(ConversionRateScript.EUR_TO_LCN).movePointRight(18).toBigInteger();
            BigInteger originBalance = new BigInteger(originWallet.getBalance());      			
            if("created".equals(order.getStatus())||"pending".equals(order.getStatus())){
                if(amount.compareTo(originBalance)<=0){
                    
                    Transaction transac = new Transaction();
                    transac.setHexHash(orderId);
                    transac.setFromHexHash(from);
                    transac.setToHexHash(destWallet);
                    
                    //FIXME: increment the nonce
                    transac.setNonce("1");
        
                    transac.setGasPrice("0");
                    transac.setGasLimit("0");
                    transac.setValue(amount.toString());
                
                    //FIXME: sign the transaction
                    transac.setSignedHash(UUID.randomUUID().toString());
                
                    transac.setCreationDate(java.time.Instant.now());
                    try {
                        Map<String, Object> context = new HashMap<>();
                        context.put("transac",transac);
                        order.setPayment("from="+from+";date="+System.currentTimeMillis()+";sig="+sig);
                        context.put("order",order);
                        scriptInstanceService.execute("io.liquichain.api.payment.MollieUpdateOrder",context);
                        if("ok".equals(context.get("RESULT").toString())){
                          String whUrl =  order.getWebhookUrl();
                          String txId = whUrl.substring(whUrl.indexOf("tx=")+3);
                          whUrl = whUrl.substring(0,whUrl.indexOf("tx=")-1);
                          log.info(" call webhook {} , tx={}, id={}",whUrl,txId,orderId);
                          WebTarget target = client.target(whUrl);
                          target.queryParam("tx", txId).queryParam("id", "ord_"+orderId);
                          final MultivaluedHashMap entity = new MultivaluedHashMap();
  						  entity.add("tx", txId);
  						  entity.add("id", "ord_"+orderId);
                          //using response object to get more information
                          try(Response s = target.request().post(Entity.form(entity))){
                              String value = s.readEntity(String.class);
                              log.info("webhook response: " + value + ",  status " + s.getStatus());
                              result ="{\"status\":\"paid\"}";
                          } catch(Exception e){
                              //FIXME: implement retry strategy
                              log.error("webhook to {} error {}",order.getWebhookUrl(),e);
                              result ="{\"warning\":\"webhook failed\"}";
                          }
                        } else {
                        	result ="{\"error\":\"error persisting order\"}";
                        }
                    } catch(Exception e){
                        log.error("transaction ko",e);
                        result ="{\"error\":\"transaction error\"}";
                    }
                } else {
                    result ="{\"error\":\"insufficient balance\"}";
                }
            } else {
                result ="{\"error\":\"invalid order status\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            result ="{\"error\":\""+e.getMessage()+"\"}";
        }
	}

}