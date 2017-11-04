package com.night3210.datasource.core;

import com.badoo.mobile.util.WeakHandler;
import com.night3210.datasource.core.fetch_result.BaseFetchResult;
import com.night3210.datasource.core.fetch_result.SimpleFetchResult;
import com.night3210.datasource.core.listeners.BoolCallback;
import com.night3210.datasource.core.listeners.ChangedCallback;
import com.night3210.datasource.core.listeners.DataCallback;
import com.night3210.datasource.core.listeners.DataObject;
import com.night3210.datasource.core.listeners.Fetch;

/**
 * Created by haritonbatkov on 11/4/17.
 */

public class FetchDataSource<T extends DataObject> extends DataSource {
    private final int DEFAULT_RELOAD_TIME = 15000;
    private final int DEFAULT_ERROR_RELOAD_TIME = 5000;

    private WeakHandler mHandler;

    protected int defaultFetchDelay;  // 15 second. How long till we reload data
    protected int defaultErrorFetchDelay; // 5 second. How long till we reload data if error occurred
    protected boolean storeFetchedObject; // Defauil is no
    protected Object fetchedObject;

    protected ChangedCallback fetchedObjectChangedListener;
    protected Fetch fetch;
    protected Fetch.Mode fetchMode = Fetch.Mode.OnlineOffline;
    protected BaseFetchResult.Provider<T> fetchResultProvider;


    public FetchDataSource(Fetch fetch) {
        super();
        if(fetch == null)
            throw new IllegalArgumentException("No fetch specified");
        this.fetch = fetch;
        this.defaultFetchDelay = DEFAULT_RELOAD_TIME;
        this.defaultErrorFetchDelay = DEFAULT_ERROR_RELOAD_TIME;
        this.storeFetchedObject = false;
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

    protected void fetchOfflineData(final boolean refresh) {
        if (fetchMode == Fetch.Mode.Online) {
            return; // Offline disabled
        }
        fetch.fetchOffline(new DataCallback() {
            @Override
            public void onSuccess(Object result) {
                if (fetchedObject == null || refresh) {
                    if (fetchMode == Fetch.Mode.Offline) {
                        fetchedObject = null;
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
        fetch.fetchOnline(null, createResultBlock());
    }

    protected void itemsLoaded(BaseFetchResult<T> fetchResult) {
        fetchedObject = null;
        if (storeFetchedObject) {
            storeItems(fetchResult);
        }
        processFetchResult(fetchResult);
        contentLoaded(null);
    }

    private void storeItems(BaseFetchResult<T> fetchResult) {
        fetch.storeItems(fetchResult, new BoolCallback() {
            @Override
            public void onSuccess() {

            }
            @Override
            public void onError(Throwable e) {

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


    public void resetData() {
        fetchedObject = null;
    }

    @Override
    public boolean hasContent() {
        return fetchedObject != null;
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
        startContentRefreshing();
        return true;
    }

    void processFetchResult(BaseFetchResult<T> fetchResult) {
        Object object = fetchResult.getItems();
        if (fetchResult.getItems().size() == 0) {
            object = null;
        } else if (fetchResult.getItems().size() == 1) {
            object = fetchResult.getItems().get(0);
        }
        fetchedObject = object;
        if (fetchedObjectChangedListener != null) {
            fetchedObjectChangedListener.changed();
        }
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

    public BaseFetchResult.Provider<T> getFetchResultProvider() {
        return fetchResultProvider;
    }

    public void setFetchResultProvider(BaseFetchResult.Provider<T> fetchResultProvider) {
        this.fetchResultProvider = fetchResultProvider;
    }

    public ChangedCallback getFetchedObjectChangedListener() {
        return fetchedObjectChangedListener;
    }

    public void setFetchedObjectChangedListener(ChangedCallback fetchedObjectChangedListener) {
        this.fetchedObjectChangedListener = fetchedObjectChangedListener;
    }

    public Object getFetchedObject() {
        return fetchedObject;
    }

    protected void reloadDataWithDelay() {
        int delay = defaultFetchDelay;
        switch (getCurrentState()) {
            case INIT:
            case LOAD_CONTENT:
            case REFRESH_CONTENT:
                return;// Do not reload on this states
            case ERROR:
                delay = defaultErrorFetchDelay;
                break;
            default:
                break;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshContentIfPossible();
            }
        }, delay);
    }

    @Override
    protected void changeStateTo(State newState) {
        super.changeStateTo(newState);
        reloadDataWithDelay();
    }
}