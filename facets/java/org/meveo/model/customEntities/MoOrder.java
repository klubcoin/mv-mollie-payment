package org.meveo.model.customEntities;

import org.meveo.model.CustomEntity;
import java.util.List;
import org.meveo.model.persistence.DBStorageType;
import java.time.Instant;
import org.meveo.model.customEntities.MoOrderLine;
import java.util.ArrayList;
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

    private String metadata;

    private String method;

    private Double amountCaptured;

    private Double amountRefunded;

    private String payment;

    private Instant creationDate;

    private List<MoOrderLine> lines = new ArrayList<>();

    private String locale;

    private String email;

    private String webhookUrl;

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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
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

    public String getPayment() {
        return payment;
    }

    public void setPayment(String payment) {
        this.payment = payment;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public List<MoOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<MoOrderLine> lines) {
        this.lines = lines;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override()
    public String getCetCode() {
        return "MoOrder";
    }
}
