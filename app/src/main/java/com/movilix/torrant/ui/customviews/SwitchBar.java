

package com.movilix.torrant.ui.customviews;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;

import com.movilix.R;
import com.movilix.torrant.core.utils.Utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

public class SwitchBar extends SwitchCompat
{
    public SwitchBar(@NonNull Context context)
    {
        super(new ContextThemeWrapper(context, R.style.SwitchBar));

        init(context);
    }

    public SwitchBar(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(new ContextThemeWrapper(context, R.style.SwitchBar), attrs);

        init(context);
    }

    public SwitchBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(new ContextThemeWrapper(context, R.style.SwitchBar), attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context)
    {
        Drawable background = ContextCompat.getDrawable(context, R.drawable.switchbar_background);
        if (background != null)
            Utils.setBackground(this, background);

        setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        setText(isChecked() ? R.string.switch_on : R.string.switch_off);
    }

    @Override
    public void setChecked(boolean checked)
    {
        super.setChecked(checked);

        setText(isChecked() ? R.string.switch_on : R.string.switch_off);
    }
}
