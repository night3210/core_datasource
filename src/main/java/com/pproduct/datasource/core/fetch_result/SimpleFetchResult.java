package com.pproduct.datasource.core.fetch_result;

import android.provider.ContactsContract;

import com.pproduct.datasource.core.listeners.DataObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Developer on 2/12/2016.
 */
public class SimpleFetchResult<T> extends BaseFetchResult {
    public SimpleFetchResult(List list) {
        super(list);
    }

    public SimpleFetchResult(Object result) {
        super(result);
    }

    @Override
    protected List parseList(List list) {
        return new CopyOnWriteArrayList(list);
    }
}