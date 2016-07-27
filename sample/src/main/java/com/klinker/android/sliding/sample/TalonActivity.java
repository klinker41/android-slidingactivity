/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.sliding.sample;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.view.Menu;
import android.view.View;

import com.klinker.android.peekview.builder.Peek;
import com.klinker.android.peekview.builder.PeekViewOptions;
import com.klinker.android.peekview.callback.SimpleOnPeek;
import com.klinker.android.sliding.SlidingActivity;

/**
 * Activity mocking out the profile page on Talon for Twitter and demonstrating the power of
 * sliding activities.
 */
public class TalonActivity extends SlidingActivity {

    /**
     * Initialize our values, this is overridden instead of onCreate as it should be in all
     * sliding activities.
     * @param savedInstanceState the saved state.
     */
    @Override
    public void init(Bundle savedInstanceState) {
        setTitle(R.string.talon_activity);

        setPrimaryColors(
                getResources().getColor(R.color.talon_activity_primary),
                getResources().getColor(R.color.talon_activity_primary_dark)
        );

        if (checkMemory()) {
            setContent(R.layout.activity_talon);

            // long clicking on the "stats" card will display the PeekView
            Peek.into(R.layout.peek_example, new SimpleOnPeek() {
                @Override
                public void onInflated(View rootView) {
                    // we won't do anything here
                }
            }).applyTo(this, findViewById(R.id.talon_stats_card));
        }

        // delay this so that the animation shows and we don't change the activity colors
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setImage(R.drawable.twitter_profile);
            }
        }, 500);

        setFab(
                getResources().getColor(R.color.talon_activity_accent),
                R.drawable.ic_add,
                null
        );

        Intent intent = getIntent();
        if (intent.getBooleanExtra(SampleActivity.ARG_USE_EXPANSION, false)) {
            expandFromPoints(
                    intent.getIntExtra(SampleActivity.ARG_EXPANSION_LEFT_OFFSET, 0),
                    intent.getIntExtra(SampleActivity.ARG_EXPANSION_TOP_OFFSET, 0),
                    intent.getIntExtra(SampleActivity.ARG_EXPANSION_VIEW_WIDTH, 0),
                    intent.getIntExtra(SampleActivity.ARG_EXPANSION_VIEW_HEIGHT, 0)
            );
        }
    }

    /**
     * Creates the options menu.
     * @param menu the menu.
     * @return true.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_talon, menu);
        return true;
    }

    /**
     * Oops, this is a really big layout with lots of images and no optimizations done to it, older
     * or lower end devices might run out of memory! Sorry to those devs with these devices.
     * @return true if device has 1GB of memory and can show the content.
     */
    private boolean checkMemory() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            long availableMegs = mi.totalMem / 1048576L;
            return availableMegs >= 1000;
        } else {
            long availableMegs = mi.availMem / 1048576L;
            return availableMegs >= 1000;
        }
    }

}
