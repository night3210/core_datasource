package com.night3210.datasource.core.listeners;

import com.night3210.datasource.core.ListDataSource;
import com.night3210.datasource.core.fetch_result.BaseFetchResult;

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
    void storeItems(BaseFetchResult<? extends DataObject> result, BoolCallback callback);
}
