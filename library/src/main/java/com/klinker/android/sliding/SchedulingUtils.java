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

import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;

/**
 * Helper for scheduling events that will be occuring.
 */
public class SchedulingUtils {

    /**
     * Do an action before the drawing occurs.
     * @param view the view to be drawn.
     * @param drawNextFrame whether we should draw the next frame or not.
     * @param runnable the runnable to run.
     */
    public static void doOnPreDraw(final View view, final boolean drawNextFrame,
                                   final Runnable runnable) {
        final OnPreDrawListener listener = new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                runnable.run();
                return drawNextFrame;
            }
        };
        view.getViewTreeObserver().addOnPreDrawListener(listener);
    }

}
