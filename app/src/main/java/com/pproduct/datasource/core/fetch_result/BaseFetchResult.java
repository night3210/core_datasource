package com.pproduct.datasource.core.fetch_result;

import com.pproduct.datasource.core.ErrorUtils;
import com.pproduct.datasource.core.listeners.DataObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Developer on 2/12/2016.
 */
public abstract class BaseFetchResult<T> {
    protected List<T> array;
    protected Throwable lastError;

    public BaseFetchResult(Object list, boolean local) {
        if(local) {
            if(validateList(list)) {
                return;
            }
        } else {
            if(validateResult(list)) {
                return;
            }
        }
        this.array = parseList((List<Object>) list);
    }

    protected boolean validateList(Object list) {
        if (!(list instanceof List<?>)) {
            failWithReason(getClass().getName());
            return true;
        }
        return false;
    }

    protected boolean validateResult(Object result) {
        if (!(result instanceof HashMap<?, ?>)) {
            failWithReason(getClass().getName());
            return true;
        }
        return false;
    }

    public boolean validate() {
        return getSections() != null;
    }

    public Throwable getLastError() {
        return lastError;
    }

    protected void failWithReason(String reason) {
        lastError = ErrorUtils.createWrongServerDataException(reason);
    }

    public List<List<T>> getSections() {
        List<List<T>> list=new CopyOnWriteArrayList<>();
        list.add(array);
        return list;
    }

    protected abstract List<T> parseList(List<Object> list);
}

