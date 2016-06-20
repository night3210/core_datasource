package com.pproduct.datasource.core.fetch_result;

import com.pproduct.datasource.core.ErrorUtils;
import com.pproduct.datasource.core.listeners.DataObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Developer on 2/12/2016.
 */
public abstract class BaseFetchResult<T extends DataObject> {
    protected List<T> array;
    protected Throwable lastError;

    public BaseFetchResult(Object list, boolean local) {
        if(local) {
            if(!validateList(list)) {
                return;
            }
        } else {
            if(!validateList(list)) {
                return;
            }
        }
        this.array = parseList((List<Object>) list);
    }

    protected boolean validateList(Object list) {
        if (!(list instanceof List<?>)) {
            failWithReason(getClass().getName());
            return false;
        }
        return true;
    }

    protected boolean validateMapResult(Object result) {
        if (!(result instanceof Map<?, ?>)) {
            failWithReason(getClass().getName());
            return false;
        }
        return true;
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

