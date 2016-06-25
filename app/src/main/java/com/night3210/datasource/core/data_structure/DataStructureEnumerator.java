package com.night3210.datasource.core.data_structure;

import com.night3210.datasource.core.listeners.DataObject;

import java.util.Enumeration;

/**
 * Created by haritonbatkov on 6/20/16.
 */
public class DataStructureEnumerator<T extends DataObject> implements Enumeration {
    private final int size;

    private int cursor;

    private final DataStructure<T> dataStructure;

    public DataStructureEnumerator(DataStructure<T> dataStructure) {
        if (dataStructure == null) {
            throw new IllegalArgumentException("dataStructure is null");
        }
        size = dataStructure.dataSize();
        this.dataStructure = dataStructure;
    }

    public boolean hasMoreElements() {
        return (cursor < size);
    }

    public Object nextElement() {
        return dataStructure.getItemAtGlobalIndex(cursor++);
    }
}
