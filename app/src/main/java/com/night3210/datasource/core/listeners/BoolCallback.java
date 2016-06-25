package com.night3210.datasource.core.listeners;

/**
 * Created by haritonbatkov on 6/19/16.
 */
public interface BoolCallback {
    void onSuccess();
    void onError(Throwable e);
}
