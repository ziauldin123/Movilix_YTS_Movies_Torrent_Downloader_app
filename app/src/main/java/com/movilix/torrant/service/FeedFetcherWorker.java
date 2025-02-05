

package com.movilix.torrant.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.movilix.torrant.core.FeedParser;
import com.movilix.torrant.core.RepositoryHelper;
import com.movilix.torrant.core.model.data.entity.FeedChannel;
import com.movilix.torrant.core.model.data.entity.FeedItem;
import com.movilix.torrant.core.settings.SettingsRepository;
import com.movilix.torrant.core.storage.FeedRepository;
import com.movilix.torrant.core.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/*
 * The worker for fetching items from RSS/Atom channels.
 */

public class FeedFetcherWorker extends Worker
{
    private static final String TAG = FeedFetcherWorker.class.getSimpleName();

    public static final String ACTION_FETCH_CHANNEL = "com.movilix.torrant.service.FeedFetcherWorker.ACTION_FETCH_CHANNEL";
    public static final String ACTION_FETCH_CHANNEL_LIST = "com.movilix.torrant.service.FeedFetcherWorker.ACTION_FETCH_CHANNEL_LIST";
    public static final String ACTION_FETCH_ALL_CHANNELS = "com.movilix.torrant.service.FeedFetcherWorker.ACTION_FETCH_ALL_CHANNELS";
    public static final String TAG_ACTION = "action";
    public static final String TAG_NO_AUTO_DOWNLOAD = "no_download";
    public static final String TAG_CHANNEL_ID = "channel_url_id";
    public static final String TAG_CHANNEL_ID_LIST = "channel_id_list";

    private Context context;
    private FeedRepository repo;
    private SettingsRepository pref;

    public FeedFetcherWorker(@NonNull Context context, @NonNull WorkerParameters params)
    {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork()
    {
        context = getApplicationContext();
        repo = RepositoryHelper.getFeedRepository(context);
        pref = RepositoryHelper.getSettingsRepository(context);

        long keepTime = pref.feedItemKeepTime();
        long keepDateBorderTime = (keepTime > 0 ? System.currentTimeMillis() - keepTime : 0);

        deleteOldItems(keepDateBorderTime);

        Data data = getInputData();
        String action = data.getString(TAG_ACTION);
        boolean noAutoDownload = data.getBoolean(TAG_NO_AUTO_DOWNLOAD, false);

        if (action == null)
            return Result.failure();

        switch (action) {
            case ACTION_FETCH_CHANNEL:
                return fetchChannel(data.getLong(TAG_CHANNEL_ID, -1),
                        keepDateBorderTime, noAutoDownload);
            case ACTION_FETCH_CHANNEL_LIST:
                return fetchChannelsByUrl(data.getLongArray(TAG_CHANNEL_ID_LIST),
                        keepDateBorderTime, noAutoDownload);
            case ACTION_FETCH_ALL_CHANNELS:
                return fetchChannels(repo.getAllFeeds(),
                        keepDateBorderTime, noAutoDownload);
            default:
                return Result.failure();
        }
    }

    private Result fetchChannelsByUrl(long[] ids, long acceptMinDate,
                                      boolean noAutoDownload)
    {
        if (ids == null)
            return Result.failure();

        ArrayList<Result> results = new ArrayList<>();
        for (long id : ids)
            results.add(fetchChannel(id, acceptMinDate, noAutoDownload));

        for (Result result : results)
            if (result instanceof Result.Failure)
                return result;

        return Result.success();
    }

    private Result fetchChannels(List<FeedChannel> channels, long acceptMinDate,
                                 boolean noAutoDownload)
    {
        if (channels == null)
            return Result.failure();

        ArrayList<Result> results = new ArrayList<>();
        for (FeedChannel channel : channels) {
            if (channel == null)
                continue;
            results.add(fetchChannel(channel.id, acceptMinDate, noAutoDownload));
        }

        for (Result result : results)
            if (result instanceof Result.Failure)
                return result;

        return Result.success();
    }

    private Result fetchChannel(long id, long acceptMinDate,
                                boolean noAutoDownload)
    {
        if (id == -1)
            return Result.failure();

        FeedChannel channel = repo.getFeedById(id);
        if (channel == null)
            return Result.failure();

        FeedParser parser;
        try {
            parser = new FeedParser(getApplicationContext(), channel);

        } catch (Exception e) {
            channel.fetchError = e.getMessage();
            repo.updateFeed(channel);

            return Result.failure();
        }

        List<FeedItem> items = parser.getItems();

        filterItems(id, items, acceptMinDate);

        if (pref.feedRemoveDuplicates())
            filterItemDuplicates(items);

        repo.addItems(items);

        channel.fetchError = null;
        if (TextUtils.isEmpty(channel.name)) {
            channel.name = parser.getTitle();
            if (TextUtils.isEmpty(channel.name))
                channel.name = channel.url;
        }
        channel.lastUpdate = System.currentTimeMillis();
        repo.updateFeed(channel);

        if (!noAutoDownload && channel.autoDownload)
            sendFetchedItems(channel, items);

        return Result.success();
    }

    private void filterItems(long id, List<FeedItem> items, long acceptMinDate)
    {
        List<String> existingItemsId = repo.getItemsIdByFeedId(id);
        Iterator<FeedItem> it = items.iterator();
        while (it.hasNext()) {
            FeedItem item = it.next();
            /* Also filtering the items that we already have in db */
            if (item != null && (item.pubDate > 0 && item.pubDate <= acceptMinDate || existingItemsId.contains(item.id)))
                it.remove();
        }
    }

    private void filterItemDuplicates(List<FeedItem> items)
    {
        List<String> titles = new ArrayList<>();
        for (FeedItem item : items)
            titles.add(item.title);

        List<String> existingTitles = repo.findItemsExistingTitles(titles);
        Iterator<FeedItem> it = items.iterator();
        while (it.hasNext()) {
            FeedItem item = it.next();
            if (item != null && existingTitles.contains(item.title))
                it.remove();
        }
    }

    private void deleteOldItems(long keepDateBorderTime)
    {
        if (keepDateBorderTime > 0)
            repo.deleteItemsOlderThan(keepDateBorderTime);
    }

    private void sendFetchedItems(FeedChannel channel, List<FeedItem> items)
    {
        ArrayList<String> ids = new ArrayList<>();
        for (FeedItem item : items) {
            if (item == null || item.read)
                continue;

            if (isMatch(item, channel.filter, channel.isRegexFilter)) {
                ids.add(item.id);
                repo.markAsRead(item.id);
            }
        }

        if (ids.isEmpty())
            return;

        Data data = new Data.Builder()
                .putString(com.movilix.torrant.service.FeedDownloaderWorker.TAG_ACTION, com.movilix.torrant.service.FeedDownloaderWorker.ACTION_DOWNLOAD_TORRENT_LIST)
                .putStringArray(com.movilix.torrant.service.FeedDownloaderWorker.TAG_ITEM_ID_LIST, ids.toArray(new String[0]))
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(com.movilix.torrant.service.FeedDownloaderWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(context).enqueue(work);
    }

    private boolean isMatch(FeedItem item, String filters, boolean isRegex)
    {
        if (filters == null || TextUtils.isEmpty(filters))
            return true;

        for (String filter : filters.split(Utils.NEWLINE_PATTERN)) {
            if (TextUtils.isEmpty(filter))
                continue;

            if (isRegex) {
                Pattern pattern;
                try {
                    pattern = Pattern.compile(filter);

                } catch (PatternSyntaxException e) {
                    /* TODO: maybe there is an option better? */
                    Log.e(TAG, "Invalid pattern: " + filter);
                    return true;
                }

                return pattern.matcher(item.title).matches();
            } else {
                String[] words = filter.split(repo.getFilterSeparator());
                for (String word : words)
                    if (item.title.toLowerCase().contains(word.toLowerCase().trim()))
                        return true;
            }
        }

        return false;
    }
}
