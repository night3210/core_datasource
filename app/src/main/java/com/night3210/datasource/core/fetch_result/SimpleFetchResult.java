package com.night3210.datasource.core.fetch_result;

import com.night3210.datasource.core.listeners.DataObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Developer on 2/12/2016.
 */
public class SimpleFetchResult<T extends DataObject> extends BaseFetchResult<T> {
    public SimpleFetchResult(Object list, boolean local) {
        super(SimpleFetchResult.tryToParseSimpleAnswer(list, local), local);
    }

    @Override
    protected List<T> parseList(List list) {
        return new CopyOnWriteArrayList(list);
    }

    private static Object tryToParseSimpleAnswer(Object list, boolean local) {
        Object objToParse = list;
        if (!local && list instanceof Map) {
            Map<String, Object> map = (Map) list;
            if (map.size() == 1) {
                String theKey = (String) map.keySet().toArray()[0];
                Object objectInside = map.get(theKey);
                List<Object> items = null;
                if (objectInside instanceof List) {
                    items = (List<Object>) objectInside;
                } else {
                    items = new ArrayList<>();
                    items.add(objectInside);
                }
                objToParse = items;
            }
        }
        return objToParse;
    }
}