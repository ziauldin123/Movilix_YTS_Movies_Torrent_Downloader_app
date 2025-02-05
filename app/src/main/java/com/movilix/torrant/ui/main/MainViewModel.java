

package com.movilix.torrant.ui.main;

import android.app.Application;
import android.text.TextUtils;

import com.movilix.torrant.core.RepositoryHelper;
import com.movilix.torrant.core.filter.TorrentFilter;
import com.movilix.torrant.core.filter.TorrentFilterCollection;
import com.movilix.torrant.core.model.TorrentEngine;
import com.movilix.torrant.core.model.TorrentInfoProvider;
import com.movilix.torrant.core.model.data.TorrentInfo;
import com.movilix.torrant.core.model.data.entity.TagInfo;
import com.movilix.torrant.core.sorting.TorrentSorting;
import com.movilix.torrant.core.sorting.TorrentSortingComparator;
import com.movilix.torrant.core.storage.TagRepository;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

public class MainViewModel extends AndroidViewModel {
    private TorrentInfoProvider stateProvider;
    private TorrentEngine engine;
    private TorrentSortingComparator sorting = new TorrentSortingComparator(
            new TorrentSorting(TorrentSorting.SortingColumns.none, TorrentSorting.Direction.ASC));
    private TorrentFilter statusFilter = TorrentFilterCollection.all();
    private TorrentFilter dateAddedFilter = TorrentFilterCollection.all();
    private TorrentFilter tagFilter = TorrentFilterCollection.all();
    private PublishSubject<Boolean> forceSortAndFilter = PublishSubject.create();
    private TagRepository tagRepo;

    private String searchQuery;
    private TorrentFilter searchFilter = (state) -> {
        if (TextUtils.isEmpty(searchQuery))
            return true;

        String filterPattern = searchQuery.toLowerCase().trim();

        return state.name.toLowerCase().contains(filterPattern);
    };

    public MainViewModel(@NonNull Application application) {
        super(application);

        stateProvider = TorrentInfoProvider.getInstance(application);
        engine = TorrentEngine.getInstance(application);
        tagRepo = RepositoryHelper.getTagRepository(application);
    }

    public Flowable<List<TorrentInfo>> observeAllTorrentsInfo() {
        return stateProvider.observeInfoList();
    }

    public Single<List<TorrentInfo>> getAllTorrentsInfoSingle() {
        return stateProvider.getInfoListSingle();
    }

    public Flowable<String> observeTorrentsDeleted() {
        return stateProvider.observeTorrentsDeleted();
    }

    public void setSort(@NonNull TorrentSortingComparator sorting, boolean force) {
        this.sorting = sorting;
        if (force && !sorting.getSorting().getColumnName().equals(TorrentSorting.SortingColumns.none.name()))
            forceSortAndFilter.onNext(true);
    }

    public TorrentSortingComparator getSorting() {
        return sorting;
    }

    public void setStatusFilter(@NonNull TorrentFilter statusFilter, boolean force) {
        this.statusFilter = statusFilter;
        if (force)
            forceSortAndFilter.onNext(true);
    }

    public void setDateAddedFilter(@NonNull TorrentFilter dateAddedFilter, boolean force) {
        this.dateAddedFilter = dateAddedFilter;
        if (force)
            forceSortAndFilter.onNext(true);
    }

    public void setTagFilter(@NonNull TorrentFilter tagFilter, boolean force) {
        this.tagFilter = tagFilter;
        if (force) {
            forceSortAndFilter.onNext(true);
        }
    }

    public TorrentFilter getFilter() {
        return (state) -> statusFilter.test(state) &&
                dateAddedFilter.test(state) &&
                searchFilter.test(state) &&
                tagFilter.test(state);
    }

    public void setSearchQuery(@Nullable String searchQuery) {
        this.searchQuery = searchQuery;
        forceSortAndFilter.onNext(true);
    }

    public void resetSearch() {
        setSearchQuery(null);
    }

    public Observable<Boolean> observeForceSortAndFilter() {
        return forceSortAndFilter;
    }

    public void pauseResumeTorrent(@NonNull String id) {
        engine.pauseResumeTorrent(id);
    }

    public void deleteTorrents(@NonNull List<String> ids, boolean withFiles) {
        engine.deleteTorrents(ids, withFiles);
    }

    public void forceRecheckTorrents(@NonNull List<String> ids) {
        engine.forceRecheckTorrents(ids);
    }

    public void forceAnnounceTorrents(@NonNull List<String> ids) {
        engine.forceAnnounceTorrents(ids);
    }

    public Flowable<Boolean> observeNeedStartEngine() {
        return engine.observeNeedStartEngine();
    }

    public void startEngine() {
        engine.start();
    }

    public void requestStopEngine() {
        engine.requestStop();
    }

    public void stopEngine() {
        engine.forceStop();
    }

    public void pauseAll() {
        engine.pauseAll();
    }

    public void resumeAll() {
        engine.resumeAll();
    }

    public Flowable<List<TagInfo>> observeTags() {
        return tagRepo.observeAll();
    }

    public Completable deleteTag(@NonNull TagInfo info) {
        return Completable.fromRunnable(() -> tagRepo.delete(info));
    }
}
