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

    private final int DEFAULT_LIMIT = 20;
    protected Paging mPaging;
    protected boolean mPagingEnabled = true;
    protected boolean mCanLoadMore;
    protected ChangedCallback mChangedListener;
    protected DataStructure<T> mDataStructure;
    protected Fetch mFetch;
    protected Fetch.Mode mFetchMode = Fetch.Mode.OnlineOffline;
    protected FetchResultProvider<T> fetchResultProvider;
    protected DataStructureProvider<T> dataStructureProvider;
    protected BoolCallback itemsStoredListener;

    public ListDataSource(Fetch fetch) {
        super();
        if(fetch == null)
            throw new IllegalArgumentException("No fetch specified");
        this.mFetch = fetch;
        this.mPagingEnabled = true;
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
                return new DataStructure<>(fetchResult);
            }
        });
    }
    public Paging getPaging() {
        if(!mPagingEnabled)
            return null;
        if(mPaging == null) {
            mPaging = new Paging();
            mPaging.limit = DEFAULT_LIMIT;
        }
        return mPaging;
    }

    protected void fetchOfflineData(final boolean refresh) {
        if (mFetchMode == Fetch.Mode.Online) {
            return; // Offline disabled
        }
        mFetch.fetchOffline(new DataCallback() {
            @Override
            public void onSuccess(Object result) {
                if (mDataStructure == null || refresh) {
                    if (mFetchMode == Fetch.Mode.Offline && shouldClearList()) {
                        mDataStructure = null;
                    }
                    BaseFetchResult<T> fetchResult = createFetchResultForLocalObject(result);
                    processFetchResult(fetchResult);
                }
                if (refresh) {
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

    protected void runRequest() {
        if (mFetchMode == Fetch.Mode.Offline) {
            fetchOfflineData(true);
            return;
        }
        LogUtils.logi("start request");
        mFetch.fetchOnline(getPaging(), createResultBlock());
    }

    protected boolean shouldClearList() {
        return getPaging().skip == 0;
    }

    protected void updatePagingFlagsForListSize() {
        if (!mPagingEnabled) {
            return;
        }
        int size = mDataStructure.dataSize();
        mCanLoadMore = mPaging.getSkip() + mPaging.getLimit() <= size;
        mPaging.skip = size;
    }

    public void loadMoreIfPossible() {
        if (!mCanLoadMore)
            return;
        if (getCurrentState() != DataSource.State.CONTENT)
            return;
        if (getPaging().skip > mDataStructure.dataSize())
            return;
        startContentRefreshing();
    }

    protected void itemsLoaded(BaseFetchResult<T> fetchResult) {
        if (shouldClearList()) {
            mDataStructure=null;
            // Not very good, but for now - ok
            mFetch.storeItems(fetchResult, new BoolCallback() {
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
        processFetchResult(fetchResult);
        updatePagingFlagsForListSize();
        contentLoaded(null);
    }
    protected DataCallback createResultBlock() {
        return new DataCallback() {
            @Override
            public void onSuccess(Object result) {
                LogUtils.logi("in");
                BaseFetchResult<T> res = createFetchResultFor(result);
                if (!res.validate()) {
                    parseRequestDidFail(res.getLastError());
                    return;
                }
                itemsLoaded(res);
            }
            @Override
            public void onError(Throwable e) {
                failIfNeeded(e);
            }
        };
    }
    protected boolean failIfNeeded(Throwable e) {
        if (e != null) {
            parseRequestDidFail(e);
            return true;
        }
        return false;
    }

    public void addItem(T item){
        mDataStructure.insertItem(item,0);
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
        mPaging.skip = getDataStructure().dataSize();
        super.startContentRefreshing();
        runRequest();
    }
    void processFetchResult(BaseFetchResult<T> fetchResult) {
        if(mDataStructure==null) {
            mDataStructure = dataStructureFromFetchResult(fetchResult);
            if(mChangedListener!=null)
                mChangedListener.changed();
            mDataStructure.setChangedListener(mChangedListener);
        } else {
            mDataStructure.processFetchResult(fetchResult);
        }
    }

    DataStructure<T> dataStructureFromFetchResult(BaseFetchResult<T> fetchResult) {
        return getDataStructureProvider() == null ? null : getDataStructureProvider().dataStructureForFetchResult(fetchResult);
    }

    public Fetch.Mode getFetchMode() {
        return mFetchMode;
    }

    public void setFetchMode(Fetch.Mode mFetchMode) {
        this.mFetchMode = mFetchMode;
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
        return mDataStructure;
    }
    @Override
    public boolean hasContent() {
        return mDataStructure != null && mDataStructure.dataSize() > 0;
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
        return mChangedListener;
    }

    public void setChangedListener(ChangedCallback mChangedListener) {
        this.mChangedListener = mChangedListener;
        if (mDataStructure != null) {
            mDataStructure.setChangedListener(mChangedListener);
        }
    }


}
