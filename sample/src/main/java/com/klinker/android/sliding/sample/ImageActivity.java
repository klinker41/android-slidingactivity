/*
 * Copyright (C) 2015 Jacob Klinker
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

import android.os.Bundle;

import com.klinker.android.sliding.SlidingActivity;

public class ImageActivity extends SlidingActivity {

    @Override
    public void init(Bundle savedInstanceState) {
        setTitle(R.string.image_activity);
        setPrimaryColor(
                getResources().getColor(R.color.image_activity_primary),
                getResources().getColor(R.color.image_activity_primary_dark)
        );
        setContent(R.layout.activity_content);
        setImage(R.drawable.profile_picture);
    }

}
