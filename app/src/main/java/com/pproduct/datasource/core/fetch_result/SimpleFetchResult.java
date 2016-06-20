package com.pproduct.datasource.core.fetch_result;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Developer on 2/12/2016.
 */
public class SimpleFetchResult<T> extends BaseFetchResult {
    public SimpleFetchResult(Object list, boolean local) {
        super(list, local);
    }

    @Override
    protected List parseList(List list) {
        return new CopyOnWriteArrayList(list);
    }
}