
package com.testapp.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

/**
 *
 */
public class SlidingUpDashboard extends SlidingUpPanelLayout {

    private static final String KEY_FIRST_LAUNCH = "isLaunch";
    public static final String KEY_DASHBOARD_ANCHOR = "dashboardAnchor";
    public static final String INSTANCE_STATE = "instanceState";
    private final long fastAnimationDuration;


    private View avatarViewContainer;

    private View avatarImage;
    private ScrollView dashboardScrollView;
    private View dashboardScrollShadow;
    private ProgressWheel avatarSpinner;
    private View avatarViewBackground;
    private LinearLayout slidingLayoutHeader;
    private ImageView avatarView;
    private View mainContent;

    private int mainContentResId = -1;
    private int avatarDrawableResId = R.mipmap.ic_launcher;
    private int dashboardBodyResId = -1;
    private DashboardListener dashboardListener;

    // STATE VARIABLES
    private boolean isFirstLaunch;
    private Float dashboardAnchor;
//    private AccessibilityUtil accessibilityUtil;

    public SlidingUpDashboard(Context context) {
        this(context, (AttributeSet) null);
    }

    public SlidingUpDashboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpDashboard(Context context, AttributeSet attrs, int defStyle) {
        super(context, processAttributes(attrs), defStyle);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpDashboard);
        if (typedArray != null) {
            mainContentResId = typedArray.getResourceId(R.styleable.SlidingUpDashboard_dashboardMainContent, -1);
            avatarDrawableResId = typedArray.getResourceId(R.styleable.SlidingUpDashboard_dashboardAvatarDrawable, avatarDrawableResId);
            dashboardBodyResId = typedArray.getResourceId(R.styleable.SlidingUpDashboard_dashboardBody, -1);
        }
        typedArray.recycle();
        if (mainContentResId == -1) {
            throw new IllegalStateException("dashboardMainContent and dashboardSlidingBody must be provided");
        }
        this.fastAnimationDuration = 200;//context.getResources().getInteger(R.integer.fast_animation_duration);
    }

    private static AttributeSet processAttributes(AttributeSet attrs) {
        //FIXME should map "umano" attrs to SlidingUpDasboard ones
        return attrs;
    }

    protected void onFinishInflate() {
        inflateComponents();
        setViewHolder();
        setListeners();
        super.onFinishInflate();
    }

    private void inflateComponents() {
        Context context = getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Adding main content
        mainContent = inflater.inflate(mainContentResId, this, false);
        this.addView(mainContent);

        // Adding sliding panel
        slidingLayoutHeader = new LinearLayout(context);
        slidingLayoutHeader.setId(R.id.sliding_layout_header);
        slidingLayoutHeader.setOrientation(LinearLayout.VERTICAL);
        slidingLayoutHeader.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        slidingLayoutHeader.setClickable(true);
        slidingLayoutHeader.setFocusable(false);
        this.addView(slidingLayoutHeader);

        // Adding Sliding panel header
        inflater.inflate(R.layout.dashboard_sliding_header, slidingLayoutHeader, true);

        // Adding Sliding panel body
        View body = inflater.inflate(R.layout.dashboard_sliding_body, slidingLayoutHeader, true);
        if (this.dashboardBodyResId != -1) {
            inflater.inflate(dashboardBodyResId, (ViewGroup) body.findViewById(R.id.fragment_dashboard), true);
        }
    }

    private void setViewHolder() {
        avatarViewContainer = this.findViewById(R.id.img_avatar_container);
        avatarImage = this.findViewById(R.id.img_avatar);
        avatarViewBackground = this.findViewById(R.id.img_avatar_dynamic_background);
        avatarSpinner = (ProgressWheel) this.findViewById(R.id.img_avatar_spinner);
        dashboardScrollView = (ScrollView) this.findViewById(R.id.dashboard_scroll_view);
        slidingLayoutHeader.setClickable(false);
        dashboardScrollShadow = this.findViewById(R.id.dashboard_scroll_shadow);
        avatarView = (ImageView) avatarViewContainer.findViewById(R.id.img_avatar);

//        accessibilityUtil = AccessibilityUtil.getInstance(getContext());
//        if (accessibilityUtil.isVoiceOverEnabled()) {
//            dashboardScrollView.setEnabled(false);
//        }
        setAvatarDrawable(avatarDrawableResId);
    }

    private void setListeners() {
        this.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                updateDynamicAvatarBackground(slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
                switch (newState) {
                    case EXPANDED:
                        onPanelExpanded();
                        break;
                    case COLLAPSED:
                        onPanelCollapsed();
                        break;
                    case HIDDEN:
                        resetDashboardScroll();
                        break;
                }
            }

            private void onPanelExpanded() {
                isFirstLaunch = false;
                setDragView(slidingLayoutHeader);
                avatarImage.setClickable(false);
                avatarImage.setFocusable(true);
                slidingLayoutHeader.setClickable(true);
//                AccessibilityUtil.addButtonSuffix(avatarImage, R.string.accessibility_open_finder_button);
//                if (accessibilityUtil.isVoiceOverEnabled()) {
//                    dashboardScrollView.setEnabled(true);
//                }

                if (dashboardListener != null) {
                    dashboardListener.onDashboardOpen();
                }
            }

            private void onPanelCollapsed() {
                setAnchorPoint(1f);
                hideKeyboard();
                isFirstLaunch = false;
                setDragView(R.id.img_avatar);
                avatarImage.setEnabled(true);
                avatarImage.setClickable(true);
                slidingLayoutHeader.setClickable(false);
//                avatarImage.setContentDescription(getContext().getString(R.string.accessibility_open_dashboard_button));
                resetDashboardScroll();

                if (dashboardListener != null) {
                    dashboardListener.onDashboardClose();
                }
            }
        });

        dashboardScrollView.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);

        //Needed if the shadow has to be shown only if scrolled (can be CPU consuming)
        dashboardScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
                    showDashboard();
                    return true;
                }
                if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    avatarImage.setEnabled(false);
                    refreshDashboardScrollShadow();
                    if (dashboardListener != null) {
                        dashboardListener.onDashboardMovingAfterClick();
                    }
                    return false;
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    avatarImage.setEnabled(true);
                }
                return false;
            }
        });
    }

    /**
     * On initial run, the dashboard should be shown with tap to explore.
     * If the app is relaunched through icon, the dash should show.
     */
    public void setFirstLaunchState(final Float welcomePosition) {
        avatarImage.setClickable(true);
        avatarImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (dashboardListener != null) {
                    dashboardListener.onAvatarClicked();
                }
                if (isFirstLaunch) {
                    hideDashboard();
                }
                return isFirstLaunch;
            }
        });
        if (dashboardAnchor == null) {
            this.post(new Runnable() {
                @Override
                public void run() {
                    if (welcomePosition != null) {
                        dashboardAnchor = welcomePosition / dashboardScrollView.getHeight();
                    } else {
                        dashboardAnchor = 0.5f;
                    }
                    setAnchorPoint(dashboardAnchor);
                    setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                    avatarView.setContentDescription("accessibility_open_finder_button");
                    updateDynamicAvatarBackground(1f);
                    mainContent.setTranslationY(getCurrentParallaxOffset());
                    mainContent.invalidate();
                }
            });
        } else {
            setAnchorPoint(dashboardAnchor);
            this.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                    setAnchorPoint(1f);
                }
            }, 100);
        }
    }

    /**
     * Updates the background of the avatar to simulate different speeds between the avatar icon and the dashboard
     *
     * @param slideOffset
     */
    private void updateDynamicAvatarBackground(float slideOffset) {
        int maxHeight = avatarViewContainer.getHeight() / 2;
        Float dashboardAnchor = this.dashboardAnchor;
        if (dashboardAnchor == null) {
            dashboardAnchor = 0.5f;
        }
        if (slideOffset < dashboardAnchor) {
            slideOffset = slideOffset / dashboardAnchor;
        } else {
            slideOffset = 1;
        }
        int height = (int) (maxHeight * slideOffset);
        avatarViewBackground.getLayoutParams().height = height;
        avatarViewBackground.requestLayout();
    }

    private void refreshDashboardScrollShadow() {
        if (dashboardScrollView.getScrollY() > 5) {
            dashboardScrollShadow.setVisibility(View.VISIBLE);
        } else {
            dashboardScrollShadow.setVisibility(View.GONE);
        }
    }

    public Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        if (dashboardAnchor != null) {
            state.putFloat(KEY_DASHBOARD_ANCHOR, dashboardAnchor);
        }
        state.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        state.putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch);
        return state;
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable instanceof Bundle) {
            Bundle state = (Bundle) parcelable;
            super.onRestoreInstanceState(state.getParcelable(INSTANCE_STATE));
            this.dashboardAnchor = state.getFloat(KEY_DASHBOARD_ANCHOR);
            if (this.dashboardAnchor == 0) {
                this.dashboardAnchor = null;
            }
            isFirstLaunch = state.getBoolean(KEY_FIRST_LAUNCH);
            if (isFirstLaunch) {
                setFirstLaunchState(null);
            }
            if (isDashboardVisible()) {
                refreshDashboardScrollShadow();
                this.post(new Runnable() {
                    @Override
                    public void run() {
                        avatarViewBackground.getLayoutParams().height = avatarViewContainer.getHeight() / 2;
                        mainContent.setTranslationY(getCurrentParallaxOffset());
                        avatarViewBackground.requestLayout();
                        mainContent.invalidate();
                    }
                });
            }
        }
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(dashboardScrollView.getWindowToken(), 0);
    }

    /**
     * This method hide the dashboard
     */
    public void hideDashboard() {
        setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    /**
     * This method show the dashboard
     */
    public void showDashboard() {
        setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }


    /**
     * Determine if dashboard is visible.
     *
     * @return true if visisble
     */
    public boolean isDashboardVisible() {
        return getPanelState() != SlidingUpPanelLayout.PanelState.COLLAPSED;
    }

    public boolean isHalfDashboardVisible() {
        return getPanelState() == PanelState.ANCHORED;
    }

    public void notifyAccessibilityStateChanged(boolean enabled) {
        if (isHalfDashboardVisible()) {
            dashboardScrollView.setEnabled(!enabled);
        }
    }

    /**
     * Scroll to spinner and start or stop animation.
     */
    public void toggleAvatarProgressIndicator(boolean visible) {
        if (visible) {
            avatarSpinner.setVisibility(View.VISIBLE);
            avatarSpinner.spin();
        } else {
            avatarSpinner.stopSpinning();
            avatarSpinner.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the avatar drawable, by setting the drawable Id
     *
     * @param drawableId
     */
    public void setAvatarDrawable(int drawableId) {
        avatarView.setImageResource(drawableId);
    }

    /**
     * Sets the avatar drawable, by setting the drawable url. Use the default as placeholder.
     *
     * @param avatarUrl
     */
    public void setAvatarDrawable(String avatarUrl) {
        // Picasso.with(getContext()).load(avatarUrl).placeholder(avatarDrawableResId).fit().transform(new CropCircleTransformation()).into(avatarView);
    }

    /**
     * Sets the dashboard listener
     *
     * @param listener
     */
    public void setDashboardListener(DashboardListener listener) {
        this.dashboardListener = listener;
    }

    /**
     * Resets the dashboard scroll to origin
     */
    public void resetDashboardScroll() {
        dashboardScrollView.scrollTo(0, 0);
        refreshDashboardScrollShadow();
    }

    /**
     * Toggles the avatar visibility
     *
     * @param visible true, indicates if the avatar should remain visible
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void toggleAvatarVisibility(boolean visible) {
        float avatarHeight = 50f;//getResources().getDimension(R.dimen.map_avatar_height);
        avatarViewContainer.animate()
                .translationY(visible ? 0 : avatarHeight)
                .setInterpolator(new LinearInterpolator()).setDuration(fastAnimationDuration);
        avatarViewContainer.setImportantForAccessibility(visible ? IMPORTANT_FOR_ACCESSIBILITY_YES : IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    /**
     * Toggles the avatar visibility without animation
     *
     * @param visible true, indicates if the avatar should remain visible
     */
    public void setAvatarVisibility(boolean visible) {
        avatarViewContainer.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * The avatar view starts the animation sent as a parameter
     *
     * @param animation
     */

    public void startAvatarAnimation(Animation animation) {
        avatarViewContainer.startAnimation(animation);
    }

    /**
     * Returns the dashboard container id to insert the dashboard fragment
     *
     * @return
     */
    public int getDashboardContainerId() {
        return R.id.fragment_dashboard;
    }

    /**
     * Adds the dashboard to the scroll container inflating the given layout
     *
     * @param dashboardLayoutId
     */
    public void setDashboard(int dashboardLayoutId) {
        Context context = getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup dashboardContainer = (ViewGroup) this.findViewById(R.id.fragment_dashboard);
        inflater.inflate(dashboardLayoutId, dashboardContainer, true);
    }

    /**
     * Adds the dashboard view to the scroll container
     *
     * @param dashboardView
     */
    public void setDashboard(View dashboardView) {
        ViewGroup dashboardContainer = (ViewGroup) this.findViewById(R.id.fragment_dashboard);
        dashboardContainer.addView(dashboardView);
    }

    /**
     * Listener for dashboard actions.
     */
    public interface DashboardListener {
        /**
         * Executed on dashboard open
         */
        void onDashboardOpen();

        /**
         * Executed on dashboard close
         */
        void onDashboardClose();

        /**
         * Executed on avatar clicked.
         */
        void onAvatarClicked();

        /**
         * Executed on dashboard moving.
         */
        void onDashboardMovingAfterClick();
    }
}