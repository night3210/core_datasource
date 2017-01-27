package com.night3210.datasource.core;

import com.night3210.datasource.core.data_structure.DataStructure;
import com.night3210.datasource.core.fetch_result.BaseFetchResult;
import com.night3210.datasource.core.fetch_result.SimpleFetchResult;
import com.night3210.datasource.core.listeners.BoolCallback;
import com.night3210.datasource.core.listeners.ChangedCallback;
import com.night3210.datasource.core.listeners.DataCallback;
import com.night3210.datasource.core.listeners.DataObject;
import com.night3210.datasource.core.listeners.Fetch;

public class ListDataSource<T extends DataObject> extends DataSource {
    private final int DEFAULT_LIMIT = 20;
    protected Paging paging;
    protected boolean pagingEnabled = true;
    protected boolean canLoadMore;
    protected ChangedCallback changedListener;
    protected DataStructure<T> dataStructure;
    protected Fetch fetch;
    protected Fetch.Mode fetchMode = Fetch.Mode.OnlineOffline;
    protected FetchResultProvider<T> fetchResultProvider;
    protected DataStructureProvider<T> dataStructureProvider;
    protected BoolCallback itemsStoredListener;
    protected DataStructure.Sorting dataStructureSorting;
    protected DataStructure.CustomSortingProvider dataStructureSortingProvider;

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
    public interface FetchResultProvider<H extends DataObject> {
        BaseFetchResult<H> fetchResultForLocal(Object object);
        BaseFetchResult<H> fetchResult(Object object);
    }
    public interface DataStructureProvider<H extends DataObject> {
        DataStructure<H> dataStructureForFetchResult(BaseFetchResult<H> fetchResult);
    }

    public ListDataSource(Fetch fetch) {
        super();
        if(fetch == null)
            throw new IllegalArgumentException("No fetch specified");
        this.fetch = fetch;
        this.pagingEnabled = true;
        setFetchResultProvider(new FetchResultProvider<T>() {
            @Override
            public BaseFetchResult<T> fetchResultForLocal(Object object) {
                return new SimpleFetchResult<>(object,true);
            }
            @Override
            public BaseFetchResult<T> fetchResult(Object object) {
                return new SimpleFetchResult<>(object,false);
            }
        });
        setDataStructureProvider(new DataStructureProvider<T>() {
            @Override
            public DataStructure<T> dataStructureForFetchResult(BaseFetchResult<T> fetchResult) {
                return new DataStructure<>(fetchResult, dataStructureSorting, dataStructureSortingProvider);
            }
        });
    }
    public Paging getPaging() {
        if(!pagingEnabled)
            return null;
        if(paging == null) {
            paging = new Paging();
            paging.limit = DEFAULT_LIMIT;
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
                if(refresh)
                    contentLoaded(null);
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
    protected void runRequest() {
        if (fetchMode == Fetch.Mode.Offline) {
            fetchOfflineData(true);
            return;
        }
        fetch.fetchOnline(getPaging(), createResultBlock());
    }
    protected boolean shouldClearList() {
        return getPaging().skip == 0;
    }
    protected void updatePagingFlagsForListSize() {
        if (!pagingEnabled)
            return;
        int size = dataStructure.dataSize();
        canLoadMore = paging.getSkip() + paging.getLimit() <= size;
        paging.skip = size;
    }
    public void loadMoreIfPossible() {
        if (!canLoadMore)
            return;
        if (getCurrentState() != DataSource.State.CONTENT)
            return;
        if (getPaging().skip > dataStructure.dataSize())
            return;
        startContentRefreshing();
    }
    protected void itemsLoaded(BaseFetchResult<T> fetchResult) {
        if (shouldClearList()) {
            dataStructure =null;
            storeItems(fetchResult);
        }
        processFetchResult(fetchResult);
        updatePagingFlagsForListSize();
        contentLoaded(null);
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
                    parseRequestDidFail(res.getLastError());
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
            parseRequestDidFail(e);
            return true;
        }
        return false;
    }
    public void addItem(T item){
        dataStructure.insertItem(item,0);
    }
    protected void parseRequestDidFail(Throwable th) {
        contentLoaded(th);
    }
    @Override
    final public void startContentLoading() {
        super.startContentLoading();
        fetchOfflineData(false);
        runRequest();
    }
    public void refreshContentIfPossible() {
        if (getCurrentState() == State.INIT
                || getCurrentState() == State.LOAD_CONTENT
                || getCurrentState() == State.REFRESH_CONTENT) {
            return;
        }
        if (getPaging() != null) {
            getPaging().skip = 0;
        }
        super.startContentRefreshing();
        runRequest();
    }
    @Override
    final public void startContentRefreshing() {
        if (getCurrentState() == State.ERROR || getCurrentState() == State.NO_CONTENT)
            changeStateTo(State.INIT);
        if (getCurrentState() == State.LOAD_CONTENT || getCurrentState() == State.REFRESH_CONTENT)
            return;
        if (getCurrentState() != State.CONTENT) {
            super.startContentLoading();
            runRequest();
            return;
        }
        // Refresh mean update, not load next
        paging.skip = 0;
        paging.limit = dataStructure.dataSize();
        super.startContentRefreshing();
        runRequest();
    }
    final public void startContentReloading() {
        if (getCurrentState() == State.ERROR || getCurrentState() == State.NO_CONTENT)
            changeStateTo(State.INIT);
        if (getCurrentState() == State.LOAD_CONTENT || getCurrentState() == State.REFRESH_CONTENT)
            return;
        if (getCurrentState() != State.CONTENT) {
            super.startContentLoading();
            runRequest();
            return;
        }
        dataStructure = null;
        paging.skip = 0;
        super.startContentRefreshing();
        runRequest();
    }
    void processFetchResult(BaseFetchResult<T> fetchResult) {
        if(dataStructure ==null) {
            dataStructure = dataStructureFromFetchResult(fetchResult);
            dataStructure.processFetchResult(fetchResult);
        } else {
            dataStructure.processFetchResult(fetchResult);
        }
        dataStructure.setChangedListener(changedListener);
        if(changedListener !=null)
            changedListener.changed();
    }
    DataStructure<T> dataStructureFromFetchResult(BaseFetchResult<T> fetchResult) {
        if(dataStructureProvider==null)
            return null;
        return getDataStructureProvider().dataStructureForFetchResult(fetchResult);
    }
    public Fetch.Mode getFetchMode() {
        return fetchMode;
    }
    public void setFetchMode(Fetch.Mode mFetchMode) {
        this.fetchMode = mFetchMode;
    }
    @Override
    final public void contentLoaded(Throwable th) {
        super.contentLoaded(th);
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
    public void setDataStructure(DataStructure value) {
        dataStructure = value;
        if(changedListener !=null)
            changedListener.changed();
        if(dataStructure !=null) {
            dataStructure.notifyListeners();
        }
    }
    @Override
    public boolean hasContent() {
        return dataStructure != null && dataStructure.dataSize() > 0;
    }

    public FetchResultProvider<T> getFetchResultProvider() {
        return fetchResultProvider;
    }

    public void setFetchResultProvider(FetchResultProvider<T> fetchResultProvider) {
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
        this.dataStructureSortingProvider = sortingProvider;
        if(dataStructure!=null)
            dataStructure.setSortingProvider(sortingProvider);
    }
    public void setDataStructureSorting(DataStructure.Sorting sorting) {
        this.dataStructureSorting = sorting;
        if(dataStructure!=null)
            dataStructure.setSorting(sorting);
    }
}
