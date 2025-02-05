

package com.movilix.torrant.ui.addfeed;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.movilix.torrant.core.utils.Utils;
import com.movilix.torrant.ui.FragmentCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class AddFeedActivity extends AppCompatActivity
    implements FragmentCallback
{
    public static final String ACTION_EDIT_FEED = "com.movilix.torrant.ui.addfeed.AddFeedActivity.ACTION_EDIT_FEED";
    public static final String TAG_FEED_ID = "feed_id";

    private static final String TAG_ADD_FEED_DIALOG = "add_feed_dialog";

    private AddFeedDialog addFeedDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        setTheme(Utils.getTranslucentAppTheme(getApplicationContext()));
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        addFeedDialog = (AddFeedDialog)fm.findFragmentByTag(TAG_ADD_FEED_DIALOG);
        if (addFeedDialog == null) {
            Intent i = getIntent();
            if (i == null)
                return;

            if (ACTION_EDIT_FEED.equals(i.getAction())) {
                long feedId = i.getLongExtra(TAG_FEED_ID, -1);
                addFeedDialog = AddFeedDialog.newInstance(feedId);
            } else {
                addFeedDialog = AddFeedDialog.newInstance(getUriFromIntent());
            }

            addFeedDialog.show(fm, TAG_ADD_FEED_DIALOG);
        }
    }

    private Uri getUriFromIntent()
    {
        Intent i = getIntent();
        if (i != null) {
            if (i.getData() != null)
                return i.getData();
            else if (!TextUtils.isEmpty(i.getStringExtra(Intent.EXTRA_TEXT)))
                return Uri.parse(i.getStringExtra(Intent.EXTRA_TEXT));
        }

        return null;
    }

    @Override
    public void onFragmentFinished(@NonNull Fragment f, Intent intent, @NonNull ResultCode code)
    {
        finish();
    }

    @Override
    public void onBackPressed()
    {
        addFeedDialog.onBackPressed();
    }
}
