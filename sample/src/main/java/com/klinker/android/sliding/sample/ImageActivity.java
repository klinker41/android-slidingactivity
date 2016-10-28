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
import android.os.Handler;

import com.klinker.android.sliding.SlidingActivity;

/**
 * Activity demonstrating showing an image at the top of the the sliding activity.
 */
public class ImageActivity extends SlidingActivity {

    /**
     * Initialize our values, this is overridden instead of onCreate as it should be in all
     * sliding activities.
     *
     * @param savedInstanceState the saved state.
     */
    @Override
    public void init(Bundle savedInstanceState) {
        setTitle(R.string.image_activity);
        setContent(R.layout.activity_content);

        // no need to set a color here, palette will generate colors for us to be set
        setImage(R.drawable.profile_picture);

        // if we wanted to set some manually instead, do this after setting the image
        // setPrimaryColors(
        //         getResources().getColor(R.color.image_activity_primary),
        //         getResources().getColor(R.color.image_activity_primary_dark)
        // );

        // if we want the image to animate in, then set it after the activity has been created
        // NOTE: this will not change the activity's colors using palette, so make sure you call
        //       setPrimaryColors() first
        // new Handler().postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        //         setImage(R.drawable.profile_picture);
        //     }
        // }, 500);

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
