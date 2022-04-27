package io.liquichain.api.payment;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.google.gson.Gson;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.MoCustomer;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MollieGetCustomer extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieGetCustomer.class);

    private static final String NOT_FOUND_STATUS = "404";
    private static final String NOT_FOUND = "Not Found";
    private static final String CUSTOMER_ID_REQUIRED = "customer Id is required.";
    private static final String CUSTOMER_NOT_FOUND = "No customer exists with customer id %s.";

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private final String BASE_URL = config.getProperty("meveo.admin.baseUrl", "http://localhost:8080/");
    private final String CONTEXT = config.getProperty("meveo.admin.webContext", "meveo");
    private final String MOLLIE_BASE_URL = BASE_URL + CONTEXT + "/rest/pg/v1/";

    private String result;
    private String customerId;

    public String getResult() {
        return this.result;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        if (customerId == null) {
            result = createErrorResponse(NOT_FOUND_STATUS, NOT_FOUND, CUSTOMER_ID_REQUIRED);
        }
        MoCustomer customer = crossStorageApi
            .find(defaultRepo, MoCustomer.class)
            .by("id", customerId)
            .getResult();
        if (customer != null) {
            result = createResponse(customer);
        } else {
            result = createErrorResponse(NOT_FOUND_STATUS, NOT_FOUND, String.format(CUSTOMER_NOT_FOUND, customerId));
        }
        super.execute(parameters);
    }

    private String nullOrString(String field) {
        if (field == null) {
            return "null";
        }
        return "\"" + field + "\"";
    }

    public String createResponse(MoCustomer customer) {
        String createdAt = null;
        if (customer.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withZone(ZoneId.from(ZoneOffset.UTC));
            createdAt = formatter.format(customer.getCreatedAt());
        }
        String selfUrl = MOLLIE_BASE_URL + "customers/" + customer.getId();
        String dashboardUrl = "https://www.mollie.com/dashboard/org_15108779/customers/" + customer.getId();
        String paymentsUrl = MOLLIE_BASE_URL + "customers/" + customer.getId() + "/payments";
        String response = "{\n" +
            "  \"resource\": \"customer\",\n" +
            "  \"id\": \"" + customer.getId() + "\",\n" +
            "  \"mode\": \"" + customer.getMode() + "\",\n" +
            "  \"name\": \"" + customer.getName() + "\",\n" +
            "  \"email\": \"" + customer.getEmail() + "\",\n" +
            "  \"locale\": " + nullOrString(customer.getLocale()) + ",\n" +
            "  \"metadata\": " + customer.getMetadata() + ",\n" +
            "  \"createdAt\": " + nullOrString(createdAt) + ",\n" +
            "  \"_links\": {\n" +
            "    \"self\": {\n" +
            "      \"href\": \"" + selfUrl +"\",\n" +
            "      \"type\": \"application/hal+json\"\n" +
            "    },\n" +
            "    \"dashboard\": {\n" +
            "      \"href\": \"" + dashboardUrl +"\",\n" +
            "      \"type\": \"text/html\"\n" +
            "    },\n" +
            "    \"payments\": {\n" +
            "      \"href\": \"" + paymentsUrl +"\",\n" +
            "      \"type\": \"application/hal+json\"\n" +
            "    },\n" +
            "    \"documentation\": {\n" +
            "      \"href\": \"https://docs.mollie.com/reference/v2/customers-api/get-customer\",\n" +
            "      \"type\": \"text/html\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
        LOG.debug("response: {}", response);
        return new Gson().toJson(response);
    }

    public String createErrorResponse(String status, String title, String detail) {
        String response = "{\n" +
            "  \"status\": " + status + ",\n" +
            "  \"title\": \"" + title + "\",\n" +
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

}
