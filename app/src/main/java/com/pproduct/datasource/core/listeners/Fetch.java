package com.pproduct.datasource.core.listeners;

import com.pproduct.datasource.core.ListDataSource;
import com.pproduct.datasource.core.fetch_result.BaseFetchResult;
import com.pproduct.datasource.core.listeners.DataCallback;

/**
 * Created by Developer on 2/11/2016.
 */
public interface Fetch {

    public enum Mode {
        OnlineOffline,
        Online,
        Offline
    }
    void fetchOnline(ListDataSource.Paging paging, DataCallback callback);
    void fetchOffline(DataCallback callback);
    void storeItems(BaseFetchResult<DataObject> result, BoolCallback callback);
}
