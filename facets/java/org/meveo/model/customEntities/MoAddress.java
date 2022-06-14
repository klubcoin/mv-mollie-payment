package org.meveo.model.customEntities;

import org.meveo.model.CustomEntity;
import java.util.List;
import org.meveo.model.persistence.DBStorageType;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class MoAddress implements CustomEntity {

    public MoAddress() {
    }

    public MoAddress(String uuid) {
        this.uuid = uuid;
    }

    private String uuid;

    @JsonIgnore()
    private DBStorageType storages;

    private String country;

    private String streetAdditional;

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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStreetAdditional() {
        return streetAdditional;
    }

    public void setStreetAdditional(String streetAdditional) {
        this.streetAdditional = streetAdditional;
    }

    @Override()
    public String getCetCode() {
        return "MoAddress";
    }
}
