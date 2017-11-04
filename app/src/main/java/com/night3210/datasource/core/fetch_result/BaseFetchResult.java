package com.night3210.datasource.core.fetch_result;

import com.night3210.datasource.core.ErrorUtils;
import com.night3210.datasource.core.LogUtils;
import com.night3210.datasource.core.listeners.DataObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Developer on 2/12/2016.
 */
public abstract class BaseFetchResult<T extends DataObject> {
    protected List<T> items;
    protected Throwable lastError;

    public List<T> getItems() {
        return items;
    }

    public interface Provider<H extends DataObject> {
        BaseFetchResult<H> fetchResultForLocal(Object object);
        BaseFetchResult<H> fetchResult(Object object);
    }

    public BaseFetchResult(Object list, boolean local) {
        if(local) {
            if(validateList(list)) {
                this.items = parseList((List) list);
            }
        } else {
            if(validateList(list)) {
                this.items = parseList((List) list);
            } else if(validateHashmap(list)) {
                this.items = parseHashMap(((HashMap)list).get("objects_list"));
            }
        }
    }
    protected List<T> parseHashMap(Object list) {
        return (List<T>) list;
    }
    protected boolean validateHashmap(Object object) {
        if(object instanceof HashMap) {
            HashMap map = (HashMap) object;
            if(map.get("objects_list")!=null && map.get("objects_list") instanceof List)
                return true;
        }
        return false;
    }
    protected boolean validateList(Object list) {
        boolean isList = list instanceof List;
        if (!isList) {
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
        list.add(items);
        return list;
    }

    protected abstract List<T> parseList(List list);
}

