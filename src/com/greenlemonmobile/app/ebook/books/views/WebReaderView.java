
package com.greenlemonmobile.app.ebook.books.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;

import com.greenlemonmobile.app.ebook.books.model.ReaderSettings;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class WebReaderView extends WebView implements OnTouchListener, OnLongClickListener {

    /** The logging tag. */
    private static final String TAG = "WebReaderView";

    /** Context. */
    protected Context ctx;

    /** The selection bounds. */
    private Rect mSelectionBounds = null;

    /** The previously selected region. */
    protected Region lastSelectedRegion = null;

    /** The selected text. */
    protected String selectedText = "";
    
    /** Identifier for the selection start handle. */
    private final int SELECTION_START_HANDLE = 0;

    /** Identifier for the selection end handle. */
    private final int SELECTION_END_HANDLE = 1;

    /** Selection mode flag. */
    protected boolean inSelectionMode = false;
    
    private OnSizeChangedListener mSizeChangedListener;
    
    private OnTouchListener mExternalOnTouchListener;
    
    private SelectionMaskView mSelectionDragLayer;
    
    private ReaderSettings mReaderSettings;
    
    private boolean mJavascriptInjected = false;

    public WebReaderView(Context context) {
        super(context);
        this.ctx = context;
        this.setup(context);
    }

    public WebReaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.ctx = context;
        this.setup(context);

    }

    public WebReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.ctx = context;
        this.setup(context);

    }
    
    public void setOnSizeChangedListener(OnSizeChangedListener sizeChangedListener) {
        mSizeChangedListener = sizeChangedListener;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(oldw, oldh, oldw, oldh);
        
        if (mSizeChangedListener != null)
            mSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
    }

    // *****************************************************
    // *
    // * Touch Listeners
    // *
    // *****************************************************

    private boolean mScrolling = false;
    private float mScrollDiffY = 0;
    private float mLastTouchY = 0;
    private float mScrollDiffX = 0;
    private float mLastTouchX = 0;

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (mExternalOnTouchListener != null)
            mExternalOnTouchListener.onTouch(v, event);

        float xPoint = getDensityIndependentValue(event.getX(), ctx)
                / getDensityIndependentValue(this.getScale(), ctx);
        float yPoint = getDensityIndependentValue(event.getY(), ctx)
                / getDensityIndependentValue(this.getScale(), ctx);

        // TODO: Need to update this to use this.getScale() as a factor.

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if(this.isInSelectionMode()) {
                this.endSelectionMode();
                return true;
            }

            String startTouchUrl = String.format("javascript:android.selection.startTouch(%f, %f);", xPoint, yPoint);

            mLastTouchX = xPoint;
            mLastTouchY = yPoint;

//            this.loadUrl(startTouchUrl);

            // Flag scrolling for first touch
            // if(!this.isInSelectionMode())
            // mScrolling = true;

        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            // Check for scrolling flag
            if (!mScrolling) {
                this.endSelectionMode();
            }

            mScrollDiffX = 0;
            mScrollDiffY = 0;
            mScrolling = false;

        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {

            mScrollDiffX += (xPoint - mLastTouchX);
            mScrollDiffY += (yPoint - mLastTouchY);

            mLastTouchX = xPoint;
            mLastTouchY = yPoint;

            // Only account for legitimate movement.
            if (Math.abs(mScrollDiffX) > 10 || Math.abs(mScrollDiffY) > 10) {
                mScrolling = true;

            }

            return (event.getAction() == MotionEvent.ACTION_MOVE);
        }

        // If this is in selection mode, then nothing else should handle this
        // touch
        return false;
    }

    @Override
    public boolean onLongClick(View v) {

        // Tell the javascript to handle this if not in selection mode
        // if(!this.isInSelectionMode()){
//        this.loadUrl("javascript:android.selection.longTouch();");
        mScrolling = true;
        // }

        // Don't let the webview handle it
        return true;
    }
    
    public void dragMove(int selectionHandle, MotionEvent event) {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        RectF rect = new RectF((int)mReaderSettings.mLeftMargin, (int)mReaderSettings.mTopMargin, (int)(width - mReaderSettings.mRightMargin), (int)(height - mReaderSettings.mBottomMargin));
        
        if (!rect.contains(event.getX(), event.getY()))
            return;
        float scale = getDensityIndependentValue(this.getScale(), ctx);
        
        float xPoint = getDensityIndependentValue(event.getX(), ctx)
                / scale;
        float yPoint = getDensityIndependentValue(event.getY(), ctx)
                / scale;

        if (selectionHandle == SELECTION_START_HANDLE) {
            String saveStartString = String.format(
                    "javascript: android.selection.setStartPos(%f, %f);", xPoint, yPoint);
//            this.loadUrl(saveStartString);
        }

        if (selectionHandle == SELECTION_END_HANDLE) {
            String saveEndString = String.format(
                    "javascript: android.selection.setEndPos(%f, %f);", xPoint, yPoint);
//            this.loadUrl(saveEndString);
        }

    }
    
    public void startTouch(MotionEvent event) {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        RectF rect = new RectF((int)mReaderSettings.mLeftMargin, (int)mReaderSettings.mTopMargin, (int)(width - mReaderSettings.mRightMargin), (int)(height - mReaderSettings.mBottomMargin));
        
        if (!rect.contains(event.getX(), event.getY()))
            return;
        float scale = getDensityIndependentValue(this.getScale(), ctx);
        
        float xPoint = getDensityIndependentValue(event.getX(), ctx)
                / scale;
        float yPoint = getDensityIndependentValue(event.getY(), ctx)
                / scale;

        String startTouchUrl = String.format("javascript:android.selection.startTouch(%f, %f);",
                xPoint, yPoint);

        mLastTouchX = xPoint;
        mLastTouchY = yPoint;

//        this.loadUrl(startTouchUrl);
    }
    
    public void onLongClick(MotionEvent event) {
        startTouch(event);
//        this.loadUrl("javascript:android.selection.longTouch();");
    }

    // *****************************************************
    // *
    // * Setup
    // *
    // *****************************************************

    /**
     * Setups up the web view.
     * 
     * @param context
     */
    protected void setup(Context context) {

        // On Touch Listener
        this.setOnLongClickListener(this);
        this.setOnTouchListener(this);

        // Webview setup
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.getSettings().setPluginsEnabled(true);

        // Only hide the scrollbar, not disables the scrolling:
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);

        // Only disabled the horizontal scrolling:
        this.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);

        // Zoom out fully
        // this.getSettings().setLoadWithOverviewMode(true);
        // this.getSettings().setUseWideViewPort(true);

        // Javascript interfaces
//        this.textSelectionJSInterface = new TextSelectionJavascriptInterface(context, this, HandlerExecutor.getUiThreadExecutor());
//        this.addJavascriptInterface(this.textSelectionJSInterface,
//                this.textSelectionJSInterface.getInterfaceName());

//        // Create the selection handles
//        createSelectionLayer(context);

        // Set to the empty region
        Region region = new Region();
        region.setEmpty();
        this.lastSelectedRegion = region;
    }

    // *****************************************************
    // *
    // * Selection Layer Handling
    // *
    // *****************************************************

    /**
     * Creates the selection layer.
     * 
     * @param context
     */
    protected void createSelectionLayer(Context context) {
        
        mSelectionDragLayer = new SelectionMaskView(this, mReaderSettings);

    }

    /**
     * The user has started dragging the selection handles.
     */
    public void startSelectionMode(ArrayList<Rect> handleBounds) {
        if (mSelectionBounds == null)
            return;

        ((ViewGroup) this.getParent()).addView(mSelectionDragLayer, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        mSelectionDragLayer.setShow(true, handleBounds);
    }

    /**
     * The user has stopped dragging the selection handles.
     */
    public void endSelectionMode() {
        mSelectionDragLayer.setShow(false, null);
        ((ViewGroup) this.getParent()).removeView(mSelectionDragLayer);
        mSelectionBounds = null;
//        loadUrl("javascript: android.selection.clearSelection();");
    }

    /**
     * Checks to see if this view is in selection mode.
     * 
     * @return
     */
    public boolean isInSelectionMode() {

        return this.mSelectionDragLayer.getParent() != null;

    }

    // *****************************************************
    // *
    // * Text Selection Javascript Interface Listener
    // *
    // *****************************************************

    /**
     * The selection has changed
     * 
     * @param range
     * @param text
     * @param handleBounds
     * @param menuBounds
     * @param showHighlight
     * @param showUnHighlight
     */
    public void selectionChanged(String text, String handleBounds, String rectString) {
        ArrayList<Rect> rects = new ArrayList<Rect>();
        if (!TextUtils.isEmpty(rectString)) {
            float scale = WebReaderView.getDensityIndependentValue(getScale(), ctx);
            String rectArray[] = rectString.split(",");
            for (int i = 0; i + 1 < rectArray.length; i += 4) {
                int left = (int) (getDensityDependentValue(Integer.valueOf(rectArray[i]).intValue(), ctx) * scale);
                int top = (int) (getDensityDependentValue(Integer.valueOf(rectArray[i + 1]).intValue(), ctx) * scale);
                int right = (int) (getDensityDependentValue(Integer.valueOf(rectArray[i + 2]).intValue(), ctx) * scale);
                int bottom = (int) (getDensityDependentValue(Integer.valueOf(rectArray[i + 3]).intValue(), ctx) * scale);
                
                rects.add(new Rect(left, top, right, bottom));
            }
            mSelectionDragLayer.selectionChanged(rects);
        }
        
        if (!TextUtils.isEmpty(handleBounds)) {
            Rect handleRect = new Rect();
            String rectArray[] = handleBounds.split(",");
            
            float scale = getDensityIndependentValue(this.getScale(), ctx);
            handleRect.left = (int) (getDensityDependentValue(Float.valueOf(rectArray[0]).floatValue(),
                    getContext()) * scale);
            handleRect.top = (int) (getDensityDependentValue(Float.valueOf(rectArray[1]).floatValue(),
                    getContext()) * scale);
            handleRect.right = (int) (getDensityDependentValue(Float.valueOf(rectArray[2]).floatValue(),
                    getContext()) * scale);
            handleRect.bottom = (int) (getDensityDependentValue(Float.valueOf(rectArray[3]).floatValue(),
                    getContext()) * scale);

            this.mSelectionBounds = handleRect;  
        }

        this.selectedText = text;

        if (!this.isInSelectionMode()) {
            this.startSelectionMode(rects);
        }
    }
    
    /**
     * Returns the density dependent value of the given float
     * 
     * @param val
     * @param ctx
     * @return
     */
    public static float getDensityDependentValue(float val, Context ctx) {

        // Get display from context
        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        // Calculate min bound based on metrics
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        return val * (metrics.densityDpi / 160f);

        // return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val,
        // metrics);

    }

    /**
     * Returns the density independent value of the given float
     * 
     * @param val
     * @param ctx
     * @return
     */
    public static float getDensityIndependentValue(float val, Context ctx) {

        // Get display from context
        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        // Calculate min bound based on metrics
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        return val / (metrics.densityDpi / 160f);

        // return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, val,
        // metrics);

    }

    public OnTouchListener getExternalOnTouchListener() {
        return mExternalOnTouchListener;
    }

    public void setExternalOnTouchListener(OnTouchListener mExternalOnTouchListener) {
        this.mExternalOnTouchListener = mExternalOnTouchListener;
    }

    public ReaderSettings getReaderSettings() {
        return mReaderSettings;
    }

    public void setReaderSettings(ReaderSettings settings) {
        this.mReaderSettings = settings;
        
        // Create the selection handles
        createSelectionLayer(getContext());
    }

    public boolean isJavascriptInjected() {
        return mJavascriptInjected;
    }

    public void setJavascriptInjected(boolean javascriptInjected) {
        this.mJavascriptInjected = javascriptInjected;
    }
    
    public void takeSnapshot(int chapterIndex, int pageIndex, Bitmap snapshotBitmap, boolean saveFile) {
        //Bitmap snapshotBitmap = null;
        FileOutputStream fos = null;
        try {
//            int snapshotWidth = getResources().getDisplayMetrics().widthPixels;
//            int snapshotHeight = getResources().getDisplayMetrics().heightPixels;
//            snapshotBitmap = Bitmap.createBitmap(snapshotWidth, snapshotHeight, Bitmap.Config.ARGB_8888);
            if (snapshotBitmap != null && !snapshotBitmap.isRecycled()) {
                Canvas canvas = new Canvas(snapshotBitmap);
                
                int count = canvas.save(1);
                draw(canvas);
                canvas.restoreToCount(count);
                
                if (saveFile) {
                    fos = new FileOutputStream("/mnt/sdcard/lebook_crack/chapter_" + chapterIndex + "_page_" + pageIndex + ".jpg");
                    snapshotBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.flush();
                }
            }
        } catch (OutOfMemoryError e) {
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                
            }
        }
        return;
    }

    public interface OnSizeChangedListener {
        public void onSizeChanged (int w, int h, int oldw, int oldh);
    }
  
    public static class StyleText {
        public int chapterIndex;
        public int startNodeIndex;
        public int startNodeOffset;
        public int endNodeIndex;
        public int endNodeOffset;
        public int type;
        public long bgColor;
    }
}
