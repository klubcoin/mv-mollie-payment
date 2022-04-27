package io.liquichain.api.payment;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolliePayOrder extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MolliePayOrder.class);
    private static final String DESTINATION_WALLET = "212dfdd1eb4ee053b2f5910808b7f53e3d49ad2f";

    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final Client client = ClientBuilder.newClient();


    private static final class LoggingFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            LOG.info(requestContext.getEntity().toString());
        }
    }


    {
        client.register(new LoggingFilter());
    }

    private String result;

    public String getResult() {
        return result;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        LOG.info(" parameters: {}", parameters);
        MoOrder order = null;
        String orderId = "" + parameters.get("orderId");
        String from = ("" + parameters.get("from")).substring(2).toLowerCase();
        String sig = "" + parameters.get("sig");
        try {
            order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);
            Wallet originWallet = crossStorageApi.find(defaultRepo, Wallet.class).by("hexHash", from).getResult();
            LOG.info(" originWallet[{}] {}", from, originWallet);
            Wallet toWallet =
                crossStorageApi.find(defaultRepo, Wallet.class).by("hexHash", DESTINATION_WALLET).getResult();
            BigInteger amount = new BigDecimal(order.getAmount() + "")
                .multiply(ConversionRateScript.EUR_TO_LCN)
                .movePointRight(18)
                .toBigInteger();
            BigInteger originBalance = new BigInteger(originWallet.getBalance());
            if ("created".equals(order.getStatus()) || "pending".equals(order.getStatus())) {
                if (amount.compareTo(originBalance) <= 0) {
                    Transaction transaction = new Transaction();
                    transaction.setHexHash(orderId);
                    transaction.setFromHexHash(from);
                    transaction.setToHexHash(DESTINATION_WALLET);
                    //FIXME: increment the nonce
                    transaction.setNonce("1");
                    transaction.setGasPrice("0");
                    transaction.setGasLimit("0");
                    transaction.setValue(amount.toString());
                    //FIXME: sign the transaction
                    transaction.setSignedHash(UUID.randomUUID().toString());
                    transaction.setCreationDate(java.time.Instant.now());
                    try {
                        Map<String, Object> context = new HashMap<>();
                        context.put("transac", transaction);
                        order.setPayment("from=" + from + ";date=" + System.currentTimeMillis() + ";sig=" + sig);
                        context.put("order", order);
                        scriptInstanceService.execute("io.liquichain.api.payment.MollieUpdateOrder", context);
                        if ("ok".equals(context.get("RESULT").toString())) {
                            String whUrl = order.getWebhookUrl();
                            String txId = whUrl.substring(whUrl.indexOf("tx=") + 3);
                            whUrl = whUrl.substring(0, whUrl.indexOf("tx=") - 1);
                            LOG.info(" call webhook {} , tx={}, id={}", whUrl, txId, orderId);
                            WebTarget target = client.target(whUrl);
                            target.queryParam("tx", txId).queryParam("id", "ord_" + orderId);
                            final MultivaluedHashMap entity = new MultivaluedHashMap();
                            entity.add("tx", txId);
                            entity.add("id", "ord_" + orderId);
                            //using response object to get more information
                            Response response = null;
                            try {
                                response = target.request().post(Entity.form(entity));
                                String value = response.readEntity(String.class);
                                LOG.info("webhook response: " + value + ",  status " + response.getStatus());
                                result = "{\"status\":\"paid\"}";
                            } catch (Exception e) {
                                //FIXME: implement retry strategy
                                LOG.error("webhook to {} error {}", order.getWebhookUrl(), e);
                                result = "{\"warning\":\"webhook failed\"}";
                            } finally {
                                if (response != null) {
                                    response.close();
                                }
                            }
                        } else {
                            result = "{\"error\":\"error persisting order\"}";
                        }
                    } catch (Exception e) {
                        LOG.error("transaction ko", e);
                        result = "{\"error\":\"transaction error\"}";
                    }
                } else {
                    result = "{\"error\":\"insufficient balance\"}";
                }
            } else {
                result = "{\"error\":\"invalid order status\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
