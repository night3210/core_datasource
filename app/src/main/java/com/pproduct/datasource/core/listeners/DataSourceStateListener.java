package com.pproduct.datasource.core.listeners;

import com.pproduct.datasource.core.DataSource;

/**
 * Created by Developer on 2/11/2016.
 */

public interface DataSourceStateListener {
    void dataSourceChangedState(DataSource dataSource, DataSource.State newState);
}
