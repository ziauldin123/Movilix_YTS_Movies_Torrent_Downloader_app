

package com.movilix.torrant.ui.log;

import com.movilix.torrant.core.logger.LogEntry;
import com.movilix.torrant.core.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;
import io.reactivex.disposables.Disposable;

class LogDataSource extends PositionalDataSource<LogEntry>
{
    private Logger logger;
    private Disposable disposable;

    LogDataSource(@NonNull Logger logger)
    {
        this.logger = logger;

        disposable = logger.observeDataSetChanged()
                .subscribe((__) -> invalidate());
    }

    @Override
    public void invalidate()
    {
        disposable.dispose();

        super.invalidate();
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params,
                            @NonNull LoadInitialCallback<LogEntry> callback)
    {
        boolean paused = false;
        if (!logger.isPaused()) {
            logger.pause();
            paused = true;
        }

        try {
            List<LogEntry> entries;
            int numEntries = logger.getNumEntries();
            int pos = params.requestedStartPosition;

            if (params.requestedStartPosition < numEntries) {
                entries = logger.getEntries(params.requestedStartPosition, params.requestedLoadSize);

            } else if (params.requestedLoadSize <= numEntries) {
                pos = numEntries - params.requestedLoadSize;
                entries = logger.getEntries(pos, params.requestedLoadSize);

            } else {
                pos = 0;
                entries = logger.getEntries(pos, numEntries);
            }

            if (entries.isEmpty())
                callback.onResult(entries, 0);
            else
                callback.onResult(entries, pos);

        } finally {
            if (paused)
                logger.resume();
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
                          @NonNull LoadRangeCallback<LogEntry> callback)
    {
        boolean paused = false;
        if (!logger.isPaused()) {
            logger.pause();
            paused = true;
        }

        try {
            List<LogEntry> entries;
            int numEntries = logger.getNumEntries();

            if (params.startPosition < numEntries)
                entries = logger.getEntries(params.startPosition, params.loadSize);
            else
                entries = new ArrayList<>(0);

            callback.onResult(entries);

        } finally {
            if (paused)
                logger.resume();
        }
    }
}
