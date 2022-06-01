package io.liquichain.api.payment;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.*;
import org.web3j.protocol.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.ClientConnectionException;

public class MolliePayOrder extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MolliePayOrder.class);
    private static final String CONTRACT_NOT_ALLOWED_ERROR = "Contract deployment not allowed.";
    private static final String PAYMENT_NOT_FOUND = "Payment for orderId: %s, does not exist.";
    private static final String TRANSACTION_FAILED = "Transaction failed.";
    private static final String SEND_TRANSACTION_FAILED = "Sending eth transaction failed.";
    private static final int SLEEP_DURATION = 1000;
    private static final int ATTEMPTS = 40;

    private Client client;
    private Web3j web3j;

    @Inject
    private CrossStorageApi crossStorageApi;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private ParamBeanFactory paramBeanFactory;

    private Repository defaultRepo;
    private String orderId;
    private String result;

    public String getResult() {
        return result;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    private void init() {
        ParamBean config = paramBeanFactory.getInstance();
        defaultRepo = repositoryService.findDefaultRepository();
        String BESU_API_URL = config.getProperty("besu.api.url", "https://testnet.liquichain.io/rpc");
        web3j = Web3j.build(new HttpService(BESU_API_URL));
    }

    private static String normalizeHash(String hash) {
        if (hash.startsWith("0x")) {
            return hash.substring(2).toLowerCase();
        }
        return hash.toLowerCase();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hexValue = new StringBuilder();
        for (byte aByte : bytes) {
            hexValue.append(String.format("%02x", aByte));
        }
        return hexValue.toString().toLowerCase();
    }

    private static String createResponse(String result) {
        String resultFormat = result.startsWith("{") ? "%s" : "\"%s\"";
        String response = "{\n" +
            "  \"result\": " + String.format(resultFormat, result) + "\n" +
            "}";
        LOG.debug("response: {}", response);
        return response;
    }

    private String createErrorResponse(String detail) {
        String response = "{\n" +
            "  \"status\": 500,\n" +
            "  \"title\": \"Internal Error\",\n" +
            "  \"detail\": \"" + detail + "\",\n" +
            "  \"_links\": {\n" +
            "    \"documentation\": {\n" +
            "      \"href\": \"https://docs.mollie.com/errors\",\n" +
            "      \"type\": \"text/html\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
        LOG.debug("error response: {}", response);
        return response;
    }

    public Optional<TransactionReceipt> sendTransactionReceiptRequest(String transactionHash)
        throws Exception {
        EthGetTransactionReceipt transactionReceipt = web3j
            .ethGetTransactionReceipt(transactionHash)
            .sendAsync()
            .get();
        return transactionReceipt.getTransactionReceipt();
    }

    private Optional<TransactionReceipt> getTransactionReceipt(String transactionHash) throws Exception {
        Optional<TransactionReceipt> receiptOptional =
            sendTransactionReceiptRequest(transactionHash);
        for (int i = 0; i < ATTEMPTS; i++) {
            if (receiptOptional.isEmpty()) {
                Thread.sleep(SLEEP_DURATION);
                receiptOptional = sendTransactionReceiptRequest(transactionHash);
            } else {
                break;
            }
        }
        return receiptOptional;
    }

    private TransactionReceipt waitForTransactionReceipt(String transactionHash) throws Exception {
        Optional<TransactionReceipt> transactionReceiptOptional = getTransactionReceipt(transactionHash);

        if (transactionReceiptOptional.isEmpty()) {
            throw new BusinessException("Transaction receipt not generated after " + ATTEMPTS + " attempts");
        }
        return transactionReceiptOptional.get();
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();
        LOG.info(" parameters: {}", parameters);
        String data = (String) parameters.get("data");
        String orderId = (String) parameters.get("orderId");
        Transaction transaction;
        try {
            transaction = crossStorageApi.find(defaultRepo, Transaction.class).by("orderId", orderId).getResult();
        } catch (Exception e) {
            String error = String.format(PAYMENT_NOT_FOUND, orderId);
            LOG.error(error, e);
            result = createErrorResponse(error);
            return;
        }

        RawTransaction rawTransaction = TransactionDecoder.decode(data);
        LOG.info("to:{} , value:{}", rawTransaction.getTo(), rawTransaction.getValue());

        String recipient = rawTransaction.getTo();
        if (recipient == null || "0x0".equals(recipient) || "0x80".equals(recipient)) {
            result = createErrorResponse(CONTRACT_NOT_ALLOWED_ERROR);
            return;
        }

        String transactionHash;
        try {
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(data).send();
            transactionHash = ethSendTransaction.getTransactionHash();
        } catch (IOException e) {
            result = createErrorResponse(SEND_TRANSACTION_FAILED);
            return;
        }
        LOG.info("pending transactionHash: {}", transactionHash);

        if (transactionHash == null || transactionHash.isEmpty()) {
            result = createErrorResponse(TRANSACTION_FAILED);
            return;
        }

        String completedTransactionHash;
        TransactionReceipt transactionReceipt;
        try {
            transactionReceipt = waitForTransactionReceipt(transactionHash);
            completedTransactionHash = transactionReceipt.getTransactionHash();
        } catch (Exception e) {
            result = createErrorResponse(e.getMessage());
            return;
        }
        LOG.info("completed transactionHash: {}", completedTransactionHash);

        if (rawTransaction instanceof SignedRawTransaction) {
            SignedRawTransaction signedTransaction = (SignedRawTransaction) rawTransaction;
            Sign.SignatureData signatureData = signedTransaction.getSignatureData();
            try {
                String v = toHex(signatureData.getV());
                String s = toHex(signatureData.getS());
                String r = toHex(signatureData.getR());
                String to = normalizeHash(rawTransaction.getTo());
                BigInteger value = rawTransaction.getValue();

                transaction.setHexHash(completedTransactionHash);
                transaction.setFromHexHash(normalizeHash(transactionReceipt.getFrom()));
                transaction.setToHexHash(normalizeHash(transactionReceipt.getTo()));
                transaction.setNonce("" + rawTransaction.getNonce());
                transaction.setGasPrice("" + rawTransaction.getGasPrice());
                transaction.setGasLimit("" + rawTransaction.getGasLimit());
                transaction.setValue("" + value);
                transaction.setSignedHash(data);
                transaction.setBlockNumber("" + transactionReceipt.getBlockNumber());
                transaction.setBlockHash(normalizeHash(transactionReceipt.getBlockHash()));
                transaction.setV(v);
                transaction.setS(s);
                transaction.setR(r);
                transaction.setData("{\"type\":\"mollie\",\"description\":\"Mollie Payment\"}");
                transaction.setType("mollie");
                String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
                LOG.info("Updated transaction on DB with uuid: {}", uuid);
            } catch (Exception e) {
                result = createErrorResponse(e.getMessage());
                return;
            }
        }

        String orderUuid = orderId.startsWith("ord_") ? orderId.substring(4) : orderId;
        MoOrder order;
        try {
            order = crossStorageApi.find(defaultRepo, orderUuid, MoOrder.class);
            order.setStatus("paid");
            order.setAmountCaptured(order.getAmount());
            crossStorageApi.createOrUpdate(defaultRepo, order);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            result = createErrorResponse(e.getMessage());
            return;
        }

        Response response = null;
        String webhookUrl = order.getWebhookUrl();
        try {
            response = client.target(webhookUrl)
                             .request(MediaType.APPLICATION_JSON)
                             .post(Entity.json("{\"id\": \"tr_" + transaction.getUuid() + "\"}"));
        } catch (Exception e) {
            String error = String.format("Invoking webhook: %s, failed.", webhookUrl);
            LOG.error(error, e);
            result = createErrorResponse(error);
            return;
        } finally {
            if (response != null) {
                response.close();
            }
        }

        result = createResponse("{\"status\": \"paid\"}");
        super.execute(parameters);
    }
}


class ErrorMessage {
    private String code;
    private String message;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


class JsonRpcError {
    private String id;
    private String jsonrpc;
    private ErrorMessage error;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public ErrorMessage getError() {
        return error;
    }

    public void setError(ErrorMessage error) {
        this.error = error;
    }
}


class HttpService extends Service {

    public static final String DEFAULT_URL = "http://localhost:8545/";
    private static final Logger LOG = LoggerFactory.getLogger(HttpService.class);

    private Client httpClient;
    private final String url;
    private final boolean includeRawResponse;
    private Map<String, String> headers = new HashMap<>();

    public HttpService(String url, Client httpClient, boolean includeRawResponse) {
        super(includeRawResponse);
        this.url = url;
        this.httpClient = httpClient;
        this.includeRawResponse = includeRawResponse;
    }

    public HttpService(Client httpClient, boolean includeRawResponse) {
        this(DEFAULT_URL, httpClient, includeRawResponse);
    }

    public HttpService(String url, Client httpClient) {
        this(url, httpClient, false);
    }

    public HttpService(String url) {
        this(url, createHttpClient());
    }

    public HttpService(String url, boolean includeRawResponse) {
        this(url, createHttpClient(), includeRawResponse);
    }

    public HttpService(Client httpClient) {
        this(DEFAULT_URL, httpClient);
    }

    public HttpService(boolean includeRawResponse) {
        this(DEFAULT_URL, includeRawResponse);
    }

    public HttpService() {
        this(DEFAULT_URL);
    }

    private static Client createHttpClient() {
        return ClientBuilder.newClient();
    }

    @Override
    protected InputStream performIO(String request) throws IOException {

        LOG.debug("Request: {}", request);

        Response response = null;
        try {
            response = httpClient.target(url)
                                 .request(MediaType.APPLICATION_JSON)
                                 .headers(convertHeaders())
                                 .post(Entity.json(request));
        } catch (ClientConnectionException e) {
            throw new IOException("Unable to connect to " + url, e);
        }

        if (response.getStatus() != 200) {
            throw new IOException(
                "Error " + response.getStatus() + ": " + response.readEntity(String.class));
        }

        if (includeRawResponse) {
            return new BufferedInputStream(response.readEntity(InputStream.class));
        }

        return new ByteArrayInputStream(response.readEntity(String.class).getBytes());
    }

    private MultivaluedMap<String, Object> convertHeaders() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            headers.put(entry.getKey(), Arrays.asList(entry.getValue()));
        }
        return headers;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addHeaders(Map<String, String> headersToAdd) {
        headers.putAll(headersToAdd);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void close() throws IOException {
    }

}
