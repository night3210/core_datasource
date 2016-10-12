package com.night3210.datasource.core.fetch_result;

import com.night3210.datasource.core.ErrorUtils;
import com.night3210.datasource.core.listeners.DataObject;

import java.util.HashMap;
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
            if(validateList(list)) {
                this.array = parseList((List) list);
                return;
            } else if(validateHashmap(list)) {
                this.array = (List<T>) ((HashMap)list).get("objects_list");
                return;
            }
        }
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

    protected abstract List<T> parseList(List list);
}

