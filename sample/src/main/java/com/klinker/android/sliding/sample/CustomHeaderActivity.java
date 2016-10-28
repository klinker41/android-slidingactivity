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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.klinker.android.sliding.MultiShrinkScroller;
import com.klinker.android.sliding.SlidingActivity;

/**
 * Activity demonstrating a very simple implementation of a sliding activity.
 */
public class CustomHeaderActivity extends SlidingActivity {

    /**
     * Initialize our values, this is overridden instead of onCreate as it should be in all
     * sliding activities.
     *
     * @param savedInstanceState the saved state.
     */
    @Override
    public void init(Bundle savedInstanceState) {
        setTitle(R.string.custom_sliding_activity);
        setPrimaryColors(
                getResources().getColor(R.color.dark_activity_primary),
                getResources().getColor(R.color.dark_activity_primary_dark)
        );
        setContent(R.layout.activity_content);
        setHeaderContent(R.layout.activity_custom_header);
        setFab(
                getResources().getColor(R.color.fab_activity_accent),
                R.drawable.ic_debug,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(CustomHeaderActivity.this, "FAB Clicked", Toast.LENGTH_SHORT).show();
                    }
                }
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

    @Override
    protected void configureScroller(MultiShrinkScroller scroller) {
        super.configureScroller(scroller);
        scroller.setIntermediateHeaderHeightRatio(1);
    }
}
