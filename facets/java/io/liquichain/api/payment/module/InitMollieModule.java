package io.liquichain.api.payment.module;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.script.module.ModuleScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitMollieModule extends ModuleScript {
    private static final Logger LOG = LoggerFactory.getLogger(InitMollieModule.class);
    private final CustomFieldTemplateService cftService = getCDIBean(CustomFieldTemplateService.class);

    @Override
    public void preInstallModule(Map<String, Object> methodContext) throws BusinessException {
        super.execute(methodContext);

        CftInstaller cftInstaller = new CftInstaller();
        cftInstaller.installCFTs(cftService, LOG);
    }
}

class CftInstaller implements Serializable {
    private static final String TRANSACTION_CET = "CE_Transaction";

    public void installCFTs(CustomFieldTemplateService cftService, Logger LOG) {
        List<CustomFieldTemplate> cfts = new ArrayList<>();
        cfts.add(buildStringField("currency", "Currency", 10L));
        cfts.add(buildStringField("description", "Description", 255L));
        cfts.add(buildStringField("redirectUrl", "Redirect URL", 255L));
        cfts.add(buildStringField("webhookUrl", "Webhoo URL", 255L));
        cfts.add(buildStringField("metadata", "Metadata", 255L));
        cfts.add(buildStringField("orderId", "Order ID", 255L));
        cfts.add(buildDateField("expirationDate", "Expiration date"));

        for (CustomFieldTemplate cft : cfts) {
            try {
                cftService.create(cft);
            } catch (Exception e) {
                // do nothing, just continue
            }
        }
    }

    private CustomFieldTemplate buildStringField(String code, String description, Long size) {
        CustomFieldTemplate cft = new CustomFieldTemplate();
        cft.setCode(code);
        cft.setDescription(description);
        cft.setFieldType(CustomFieldTypeEnum.STRING);
        cft.setStorageType(CustomFieldStorageTypeEnum.SINGLE);
        cft.setAppliesTo(TRANSACTION_CET);
        cft.setMaxValue(size);
        cft.setPersisted(true);
        cft.setFilter(true);
        cft.setAllowEdit(true);
        return cft;
    }

    private CustomFieldTemplate buildDateField(String code, String description) {
        CustomFieldTemplate cft = new CustomFieldTemplate();
        cft.setCode(code);
        cft.setDescription(description);
        cft.setFieldType(CustomFieldTypeEnum.STRING);
        cft.setStorageType(CustomFieldStorageTypeEnum.SINGLE);
        cft.setAppliesTo(TRANSACTION_CET);
        cft.setPersisted(true);
        cft.setFilter(true);
        cft.setAllowEdit(true);
        cft.setDisplayFormat("dd-M-yyyy HH:mm:ss");
        return cft;
    }

}