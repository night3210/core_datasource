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
    protected List<T> array;
    protected Throwable lastError;

    public BaseFetchResult(Object list, boolean local) {
        if(local) {
            if(validateList(list)) {
                this.array = parseList((List) list);
            }
        } else {
            if(validateList(list)) {
                this.array = parseList((List) list);
                return;
            } else if(validateHashmap(list)) {
                LogUtils.logi("xxa online. is a hashmap");
                this.array = parseHashMap(((HashMap)list).get("objects_list"));
                return;
            }
        }
    }
    protected List<T> parseHashMap(Object list) {
        return (List<T>) ((HashMap)list).get("objects_list");
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
        boolean chk = list instanceof List;
        LogUtils.logi("xxa "+chk+"/"+list);
        if (chk==false) {
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
        LogUtils.loge("xxa ds failed:"+reason);
    }

    public List<List<T>> getSections() {
        List<List<T>> list=new CopyOnWriteArrayList<>();
        list.add(array);
        return list;
    }

    protected abstract List<T> parseList(List list);
}

