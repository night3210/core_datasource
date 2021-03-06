package com.night3210.datasource.core.data_structure;

import com.night3210.datasource.core.LogUtils;
import com.night3210.datasource.core.fetch_result.BaseFetchResult;
import com.night3210.datasource.core.listeners.ChangedCallback;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;

import com.night3210.datasource.core.listeners.DataObject;

/**
 * Created by Developer on 1/28/2016.
 */
public class DataStructure<T extends DataObject> {
    // index path, can be used in tabs
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
    // Custom sorting for sort objecs
    public interface CustomSortingProvider<H extends DataObject> {
        List<H> sortedArrayFrom(List<H> sourceArray);
    }
    public enum Sorting {
        UpdatedAt,
        UpdatedAtReverse,
        CreatedAt,
        CreatedAtReverse,
        Custom,
        NoSorting
    }
    protected Sorting sorting = Sorting.CreatedAt;      // by default, sort by CreatedAt

    protected CustomSortingProvider sortingProvider;
    protected CopyOnWriteArrayList<T> sourceArray;
    protected List<List<T>> sections;
    protected List<Map> metadata;
    protected ChangedCallback changedListener;
    public DataStructure(BaseFetchResult<T> fetchResult) {
        this(fetchResult, Sorting.CreatedAt);
    }
    public DataStructure(BaseFetchResult<T> fetchResult, Sorting sorting) {
        this(fetchResult, sorting, null);
    }
    public DataStructure(BaseFetchResult<T> fetchResult, Sorting sorting, CustomSortingProvider sortingProvider) {
        if(sorting!=null)
            this.sorting = sorting;
        this.sortingProvider = sortingProvider;
        processFetchResult(fetchResult);
    }
    public void setSorting(Sorting sorting) {
        this.sorting = sorting;
    }
    public void processFetchResult(BaseFetchResult<T> fetchResult) {
        List<List<T>> frSections = fetchResult.getSections();
        for(int i=0;i<frSections.size();i++) {
            putSection(processItems(frSections.get(i), i), i);
        }
        notifyListeners();
    }
    protected void putSection(List<T> array, int section) {
        if(sections==null)
            sections=new CopyOnWriteArrayList<>();
        while(sections.size() <= section)
            sections.add(new CopyOnWriteArrayList<T>());
        sections.set(section, array);
    }
    public int getSectionsCount() {
        return sections.size();
    }
    public int itemsCountForSection(int section){
        return sections.get(section).size();
    }
    public void setChangedListener(ChangedCallback listener) {
        this.changedListener = listener;
    }
    public Map getMetadataForSection(int section) {
        return metadata == null ? null : metadata.get(section);
    }
    public boolean removeItem(T item, int section) {
        List items=sections.get(section);
        int oldCount = items.size();
        items.remove(item);
        notifyListeners();
        return items.size() != oldCount;
    }

    public void insertItem(T item, int section) {
        CopyOnWriteArrayList<T> array=new CopyOnWriteArrayList<>();
        array.add(item);
        sections.set(section, processItems(array, section));
        notifyListeners();
    }

    public boolean hasContent() {
        if(sections==null)
            return false;
        for(List list:sections)
            if(list!=null && list.size()>0)
                return true;
        return false;
    }

    public List<T> processItems(List<T> items, int section) {
        if(items==null)
            return null;
        CopyOnWriteArrayList<T> sectionArray = null;
        if (sections != null && sections.size() > section) {
            sectionArray = (CopyOnWriteArrayList<T>) sections.get(section);
        } else {
            sectionArray = new CopyOnWriteArrayList<>();
        }
        sectionArray.addAllAbsent(items);
        List<T> modifiableList = new ArrayList<>(sectionArray);
        switch (sorting) {
            case UpdatedAt:
            case UpdatedAtReverse:
                Collections.sort(modifiableList, new Comparator<DataObject>() {
                    @Override
                    public int compare(DataObject lhs, DataObject rhs) {
                        Date rd=rhs.getUpdatedAt();
                        Date ld=lhs.getUpdatedAt();
                        return rd.compareTo(ld) * (sorting == Sorting.UpdatedAtReverse ? -1 : 1);
                    }
                });
                break;
            case CreatedAt:
            case CreatedAtReverse:
                Collections.sort(modifiableList, new Comparator<DataObject>() {
                    @Override
                    public int compare(DataObject lhs, DataObject rhs) {
                        Date rd=rhs.getCreatedAt();
                        Date ld=lhs.getCreatedAt();
                        return rd.compareTo(ld) * (sorting == Sorting.CreatedAtReverse ? -1 : 1);
                    }
                });
                break;
            case Custom:
                    if (sortingProvider == null) {
                        throw new IllegalStateException("You need to provide sortingProvider if you choose Custom soring");
                    }
                modifiableList = (List<T>) sortingProvider.sortedArrayFrom(modifiableList);
                break;
            case NoSorting:
                break;
        }
        return new CopyOnWriteArrayList<>(modifiableList);
    }
    public void notifyListeners() {
        LogUtils.logi("zff notifylisteners");
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
    public T getItemForIndexPath(IndexPath path) {
        return sections.get(path.section).get(path.row);
    }
    public Object getObjectForIndexPath(IndexPath path) {
        return sections.get(path.section).get(path.row);
    }
    public IndexPath getIndexPathForObject(T object) {
        for (int section = 0; section < getSectionsCount(); section++) {
            List<T> sectionList = sections.get(section);
            for (int row = 0; row < sectionList.size(); row++) {
                T obj = sectionList.get(row);
                if (obj.equals(object)) {
                    return new IndexPath(row, section);
                }
            }
        }
        return null;
    }
    /* package */ T getItemAtGlobalIndex(int globalIndex) {
        int counter = 0;
        for (List<T> section : sections) {
            int sectionIndex = globalIndex - counter;
            if (section.size() > sectionIndex) {
                return section.get(sectionIndex);
            }
            counter += section.size();
        }
        throw new IndexOutOfBoundsException();
    }
    public void clear() {
        sections.clear();
    }
    public <T> void addAll(List<T> newItems) {
    }
    public void setSortingProvider(CustomSortingProvider sortingProvider) {
        this.sortingProvider = sortingProvider;
    }
}
