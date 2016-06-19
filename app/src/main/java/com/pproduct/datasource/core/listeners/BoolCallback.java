package com.pproduct.datasource.core.listeners;

/**
 * Created by haritonbatkov on 6/19/16.
 */
public interface BoolCallback {
    void onSuccess();
    void onError(Exception e);
}
