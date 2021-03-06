package org.meveo.model.customEntities;

import org.meveo.model.CustomEntity;
import java.util.List;
import org.meveo.model.persistence.DBStorageType;
import java.time.Instant;
import org.meveo.model.customEntities.MoOrderLine;
import java.util.ArrayList;
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

    private Instant consumerDateOfBirth;

    private String metadata;

    private Double quantity_shipped;

    private String orderNumber;

    private String redirectUrl;

    private Double amountCaptured;

    private String locale;

    private String assignedTo;

    private Instant expiredAt;

    private Boolean shopperCountryMustMatchBillingCountry;

    private Double amountRefunded;

    private String currency;

    private String payment;

    private List<MoOrderLine> lines = new ArrayList<>();

    private String email;

    private String group;

    private Double amount;

    private String method;

    private Instant creationDate;

    private Instant expiresAt;

    private String webhookUrl;

    private Instant canceledAt;

    private Instant paidAt;

    private MoAddress shippingAddress;

    private MoAddress billingAddress;

    private String status;

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

    public Instant getConsumerDateOfBirth() {
        return consumerDateOfBirth;
    }

    public void setConsumerDateOfBirth(Instant consumerDateOfBirth) {
        this.consumerDateOfBirth = consumerDateOfBirth;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Double getQuantity_shipped() {
        return quantity_shipped;
    }

    public void setQuantity_shipped(Double quantity_shipped) {
        this.quantity_shipped = quantity_shipped;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public Double getAmountCaptured() {
        return amountCaptured;
    }

    public void setAmountCaptured(Double amountCaptured) {
        this.amountCaptured = amountCaptured;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Boolean getShopperCountryMustMatchBillingCountry() {
        return shopperCountryMustMatchBillingCountry;
    }

    public void setShopperCountryMustMatchBillingCountry(Boolean shopperCountryMustMatchBillingCountry) {
        this.shopperCountryMustMatchBillingCountry = shopperCountryMustMatchBillingCountry;
    }

    public Double getAmountRefunded() {
        return amountRefunded;
    }

    public void setAmountRefunded(Double amountRefunded) {
        this.amountRefunded = amountRefunded;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPayment() {
        return payment;
    }

    public void setPayment(String payment) {
        this.payment = payment;
    }

    public List<MoOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<MoOrderLine> lines) {
        this.lines = lines;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(Instant canceledAt) {
        this.canceledAt = canceledAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public MoAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(MoAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public MoAddress getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(MoAddress billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override()
    public String getCetCode() {
        return "MoOrder";
    }
}
