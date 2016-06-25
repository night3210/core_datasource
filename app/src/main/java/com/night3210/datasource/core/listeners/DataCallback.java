package com.night3210.datasource.core.listeners;

/**
 * Created by Developer on 2/11/2016.
 */
public interface DataCallback {
    void onSuccess(Object result);
    void onError(Throwable th);
}
