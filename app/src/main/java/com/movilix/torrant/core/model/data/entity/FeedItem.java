

package com.movilix.torrant.core.model.data.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/*
 * Representation of the feed article.
 */

@Entity(indices = {@Index(value = "feedId")},
        foreignKeys = @ForeignKey(
                entity = FeedChannel.class,
                parentColumns = "id",
                childColumns = "feedId",
                onDelete = CASCADE))

public class FeedItem implements Parcelable
{
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String title;
    public long feedId;
    public String downloadUrl;
    public String articleUrl;
    public long pubDate;
    public long fetchDate;
    public boolean read = false;

    public FeedItem(@NonNull String id, long feedId,
                    String downloadUrl, String articleUrl,
                    @NonNull String title, long pubDate)
    {
        this.id = id;
        this.feedId = feedId;
        this.downloadUrl = downloadUrl;
        this.articleUrl = articleUrl;
        this.title = title;
        this.pubDate = pubDate;
    }

    @Ignore
    public FeedItem(long feedId, String downloadUrl,
                    String articleUrl, @NonNull String title,
                    long pubDate)
    {
        this(feedId + "_" + title, feedId, downloadUrl, articleUrl, title, pubDate);
    }

    @Ignore
    public FeedItem(Parcel source)
    {
        id = source.readString();
        feedId = source.readLong();
        downloadUrl = source.readString();
        articleUrl = source.readString();
        title = source.readString();
        pubDate = source.readLong();
        fetchDate = source.readLong();
        read = source.readByte() != 0;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(id);
        dest.writeLong(feedId);
        dest.writeString(downloadUrl);
        dest.writeString(articleUrl);
        dest.writeString(title);
        dest.writeLong(pubDate);
        dest.writeLong(fetchDate);
        dest.writeInt((byte)(read ? 1 : 0));
    }

    public static final Creator<FeedItem> CREATOR =
            new Creator<FeedItem>()
            {
                @Override
                public FeedItem createFromParcel(Parcel source)
                {
                    return new FeedItem(source);
                }

                @Override
                public FeedItem[] newArray(int size)
                {
                    return new FeedItem[size];
                }
            };

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof FeedItem && (o == this || id.equals(((FeedItem)o).id));
    }

    @Override
    public String toString()
    {
        return "FeedItem{" +
                "id=" + id +
                "feedId='" + feedId + '\'' +
                ", title='" + title + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", articleUrl='" + articleUrl + '\'' +
                ", pubDate=" + SimpleDateFormat.getDateTimeInstance().format(new Date(pubDate)) +
                ", fetchDate=" + SimpleDateFormat.getDateTimeInstance().format(new Date(fetchDate)) +
                ", read=" + read +
                '}';
    }
}
