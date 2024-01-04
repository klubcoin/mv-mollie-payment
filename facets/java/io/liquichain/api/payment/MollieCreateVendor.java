package io.liquichain.api.payment;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.MoVendor;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MollieCreateVendor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MollieCreateVendor.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String domain;
    private String walletAddress;
    private String method;

    private Map<String, Object> result = new LinkedHashMap<>();

    public Map<String, Object> getResult() {
        return result;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
		LOG.info("MollieCreateVendor - START");
        super.execute(parameters);

        MoVendor vendor = new MoVendor();
        vendor.setDomain(domain);
        vendor.setWalletAddress(walletAddress);
        vendor.setMethod(method);

        try {
            String uuid = crossStorageApi.createOrUpdate(defaultRepo, vendor);
            vendor.setUuid(uuid);
			LOG.info("MollieCreateVendor - vendor created: {}", vendor);
            result.put("status", "success");
            result.put("result", vendor);
        } catch (IOException e) {
            result.put("status", "fail");
            result.put("result", e.getMessage());
        }
		LOG.info("MollieCreateVendor - END");
    }

}