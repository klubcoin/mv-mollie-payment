package io.liquichain.api.payment;

import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

public class MollieGetPayment extends Script {
    private String paymentId;
    private String result;

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getResult() {
        return this.result;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }
}
