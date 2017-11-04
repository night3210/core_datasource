package com.night3210.datasource.core;

import com.night3210.datasource.core.data_structure.DataStructure;
import com.night3210.datasource.core.fetch_result.BaseFetchResult;
import com.night3210.datasource.core.fetch_result.SimpleFetchResult;
import com.night3210.datasource.core.listeners.BoolCallback;
import com.night3210.datasource.core.listeners.ChangedCallback;
import com.night3210.datasource.core.listeners.DataCallback;
import com.night3210.datasource.core.listeners.DataObject;
import com.night3210.datasource.core.listeners.Fetch;

import org.jetbrains.annotations.NotNull;

public class ListDataSource<T extends DataObject> extends DataSource {
    private final int DEFAULT_LIMIT = 20;
    protected int defaultPageSize = DEFAULT_LIMIT;
    protected Paging paging;
    protected boolean pagingEnabled = true;
    protected boolean canLoadMore = false;
    protected boolean autoAdvance = false;
    protected ChangedCallback changedListener;
    protected DataStructure<T> dataStructure;
    protected Fetch fetch;
    protected Fetch.Mode fetchMode = Fetch.Mode.OnlineOffline;
    protected BaseFetchResult.Provider<T> fetchResultProvider;
    protected DataStructureProvider<T> dataStructureProvider;
    protected BoolCallback itemsStoredListener;

    protected DataSortingProvider dataSortingProvider;
    protected DataStructure.CustomSortingProvider dataCustomSortingProvider;

    protected StoragePolicy storagePolicy = StoragePolicy.FIRST_PAGE;

    public class Paging {
        private int skip;
        private int limit;
        public int getLimit() {
            return limit;
        }
        public int getSkip() {
            return skip;
        }
    }

    public enum StoragePolicy {FIRST_PAGE, ALL_DATA}

    public interface DataStructureProvider<H extends DataObject> {
        DataStructure<H> dataStructureForFetchResult(BaseFetchResult<H> fetchResult);
    }
    public interface DataSortingProvider<H extends DataObject> {
        DataStructure.Sorting dataSortingForFetchResult(BaseFetchResult<H> fetchResult);
    }

    public ListDataSource(Fetch fetch) {
        super();
        if(fetch == null)
            throw new IllegalArgumentException("No fetch specified");
        this.fetch = fetch;
        this.pagingEnabled = true;
        setFetchResultProvider(new BaseFetchResult.Provider<T>() {
            @Override
            public BaseFetchResult<T> fetchResultForLocal(Object object) {
                return new SimpleFetchResult<>(object,true);
            }
            @Override
            public BaseFetchResult<T> fetchResult(Object object) {
                return new SimpleFetchResult<>(object,false);
            }
        });
    }

    public Paging getPaging() {
        if(!pagingEnabled)
            return null;
        if(paging == null) {
            paging = new Paging();
            paging.limit = getDefaultPageSize();
        }
        return paging;
    }

    protected void fetchOfflineData(final boolean refresh) {
        if (fetchMode == Fetch.Mode.Online) {
            return; // Offline disabled
        }
        fetch.fetchOffline(new DataCallback() {
            @Override
            public void onSuccess(Object result) {
                if (dataStructure == null || refresh) {
                    if (fetchMode == Fetch.Mode.Offline && shouldClearList()) {
                        dataStructure = null;
                    }
                    BaseFetchResult<T> fetchResult = createFetchResultForLocalObject(result);
                    processFetchResult(fetchResult);
                }
                if(refresh) {
                    contentLoaded(null);
                }
            }
            @Override
            public void onError(Throwable th) {
                th.printStackTrace();
                // Don't know why can it be
                if (refresh) {
                    contentLoaded(th);
                }
            }
        });
    }

    public void resetData() {
        canLoadMore = true;
        paging = null;
        dataStructure = null;
    }

    protected void runRequest() {
        if (fetchMode == Fetch.Mode.Offline) {
            fetchOfflineData(true);
            return;
        }
        fetch.fetchOnline(getPaging(), createResultBlock());
    }

    protected boolean shouldClearList() {
        if (!pagingEnabled) {
            return true;
        }

        return getPaging().skip == 0;
    }

    protected void updatePagingFlagsForListSize() {
        if (!pagingEnabled)
            return;
        int size = dataStructure.dataSize();
        canLoadMore = paging.getSkip() + paging.getLimit() <= size;
        paging.skip = size;
    }

    protected void itemsLoaded(BaseFetchResult<T> fetchResult) {
        boolean calledForStore = false;
        if (shouldClearList()) {
            dataStructure =null;
            if (storagePolicy == StoragePolicy.FIRST_PAGE) {
                storeItems(fetchResult);
                calledForStore = true;
            }
        }

        if (storagePolicy == StoragePolicy.ALL_DATA && !calledForStore) {
            storeItems(fetchResult);
        }

        processFetchResult(fetchResult);
        updatePagingFlagsForListSize();
        contentLoaded(null);
        loadNextPageIfAutoAdvance();
    }

    private void storeItems(BaseFetchResult<T> fetchResult) {
        fetch.storeItems(fetchResult, new BoolCallback() {
            @Override
            public void onSuccess() {
                if (itemsStoredListener != null) {
                    itemsStoredListener.onSuccess();
                }
            }
            @Override
            public void onError(Throwable e) {
                if (itemsStoredListener != null) {
                    itemsStoredListener.onError(e);
                }
            }
        });
    }

    protected DataCallback createResultBlock() {
        return new DataCallback() {
            @Override
            public void onSuccess(Object result) {
                BaseFetchResult<T> res = createFetchResultFor(result);
                if (!res.validate()) {
                    contentLoaded(res.getLastError());
                    return;
                }
                if(getCurrentState()!=State.CONTENT)
                    itemsLoaded(res);
            }
            @Override
            public void onError(Throwable e) {
                failIfNeeded(e);
            }
        };
    }

    protected boolean failIfNeeded(Throwable e) {
        if (e != null) {//TODO ADD CHECK ON CONTENT?
            contentLoaded(e);
            return true;
        }
        return false;
    }

    @Override
    final public void startContentLoading() {
        super.startContentLoading();
        if (fetchMode != Fetch.Mode.Offline) {
            fetchOfflineData(false);
        }
        runRequest();
    }

    @Override
    final public void startContentRefreshing() {
        super.startContentRefreshing();
        runRequest();
    }

    public boolean refreshContentIfPossible() {
        if (getCurrentState() == State.INIT) {
            throw new IllegalStateException("You need to call startContentLoading first");
        }
        if (getCurrentState() == State.LOAD_CONTENT
                || getCurrentState() == State.REFRESH_CONTENT) {
            return false;
        }
        paging = null;
        startContentRefreshing();
        return true;
    }

    public boolean loadMoreIfPossible() {
        if (getCurrentState() == State.INIT) {
            throw new IllegalStateException("You need to call startContentLoading first");
        }
        if (getCurrentState() != State.CONTENT) {
            return false;
        }

        // We shouldn't check here for canLoadMore
        // Case user awaits for next item to appear
        // and swipe reload from bottom
        startContentRefreshing();
        return true;
    }

    protected void processFetchResult(BaseFetchResult<T> fetchResult) {
        if(dataStructure ==null) {
            dataStructure = dataStructureFromFetchResult(fetchResult);
        } else {
            dataStructure.processFetchResult(fetchResult);
        }
        dataStructure.setChangedListener(changedListener);
        if(changedListener !=null)
            changedListener.changed();
    }

    protected void loadNextPageIfNeeded() {
        if (!canLoadMore)
            return;
        if (getCurrentState() != DataSource.State.CONTENT)
            return;
        startContentRefreshing();
    }

    DataStructure<T> dataStructureFromFetchResult(BaseFetchResult<T> fetchResult) {
        if(dataStructureProvider != null) {
            if (dataSortingProvider != null) {
                throw new IllegalStateException("dataSortingProvider is ignored if you are using dataStructureProvider");
            }
            return dataStructureProvider.dataStructureForFetchResult(fetchResult);
        }

        if (dataSortingProvider != null) {
            DataStructure.Sorting sorting = dataSortingProvider.dataSortingForFetchResult(fetchResult);
            return new DataStructure<T>(fetchResult, sorting, dataCustomSortingProvider);
        }
        return new DataStructure<T>(fetchResult);
    }
    public Fetch.Mode getFetchMode() {
        return fetchMode;
    }
    public void setFetchMode(Fetch.Mode mFetchMode) {
        this.fetchMode = mFetchMode;
    }

    protected BaseFetchResult<T> createFetchResultFor(Object result) {
        return getFetchResultProvider().fetchResult(result);
    }
    protected BaseFetchResult<T> createFetchResultForLocalObject(Object result) {
        return getFetchResultProvider().fetchResultForLocal(result);
    }
    public DataStructure<T> getDataStructure(){
        return dataStructure;
    }

    @Override
    public boolean hasContent() {
        return dataStructure != null && dataStructure.hasContent();
    }

    public BaseFetchResult.Provider<T> getFetchResultProvider() {
        return fetchResultProvider;
    }

    public void setFetchResultProvider(BaseFetchResult.Provider<T> fetchResultProvider) {
        this.fetchResultProvider = fetchResultProvider;
    }

    public DataStructureProvider<T> getDataStructureProvider() {
        return dataStructureProvider;
    }

    public void setDataStructureProvider(DataStructureProvider<T> dataStructureProvider) {
        this.dataStructureProvider = dataStructureProvider;
    }

    public BoolCallback getItemsStoredListener() {
        return itemsStoredListener;
    }

    public void setItemsStoredListener(BoolCallback itemsStoredListener) {
        this.itemsStoredListener = itemsStoredListener;
    }

    public ChangedCallback getChangedListener() {
        return changedListener;
    }

    public void setChangedListener(ChangedCallback mChangedListener) {
        this.changedListener = mChangedListener;
        if (dataStructure != null) {
            dataStructure.setChangedListener(mChangedListener);
        }
    }
    public void setDataStructureSortingProvider(DataStructure.CustomSortingProvider sortingProvider) {
        this.dataCustomSortingProvider = sortingProvider;
        if(dataStructure!=null)
            dataStructure.setSortingProvider(sortingProvider);
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public StoragePolicy getStoragePolicy() {
        return storagePolicy;
    }

    public void setStoragePolicy(@NotNull StoragePolicy storagePolicy) {
        this.storagePolicy = storagePolicy;
    }

    protected void loadNextPageIfAutoAdvance() {
        if (!autoAdvance) {
            return;
        }
        if (!pagingEnabled) {
            return;
        }
        new android.os.Handler().post(new Runnable() {
            @Override
            public void run() {
                loadNextPageIfNeeded();
            }
        });
    }

    public boolean isAutoAdvance() {
        return autoAdvance;
    }

    public void setAutoAdvance(boolean autoAdvance) {
        this.autoAdvance = autoAdvance;
    }
}
