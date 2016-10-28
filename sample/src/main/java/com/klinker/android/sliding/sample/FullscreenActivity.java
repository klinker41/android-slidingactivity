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

/**
 * Activity similar to the normal activity, except it will start fullscreen instead of requiring to
 * be scrolled up.
 */
public class FullscreenActivity extends NormalActivity {

    /**
     * Make sure we call super to set everything else up, then show as fullscreen.
     *
     * @param savedInstanceState the saved state.
     */
    @Override
    public void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        enableFullscreen();
    }

}
