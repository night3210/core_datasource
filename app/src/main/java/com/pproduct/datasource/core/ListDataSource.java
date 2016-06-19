package com.pproduct.datasource.core;

import com.pproduct.datasource.core.fetch_result.BaseFetchResult;
import com.pproduct.datasource.core.fetch_result.SimpleFetchResult;
import com.pproduct.datasource.core.listeners.ChangedCallback;

import com.pproduct.datasource.core.listeners.DataCallback;
import com.pproduct.datasource.core.listeners.DataObject;
import com.pproduct.datasource.core.listeners.Fetch;

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
    protected interface FetchResultInterface {
        BaseFetchResult localFetch(Object object, boolean isLocal);
        BaseFetchResult onlineFetch(Object object, boolean isLocal);
    }
    private final int DEFAULT_LIMIT = 20;
    protected Paging mPaging;
    protected boolean mPagingEnabled = true;
    protected boolean mCanLoadMore;
    protected ChangedCallback mChangedListener;
    protected DataStructure mDataStructure;
    protected Fetch mFetch;
    protected Fetch.Mode mFetchMode = Fetch.Mode.OnlineOffline;
    protected FetchResultInterface fetchResultBlock;

    public ListDataSource(Fetch fetch) {
        super();
        if(fetch == null)
            throw new IllegalArgumentException("No fetch specified");
        this.mFetch = fetch;
        this.mPagingEnabled = true;
        fetchResultBlock = new FetchResultInterface() {
            @Override
            public BaseFetchResult localFetch(Object object,boolean isLocal) {
                return new SimpleFetchResult(object,true);
            }
            @Override
            public BaseFetchResult onlineFetch(Object object,boolean isLocal) {
                return new SimpleFetchResult(object,false);
            }
        };
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
    protected void runRequest() {
        LogUtils.logi("start request");
        mFetch.fetchOnline(getPaging(), createResultBlock());
    }
    protected boolean shouldClearList() {
        return getPaging().skip == 0;
    }
    public void loadNextPageIfNeeded() {
        if (!mCanLoadMore)
            return;
        if (getCurrentState() != DataSource.State.CONTENT)
            return;
        if (getPaging().skip > mDataStructure.dataSize())
            return;
        startContentRefreshing();
    }
    protected void itemsLoaded(BaseFetchResult fetchResult) {
        if (shouldClearList()) {
            mDataStructure=null;
        }
        processFetchResult(fetchResult);
        contentLoaded(null);
    }
    protected DataCallback createResultBlock() {
        return new DataCallback() {
            @Override
            public void onSuccess(Object result) {
                LogUtils.logi("in");
                BaseFetchResult res = createFetchResultFor(result);
                if (!res.validate()) {
                    parseRequestDidFail(res.getLastError());
                    return;
                }
                itemsLoaded(res);
            }
            @Override
            public void onError(Exception e) {
                if (failIfNeeded(e)) return;
            }
        };
    }
    protected boolean failIfNeeded(Exception e) {
        if (e != null) {
            parseRequestDidFail(e);
            return true;
        }
        return false;
    }
    public void addItem(T item){
        mDataStructure.insertItem(item,0);
    }
    protected void parseRequestDidFinish() {
        contentLoaded(null);
        notifyDataSetChanged();
    }
    protected void parseRequestDidFail(Throwable th) {
        contentLoaded(th);
    }

    private void fetchOfflineData(final boolean refresh) {
        if(mFetchMode == Fetch.Mode.Online)
            return;
        mFetch.fetchOffline(new DataCallback() {
            @Override
            public void onSuccess(Object result) {
                if(refresh || mDataStructure==null) {
                    BaseFetchResult res = createFetchResultForLocalObject(result);
                    processFetchResult(res);
                }
            }
            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    final public void startContentLoading() {
        super.startContentLoading();
        fetchOfflineData(false);
        runRequest();
    }
    public void startContentReloading() {
        getPaging().skip = 0;
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
    void processFetchResult(BaseFetchResult fetchResult) {
        if(mDataStructure==null) {
            mDataStructure = dataStructureFromFetchResult(fetchResult);
        } else {
            mDataStructure.processFetchResult(fetchResult);
            //mDataStructure.clear();
            //mDataStructure.addAll(fetchResult.getSections());
        }
        if(mChangedListener!=null)
            mChangedListener.changed();
    }
    DataStructure dataStructureFromFetchResult(BaseFetchResult fetchResult) {
        return new DataStructure(fetchResult);
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

    protected BaseFetchResult createFetchResultFor(Object result) {
        return fetchResultBlock==null ? null : fetchResultBlock.localFetch(result, false);
    }
    protected BaseFetchResult createFetchResultForLocalObject(Object result) {
        return fetchResultBlock==null ? null : fetchResultBlock.localFetch(result, true);
    }
    public DataStructure getDataStructure(){
        return mDataStructure;
    }
    @Override
    public boolean hasContent() {
        return mDataStructure.dataSize() > 0;
    }
}
