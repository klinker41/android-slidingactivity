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

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * A {@link ScrollView} that doesn't respond or intercept touch events.
 *
 * This is used in combination with {@link com.klinker.android.sliding.MultiShrinkScroller} so
 * that MultiShrinkScroller can handle all scrolling and saving.
 */
public class TouchlessScrollView extends ScrollView {

    /**
     * Creates a new instance of TouchlessScrollview.
     * @param context
     */
    public TouchlessScrollView(Context context) {
        this(context, null);
    }

    /**
     * Creates a new instance of TouchlessScrollview.
     * @param context
     * @param attrs
     */
    public TouchlessScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Creates a new instance of TouchlessScrollview.
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public TouchlessScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Save the current y scroll position for later.
     * @return
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        // Do not save the current scroll position. Always store scrollY=0 and delegate
        // responsibility of saving state to the MultiShrinkScroller.
        final int scrollY = getScrollY();
        setScrollY(0);
        final Parcelable returnValue = super.onSaveInstanceState();
        setScrollY(scrollY);
        return returnValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}