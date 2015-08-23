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
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A custom {@link ViewGroup} that operates similarly to a {@link ScrollView}, except with multiple
 * subviews. These subviews are scrolled or shrinked one at a time, until each reaches their
 * minimum or maximum value.
 *
 * MultiShrinkScroller is designed for a specific problem. As such, this class is designed to be
 * used with a specific layout file: quickcontact_activity.xml. MultiShrinkScroller expects subviews
 * with specific ID values.
 *
 * MultiShrinkScroller's code is heavily influenced by ScrollView. Nonetheless, several ScrollView
 * features are missing. For example: handling of KEYCODES, OverScroll bounce and saving
 * scroll state in savedInstanceState bundles.
 *
 * Before copying this approach to nested scrolling, consider whether something simpler and less
 * customized will work for you. For example, see the re-usable StickyHeaderListView used by
 * WifiSetupActivity (very nice). Alternatively, check out Google+'s cover photo scrolling or
 * Android L's built in nested scrolling support. I thought I needed a more custom ViewGroup in
 * order to track velocity, modify EdgeEffect color and perform the originally specified animations.
 * As a result this ViewGroup has non-standard talkback and keyboard support.
 */
public class MultiShrinkScroller extends FrameLayout {

    /**
     * 1000 pixels per millisecond. Ie, 1 pixel per second.
     */
    private static final int PIXELS_PER_SECOND = 1000;

    /**
     * The duration that the activity should take to animate onto screen.
     */
    private static final int ANIMATION_DURATION = 300;

    /**
     * Length of the acceleration animations. This value was taken from ValueAnimator.java.
     */
    private static final int EXIT_FLING_ANIMATION_DURATION_MS = 250;

    /**
     * In portrait mode, the height:width ratio of the photo's starting height.
     */
    private static final float INTERMEDIATE_HEADER_HEIGHT_RATIO = 0.6f;

    /**
     * Color blending will only be performed on the contact photo once the toolbar is compressed
     * to this ratio of its full height.
     */
    private static final float COLOR_BLENDING_START_RATIO = 0.5f;

    /**
     * Dampen the animations slightly.
     */
    private static final float SPRING_DAMPENING_FACTOR = 0.01f;

    private float[] lastEventPosition = { 0, 0 };
    private VelocityTracker velocityTracker;
    private boolean isBeingDragged = false;
    private boolean receivedDown = false;
    private boolean isFullscreenDownwardsFling = false;
    private ScrollView scrollView;
    private View scrollViewChild;
    private View toolbar;
    private ImageView photoView;
    private FloatingActionButton fab;
    private View photoViewContainer;
    private View transparentView;
    private MultiShrinkScrollerListener listener;
    private TextView largeTextView;
    private View photoTouchInterceptOverlay;
    private TextView invisiblePlaceholderTextView;
    private View titleGradientView;
    private View actionBarGradientView;
    private View startColumn;
    private int headerTintColor;
    private int maximumHeaderHeight;
    private int minimumHeaderHeight;
    private int intermediateHeaderHeight;
    private boolean isOpenImageSquare;
    private int maximumHeaderTextSize;
    private int collapsedTitleBottomMargin;
    private int collapsedTitleStartMargin;
    private boolean hasEverTouchedTheTop;
    private boolean isTouchDisabledForDismissAnimation;
    private boolean enableFab = false;

    private final Scroller scroller;
    private final EdgeEffect edgeGlowBottom;
    private final EdgeEffect edgeGlowTop;
    private final int touchSlop;
    private final int maximumVelocity;
    private final int minimumVelocity;
    private final int dismissDistanceOnScroll;
    private final int dismissDistanceOnRelease;
    private final int snapToTopSlopHeight;
    private final int transparentStartHeight;
    private final int maximumTitleMargin;
    private final float toolbarElevation;
    private final boolean isTwoPanel;
    private final float landscapePhotoRatio;
    private final int actionBarSize;

    private static final float X1 = 0.16f;
    private static final float Y1 = 0.4f;
    private static final float X2 = 0.2f;
    private static final float Y2 = 1f;
    private final PathInterpolator textSizePathInterpolator;

    private final int[] gradientColors = new int[] {0,0x88000000};
    private GradientDrawable titleGradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, gradientColors);
    private GradientDrawable actionBarGradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP, gradientColors);

    /**
     * Interface for listening to scroll events.
     */
    public interface MultiShrinkScrollerListener {

        void onScrolledOffBottom();
        void onStartScrollOffBottom();
        void onTransparentViewHeightChange(float ratio);
        void onEntranceAnimationDone();
        void onEnterFullscreen();
        void onExitFullscreen();

    }

    /**
     * Listener for snapping the content to the bottom of the screen.
     */
    private final AnimatorListener snapToBottomListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (getScrollUntilOffBottom() > 0 && listener != null) {
                // Due to a rounding error, after the animation finished we haven't fully scrolled
                // off the screen. Lie to the listener: tell it that we did scroll off the screen.
                listener.onScrolledOffBottom();

                // No other messages need to be sent to the listener.
                listener = null;
            }
        }
    };

    /**
     * Interpolator from android.support.v4.view.ViewPager. Snappier and more elastic feeling
     * than the default interpolator.
     */
    private static final Interpolator INTERPOLATOR = new Interpolator() {

        /**
         * {@inheritDoc}
         */
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    /**
     * Create a new instance of MultiShrinkScroller.
     * @param context
     */
    public MultiShrinkScroller(Context context) {
        this(context, null);
    }

    /**
     * Create a new instance of MultiShrinkScroller.
     * @param context
     * @param attrs
     */
    public MultiShrinkScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Create a new instance of MultiShrinkScroller.
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public MultiShrinkScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        setFocusable(false);
        setWillNotDraw(false);

        edgeGlowBottom = new EdgeEffect(context);
        edgeGlowTop = new EdgeEffect(context);
        scroller = new Scroller(context, INTERPOLATOR);
        touchSlop = configuration.getScaledTouchSlop();
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
        transparentStartHeight = (int) getResources().getDimension(
                R.dimen.sliding_starting_empty_height);
        toolbarElevation = getResources().getDimension(
                R.dimen.sliding_toolbar_elevation);
        isTwoPanel = getResources().getBoolean(R.bool.sliding_two_panel);
        maximumTitleMargin = (int) getResources().getDimension(
                R.dimen.sliding_title_initial_margin);

        dismissDistanceOnScroll = (int) getResources().getDimension(
                R.dimen.sliding_dismiss_distance_on_scroll);
        dismissDistanceOnRelease = (int) getResources().getDimension(
                R.dimen.sliding_dismiss_distance_on_release);
        snapToTopSlopHeight = (int) getResources().getDimension(
                R.dimen.sliding_snap_to_top_slop_height);

        final TypedValue photoRatio = new TypedValue();
        getResources().getValue(R.dimen.sliding_landscape_photo_ratio, photoRatio, true);
        landscapePhotoRatio = photoRatio.getFloat();

        final TypedArray attributeArray = context.obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        actionBarSize = attributeArray.getDimensionPixelSize(0, 0);
        minimumHeaderHeight = actionBarSize;
        attributeArray.recycle();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textSizePathInterpolator = new PathInterpolator(X1, Y1, X2, Y2);
        } else {
            textSizePathInterpolator = null;
        }
    }

    /**
     * This method must be called inside the Activity's onCreate. Initialize everything.
     */
    public void initialize(MultiShrinkScrollerListener listener, boolean isOpenContactSquare) {
        scrollView = (ScrollView) findViewById(R.id.content_scroller);
        scrollViewChild = findViewById(R.id.content_container);
        toolbar = findViewById(R.id.toolbar_parent);
        photoViewContainer = findViewById(R.id.toolbar_parent);
        transparentView = findViewById(R.id.transparent_view);
        largeTextView = (TextView) findViewById(R.id.large_title);
        invisiblePlaceholderTextView = (TextView) findViewById(R.id.placeholder_textview);
        startColumn = findViewById(R.id.empty_start_column);

        // Touching the empty space should close the card
        if (startColumn != null) {
            startColumn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    scrollOffBottom();
                }
            });
            findViewById(R.id.empty_end_column).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    scrollOffBottom();
                }
            });
        }
        this.listener = listener;
        this.isOpenImageSquare = isOpenContactSquare;

        photoView = (ImageView) findViewById(R.id.photo);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        titleGradientView = findViewById(R.id.title_gradient);
        titleGradientView.setBackgroundDrawable(titleGradientDrawable);
        actionBarGradientView = findViewById(R.id.action_bar_gradient);
        actionBarGradientView.setBackgroundDrawable(actionBarGradientDrawable);
        collapsedTitleStartMargin = ((Toolbar) findViewById(R.id.toolbar)).getContentInsetStart();

        photoTouchInterceptOverlay = findViewById(R.id.photo_touch_intercept_overlay);
        if (!isTwoPanel) {
            photoTouchInterceptOverlay.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    expandHeader();
                }
            });
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scrollView.setOnScrollChangeListener(new OnScrollChangeListener() {
                    @Override
                    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                        updateFabStatus(scrollY);
                    }
                });
            } else {
                scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        updateFabStatus(scrollView.getScrollY());
                    }
                });
            }
        }

        SchedulingUtils.doOnPreDraw(this, false, new Runnable() {
            @Override
            public void run() {
                if (!isTwoPanel) {
                    maximumHeaderHeight = getResources().getDimensionPixelSize(R.dimen.sliding_header_max_height);
                    intermediateHeaderHeight = (int) (maximumHeaderHeight
                            * INTERMEDIATE_HEADER_HEIGHT_RATIO);
                }
                setHeaderHeight(getMaximumScrollableHeaderHeight());
                maximumHeaderTextSize = largeTextView.getHeight();
                if (isTwoPanel) {
                    maximumHeaderHeight = getHeight();
                    minimumHeaderHeight = maximumHeaderHeight;
                    intermediateHeaderHeight = maximumHeaderHeight;

                    // Permanently set photo width and height.
                    final ViewGroup.LayoutParams photoLayoutParams
                            = photoViewContainer.getLayoutParams();
                    photoLayoutParams.height = maximumHeaderHeight;
                    photoLayoutParams.width = (int) (maximumHeaderHeight * landscapePhotoRatio);
                    photoViewContainer.setLayoutParams(photoLayoutParams);

                    // Permanently set title width and margin.
                    final LayoutParams largeTextLayoutParams
                            = (LayoutParams) largeTextView.getLayoutParams();
                    largeTextLayoutParams.width = photoLayoutParams.width -
                            largeTextLayoutParams.leftMargin - largeTextLayoutParams.rightMargin;
                    largeTextLayoutParams.gravity = Gravity.BOTTOM | Gravity.START;
                    largeTextView.setLayoutParams(largeTextLayoutParams);
                } else {
                    // Set the width of largeTextView as if it was nested inside
                    // photoViewContainer.
                    largeTextView.setWidth(photoViewContainer.getWidth()
                            - 2 * maximumTitleMargin);
                }

                calculateCollapsedLargeTitlePadding();
                updateHeaderTextSizeAndMargin();
                configureGradientViewHeights();
            }
        });
    }

    private void configureGradientViewHeights() {
        final LayoutParams actionBarGradientLayoutParams
                = (LayoutParams) actionBarGradientView.getLayoutParams();
        actionBarGradientLayoutParams.height = actionBarSize;
        actionBarGradientView.setLayoutParams(actionBarGradientLayoutParams);
        final LayoutParams titleGradientLayoutParams
                = (LayoutParams) titleGradientView.getLayoutParams();
        final float TITLE_GRADIENT_SIZE_COEFFICIENT = 1.25f;
        final LayoutParams largeTextLayoutParms
                = (LayoutParams) largeTextView.getLayoutParams();
        titleGradientLayoutParams.height = (int) ((largeTextView.getHeight()
                + largeTextLayoutParms.bottomMargin) * TITLE_GRADIENT_SIZE_COEFFICIENT);
        titleGradientView.setLayoutParams(titleGradientLayoutParams);
    }

    /**
     * Set the title for the large text view that will be adjusted as the activity scrolls.
     * @param title the title.
     */
    public void setTitle(String title) {
        largeTextView.setText(title);
        photoTouchInterceptOverlay.setContentDescription(title);
    }

    /**
     * Disables the header at the top of the activity, only the content will be shown.
     */
    public void disableHeader() {
        intermediateHeaderHeight = 0;
        maximumHeaderHeight = 0;
        minimumHeaderHeight = 0;
        largeTextView.setVisibility(View.GONE);
        ((View) photoView.getParent()).setVisibility(View.GONE);
    }

    /**
     * Catch the touch event and act on it.
     * @param event the touch event.
     * @return true if we should start dragging, otherwise false.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        // The only time we want to intercept touch events is when we are being dragged.
        return shouldStartDrag(event);
    }

    private boolean shouldStartDrag(MotionEvent event) {
        if (isTouchDisabledForDismissAnimation) return false;

        if (isBeingDragged) {
            isBeingDragged = false;
            return false;
        }

        switch (event.getAction()) {
            // If we are in the middle of a fling and there is a down event, we'll steal it and
            // start a drag.
            case MotionEvent.ACTION_DOWN:
                updateLastEventPosition(event);
                if (!scroller.isFinished()) {
                    startDrag();
                    return true;
                } else {
                    receivedDown = true;
                }
                break;

            // Otherwise, we will start a drag if there is enough motion in the direction we are
            // capable of scrolling.
            case MotionEvent.ACTION_MOVE:
                if (motionShouldStartDrag(event)) {
                    updateLastEventPosition(event);
                    startDrag();
                    return true;
                }
                break;
        }

        return false;
    }

    /**
     * Catch the touch event and act on it.
     * @param event the touch event.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isTouchDisabledForDismissAnimation) return true;

        final int action = event.getAction();

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        if (!isBeingDragged) {
            if (shouldStartDrag(event)) {
                return true;
            }

            if (action == MotionEvent.ACTION_UP && receivedDown) {
                receivedDown = false;
                return performClick();
            }
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                final float delta = updatePositionAndComputeDelta(event);
                scrollTo(0, getScroll() + (int) delta);
                receivedDown = false;

                if (isBeingDragged) {
                    final int distanceFromMaxScrolling = getMaximumScrollUpwards() - getScroll();
                    if (delta > distanceFromMaxScrolling && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // The ScrollView is being pulled upwards while there is no more
                        // content offscreen, and the view port is already fully expanded.
                        edgeGlowBottom.onPull(delta / getHeight(), 1 - event.getX() / getWidth());
                    }

                    if (!edgeGlowBottom.isFinished()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            postInvalidateOnAnimation();
                        } else {
                            postInvalidate();
                        }
                    }

                    if (shouldDismissOnScroll()) {
                        scrollOffBottom();
                    }

                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopDrag(action == MotionEvent.ACTION_CANCEL);
                receivedDown = false;
                break;
        }

        return true;
    }

    /**
     * Sets the tint color that should be applied to the header. If an image is present, this
     * will go behind the image and show over it as the activity is scrolled, otherwise it will
     * just be the color displayed at the top of the screen.
     * @param color the primary color for the activity to display.
     */
    public void setHeaderTintColor(int color) {
        headerTintColor = color;
        updatePhotoTintAndDropShadow();
        // We want to use the same amount of alpha on the new tint color as the previous tint color.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final int edgeEffectAlpha = Color.alpha(edgeGlowBottom.getColor());
            edgeGlowBottom.setColor((color & 0xffffff) | Color.argb(edgeEffectAlpha, 0, 0, 0));
            edgeGlowTop.setColor(edgeGlowBottom.getColor());
        }
    }

    /**
     * Expand to maximum size.
     */
    private void expandHeader() {
        if (getHeaderHeight() != maximumHeaderHeight) {
            final ObjectAnimator animator = ObjectAnimator.ofInt(this, "headerHeight",
                    maximumHeaderHeight);
            animator.setDuration(ANIMATION_DURATION);
            animator.start();
            // Scroll nested scroll view to its top
            if (scrollView.getScrollY() != 0) {
                ObjectAnimator.ofInt(scrollView, "scrollY", -scrollView.getScrollY()).start();
            }
        }
    }

    private void startDrag() {
        isBeingDragged = true;
        scroller.abortAnimation();
    }

    private void stopDrag(boolean cancelled) {
        isBeingDragged = false;
        if (!cancelled && getChildCount() > 0) {
            final float velocity = getCurrentVelocity();
            if (velocity > minimumVelocity || velocity < -minimumVelocity) {
                fling(-velocity);
                onDragFinished(scroller.getFinalY() - scroller.getStartY());
            } else {
                onDragFinished(/* flingDelta = */ 0);
            }
        } else {
            onDragFinished(/* flingDelta = */ 0);
        }

        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }

        edgeGlowBottom.onRelease();
    }

    private void onDragFinished(int flingDelta) {
        if (getTransparentViewHeight() <= 0) {
            // Don't perform any snapping if quick contacts is full screen.
            return;
        }
        if (!snapToTopOnDragFinished(flingDelta)) {
            // The drag/fling won't result in the content at the top of the Window. Consider
            // snapping the content to the bottom of the window.
            snapToBottomOnDragFinished();
        }
    }

    /**
     * If needed, snap the subviews to the top of the Window.
     *
     * @return TRUE if QuickContacts will snap/fling to to top after this method call.
     */
    private boolean snapToTopOnDragFinished(int flingDelta) {
        if (!hasEverTouchedTheTop) {
            // If the current fling is predicted to scroll past the top, then we don't need to snap
            // to the top. However, if the fling only flings past the top by a tiny amount,
            // it will look nicer to snap than to fling.
            final float predictedScrollPastTop = getTransparentViewHeight() - flingDelta;
            if (predictedScrollPastTop < -snapToTopSlopHeight) {
                return false;
            }

            if (getTransparentViewHeight() <= transparentStartHeight) {
                // We are above the starting scroll position so snap to the top.
                scroller.forceFinished(true);
                smoothScrollBy(getTransparentViewHeight());
                return true;
            }
            return false;
        }
        if (getTransparentViewHeight() < dismissDistanceOnRelease) {
            scroller.forceFinished(true);
            smoothScrollBy(getTransparentViewHeight());
            return true;
        }
        return false;
    }

    /**
     * If needed, scroll all the subviews off the bottom of the Window.
     */
    private void snapToBottomOnDragFinished() {
        if (hasEverTouchedTheTop) {
            if (getTransparentViewHeight() > dismissDistanceOnRelease) {
                scrollOffBottom();
            }
            return;
        }
        if (getTransparentViewHeight() > transparentStartHeight) {
            scrollOffBottom();
        }
    }

    /**
     * Returns TRUE if we have scrolled far QuickContacts far enough that we should dismiss it
     * without waiting for the user to finish their drag.
     */
    private boolean shouldDismissOnScroll() {
        return hasEverTouchedTheTop && getTransparentViewHeight() > dismissDistanceOnScroll;
    }

    /**
     * Return ratio of non-transparent:viewgroup-height for this viewgroup at the starting position.
     */
    public float getStartingTransparentHeightRatio() {
        return getTransparentHeightRatio(transparentStartHeight);
    }

    private float getTransparentHeightRatio(int transparentHeight) {
        final float heightRatio = (float) transparentHeight / getHeight();
        // Clamp between [0, 1] in case this is called before height is initialized.
        return 1.0f - Math.max(Math.min(1.0f, heightRatio), 0f);
    }

    /**
     * Scroll the activity off the bottom of the screen.
     */
    public void scrollOffBottom() {
        isTouchDisabledForDismissAnimation = true;
        final Interpolator interpolator = new AcceleratingFlingInterpolator(
                EXIT_FLING_ANIMATION_DURATION_MS, getCurrentVelocity(),
                getScrollUntilOffBottom());
        scroller.forceFinished(true);
        ObjectAnimator translateAnimation = ObjectAnimator.ofInt(this, "scroll",
                getScroll() - getScrollUntilOffBottom());
        translateAnimation.setRepeatCount(0);
        translateAnimation.setInterpolator(interpolator);
        translateAnimation.setDuration(EXIT_FLING_ANIMATION_DURATION_MS);
        translateAnimation.addListener(snapToBottomListener);
        translateAnimation.start();
        if (listener != null) {
            listener.onStartScrollOffBottom();
        }
    }

    /**
     * Scroll the activity up as the entrace animation.
     * @param scrollToCurrentPosition if true, will scroll from the bottom of the screen to the
     * current position. Otherwise, will scroll from the bottom of the screen to the top of the
     * screen.
     */
    public void scrollUpForEntranceAnimation(boolean scrollToCurrentPosition) {
        final int currentPosition = getScroll();
        final int bottomScrollPosition = currentPosition
                - (getHeight() - getTransparentViewHeight()) + 1;
        final Interpolator interpolator;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            interpolator = AnimationUtils.loadInterpolator(getContext(),
                    android.R.interpolator.linear_out_slow_in);
        } else {
            interpolator = new DecelerateInterpolator();
        }

        final int desiredValue = currentPosition + (scrollToCurrentPosition ? currentPosition
                : getTransparentViewHeight());
        final ObjectAnimator animator = ObjectAnimator.ofInt(this, "scroll", bottomScrollPosition,
                desiredValue);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedValue().equals(desiredValue) && listener != null) {
                    listener.onEntranceAnimationDone();
                }
            }
        });
        animator.start();
    }

    /**
     * Scroll to a certain position.
     * @param x the x position.
     * @param y the y position.
     */
    @Override
    public void scrollTo(int x, int y) {
        final int delta = y - getScroll();
        boolean wasFullscreen = getScrollNeededToBeFullScreen() <= 0;

        if (delta > 0) {
            scrollUp(delta);
        } else {
            scrollDown(delta);
        }

        updatePhotoTintAndDropShadow();
        updateHeaderTextSizeAndMargin();
        updateFabStatus();
        final boolean isFullscreen = getScrollNeededToBeFullScreen() <= 0;
        hasEverTouchedTheTop |= isFullscreen;

        if (listener != null) {
            if (wasFullscreen && !isFullscreen) {
                 listener.onExitFullscreen();
            } else if (!wasFullscreen && isFullscreen) {
                listener.onEnterFullscreen();
            }
            if (!isFullscreen || !wasFullscreen) {
                listener.onTransparentViewHeightChange(
                        getTransparentHeightRatio(getTransparentViewHeight()));
            }
        }
    }

    /**
     * Get the current toolbar height.
     * @return the toolbar height.
     */
    public int getToolbarHeight() {
        return toolbar.getLayoutParams().height;
    }

    /**
     * Set the height of the toolbar and update its tint accordingly.
     */
    public void setHeaderHeight(int height) {
        final ViewGroup.LayoutParams toolbarLayoutParams
                = toolbar.getLayoutParams();
        toolbarLayoutParams.height = height;
        toolbar.setLayoutParams(toolbarLayoutParams);
        updatePhotoTintAndDropShadow();
        updateHeaderTextSizeAndMargin();
    }

    /**
     * Gets the current header height.
     * @return the header height.
     */
    public int getHeaderHeight() {
        return toolbar.getLayoutParams().height;
    }

    /**
     * Set where to scroll to.
     * @param scroll the y scroll position.
     */
    public void setScroll(int scroll) {
        scrollTo(0, scroll);
    }

    /**
     * Returns the total amount scrolled inside the nested ScrollView + the amount of shrinking
     * performed on the ToolBar. This is the value inspected by animators.
     */
    public int getScroll() {
        return transparentStartHeight - getTransparentViewHeight()
                + getMaximumScrollableHeaderHeight() - getToolbarHeight()
                + scrollView.getScrollY();
    }

    private int getMaximumScrollableHeaderHeight() {
        return isOpenImageSquare ? maximumHeaderHeight : intermediateHeaderHeight;
    }

    /**
     * A variant of {@link #getScroll} that pretends the header is never larger than
     * than intermediateHeaderHeight. This function is sometimes needed when making scrolling
     * decisions that will not change the header size (ie, snapping to the bottom or top).
     *
     * When isOpenImageSquare is true, this function considers intermediateHeaderHeight ==
     * maximumHeaderHeight, since snapping decisions will be made relative the full header
     * size when isOpenImageSquare = true.
     *
     * This value should never be used in conjunction with {@link #getScroll} values.
     */
    private int getScroll_ignoreOversizedHeaderForSnapping() {
        return transparentStartHeight - getTransparentViewHeight()
                + Math.max(getMaximumScrollableHeaderHeight() - getToolbarHeight(), 0)
                + scrollView.getScrollY();
    }

    /**
     * Amount of transparent space above the header/toolbar.
     */
    public int getScrollNeededToBeFullScreen() {
        return getTransparentViewHeight();
    }

    /**
     * Return amount of scrolling needed in order for all the visible subviews to scroll off the
     * bottom.
     */
    private int getScrollUntilOffBottom() {
        return getHeight() + getScroll_ignoreOversizedHeaderForSnapping()
                - transparentStartHeight;
    }

    /**
     * Compute the scroll for the action.
     */
    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            // Examine the fling results in order to activate EdgeEffect and halt flings.
            final int oldScroll = getScroll();
            scrollTo(0, scroller.getCurrY());
            final int delta = scroller.getCurrY() - oldScroll;
            final int distanceFromMaxScrolling = getMaximumScrollUpwards() - getScroll();

            if (delta > distanceFromMaxScrolling && distanceFromMaxScrolling > 0) {
                edgeGlowBottom.onAbsorb((int) scroller.getCurrVelocity());
            }

            if (isFullscreenDownwardsFling && getTransparentViewHeight() > 0) {
                // Halt the fling once QuickContact's top is on screen.
                scrollTo(0, getScroll() + getTransparentViewHeight());
                edgeGlowTop.onAbsorb((int) scroller.getCurrVelocity());
                scroller.abortAnimation();
                isFullscreenDownwardsFling = false;
            }

            if (!awakenScrollBars()) {
                // Keep on drawing until the animation has finished.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    postInvalidateOnAnimation();
                } else {
                    postInvalidate();
                }
            }

            if (scroller.getCurrY() >= getMaximumScrollUpwards()) {
                // Halt the fling once QuickContact's bottom is on screen.
                scroller.abortAnimation();
                isFullscreenDownwardsFling = false;
            }
        }
    }

    /**
     * Draw all components on the screen.
     * @param canvas the canvas to draw on.
     */
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight();

        if (!edgeGlowBottom.isFinished()) {
            final int restoreCount = canvas.save();

            // Draw the EdgeEffect on the bottom of the Window (Or a little bit below the bottom
            // of the Window if we start to scroll upwards while EdgeEffect is visible). This
            // does not need to consider the case where this MultiShrinkScroller doesn't fill
            // the Window, since the nested ScrollView should be set to fillViewport.
            canvas.translate(-width + getPaddingLeft(),
                    height + getMaximumScrollUpwards() - getScroll());

            canvas.rotate(180, width, 0);
            if (isTwoPanel) {
                // Only show the EdgeEffect on the bottom of the ScrollView.
                edgeGlowBottom.setSize(scrollView.getWidth(), height);
            } else {
                edgeGlowBottom.setSize(width, height);
            }
            if (edgeGlowBottom.draw(canvas)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    postInvalidateOnAnimation();
                } else {
                    postInvalidate();
                }
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!edgeGlowTop.isFinished()) {
            final int restoreCount = canvas.save();
            if (isTwoPanel) {
                edgeGlowTop.setSize(scrollView.getWidth(), height);
                canvas.translate(photoViewContainer.getWidth(), 0);
            } else {
                edgeGlowTop.setSize(width, height);
            }
            if (edgeGlowTop.draw(canvas)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    postInvalidateOnAnimation();
                } else {
                    postInvalidate();
                }
            }
            canvas.restoreToCount(restoreCount);
        }
    }

    private float getCurrentVelocity() {
        if (velocityTracker == null) {
            return 0;
        }
        velocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND, maximumVelocity);
        return velocityTracker.getYVelocity();
    }

    private void fling(float velocity) {
        // For reasons I do not understand, scrolling is less janky when maxY=Integer.MAX_VALUE
        // then when maxY is set to an actual value.
        scroller.fling(0, getScroll(), 0, (int) velocity, 0, 0, -Integer.MAX_VALUE,
                Integer.MAX_VALUE);
        if (velocity < 0 && transparentView.getHeight() <= 0) {
            isFullscreenDownwardsFling = true;
        }
        invalidate();
    }

    private int getMaximumScrollUpwards() {
        if (!isTwoPanel) {
            return transparentStartHeight
                    // How much the Header view can compress
                    + getMaximumScrollableHeaderHeight() - getFullyCompressedHeaderHeight()
                    // How much the ScrollView can scroll. 0, if child is smaller than ScrollView.
                    + Math.max(0, scrollViewChild.getHeight() - getHeight()
                    + getFullyCompressedHeaderHeight());
        } else {
            return transparentStartHeight
                    // How much the ScrollView can scroll. 0, if child is smaller than ScrollView.
                    + Math.max(0, scrollViewChild.getHeight() - getHeight());
        }
    }

    private int getTransparentViewHeight() {
        return transparentView.getLayoutParams().height;
    }

    private void setTransparentViewHeight(int height) {
        transparentView.getLayoutParams().height = height;
        transparentView.setLayoutParams(transparentView.getLayoutParams());
    }

    private void scrollUp(int delta) {
        if (getTransparentViewHeight() != 0) {
            final int originalValue = getTransparentViewHeight();
            setTransparentViewHeight(getTransparentViewHeight() - delta);
            setTransparentViewHeight(Math.max(0, getTransparentViewHeight()));
            delta -= originalValue - getTransparentViewHeight();
        }
        final ViewGroup.LayoutParams toolbarLayoutParams
                = toolbar.getLayoutParams();
        if (toolbarLayoutParams.height > getFullyCompressedHeaderHeight()) {
            final int originalValue = toolbarLayoutParams.height;
            toolbarLayoutParams.height -= delta;
            toolbarLayoutParams.height = Math.max(toolbarLayoutParams.height,
                    getFullyCompressedHeaderHeight());
            toolbar.setLayoutParams(toolbarLayoutParams);
            delta -= originalValue - toolbarLayoutParams.height;
        }
        scrollView.scrollBy(0, delta);
    }

    /**
     * Returns the minimum size that we want to compress the header to, given that we don't want to
     * allow the the ScrollView to scroll unless there is new content off of the edge of ScrollView.
     */
    private int getFullyCompressedHeaderHeight() {
        return Math.min(Math.max(toolbar.getLayoutParams().height - getOverflowingChildViewSize(),
                minimumHeaderHeight), getMaximumScrollableHeaderHeight());
    }

    /**
     * Returns the amount of scrollViewChild that doesn't fit inside its parent.
     */
    private int getOverflowingChildViewSize() {
        final int usedScrollViewSpace = scrollViewChild.getHeight();
        return -getHeight() + usedScrollViewSpace + toolbar.getLayoutParams().height;
    }

    private void scrollDown(int delta) {
        if (scrollView.getScrollY() > 0) {
            final int originalValue = scrollView.getScrollY();
            scrollView.scrollBy(0, delta);
            delta -= scrollView.getScrollY() - originalValue;
        }
        final ViewGroup.LayoutParams toolbarLayoutParams = toolbar.getLayoutParams();
        if (toolbarLayoutParams.height < getMaximumScrollableHeaderHeight()) {
            final int originalValue = toolbarLayoutParams.height;
            toolbarLayoutParams.height -= delta;
            toolbarLayoutParams.height = Math.min(toolbarLayoutParams.height,
                    getMaximumScrollableHeaderHeight());
            toolbar.setLayoutParams(toolbarLayoutParams);
            delta -= originalValue - toolbarLayoutParams.height;
        }
        setTransparentViewHeight(getTransparentViewHeight() - delta);

        if (getScrollUntilOffBottom() <= 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onScrolledOffBottom();
                        // No other messages need to be sent to the listener.
                        listener = null;
                    }
                }
            });
        }
    }

    /**
     * Set the header size and padding, based on the current scroll position.
     */
    private void updateHeaderTextSizeAndMargin() {
        if (isTwoPanel) {
            // The text size stays at a constant size & location in two panel layouts.
            return;
        }

        // The pivot point for scaling should be middle of the starting side.
        largeTextView.setPivotX(0);
        largeTextView.setPivotY(largeTextView.getHeight() / 2);

        final int toolbarHeight = toolbar.getLayoutParams().height;
        photoTouchInterceptOverlay.setClickable(toolbarHeight != maximumHeaderHeight);

        if (toolbarHeight >= maximumHeaderHeight) {
            // Everything is full size when the header is fully expanded.
            largeTextView.setScaleX(1);
            largeTextView.setScaleY(1);
            setInterpolatedTitleMargins(1);
            return;
        }

        final float ratio = (toolbarHeight  - minimumHeaderHeight)
                / (float)(maximumHeaderHeight - minimumHeaderHeight);
        final float minimumSize = invisiblePlaceholderTextView.getHeight();
        float bezierOutput;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bezierOutput = textSizePathInterpolator.getInterpolation(ratio);
        } else {
            // since we can't use the path interpolator here, just interpolate linearly instead
            bezierOutput = ratio;
        }

        float scale = (minimumSize + (maximumHeaderTextSize - minimumSize) * bezierOutput)
                / maximumHeaderTextSize;

        // Clamp to reasonable/finite values before passing into framework. The values
        // can be wacky before the first pre-render.
        bezierOutput = Math.min(bezierOutput, 1.0f);
        scale = Math.min(scale, 1.0f);

        largeTextView.setScaleX(scale);
        largeTextView.setScaleY(scale);
        setInterpolatedTitleMargins(bezierOutput);
    }

    /**
     * Update the FAB visibility state depending on toolbar height.
     */
    private void updateFabStatus() {
        if (enableFab) {
            if (getToolbarHeight() >= intermediateHeaderHeight) {
                if (!fab.isShown()) {
                    fab.show();
                }
            } else {
                if (fab.isShown()) {
                    fab.hide();
                }
            }
        }
    }

    /**
     * Update the FAB visibility state depending on scrollview state.
     * @param scrollY the y value of the scroll.
     */
    private void updateFabStatus(int scrollY) {
        if (enableFab) {
            if (scrollY < scrollView.getMeasuredHeight() / 10) {
                if (!fab.isShown()) {
                    fab.show();
                }
            } else {
                if (fab.isShown()) {
                    fab.hide();
                }
            }
        }
    }

    /**
     * Set whether or not the FAB should be displayed.
     * @param enableFab whether to enable the fab.
     */
    public void setEnableFab(boolean enableFab) {
        this.enableFab = enableFab;

        if (!enableFab) {
            fab.hide();
        }
    }

    /**
     * Calculate the padding around largeTextView so that it will look appropriate once it
     * finishes moving into its target location/size.
     */
    private void calculateCollapsedLargeTitlePadding() {
        collapsedTitleBottomMargin =
                (minimumHeaderHeight - largeTextView.getMeasuredHeight()) / 2;
    }

    /**
     * Interpolate the title's margin size. When {@param x}=1, use the maximum title margins.
     * When {@param x}=0, use the margin values taken from {@link #invisiblePlaceholderTextView}.
     */
    private void setInterpolatedTitleMargins(float x) {
        final LayoutParams titleLayoutParams
                = (LayoutParams) largeTextView.getLayoutParams();
        final ViewGroup.LayoutParams toolbarLayoutParams
                = toolbar.getLayoutParams();

        // Need to add more to margin start if there is a start column
        int startColumnWidth = startColumn == null ? 0 : startColumn.getWidth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            titleLayoutParams.setMarginStart((int) (collapsedTitleStartMargin * (1 - x)
                    + maximumTitleMargin * x) + startColumnWidth);
        }

        // How offset the title should be from the bottom of the toolbar
        final int pretendBottomMargin =  (int) (collapsedTitleBottomMargin * (1 - x)
                + maximumTitleMargin * x) ;
        // Calculate how offset the title should be from the top of the screen. Instead of
        // calling largeTextView.getHeight() use the maximumHeaderTextSize for this calculation.
        // The getHeight() value acts unexpectedly when largeTextView is partially clipped by
        // its parent.
        titleLayoutParams.topMargin = getTransparentViewHeight()
                + toolbarLayoutParams.height - pretendBottomMargin
                - maximumHeaderTextSize;
        titleLayoutParams.bottomMargin = 0;
        largeTextView.setLayoutParams(titleLayoutParams);
    }

    private void updatePhotoTintAndDropShadow() {
        // We need to use toolbarLayoutParams to determine the height, since the layout
        // params can be updated before the height change is reflected inside the View#getHeight().
        final int toolbarHeight = getToolbarHeight();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (toolbarHeight <= minimumHeaderHeight && !isTwoPanel) {
                photoViewContainer.setElevation(toolbarElevation);
            } else {
                photoViewContainer.setElevation(0);
            }
        }

        final int gradientAlpha;
        final float colorAlpha;
        if (isTwoPanel) {
            colorAlpha = 0;
            gradientAlpha = 0x44;
        } else {
            float ratio = ((toolbarHeight - minimumHeaderHeight) /
                    (float) (maximumHeaderHeight - minimumHeaderHeight));
            if (toolbarHeight >= maximumHeaderHeight) {
                colorAlpha = 0;
            } else if (toolbarHeight >= intermediateHeaderHeight && toolbarHeight < maximumHeaderHeight) {
                colorAlpha = (-0.25f * ratio) + .25f;
            } else {
                colorAlpha = (-1.5f * ratio) + 1;
            }

            gradientAlpha = 0;
        }

        // Tell the photo view what tint we are trying to achieve. Depending on the type of
        // drawable used, the photo view may or may not use this tint.
        photoView.setBackgroundColor(headerTintColor);
        photoTouchInterceptOverlay.setBackgroundColor(ColorUtils.adjustAlpha(headerTintColor, colorAlpha));
        titleGradientDrawable.setAlpha(gradientAlpha);
        actionBarGradientDrawable.setAlpha(gradientAlpha);
    }

    private void updateLastEventPosition(MotionEvent event) {
        lastEventPosition[0] = event.getX();
        lastEventPosition[1] = event.getY();
    }

    private boolean motionShouldStartDrag(MotionEvent event) {
        final float deltaY = event.getY() - lastEventPosition[1];
        return deltaY > touchSlop || deltaY < -touchSlop;
    }

    private float updatePositionAndComputeDelta(MotionEvent event) {
        final int VERTICAL = 1;
        final float position = lastEventPosition[VERTICAL];
        updateLastEventPosition(event);
        float elasticityFactor = 1;
        if (position < lastEventPosition[VERTICAL] && hasEverTouchedTheTop) {
            // As QuickContacts is dragged from the top of the window, its rate of movement will
            // slow down in proportion to its distance from the top. This will feel springy.
            elasticityFactor += transparentView.getHeight() * SPRING_DAMPENING_FACTOR;
        }
        return (position - lastEventPosition[VERTICAL]) / elasticityFactor;
    }

    private void smoothScrollBy(int delta) {
        if (delta == 0) {
            // Delta=0 implies the code calling smoothScrollBy is sloppy. We should avoid doing
            // this, since it prevents Views from being able to register any clicks for 250ms.
            throw new IllegalArgumentException("Smooth scrolling by delta=0 is "
                    + "pointless and harmful");
        }
        scroller.startScroll(0, getScroll(), 0, delta);
        invalidate();
    }

    /**
     * Interpolator that enforces a specific starting velocity. This is useful to avoid a
     * discontinuity between dragging speed and flinging speed.
     *
     * Similar to a {@link android.view.animation.AccelerateInterpolator} in the sense that
     * getInterpolation() is a quadratic function.
     */
    private static class AcceleratingFlingInterpolator implements Interpolator {

        private final float startingSpeedPixelsPerFrame;
        private final float durationMs;
        private final int pixelsDelta;
        private final float numberFrames;

        public AcceleratingFlingInterpolator(int durationMs, float startingSpeedPixelsPerSecond,
                int pixelsDelta) {
            startingSpeedPixelsPerFrame = startingSpeedPixelsPerSecond / getRefreshRate();
            this.durationMs = durationMs;
            this.pixelsDelta = pixelsDelta;
            numberFrames = this.durationMs / getFrameIntervalMs();
        }

        @Override
        public float getInterpolation(float input) {
            final float animationIntervalNumber = numberFrames * input;
            final float linearDelta = (animationIntervalNumber * startingSpeedPixelsPerFrame)
                    / pixelsDelta;
            // Add the results of a linear interpolator (with the initial speed) with the
            // results of a AccelerateInterpolator.
            if (startingSpeedPixelsPerFrame > 0) {
                return Math.min(input * input + linearDelta, 1);
            } else {
                // Initial fling was in the wrong direction, make sure that the quadratic component
                // grows faster in order to make up for this.
                return Math.min(input * (input - linearDelta) + linearDelta, 1);
            }
        }

        private float getRefreshRate() {
            // TODO
//            DisplayInfo di = DisplayManagerGlobal.getInstance().getDisplayInfo(
//                    Display.DEFAULT_DISPLAY);
//            return di.refreshRate;
            return 30f;
        }

        public long getFrameIntervalMs() {
            return (long)(1000 / getRefreshRate());
        }
    }

}
