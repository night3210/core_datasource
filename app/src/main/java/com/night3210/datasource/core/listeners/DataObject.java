package com.night3210.datasource.core.listeners;

import java.util.Date;

/**
 * Created by Developer on 2/11/2016.
 */
public interface DataObject {
    public Date getCreatedAt();
    public Date getUpdatedAt();
    public String getObjectId();
}
