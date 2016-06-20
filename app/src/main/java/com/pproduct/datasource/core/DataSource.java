package com.pproduct.datasource.core;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import com.pproduct.datasource.core.listeners.DataSourceStateListener;

import java.util.Observable;

public abstract class DataSource extends Observable{
    public enum State {INIT, LOAD_CONTENT, ERROR, CONTENT, NO_CONTENT, REFRESH_CONTENT}

    private State currentState = State.INIT;
    private Throwable currentError = null;

    private DataSourceStateListener stateListener;

    public DataSource() {
    }

    public State getCurrentState() {
        return currentState;
    }

    public Throwable getCurrentError() {
        return currentError;
    }

    public DataSourceStateListener getStateListener() {
        return stateListener;
    }

    public void setStateListener(DataSourceStateListener stateListener) {
        this.stateListener = stateListener;
    }

    public boolean canRefresh() {
        return currentState == State.ERROR
                || currentState == State.CONTENT
                || currentState == State.NO_CONTENT;
    }

    public void contentLoaded(Throwable th) {
        if (th != null) {
            this.currentError = th;
            fail();
            return;
        }
        if (hasContent()) {
            success();
            return;
        }
        noContent();
    }
    public void startContentLoading() {
        throwNotInitState("load");
        this.currentError = null;
        changeStateTo(State.LOAD_CONTENT);
    }

    public void startContentRefreshing() {
        throwNotDisplayingState("refresh");
        this.currentError = null;
        changeStateTo(State.REFRESH_CONTENT);
    }

    public abstract boolean hasContent();

    private void fail() {
        throwNotLoadingState("fail");
        changeStateTo(State.ERROR);
    }

    private void success() {
        throwNotLoadingState("success");
        changeStateTo(State.CONTENT);
    }

    private void noContent() {
        throwNotLoadingState("noContent");
        changeStateTo(State.NO_CONTENT);
    }

    private void throwNotLoadingState(String action) {
        if (currentState != State.NO_CONTENT && currentState != State.LOAD_CONTENT && currentState != State.REFRESH_CONTENT)
            throw new IllegalStateException("Cannot " + action + " from state " + currentState);
    }

    private void throwNotDisplayingState(String action) {
        if (currentState != State.ERROR
                && currentState != State.CONTENT && currentState != State.NO_CONTENT)
            throw new IllegalStateException("Cannot " + action + " from state " + currentState);
    }

    private void throwNotInitState(String action) {
        if (currentState != State.INIT)
            throw new IllegalStateException("Cannot " + action + " from state " + currentState);
    }

    protected void changeStateTo(State newState) {
        currentState = newState;
        setChanged();
        notifyObservers();
        if (stateListener != null)
            stateListener.dataSourceChangedState(this, newState);
    }
}
