package com.pproduct.datasource.core;



import com.pproduct.datasource.core.fetch_result.BaseFetchResult;
import com.pproduct.datasource.core.listeners.ChangedCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.pproduct.datasource.core.listeners.DataObject;

/**
 * Created by Developer on 1/28/2016.
 */
public class DataStructure {
    public static class IndexPath {
        int row, section;
        public IndexPath(int row, int section) {
            this.row = row;
            this.section = section;
        }
        public int getRow() {
            return row;
        }
        public int getSection() {
            return section;
        }
    }
    public void clear() {
        sections.clear();
    }
    public <T> void addAll(List<T> newItems) {

    }

    protected enum DataStructureSorting {
        DataStructureSortingUpdatedAt,
        DataStructureSortingCreatedAt,
        DataStructureSortingCreatedAtReverse,
        DataStructureSortingSortingCustom
    }
    protected DataStructureSorting sorting = DataStructureSorting.DataStructureSortingCreatedAt;
    protected CopyOnWriteArrayList<DataObject> sourceArray;
    protected List<List<DataObject>> sections;
    protected HashMap metadata;
    protected ChangedCallback changedListener;
    public DataStructure(BaseFetchResult fetchResult) {
        sections = new CopyOnWriteArrayList<>(fetchResult.getSections());
    }
    public int sectionsCount() {
        return sections.size();
    }
    public int itemsCountForSection(int section){
        return sections.get(section).size();
    }
    public void setChangedListener(ChangedCallback listener) {
        this.changedListener = listener;
    }
    public boolean removeItem(DataObject item, int section) {
        List items=sections.get(section);
        int oldCount = items.size();
        items.remove(item);
        notifyListeners();
        return items.size() != oldCount;
    }
    public void insertItem(DataObject item, int section) {
        CopyOnWriteArrayList<DataObject> array=new CopyOnWriteArrayList<>();
        array.add(item);
        sections.set(section, processItems(array, section));
        notifyListeners();
    }
    boolean hasContent() {
        if(sections==null)
            return false;
        for(List list:sections)
            if(list!=null && list.size()>0)
                return true;
        return false;
    }
    public List<DataObject> processItems(List<DataObject> items, int section) {
        if(items==null)
            return null;
        CopyOnWriteArrayList<DataObject> sectionArray = (CopyOnWriteArrayList<DataObject>) sections.get(section);
        sectionArray.addAllAbsent(items);
        if(false && sorting!=null) {
            Collections.sort(sectionArray, new Comparator<DataObject>() {
                @Override
                public int compare(DataObject lhs, DataObject rhs) {
                    int dir = 1;
                    Date rd=null, ld=null;
                    switch (sorting) {
                        case DataStructureSortingUpdatedAt:
                            ld = lhs.getUpdatedAt();
                            rd = rhs.getUpdatedAt();
                            break;
                        case DataStructureSortingCreatedAtReverse:
                            dir = -1;
                        case DataStructureSortingCreatedAt:
                            ld = lhs.getCreatedAt();
                            rd = rhs.getCreatedAt();
                            break;
//                        default:
//                            throw new IllegalStateException("sorting not set");
                    }
                    return rd.compareTo(ld) * dir;
                }
            });
        }
        return sectionArray;
    }
    protected void notifyListeners() {
        if(changedListener!=null)
            changedListener.changed();

    }
    public int dataSize() {
        int dataSize = 0;
        for(List list:sections)
            if(list!=null)
                dataSize+=list.size();
        return dataSize;
    }
    public DataObject getItemForIndexPath(IndexPath path) {
        return sections.get(path.section).get(path.row);
    }
    public void processFetchResult(BaseFetchResult fetchResult) {
        List<List<DataObject>> frSections = fetchResult.getSections();
        for(int i=0;i<frSections.size();i++) {
            putSection(processItems(frSections.get(i), i), i);
        }
        if(changedListener!=null)
            changedListener.changed();
    }
    private void putSection(List<DataObject> array, int section) {
        if(sections==null)
            sections=new CopyOnWriteArrayList<>();
        while(sections.size()<section)
            sections.add(new CopyOnWriteArrayList<DataObject>());
        sections.set(section, array);
    }
}
