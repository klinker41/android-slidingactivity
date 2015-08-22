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

package com.klinker.android.sliding;

import android.content.res.Resources;
import android.graphics.Outline;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * Provides static functions to work with views.
 */
public class ViewUtil {

    // don't allow for instantiation
    private ViewUtil() {

    }

    /**
     * Adds a rectangular outline to a view. This can be useful when you want to add a shadow
     * to a transparent view. See b/16856049.
     * @param view view that the outline is added to
     * @param res The resources file.
     */
    public static void addRectangularOutlineProvider(View view, Resources res) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewOutlineProvider rectOutlineProvider = new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        outline.setRect(0, 0, view.getWidth(), view.getHeight());
                    }
                }
            };

            view.setOutlineProvider(rectOutlineProvider);
        }
    }

}
