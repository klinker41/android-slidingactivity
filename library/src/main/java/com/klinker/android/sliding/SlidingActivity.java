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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Sliding activity that handles all interaction with users. It will be shown with about 150dp of
 * space at the top when initially launched and you can then scroll up the activity and close this
 * gap. Once it has been scrolled up, scrolling down will dismiss the activity.
 *
 * Usage will vary on what you would like to do. The following methods are available to customize
 * this activity:
 *
 * setTitle()
 * setImage()
 * setContent()
 * setPrimaryColors()
 * setFab()
 * disableHeader()
 * enableFullscreen()
 *
 * You may use any combination of these to achieve the desired look.
 */
public abstract class SlidingActivity extends AppCompatActivity {

    private static final int ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION = 150;
    private static final int SCRIM_COLOR = Color.argb(0xC8, 0, 0, 0);
    private static final int DEFAULT_PRIMARY_COLOR = 0xff607D8B;
    private static final int DEFAULT_PRIMARY_DARK_COLOR = 0xff37474F;

    private int statusBarColor;
    private boolean hasAlreadyBeenOpened;
    private ImageView photoView;
    private FloatingActionButton fab;
    private View photoViewTempBackground;
    private MultiShrinkScroller scroller;
    private FrameLayout content;
    private ColorDrawable windowScrim;
    private boolean isEntranceAnimationFinished;
    private boolean isExitAnimationInProgress;
    private boolean isStarting;
    private boolean startFullscreen = false;

    /**
     * Set up all relevant data for the activity including scrollers, etc. This is a final method,
     * any implementing class should instead implement init() and do what work you would like to do
     * there instead.
     * @param savedInstanceState the saved instance state.
     */
    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isStarting = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.sliding_activity);

        scroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);
        content = (FrameLayout) findViewById(R.id.content_container);

        photoView = (ImageView) findViewById(R.id.photo);
        photoViewTempBackground = findViewById(R.id.photo_background);
        final View transparentView = findViewById(R.id.transparent_view);
        if (scroller != null) {
            transparentView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    scroller.scrollOffBottom();
                }
            });
        }

        // Allow a shadow to be shown under the toolbar.
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
        }

        // Put a TextView with a known resource id into the ActionBar. This allows us to easily
        // find the correct TextView location & size later.
        toolbar.addView(getLayoutInflater().inflate(R.layout.sliding_title_placeholder, null));

        hasAlreadyBeenOpened = savedInstanceState != null;
        isEntranceAnimationFinished = hasAlreadyBeenOpened;
        windowScrim = new ColorDrawable(SCRIM_COLOR);
        windowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(windowScrim);

        scroller.initialize(multiShrinkScrollerListener, false);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        disableFab(); // default to having the fab be off

        SchedulingUtils.doOnPreDraw(scroller, true,
                new Runnable() {
                    @Override
                    public void run() {
                        // The initial scrim opacity must match the scrim opacity that would be
                        // achieved by scrolling to the starting position.
                        final float alphaRatio = scroller.getStartingTransparentHeightRatio();
                        final int duration = getResources().getInteger(
                                android.R.integer.config_shortAnimTime);
                        final int desiredAlpha = (int) (0xFF * alphaRatio);
                        ObjectAnimator o = ObjectAnimator.ofInt(windowScrim, "alpha", 0,
                                desiredAlpha).setDuration(duration);

                        o.start();
                    }
                });

        init(savedInstanceState);
        showActivity();

        isStarting = false;
    }

    /**
     * Initialize all of your data here, as you would with onCreate() normally.
     * @param savedInstanceState the saved instance state.
     */
    public abstract void init(Bundle savedInstanceState);

    /**
     * Set the title for the scroller.
     * @param title the title to display.
     */
    @Override
    public final void setTitle(CharSequence title) {
        setHeaderNameText(title.toString());
    }

    /**
     * Set the title for the scroller.
     * @param resId the title res id to display.
     */
    @Override
    public final void setTitle(int resId) {
        setHeaderNameText(resId);
    }

    /**
     * Set the primary colors for the activity. The primary color will be displayed as the
     * background on the header, the primary color dark will be used for coloring the status
     * bar on Lollipop+.
     * @param primaryColor the primary color to display.
     * @param primaryColorDark the primary dark color to display.
     */
    public void setPrimaryColors(int primaryColor, int primaryColorDark) {
        setThemeColor(primaryColor, primaryColorDark);
    }

    /**
     * Enable an FAB on the screen.
     * @param color the color for the FAB.
     * @param drawableRes the drawable to display on the FAB.
     * @param onClickListener the listener to activate when clicked on.
     */
    public void setFab(int color, int drawableRes, OnClickListener onClickListener) {
        fab.setBackgroundTintList(
                new ColorStateList(
                        new int[][] {
                                new int[] {}
                        },
                        new int[] {
                                color
                        }
                )
        );

        fab.setImageDrawable(getResources().getDrawable(drawableRes));
        fab.setOnClickListener(onClickListener);

        scroller.setEnableFab(true);
    }

    /**
     * Disable showing the FAB on the screen.
     */
    private void disableFab() {
        scroller.setEnableFab(false);
    }

    /**
     * Set the content to be displayed in the scrolling area.
     * @param resId the resource id to inflate for the content.
     */
    public void setContent(int resId) {
        setContent(getLayoutInflater().inflate(resId, null, false));
    }

    /**
     * Set the content to be displayed in the scrolling area.
     * @param view the view to use for the content.
     */
    public void setContent(View view) {
        content.addView(view);
    }

    /**
     * Set the image to be displayed in the header.
     * @param resId the resource id to use for the bitmap to be created for the header.
     */
    public void setImage(int resId) {
        setImage(BitmapFactory.decodeResource(getResources(), resId));
    }

    /**
     * Set the image to be displayed in the header. The image will be set immediately.
     *
     * If the activity is still starting when it is set (ie you call this in your init() method)
     * then the activity will use Palette to extract the primary colors from the image for you and
     * set them correctly. In this case, there is no reason to call setPrimaryColors(). If you
     * would like to manually set the colors still, call setPrimaryColors() after you have called
     * setImage().
     *
     * If the activity has already been started and your calling this after the fact (ie you might
     * have just downloaded the image from a url or needed to load it in the background) then the
     * activity will not use Palette to extract any colors. If this is the case, you should still
     * call setPrimaryColors() in the init() method. The image will be animated in with a circular
     * reveal in this case if the user is on a compatible system. Otherwise it will fade in.
     *
     * @param bitmap the bitmap to use for the animation.
     */
    public void setImage(Bitmap bitmap) {
        photoView.setImageBitmap(bitmap);

        if (isStarting) {
            Palette palette = Palette.from(bitmap).generate();
            setPrimaryColors(palette.getVibrantColor(DEFAULT_PRIMARY_COLOR),
                    palette.getDarkVibrantColor(DEFAULT_PRIMARY_DARK_COLOR));
        } else {
            photoViewTempBackground.setBackgroundDrawable(photoView.getBackground());
            photoViewTempBackground.setVisibility(View.VISIBLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int cx = (int) photoView.getX() + photoView.getMeasuredWidth() / 2;
                int cy = (int) photoView.getY() + photoView.getMeasuredHeight() / 2;
                int finalRadius = photoView.getWidth();

                Animator anim = ViewAnimationUtils.createCircularReveal(photoView, cx, cy,
                        0, finalRadius);
                anim.setDuration(500);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        photoViewTempBackground.setVisibility(View.GONE);
                    }
                });
                anim.start();
            } else {
                photoView.setAlpha(0f);
                photoView.animate()
                        .alpha(1f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                photoView.setAlpha(1f);
                                photoViewTempBackground.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
        }
    }

    /**
     * Disables the header and only displays the scrolling content below it.
     */
    public void disableHeader() {
        scroller.disableHeader();
    }

    /**
     * Starts the activity as fullscreen instead of first requiring the user to scroll up. This
     * needs to be called in init()
     */
    public void enableFullscreen() {
        startFullscreen = true;
    }

    @Override
    protected final void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        hasAlreadyBeenOpened = true;
        isEntranceAnimationFinished = true;
    }

    private void runEntranceAnimation() {
        if (hasAlreadyBeenOpened) {
            return;
        }
        hasAlreadyBeenOpened = true;
        scroller.scrollUpForEntranceAnimation(
                getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE &&
                        !startFullscreen
        );
    }

    private void setHeaderNameText(int resId) {
        if (scroller != null) {
            scroller.setTitle(getText(resId) == null ? null : getText(resId).toString());
        }
    }

    private void setHeaderNameText(String value) {
        if (!TextUtils.isEmpty(value)) {
            if (scroller != null) {
                scroller.setTitle(value);
            }
        }
    }

    private void showActivity() {
        if (scroller != null) {
            scroller.setVisibility(View.VISIBLE);
            SchedulingUtils.doOnPreDraw(scroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            runEntranceAnimation();
                        }
                    });
        }
    }

    private void setThemeColor(int primaryColor, int primaryColorDark) {
        scroller.setHeaderTintColor(primaryColor);
        statusBarColor = primaryColorDark;
        updateStatusBarColor();
    }

    private void updateStatusBarColor() {
        if (scroller == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final int desiredStatusBarColor;
            // Only use a custom status bar color if QuickContacts touches the top of the viewport.
            if (scroller.getScrollNeededToBeFullScreen() <= 0) {
                desiredStatusBarColor = statusBarColor;
            } else {
                desiredStatusBarColor = Color.TRANSPARENT;
            }
            // Animate to the new color.
            final ObjectAnimator animation = ObjectAnimator.ofInt(getWindow(), "statusBarColor",
                    getWindow().getStatusBarColor(), desiredStatusBarColor);
            animation.setDuration(ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION);
            animation.setEvaluator(new ArgbEvaluator());
            animation.start();
        }
    }

    /**
     * Handle the back button being pressed, dismiss the activity.
     */
    @Override
    public void onBackPressed() {
        if (scroller != null) {
            if (!isExitAnimationInProgress) {
                scroller.scrollOffBottom();
            }
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Finish the activity and override the normal animation, we already animated the activity
     * off of the screen by scrolling it off the bottom.
     */
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }


    private final MultiShrinkScroller.MultiShrinkScrollerListener multiShrinkScrollerListener
            = new MultiShrinkScroller.MultiShrinkScrollerListener() {
        @Override
        public void onScrolledOffBottom() {
            finish();
        }

        @Override
        public void onEnterFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onExitFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onStartScrollOffBottom() {
            isExitAnimationInProgress = true;
        }

        @Override
        public void onEntranceAnimationDone() {
            isEntranceAnimationFinished = true;
        }

        @Override
        public void onTransparentViewHeightChange(float ratio) {
            if (isEntranceAnimationFinished) {
                windowScrim.setAlpha((int) (0xFF * ratio));
            }
        }
    };

}
