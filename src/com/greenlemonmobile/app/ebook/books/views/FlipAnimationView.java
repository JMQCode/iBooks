/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.greenlemonmobile.app.ebook.books.views;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.greenlemonmobile.app.ebook.books.views.ScaleGestureDetector.OnScaleGestureListener;

/**
 * OpenGL ES View.
 * 
 * @author harism
 */
public class FlipAnimationView extends GLSurfaceView implements FlipAnimationRenderer.Observer, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, OnScaleGestureListener {
    /**
     * Provider for feeding 'book' with bitmaps which are used for rendering
     * pages.
     */
    public interface PageProvider {

        /**
         * Return number of pages available.
         */
        public int getPageCount();

        /**
         * Called once new bitmaps/textures are needed. Width and height are in
         * pixels telling the size it will be drawn on screen and following them
         * ensures that aspect ratio remains. But it's possible to return bitmap
         * of any size though. You should use provided CurlPage for storing page
         * information for requested page number.<br/>
         * <br/>
         * Index is a number between 0 and getBitmapCount() - 1.
         */
        public void updatePage(FlipAnimationPage page, int width, int height, int index);

        public void prepareNextPage();

        public void preparePreviousPage();
    }
    
    /**
     * Simple holder for pointer position.
     */
    private class PointerPosition {
        PointF mPos = new PointF();
        float mPressure;
    }

    /**
     * Observer interface for handling CurlView size changes.
     */
    public interface SizeChangedObserver {

        /**
         * Called once CurlView size changes.
         */
        public void onSizeChanged(int width, int height);
    }

    public static final int ANIMATION_CURL = 1;
    public static final int ANIMATION_SLIDE = 2;

    // Curl state. We are flipping none, left or right page.
    private static final int CURL_LEFT = 1;
    private static final int CURL_NONE = 0;
    private static final int CURL_RIGHT = 2;

    // Constants for mAnimationTargetEvent.
    private static final int SET_CURL_TO_LEFT = 1;
    private static final int SET_CURL_TO_RIGHT = 2;

    // Shows one page at the center of view.
    public static final int SHOW_ONE_PAGE = 1;
    // Shows two pages side by side.
    public static final int SHOW_TWO_PAGES = 2;
    
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    private boolean mAllowLastPageCurl = true;

    private boolean mAnimate = false;
    private boolean mAnimateEnabled = false;
    private long mAnimationDurationTime = 300;
    private PointF mAnimationSource = new PointF();
    private long mAnimationStartTime;
    private PointF mAnimationTarget = new PointF();
    private int mAnimationTargetEvent;

    // animation curl related members
    private PointF mCurlDir = new PointF();
    private PointF mCurlPos = new PointF();
    private int mCurlState = CURL_NONE;

    // Current bitmap index. This is always showed as front of right page.
    private int mCurrentIndex = 0;

    // Start position for dragging.
    private PointF mDragStartPos = new PointF();
    private PointF mDragStartRawPos = new PointF();

    private boolean mEnableTouchPressure = false;
    // Bitmap size. These are updated from renderer once it's initialized.
    private int mPageBitmapHeight = -1;

    private int mPageBitmapWidth = -1;
    // Page meshes. Left and right meshes are 'static' while curl is used to
    // show page flipping.
    private FlipAnimationMesh mCurPage;
    private FlipAnimationMesh mLeftPage;
    private FlipAnimationMesh mRightPage;
    private PageProvider mPageProvider;

    private PointerPosition mPointerPos = new PointerPosition();

    private FlipAnimationRenderer mRenderer;
    private boolean mRenderLeftPage = true;
    private SizeChangedObserver mSizeChangedObserver;

    // One page is the default.
    private int mViewMode = SHOW_ONE_PAGE;

    private int mAnimationType = ANIMATION_CURL;
    
    private float mScale = 1.0f;
    private boolean mScaling = false;
    
    private boolean mStartMoving = false;

    /**
     * Default constructor.
     */
    public FlipAnimationView(Context ctx) {
        super(ctx);
        init(ctx);
    }

    /**
     * Default constructor.
     */
    public FlipAnimationView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init(ctx);
    }

    /**
     * Default constructor.
     */
    public FlipAnimationView(Context ctx, AttributeSet attrs, int defStyle) {
        this(ctx, attrs);
    }

    /**
     * Get current page index. Page indices are zero based values presenting
     * page being shown on right side of the book.
     */
    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    /**
     * Initialize method.
     */
    private void init(Context ctx) {
        mRenderer = new FlipAnimationRenderer(this);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        mGestureDetector = new GestureDetector(ctx, this);
        mGestureDetector.setIsLongpressEnabled(true);
        mGestureDetector.setOnDoubleTapListener(this);
        
        mScaleGestureDetector = new ScaleGestureDetector(ctx, this);

        // Even though left and right pages are static we have to allocate room
        // for curl on them too as we are switching meshes. Another way would be
        // to swap texture ids only.
        mLeftPage = new FlipAnimationMesh(10);
        mRightPage = new FlipAnimationMesh(10);
        mCurPage = new FlipAnimationMesh(10);
        mLeftPage.setFlipTexture(true);
        mRightPage.setFlipTexture(false);
    }

    @Override
    public void onDrawFrame() {
        // We are not animating.
        if (mAnimate == false) {
            return;
        }

        switch (mAnimationType) {
            case ANIMATION_CURL:
                onDrawCurlFrame();
                break;
            case ANIMATION_SLIDE:
                onDrawSlideFrame();
                break;
        }
    }

    @Override
    public void onPageSizeChanged(int width, int height) {
        mPageBitmapWidth = width;
        mPageBitmapHeight = height;
        updatePages();
        requestRender();
    }

    @Override
    public void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        requestRender();
        if (mSizeChangedObserver != null) {
            mSizeChangedObserver.onSizeChanged(w, h);
        }
    }

    @Override
    public void onSurfaceCreated() {
        // In case surface is recreated, let page meshes drop allocated texture
        // ids and ask for new ones. There's no need to set textures here as
        // onPageSizeChanged should be called later on.
        mLeftPage.resetTexture();
        mRightPage.resetTexture();
        mCurPage.resetTexture();
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {        
        mScaleGestureDetector.onTouchEvent(me);
        
        switch (me.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mStartMoving = false;
                break;
            case MotionEvent.ACTION_UP:
                mStartMoving = false;
                break;
        }

        if (!mScaling && !mScaleGestureDetector.isInProgress()) {
            switch (mAnimationType) {
                case ANIMATION_CURL:
                    return onTouchCurl(me);
                case ANIMATION_SLIDE:
                    return onTouchSlide(me);
            }
        }
        
        if (!mScaling)
            mGestureDetector.onTouchEvent(me);
        return true;
    }
    /**
     * Allow the last page to curl.
     */
    public void setAllowLastPageCurl(boolean allowLastPageCurl) {
        mAllowLastPageCurl = allowLastPageCurl;
    }

    /**
     * Sets background color - or OpenGL clear color to be more precise. Color
     * is a 32bit value consisting of 0xAARRGGBB and is extracted using
     * android.graphics.Color eventually.
     */
    @Override
    public void setBackgroundColor(int color) {
        mRenderer.setBackgroundColor(color);
        requestRender();
    }

    /**
     * Set current page index. Page indices are zero based values presenting
     * page being shown on right side of the book. E.g if you set value to 4;
     * right side front facing bitmap will be with index 4, back facing 5 and
     * for left side page index 3 is front facing, and index 2 back facing (once
     * page is on left side it's flipped over). Current index is rounded to
     * closest value divisible with 2.
     */
    public void setCurrentIndex(int index) {
        if (mPageProvider == null || index < 0) {
            mCurrentIndex = 0;
        } else {
            if (mAllowLastPageCurl) {
                mCurrentIndex = Math.min(index, mPageProvider.getPageCount());
            } else {
                mCurrentIndex = Math.min(index,
                        mPageProvider.getPageCount() - 1);
            }
        }
        mAnimateEnabled = true;
        updatePages();
        requestRender();
    }

    /**
     * If set to true, touch event pressure information is used to adjust curl
     * radius. The more you press, the flatter the curl becomes. This is
     * somewhat experimental and results may vary significantly between devices.
     * On emulator pressure information seems to be flat 1.0f which is maximum
     * value and therefore not very much of use.
     */
    public void setEnableTouchPressure(boolean enableTouchPressure) {
        mEnableTouchPressure = enableTouchPressure;
    }

    /**
     * Set margins (or padding). Note: margins are proportional. Meaning a value
     * of .1f will produce a 10% margin.
     */
    public void setMargins(float left, float top, float right, float bottom) {
        mRenderer.setMargins(left, top, right, bottom);
    }

    /**
     * Update/set page provider.
     */
    public void setPageProvider(PageProvider pageProvider) {
        mPageProvider = pageProvider;
        mCurrentIndex = 0;
        updatePages();
        requestRender();
    }

    /**
     * Setter for whether left side page is rendered. This is useful mostly for
     * situations where right (main) page is aligned to left side of screen and
     * left page is not visible anyway.
     */
    public void setRenderLeftPage(boolean renderLeftPage) {
        mRenderLeftPage = renderLeftPage;
    }

    /**
     * Sets SizeChangedObserver for this View. Call back method is called from
     * this View's onSizeChanged method.
     */
    public void setSizeChangedObserver(SizeChangedObserver observer) {
        mSizeChangedObserver = observer;
    }

    /**
     * Sets view mode. Value can be either SHOW_ONE_PAGE or SHOW_TWO_PAGES. In
     * former case right page is made size of display, and in latter case two
     * pages are laid on visible area.
     */
    public void setViewMode(int viewMode) {
        switch (viewMode) {
            case SHOW_ONE_PAGE:
                mViewMode = viewMode;
                mLeftPage.setFlipTexture(true);
                mRenderer.setViewMode(FlipAnimationRenderer.SHOW_ONE_PAGE);
                break;
            case SHOW_TWO_PAGES:
                mViewMode = viewMode;
                mLeftPage.setFlipTexture(false);
                mRenderer.setViewMode(FlipAnimationRenderer.SHOW_TWO_PAGES);
                break;
        }
    }
    
    public void setAnimationType(int animType) {
        mAnimationType = animType;
        switch (mAnimationType) {
            case ANIMATION_SLIDE:
                mViewMode = SHOW_ONE_PAGE;
                mLeftPage.setFlipTexture(true);
                mRenderer.setViewMode(FlipAnimationRenderer.SHOW_ONE_PAGE);
                break;
        }
    }

    private boolean onTouchSlide(MotionEvent me) {
        if (mAnimate || mPageProvider == null || !mAnimateEnabled) {
            return false;
        }
        return true;
    }

    private void onDrawSlideFrame() {

    }

    private boolean onTouchCurl(MotionEvent me) {
        if (mAnimate || mPageProvider == null || !mAnimateEnabled) {
            return false;
        }
        
        // We need page rects quite extensively so get them for later use.
        RectF rightRect = mRenderer.getPageRect(FlipAnimationRenderer.PAGE_RIGHT);
        RectF leftRect = mRenderer.getPageRect(FlipAnimationRenderer.PAGE_LEFT);

        // Store pointer position.
        mPointerPos.mPos.set(me.getX(), me.getY());
        mRenderer.translate(mPointerPos.mPos);
        if (mEnableTouchPressure) {
            mPointerPos.mPressure = me.getPressure();
        } else {
            mPointerPos.mPressure = 0.8f;
        }

        switch (me.getAction()) {
            case MotionEvent.ACTION_DOWN: {

                // Once we receive pointer down event its position is mapped to
                // right or left edge of page and that'll be the position from
                // where
                // user is holding the paper to make curl happen.
                mDragStartPos.set(mPointerPos.mPos);
                mDragStartRawPos.set(me.getX(), me.getY());

                // First we make sure it's not over or below page. Pages are
                // supposed to be same height so it really doesn't matter do we
                // use
                // left or right one.
                if (mDragStartPos.y > rightRect.top) {
                    mDragStartPos.y = rightRect.top;
                } else if (mDragStartPos.y < rightRect.bottom) {
                    mDragStartPos.y = rightRect.bottom;
                }
                
                mStartMoving = false;
            }
            case MotionEvent.ACTION_MOVE: {
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (!mStartMoving && Math.abs(me.getX() - mDragStartRawPos.x) > slop && Math.abs(me.getY() - mDragStartRawPos.y) > slop) {
                    mStartMoving = true;
                    
                    // Then we have to make decisions for the user whether curl is
                    // going
                    // to happen from left or right, and on which page.
                    if (mViewMode == SHOW_TWO_PAGES) {
                        // If we have an open book and pointer is on the left from
                        // right
                        // page we'll mark drag position to left edge of left page.
                        // Additionally checking mCurrentIndex is higher than zero
                        // tells
                        // us there is a visible page at all.
                        if (mDragStartPos.x < rightRect.left && mCurrentIndex > 0) {
                            mDragStartPos.x = leftRect.left;
                            startCurl(CURL_LEFT);
                        }
                        // Otherwise check pointer is on right page's side.
                        else if (mDragStartPos.x >= rightRect.left
                                && mCurrentIndex < mPageProvider.getPageCount()) {
                            mDragStartPos.x = rightRect.right;
                            if (!mAllowLastPageCurl
                                    && mCurrentIndex >= mPageProvider.getPageCount() - 1) {
                                return false;
                            }
                            startCurl(CURL_RIGHT);
                        }
                    } else if (mViewMode == SHOW_ONE_PAGE) {
                        float halfX = (rightRect.right + rightRect.left) / 2;
                        if (mDragStartPos.x < halfX && mCurrentIndex > 0) {
                            mDragStartPos.x = rightRect.left;
                            startCurl(CURL_LEFT);
                        } else if (mDragStartPos.x >= halfX
                                && mCurrentIndex < mPageProvider.getPageCount()) {
                            mDragStartPos.x = rightRect.right;
                            if (!mAllowLastPageCurl
                                    && mCurrentIndex >= mPageProvider.getPageCount() - 1) {
                                return false;
                            }
                            startCurl(CURL_RIGHT);
                        } else if (mDragStartPos.x < halfX && mCurrentIndex == 0) {
                            mPageProvider.preparePreviousPage();
                        }
                    }
                    // If we have are in curl state, let this case clause flow
                    // through
                    // to next one. We have pointer position and drag position
                    // defined
                    // and this will create first render request given these points.
                    if (mCurlState == CURL_NONE) {
                        return false;
                    }
                } else {
                    updateCurlPos(mPointerPos);
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (mCurlState == CURL_LEFT || mCurlState == CURL_RIGHT) {
                    // Animation source is the point from where animation
                    // starts.
                    // Also it's handled in a way we actually simulate touch
                    // events
                    // meaning the output is exactly the same as if user drags
                    // the
                    // page to other side. While not producing the best looking
                    // result (which is easier done by altering curl position
                    // and/or
                    // direction directly), this is done in a hope it made code
                    // a
                    // bit more readable and easier to maintain.
                    mAnimationSource.set(mPointerPos.mPos);
                    mAnimationStartTime = System.currentTimeMillis();

                    // Given the explanation, here we decide whether to simulate
                    // drag to left or right end.
                    if ((mViewMode == SHOW_ONE_PAGE && mPointerPos.mPos.x > (rightRect.left + rightRect.right) / 2)
                            || mViewMode == SHOW_TWO_PAGES
                            && mPointerPos.mPos.x > rightRect.left) {
                        // On right side target is always right page's right
                        // border.
                        mAnimationTarget.set(mDragStartPos);
                        mAnimationTarget.x = mRenderer
                                .getPageRect(FlipAnimationRenderer.PAGE_RIGHT).right;
                        mAnimationTargetEvent = SET_CURL_TO_RIGHT;
                    } else {
                        // On left side target depends on visible pages.
                        mAnimationTarget.set(mDragStartPos);
                        if (mCurlState == CURL_RIGHT || mViewMode == SHOW_TWO_PAGES) {
                            mAnimationTarget.x = leftRect.left;
                        } else {
                            mAnimationTarget.x = rightRect.left;
                        }
                        mAnimationTargetEvent = SET_CURL_TO_LEFT;
                    }
                    mAnimate = true;
                    requestRender();
                }
                break;
            }
        }
        return true;
    }

    private void onDrawCurlFrame() {
        long currentTime = System.currentTimeMillis();
        // If animation is done.
        if (currentTime >= mAnimationStartTime + mAnimationDurationTime) {
            if (mAnimationTargetEvent == SET_CURL_TO_RIGHT) {
                // Switch curled page to right.
                FlipAnimationMesh right = mCurPage;
                FlipAnimationMesh curl = mRightPage;
                right.setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_RIGHT));
                right.setFlipTexture(false);
                right.reset();
                mRenderer.removeCurlMesh(curl);
                mCurPage = curl;
                mRightPage = right;
                // If we were curling left page update current index.
                if (mCurlState == CURL_LEFT) {
                    --mCurrentIndex;
                    mAnimateEnabled = false;
                    mPageProvider.preparePreviousPage();
                }
            } else if (mAnimationTargetEvent == SET_CURL_TO_LEFT) {
                // Switch curled page to left.
                FlipAnimationMesh left = mCurPage;
                FlipAnimationMesh curl = mLeftPage;
                left.setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_LEFT));
                left.setFlipTexture(true);
                left.reset();
                mRenderer.removeCurlMesh(curl);
                if (!mRenderLeftPage) {
                    mRenderer.removeCurlMesh(left);
                }
                mCurPage = curl;
                mLeftPage = left;
                // If we were curling right page update current index.
                if (mCurlState == CURL_RIGHT) {
                    ++mCurrentIndex;
                    mAnimateEnabled = false;
                    mPageProvider.prepareNextPage();
                }
            }
            mCurlState = CURL_NONE;
            mAnimate = false;
            requestRender();
        } else {
            mPointerPos.mPos.set(mAnimationSource);
            float t = 1f - ((float) (currentTime - mAnimationStartTime) / mAnimationDurationTime);
            t = 1f - (t * t * t * (3 - 2 * t));
            mPointerPos.mPos.x += (mAnimationTarget.x - mAnimationSource.x) * t;
            mPointerPos.mPos.y += (mAnimationTarget.y - mAnimationSource.y) * t;
            updateCurlPos(mPointerPos);
        }
    }

    /**
     * Sets mPageCurl curl position.
     */
    private void setCurlPos(PointF curlPos, PointF curlDir, double radius) {

        // First reposition curl so that page doesn't 'rip off' from book.
        if (mCurlState == CURL_RIGHT
                || (mCurlState == CURL_LEFT && mViewMode == SHOW_ONE_PAGE)) {
            RectF pageRect = mRenderer.getPageRect(FlipAnimationRenderer.PAGE_RIGHT);
            if (curlPos.x >= pageRect.right) {
                mCurPage.reset();
                requestRender();
                return;
            }
            if (curlPos.x < pageRect.left) {
                curlPos.x = pageRect.left;
            }
            if (curlDir.y != 0) {
                float diffX = curlPos.x - pageRect.left;
                float leftY = curlPos.y + (diffX * curlDir.x / curlDir.y);
                if (curlDir.y < 0 && leftY < pageRect.top) {
                    curlDir.x = curlPos.y - pageRect.top;
                    curlDir.y = pageRect.left - curlPos.x;
                } else if (curlDir.y > 0 && leftY > pageRect.bottom) {
                    curlDir.x = pageRect.bottom - curlPos.y;
                    curlDir.y = curlPos.x - pageRect.left;
                }
            }
        } else if (mCurlState == CURL_LEFT) {
            RectF pageRect = mRenderer.getPageRect(FlipAnimationRenderer.PAGE_LEFT);
            if (curlPos.x <= pageRect.left) {
                mCurPage.reset();
                requestRender();
                return;
            }
            if (curlPos.x > pageRect.right) {
                curlPos.x = pageRect.right;
            }
            if (curlDir.y != 0) {
                float diffX = curlPos.x - pageRect.right;
                float rightY = curlPos.y + (diffX * curlDir.x / curlDir.y);
                if (curlDir.y < 0 && rightY < pageRect.top) {
                    curlDir.x = pageRect.top - curlPos.y;
                    curlDir.y = curlPos.x - pageRect.right;
                } else if (curlDir.y > 0 && rightY > pageRect.bottom) {
                    curlDir.x = curlPos.y - pageRect.bottom;
                    curlDir.y = pageRect.right - curlPos.x;
                }
            }
        }

        // Finally normalize direction vector and do rendering.
        double dist = Math.sqrt(curlDir.x * curlDir.x + curlDir.y * curlDir.y);
        if (dist != 0) {
            curlDir.x /= dist;
            curlDir.y /= dist;
            mCurPage.curl(curlPos, curlDir, radius);
        } else {
            mCurPage.reset();
        }

        requestRender();
    }

    /**
     * Switches meshes and loads new bitmaps if available. Updated to support 2
     * pages in landscape
     */
    private void startCurl(int page) {
        switch (page) {

        // Once right side page is curled, first right page is assigned into
        // curled page. And if there are more bitmaps available new bitmap is
        // loaded into right side mesh.
            case CURL_RIGHT: {
                // Remove meshes from renderer.
                mRenderer.removeCurlMesh(mLeftPage);
                mRenderer.removeCurlMesh(mRightPage);
                mRenderer.removeCurlMesh(mCurPage);

                // We are curling right page.
                FlipAnimationMesh curl = mRightPage;
                mRightPage = mCurPage;
                mCurPage = curl;

                if (mCurrentIndex > 0) {
                    mLeftPage.setFlipTexture(true);
                    mLeftPage.setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_LEFT));
                    mLeftPage.reset();
                    if (mRenderLeftPage) {
                        mRenderer.addCurlMesh(mLeftPage);
                    }
                }
                if (mCurrentIndex < mPageProvider.getPageCount() - 1) {
                    updatePage(mRightPage.getTexturePage(), mCurrentIndex + 1);
                    mRightPage.setRect(mRenderer
                            .getPageRect(FlipAnimationRenderer.PAGE_RIGHT));
                    mRightPage.setFlipTexture(false);
                    mRightPage.reset();
                    mRenderer.addCurlMesh(mRightPage);
                }

                // Add curled page to renderer.
                mCurPage.setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_RIGHT));
                mCurPage.setFlipTexture(false);
                mCurPage.reset();
                mRenderer.addCurlMesh(mCurPage);

                mCurlState = CURL_RIGHT;
                break;
            }

            // On left side curl, left page is assigned to curled page. And if
            // there are more bitmaps available before currentIndex, new bitmap
            // is loaded into left page.
            case CURL_LEFT: {
                // Remove meshes from renderer.
                mRenderer.removeCurlMesh(mLeftPage);
                mRenderer.removeCurlMesh(mRightPage);
                mRenderer.removeCurlMesh(mCurPage);

                // We are curling left page.
                FlipAnimationMesh curl = mLeftPage;
                mLeftPage = mCurPage;
                mCurPage = curl;

                if (mCurrentIndex > 1) {
                    updatePage(mLeftPage.getTexturePage(), mCurrentIndex - 2);
                    mLeftPage.setFlipTexture(true);
                    mLeftPage
                            .setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_LEFT));
                    mLeftPage.reset();
                    if (mRenderLeftPage) {
                        mRenderer.addCurlMesh(mLeftPage);
                    }
                }

                // If there is something to show on right page add it to
                // renderer.
                if (mCurrentIndex < mPageProvider.getPageCount()) {
                    mRightPage.setFlipTexture(false);
                    mRightPage.setRect(mRenderer
                            .getPageRect(FlipAnimationRenderer.PAGE_RIGHT));
                    mRightPage.reset();
                    mRenderer.addCurlMesh(mRightPage);
                }

                // How dragging previous page happens depends on view mode.
                if (mViewMode == SHOW_ONE_PAGE
                        || (mCurlState == CURL_LEFT && mViewMode == SHOW_TWO_PAGES)) {
                    mCurPage.setRect(mRenderer
                            .getPageRect(FlipAnimationRenderer.PAGE_RIGHT));
                    mCurPage.setFlipTexture(false);
                } else {
                    mCurPage
                            .setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_LEFT));
                    mCurPage.setFlipTexture(true);
                }
                mCurPage.reset();
                mRenderer.addCurlMesh(mCurPage);

                mCurlState = CURL_LEFT;
                break;
            }

        }
    }

    /**
     * Updates curl position.
     */
    private void updateCurlPos(PointerPosition pointerPos) {

        // Default curl radius.
        double radius = mRenderer.getPageRect(CURL_RIGHT).width() / 3;
        // TODO: This is not an optimal solution. Based on feedback received so
        // far; pressure is not very accurate, it may be better not to map
        // coefficient to range [0f, 1f] but something like [.2f, 1f] instead.
        // Leaving it as is until get my hands on a real device. On emulator
        // this doesn't work anyway.
        radius *= Math.max(1f - pointerPos.mPressure, 0f);
        // NOTE: Here we set pointerPos to mCurlPos. It might be a bit confusing
        // later to see e.g "mCurlPos.x - mDragStartPos.x" used. But it's
        // actually pointerPos we are doing calculations against. Why? Simply to
        // optimize code a bit with the cost of making it unreadable. Otherwise
        // we had to this in both of the next if-else branches.
        mCurlPos.set(pointerPos.mPos);

        // If curl happens on right page, or on left page on two page mode,
        // we'll calculate curl position from pointerPos.
        if (mCurlState == CURL_RIGHT
                || (mCurlState == CURL_LEFT && mViewMode == SHOW_TWO_PAGES)) {

            mCurlDir.x = mCurlPos.x - mDragStartPos.x;
            mCurlDir.y = mCurlPos.y - mDragStartPos.y;
            float dist = (float) Math.sqrt(mCurlDir.x * mCurlDir.x + mCurlDir.y
                    * mCurlDir.y);

            // Adjust curl radius so that if page is dragged far enough on
            // opposite side, radius gets closer to zero.
            float pageWidth = mRenderer.getPageRect(FlipAnimationRenderer.PAGE_RIGHT)
                    .width();
            double curlLen = radius * Math.PI;
            if (dist > (pageWidth * 2) - curlLen) {
                curlLen = Math.max((pageWidth * 2) - dist, 0f);
                radius = curlLen / Math.PI;
            }

            // Actual curl position calculation.
            if (dist >= curlLen) {
                double translate = (dist - curlLen) / 2;
                if (mViewMode == SHOW_TWO_PAGES) {
                    mCurlPos.x -= mCurlDir.x * translate / dist;
                } else {
                    float pageLeftX = mRenderer
                            .getPageRect(FlipAnimationRenderer.PAGE_RIGHT).left;
                    radius = Math.max(Math.min(mCurlPos.x - pageLeftX, radius),
                            0f);
                }
                mCurlPos.y -= mCurlDir.y * translate / dist;
            } else {
                double angle = Math.PI * Math.sqrt(dist / curlLen);
                double translate = radius * Math.sin(angle);
                mCurlPos.x += mCurlDir.x * translate / dist;
                mCurlPos.y += mCurlDir.y * translate / dist;
            }
        }
        // Otherwise we'll let curl follow pointer position.
        else if (mCurlState == CURL_LEFT) {

            // Adjust radius regarding how close to page edge we are.
            float pageLeftX = mRenderer.getPageRect(FlipAnimationRenderer.PAGE_RIGHT).left;
            radius = Math.max(Math.min(mCurlPos.x - pageLeftX, radius), 0f);

            float pageRightX = mRenderer.getPageRect(FlipAnimationRenderer.PAGE_RIGHT).right;
            mCurlPos.x -= Math.min(pageRightX - mCurlPos.x, radius);
            mCurlDir.x = mCurlPos.x + mDragStartPos.x;
            mCurlDir.y = mCurlPos.y - mDragStartPos.y;
        }

        setCurlPos(mCurlPos, mCurlDir, radius);
    }

    /**
     * Updates given CurlPage via PageProvider for page located at index.
     */
    private void updatePage(FlipAnimationPage page, int index) {
        // First reset page to initial state.
        page.reset();
        // Ask page provider to fill it up with bitmaps and colors.
        mPageProvider.updatePage(page, mPageBitmapWidth, mPageBitmapHeight,
                index);
    }

    /**
     * Updates bitmaps for page meshes.
     */
    private void updatePages() {
        if (mPageProvider == null || mPageBitmapWidth <= 0
                || mPageBitmapHeight <= 0) {
            return;
        }

        // Remove meshes from renderer.
        mRenderer.removeCurlMesh(mLeftPage);
        mRenderer.removeCurlMesh(mRightPage);
        mRenderer.removeCurlMesh(mCurPage);

        int leftIdx = mCurrentIndex - 1;
        int rightIdx = mCurrentIndex;
        int curlIdx = -1;
        if (mCurlState == CURL_LEFT) {
            curlIdx = leftIdx;
            --leftIdx;
        } else if (mCurlState == CURL_RIGHT) {
            curlIdx = rightIdx;
            ++rightIdx;
        }

        if (rightIdx >= 0 && rightIdx < mPageProvider.getPageCount()) {
            updatePage(mRightPage.getTexturePage(), rightIdx);
            mRightPage.setFlipTexture(false);
            mRightPage.setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_RIGHT));
            mRightPage.reset();
            mRenderer.addCurlMesh(mRightPage);
        }
        if (leftIdx >= 0 && leftIdx < mPageProvider.getPageCount()) {
            updatePage(mLeftPage.getTexturePage(), leftIdx);
            mLeftPage.setFlipTexture(true);
            mLeftPage.setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_LEFT));
            mLeftPage.reset();
            if (mRenderLeftPage) {
                mRenderer.addCurlMesh(mLeftPage);
            }
        }
        if (curlIdx >= 0 && curlIdx < mPageProvider.getPageCount()) {
            updatePage(mCurPage.getTexturePage(), curlIdx);

            if (mCurlState == CURL_RIGHT) {
                mCurPage.setFlipTexture(true);
                mCurPage.setRect(mRenderer
                        .getPageRect(FlipAnimationRenderer.PAGE_RIGHT));
            } else {
                mCurPage.setFlipTexture(false);
                mCurPage
                        .setRect(mRenderer.getPageRect(FlipAnimationRenderer.PAGE_LEFT));
            }

            mCurPage.reset();
            mRenderer.addCurlMesh(mCurPage);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {        
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = detector.getScaleFactor();
        if (Float.isInfinite(scale) || Float.isNaN(scale))
            return true;
        
        mScaling = true;
        
        mScale *= scale;
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mScale = 1.0f;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mScale = 1.0f;
        mScaling = false;
    }

}
