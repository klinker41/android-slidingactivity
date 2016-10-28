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

import com.klinker.android.sliding.SlidingActivity;

/**
 * Activity demonstrating a very simple implementation of a sliding activity with no header.
 */
public class NoHeaderActivity extends SlidingActivity {

    /**
     * Initialize our values, this is overridden instead of onCreate as it should be in all
     * sliding activities.
     *
     * @param savedInstanceState the saved state.
     */
    @Override
    public void init(Bundle savedInstanceState) {
        disableHeader();
        enableFullscreen();
        setPrimaryColors(
                getResources().getColor(R.color.normal_sliding_activity_primary),
                getResources().getColor(R.color.normal_sliding_activity_primary_dark)
        );
        setContent(R.layout.activity_content);

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

}
