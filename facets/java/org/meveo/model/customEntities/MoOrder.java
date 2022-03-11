package org.meveo.model.customEntities;

import org.meveo.model.CustomEntity;
import java.util.List;
import org.meveo.model.persistence.DBStorageType;
import java.time.Instant;
import org.meveo.model.customEntities.MoAddress;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class MoOrder implements CustomEntity {

    public MoOrder() {
    }

    public MoOrder(String uuid) {
        this.uuid = uuid;
    }

    private String uuid;

    @JsonIgnore()
    private DBStorageType storages;

    private Double amount;

    private Instant consumerDateOfBirth;

    private Double amountCaptured;

    private Double amountRefunded;

    private MoAddress billingAddress;

    private String assignedTo;

    @Override()
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public DBStorageType getStorages() {
        return storages;
    }

    public void setStorages(DBStorageType storages) {
        this.storages = storages;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Instant getConsumerDateOfBirth() {
        return consumerDateOfBirth;
    }

    public void setConsumerDateOfBirth(Instant consumerDateOfBirth) {
        this.consumerDateOfBirth = consumerDateOfBirth;
    }

    public Double getAmountCaptured() {
        return amountCaptured;
    }

    public void setAmountCaptured(Double amountCaptured) {
        this.amountCaptured = amountCaptured;
    }

    public Double getAmountRefunded() {
        return amountRefunded;
    }

    public void setAmountRefunded(Double amountRefunded) {
        this.amountRefunded = amountRefunded;
    }

    public MoAddress getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(MoAddress billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    @Override()
    public String getCetCode() {
        return "MoOrder";
    }
}
