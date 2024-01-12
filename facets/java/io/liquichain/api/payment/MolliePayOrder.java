package io.liquichain.api.payment;

import static io.liquichain.api.payment.PaymentService.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionDecoder;
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

    private Web3j web3j;

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ConversionRateScript conversionRateScript = new ConversionRateScript();

    private Repository defaultRepo;
    private String orderId;
    private String data;
    private String result;

    public String getResult() {
        return result;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    private void init() {
        LOG.info(" init payOrder ");
        ParamBean config = paramBeanFactory.getInstance();
        defaultRepo = repositoryService.findDefaultRepository();
        String BESU_API_URL = config.getProperty("besu.api.url", "https://testnet.liquichain.io/rpc");
        web3j = Web3j.build(new HttpService(BESU_API_URL));
    }

    private String createError(String error) {
        return createErrorResponse("500", "Internal Server Error", error);
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

    private TransactionReceipt processTransaction(String data) {
        String transactionHash;
        try {
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(data).send();
            transactionHash = ethSendTransaction.getTransactionHash();
        } catch (IOException e) {
            throw new RuntimeException(createError(SEND_TRANSACTION_FAILED), e);
        }
        LOG.info("pending transactionHash: {}", transactionHash);

        if (transactionHash == null || transactionHash.isEmpty()) {
            throw new RuntimeException(createError(TRANSACTION_FAILED));
        }

        TransactionReceipt transactionReceipt;
        try {
            transactionReceipt = waitForTransactionReceipt(transactionHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return transactionReceipt;
    }

    private BigInteger computeAmount(MoOrder order) {
        String currency = order.getCurrency();
        LOG.info("currency: {}", currency);
        double amount = order.getAmount();
        LOG.info("amount: {}", amount);
        BigDecimal value = new BigDecimal(amount);
        BigDecimal conversionRate = conversionRateScript.getConversionRate(currency + "_TO_KLUB");
        LOG.info("conversionRate: {}", conversionRate);
        BigDecimal convertedValue = value.multiply(conversionRate);
        LOG.info("convertedValue: {}", convertedValue);
        BigInteger computedAmount = convertedValue.multiply(BigDecimal.TEN.pow(18)).toBigInteger();
        LOG.info("computedAmount: {}", computedAmount);
        return computedAmount;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();
        LOG.info("MolliePayOrder parameters: {}", parameters);

        Transaction transaction;
        try {
            orderId = orderId != null && orderId.startsWith("ord_") ? orderId : "ord_" + orderId;
            transaction = crossStorageApi.find(defaultRepo, Transaction.class).by("orderId", orderId).getResult();
        } catch (Exception e) {
            String error = String.format(PAYMENT_NOT_FOUND, orderId);
            LOG.error(error, e);
            result = createError(error);
            return;
        }

        RawTransaction rawTransaction = TransactionDecoder.decode(data);
        LOG.info("to:{} , value:{}", rawTransaction.getTo(), rawTransaction.getData());

        String recipient = rawTransaction.getTo();
        if (recipient == null || "0x0".equals(recipient) || "0x80".equals(recipient)) {
            result = createError(CONTRACT_NOT_ALLOWED_ERROR);
            return;
        }

        if (rawTransaction instanceof SignedRawTransaction) {
            SignedRawTransaction signedTransaction = (SignedRawTransaction) rawTransaction;
            Sign.SignatureData signatureData = signedTransaction.getSignatureData();

            String v = toHex(signatureData.getV());
            String s = toHex(signatureData.getS());
            String r = toHex(signatureData.getR());
            String to = normalizeHash(rawTransaction.getTo());
            String transactionData = signedTransaction.getTransaction().getData();
            BigInteger value = transactionData.startsWith("a9059cbb")
                    ? new BigInteger(transactionData.substring(72), 16)
                    : signedTransaction.getTransaction().getValue();

            String orderUuid = orderId.startsWith("ord_") ? orderId.substring(4) : orderId;
            MoOrder order;
            try {
                order = crossStorageApi.find(defaultRepo, orderUuid, MoOrder.class);
            } catch (Exception e) {
                LOG.error("Failed to retrieve order: " + orderId, e);
                result = createError(e.getMessage());
                return;
            }

            LOG.info("amount to be paid: {}", value);

            TransactionReceipt transactionReceipt;
            String completedTransactionHash;
            try {
                transactionReceipt = processTransaction(data);
                completedTransactionHash = transactionReceipt.getTransactionHash();
            } catch (RuntimeException e) {
                LOG.error(e.getMessage(), e);
                result = createError(e.getMessage());
                return;
            }

            LOG.info("completed transactionHash: {}", completedTransactionHash);

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
            transaction.setData("{\"type\":\"payonline\",\"description\":\"Pay online payment\"}");
            transaction.setType("payonline");
            try {
                String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
                LOG.info("Updated transaction on DB with uuid: {}", uuid);
            } catch (Exception e) {
                result = createError(e.getMessage());
                return;
            }

            try {
                order.setStatus("paid");
                order.setPaidAt(Instant.now());
                order.setAmountCaptured(order.getAmount());
                crossStorageApi.createOrUpdate(defaultRepo, order);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                result = createError(e.getMessage());
                return;
            }

            result = createResponse("{\"status\": \"paid\"}");

        } else {
            result = createError("Raw transaction was invalid.");
        }
        super.execute(parameters);
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
