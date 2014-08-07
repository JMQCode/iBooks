package com.greenlemonmobile.app.ebook.books.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.greenlemonmobile.app.ebook.R;
import com.greenlemonmobile.app.ebook.books.model.ReaderSettings;
import com.greenlemonmobile.app.utils.DPIUtil;
import com.greenlemonmobile.app.utils.ImageTool;

import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class SelectionMaskView extends ImageView implements GestureDetector.OnGestureListener {
    // 放大镜的半径
    private static final float RADIUS = 100.0f;
    // 放大倍数
    private static final float FACTOR = 1.5f;  
    
    private static final float OFFSET_X = RADIUS / 4.0f;
    private static final float TUNING_OFFSET_Y = 120.0f * 3.0f / 4.0f ;
    private static float MAGNIFIER_OFFSET_Y = 2 * RADIUS;
    
    // 复制的最大字符限制
    private static final int MAXIMUM_COPY_TEXT_COUNT = 200;
    
    private static final int MAXIMUM_COPY_COUNT = 50;
    
    // 行间距
    private int mTextHeight = 20;
    
    private Context context;
    
    private Bitmap mBackgroudBitmap = null;
    private Bitmap mMaskBitmap = null;
    
    private GestureDetector mGestureDetector;

    private Path mMagnifierPath = new Path();
    private Matrix mMagnifierMatrix = new Matrix();
    
    private Path mTuningSelectPath = new Path();
    private Matrix mTuningSelectMatrix = new Matrix();
    
    private int mCurrentX, mCurrentY;
    private int mStartX, mStartY;
    private int mEndX, mEndY;
    
    private int mCurrentIndex;
    
    private boolean isMagnifier = false;
    private boolean isActionDown = false;
    private boolean isActionMoving = false;
    
    private boolean isLeftSelectHandlePressed = false;
    private boolean isRightSelectHandlePressed = false;
    
    // 操作面板（复制、批注、分享等等）
    private PopupWindow operatorWindow;
    
    private int mOperatorBackgroundMargin = 18;
    
    // 选中微调时的放大镜
    private PopupWindow mTuningMagnifierWindow;
    
    private boolean usingPopupWindow4TuningMagnifier = false;
    
    private boolean isLeftSelectHandleDisplayed = false;
    private boolean isRightSelectHandleDisplayed = false;
    
    private Drawable mTopOperatorBackgroundDrawable;
    private Drawable mBottomOperatorBackgroundDrawable;
    private Drawable mMagnifierDrawable;
    private Drawable mLeftSelectHandleDrawable;
    private Drawable mRightSelectHandleDrawable;
    private Drawable mTuningMagnifierDrawable;
    private BitmapDrawable mTuningMagnifierBitmapDrawable;
    
    private static float mTuningMagnifierHorizontalInitMargin = 13.0f;
    private static float mTuningMagnifierVerticalInitMargin = 14.0f;
    private static float mTuningMagnifierContentInitHeight = 88.0f;
    
    private int mTuningMagnifierHorizontalMargin = 13;
    private int mTuningMagnifierVerticalMargin = 14;
    
    private Rect mLeftSelectHandleRect = new Rect();
    private Rect mRightSelectHandleRect = new Rect();
    private Rect mSelectPaintArea = new Rect();
    
    private Paint mSelectMaskPaint;
    
    private Paint mSelectLinePaint;
    
    private Paint mMagnifierPaint;
    
    private ArrayList<Rect> selections = new ArrayList<Rect>();
    
    private final WebReaderView mWebReaderView;
    private final ReaderSettings mReaderSettings;

    public SelectionMaskView(WebReaderView view, ReaderSettings settings) {
        super(view.getContext());
        mWebReaderView = view;
        mReaderSettings = settings;
        init(view.getContext());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
        if (mTuningMagnifierWindow != null)
            mTuningMagnifierWindow.dismiss();
        mTuningMagnifierWindow = null;

        if (operatorWindow != null)
            operatorWindow.dismiss();
        operatorWindow = null;
    }

    private void init(Context context) {
        this.context = context;
        
        //DPIUtil.setDensity(context.getResources().getDisplayMetrics().density);
        
        mMagnifierPath.addCircle(RADIUS, RADIUS, RADIUS, Direction.CW);
        mMagnifierMatrix.setScale(FACTOR, FACTOR);
        
//        mTuningSelectPath.addRect(0, 0, 2 * RADIUS, RADIUS, Direction.CW);
        mTuningSelectMatrix.setScale(FACTOR, FACTOR);
        
        mSelectMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectMaskPaint.setColor(0xff005a);
        mSelectMaskPaint.setAlpha(25);
        mSelectMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        
        mSelectLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectLinePaint.setColor(0xff005a);
        mSelectLinePaint.setAlpha(250);
        mSelectLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        
        mMagnifierPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMagnifierPaint.setColor(Color.WHITE);
        mMagnifierPaint.setAlpha(180);

        mLeftSelectHandleDrawable = context.getResources().getDrawable(R.drawable.select_handle_left);
        mRightSelectHandleDrawable = context.getResources().getDrawable(R.drawable.select_handle_right);
        mMagnifierDrawable = context.getResources().getDrawable(R.drawable.magnifier);
        
        mGestureDetector = new GestureDetector(this);
        
        initOperatorWindow();
        initTuningMagnifierWindow();
    }
    
    public void setCurrentPoint(Point point) {
        mCurrentX = point.x;
        mCurrentY = point.y;
    }
    
    public void selectionChanged(ArrayList<Rect> rects) {
        selections.clear();
        if (rects != null && !rects.isEmpty()) {
            for (Rect rect : rects) {
                selections.add(rect);
            }
            mStartX = selections.get(0).left;
            mStartY = selections.get(0).top;
            mEndX = selections.get(selections.size() - 1).right;
            mEndY = selections.get(selections.size() - 1).bottom;
            updateLeftSelectHandlePosition(mStartX, mStartY);
            updateRightSelectHandlePosition(mEndX, mEndY);
        }
        invalidate();
    }
    
    public void setShow(boolean show, ArrayList<Rect> handleBounds) {
        //setDrawingCacheEnabled(show);
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        
        mWebReaderView.setDrawingCacheEnabled(show);
        mSelectPaintArea.set((int)mReaderSettings.mLeftMargin, (int)mReaderSettings.mTopMargin, (int)(width - mReaderSettings.mRightMargin), (int)(height - mReaderSettings.mBottomMargin));
        if (show) {
            mBackgroudBitmap = mWebReaderView.getDrawingCache();
            isMagnifier = false;
            isActionDown = false;
            isActionMoving = false;
            
            mTextHeight = (int) mReaderSettings.mTextSize;
            if (mTextHeight < 25)
                mTextHeight = 25;
            if (handleBounds != null && !handleBounds.isEmpty()) {
                mCurrentX = mStartX = handleBounds.get(0).left;
                mCurrentY = mStartY = handleBounds.get(0).top;
                
                mEndX = handleBounds.get(handleBounds.size() - 1).right;
                mEndY = handleBounds.get(handleBounds.size() - 1).bottom;
            } else {
                mEndX = mCurrentX + mTextHeight;
                mEndY = mCurrentY + mTextHeight;
                mStartX = mCurrentX;
                mStartY = mCurrentY;
            }
            
            float scaleHeightRadio = (FACTOR * mTextHeight) / mTuningMagnifierContentInitHeight;
            float scaleHeight = scaleHeightRadio * mTuningMagnifierDrawable.getIntrinsicHeight();
            
            if (mTuningMagnifierBitmapDrawable != null && mTuningMagnifierBitmapDrawable.getBitmap().getHeight() != (int)scaleHeight) {
                try {
                    if (mTuningMagnifierBitmapDrawable.getBitmap() != null && !mTuningMagnifierBitmapDrawable.getBitmap().isRecycled())
                        mTuningMagnifierBitmapDrawable.getBitmap().recycle();
                    
                    float scaleWidth = scaleHeightRadio * mTuningMagnifierDrawable.getIntrinsicWidth();
                    
                    mTuningMagnifierHorizontalMargin = (int)(scaleHeightRadio * mTuningMagnifierHorizontalInitMargin);
                    mTuningMagnifierVerticalMargin = (int)(scaleHeightRadio * mTuningMagnifierVerticalInitMargin);
                    
                    Bitmap bmp = ImageTool.drawableToBitmap(mTuningMagnifierDrawable);
                    Bitmap scaleBmp = ImageTool.getScaleImage(bmp, (int)scaleWidth, (int)scaleHeight, true, true, true);
                    //Log.e("MaskView", "width = " + scaleWidth + " height = " + scaleHeight);
                    
                    mTuningMagnifierBitmapDrawable = new BitmapDrawable(scaleBmp);
                    bmp.recycle();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            isMagnifier = false;
            isActionDown = true;
            isActionMoving = true;
            isLeftSelectHandleDisplayed = false;
            isRightSelectHandleDisplayed = false;
            updateLeftSelectHandlePosition(mStartX, mStartY);
            updateRightSelectHandlePosition(mEndX, mEndY);

            hideOperatorWindow();
        } else {
            //NativeJeb.JEBClearSelectRectangle();
            selections.clear();
            isLeftSelectHandleDisplayed = false;
            isRightSelectHandleDisplayed = false;
            isLeftSelectHandlePressed = false;
            isRightSelectHandlePressed = false;
            isMagnifier = false;
            isActionDown = false;
            isActionMoving = false;

            setVisibility(View.GONE);
            
            hideOperatorWindow();
            hideTuningMagnifierWindow();
            if (mMaskBitmap != null && !mMaskBitmap.isRecycled()) {
                mMaskBitmap.recycle();
                mMaskBitmap = null;
            }
            if (mBackgroudBitmap != null && !mBackgroudBitmap.isRecycled()) {
                mBackgroudBitmap.recycle();
                mBackgroudBitmap = null;
            }
        }
        
        isLeftSelectHandlePressed = false;
        isRightSelectHandlePressed = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event))
            return true;
        
        mCurrentX = (int) event.getX();
        mCurrentY = (int) event.getY();
        
        if (!mSelectPaintArea.contains(mCurrentX, mCurrentY)) {
            if (mCurrentX < mSelectPaintArea.left)
                mCurrentX = mSelectPaintArea.left;
            if (mCurrentX > mSelectPaintArea.right)
                mCurrentX = mSelectPaintArea.right;
            
            if (mCurrentY < mSelectPaintArea.top)
                mCurrentY = mSelectPaintArea.top;
            if (mCurrentY > mSelectPaintArea.bottom)
                mCurrentY = mSelectPaintArea.bottom;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //Log.e("MaskView", "MotionEvent.ACTION_DOWN");
                isMagnifier = false;
                isActionMoving = true;
                isActionDown = true;
                
                isLeftSelectHandlePressed = false;
                isRightSelectHandlePressed = false;
                
                Rect leftTempRect = new Rect(mLeftSelectHandleRect);
                leftTempRect.left -= DPIUtil.px2dip(context, mLeftSelectHandleDrawable.getIntrinsicWidth());
                leftTempRect.right += DPIUtil.px2dip(context, mLeftSelectHandleDrawable.getIntrinsicWidth());
                leftTempRect.top -= DPIUtil.px2dip(context, mLeftSelectHandleDrawable.getIntrinsicHeight() / 2);
                leftTempRect.bottom += DPIUtil.px2dip(context, mLeftSelectHandleDrawable.getIntrinsicHeight() / 2);
                
                Rect rightTempRect = new Rect(mRightSelectHandleRect);
                rightTempRect.left -= DPIUtil.px2dip(context, mRightSelectHandleDrawable.getIntrinsicWidth());
                rightTempRect.right += DPIUtil.px2dip(context, mRightSelectHandleDrawable.getIntrinsicWidth());
                rightTempRect.top -= DPIUtil.px2dip(context, mRightSelectHandleDrawable.getIntrinsicHeight() / 2);
                rightTempRect.bottom += DPIUtil.px2dip(context, mRightSelectHandleDrawable.getIntrinsicHeight() / 2);
                if (leftTempRect.contains(mCurrentX, mCurrentY)) {
                    isLeftSelectHandlePressed = true;
                    mStartX = mCurrentX;
                    mStartY = mCurrentY;
                } else if (rightTempRect.contains(mCurrentX, mCurrentY)) {
                    isRightSelectHandlePressed = true;
                    mEndX = mCurrentX;
                    mEndY = mCurrentY;
                } else {
                    mStartX = mCurrentX;
                    mStartY = mCurrentY;
                    isLeftSelectHandleDisplayed = false;
                    isRightSelectHandleDisplayed = false;                    
                }

                hideOperatorWindow();
                hideTuningMagnifierWindow();
            case MotionEvent.ACTION_MOVE:
                if (isActionDown) {
                    if (isLeftSelectHandlePressed) {
                        if (mCurrentY >= mEndY)
                            return false;
                        mWebReaderView.dragMove(0, event);

                        mStartX = mCurrentX;
                        mStartY = mCurrentY;

                        updateLeftSelectHandlePosition(mStartX, mStartY);
                        updateRightSelectHandlePosition(mEndX, mEndY);
                        
                        updateTuningMagnifierWindow(mCurrentX, mCurrentY);
                    } else if (isRightSelectHandlePressed) {
                        if (mCurrentY <= mStartY)
                            return false;
                        mWebReaderView.dragMove(1, event);

                        mEndX = mCurrentX;
                        mEndY = mCurrentY;

                        updateRightSelectHandlePosition(mEndX, mEndY);
                        updateLeftSelectHandlePosition(mStartX, mStartY);
                        
                        updateTuningMagnifierWindow(mCurrentX, mCurrentY);
                    } else {
                        mWebReaderView.onLongClick(event);
                        mStartX = mCurrentX;
                        mStartY = mCurrentY;
                        isMagnifier = true;
                    }
                    
                    isActionMoving = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                //Log.e("MaskView", "MotionEvent.ACTION_UP");
                if (isLeftSelectHandlePressed || isRightSelectHandlePressed) {
//                    updateLeftSelectHandlePosition(mStartX, mStartY);
//                    updateRightSelectHandlePosition(mEndX, mEndY);
                } else {
                    mEndX = mCurrentX;
                    mEndY = mCurrentY;
                }
                
                if (isMagnifier) {
                    if (selections != null && !selections.isEmpty()) {
                        mStartX = selections.get(0).left;
                        mStartY = selections.get(0).top;
                    }

                    if (selections != null && !selections.isEmpty()) {
                        mEndX = selections.get(selections.size() - 1).right;
                        mEndY = selections.get(selections.size() - 1).bottom;
                    }
                    updateLeftSelectHandlePosition(mStartX, mStartY);
                    updateRightSelectHandlePosition(mEndX, mEndY);
                }
                isActionMoving = false;
                isActionDown = false;
                isMagnifier = false;
                
                isLeftSelectHandlePressed = false;
                isRightSelectHandlePressed = false;
                
                if (mStartY > mEndY) {
                    int temp = mStartX;
                    mStartX = mEndX;
                    mEndX = temp;
                    temp = mStartY;
                    mStartY = mEndY;
                    mEndY = temp;
                }
                hideTuningMagnifierWindow();
                updateOperatorWindow();
                
                //constructSelectionPath(mStartX, mStartY, mEndX, mEndY);
//                constructSelectionPath(selectRect);
                break;
        }
        invalidate();
        return true;//super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mMaskBitmap == null)
            mMaskBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

        if (mMaskBitmap != null && !mMaskBitmap.isRecycled() && mBackgroudBitmap != null && !mBackgroudBitmap.isRecycled()) {
            Canvas maskCanvas = new Canvas(mMaskBitmap);
            maskCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            
            if (!mBackgroudBitmap.isRecycled()) {
                //底图
                maskCanvas.drawBitmap(mBackgroudBitmap, 0, 0, null);
                //maskCanvas.drawBitmap(getDrawingCache(), 0, 0, null);
                
                for (Rect rect : selections)
                    maskCanvas.drawRect(rect, mSelectMaskPaint);

                if (!isMagnifier && selections != null && !selections.isEmpty()) {
                    Rect rect = selections.get(0);
                    Rect drawRect = new Rect(rect.left - 3, rect.top, rect.left, rect.bottom);
                    
                    maskCanvas.drawRect(drawRect, mSelectLinePaint);
                    
                    rect = selections.get(selections.size() - 1);
                    drawRect = new Rect(rect.right, rect.top, rect.right + 3, rect.bottom);
                    
                    maskCanvas.drawRect(drawRect, mSelectLinePaint);
                }
                
                // 画放大镜
                if (isMagnifier) {
                    int centerX = (int) (mCurrentX - OFFSET_X);
                    int centerY = (int) (mCurrentY - RADIUS - 30);
                    
                    maskCanvas.save();
                    //剪切
                    maskCanvas.translate(centerX - RADIUS, centerY - RADIUS);
                    maskCanvas.clipPath(mMagnifierPath);
                    maskCanvas.drawPath(mMagnifierPath, mMagnifierPaint);
                    
                    Rect bounds = new Rect();
                    bounds.left = centerX - DPIUtil.px2dip(context, mMagnifierDrawable.getIntrinsicWidth()) / 2;
                    bounds.right = centerX + DPIUtil.px2dip(context, mMagnifierDrawable.getIntrinsicWidth()) / 2;
                    
                    bounds.top = centerY - DPIUtil.px2dip(context, mMagnifierDrawable.getIntrinsicHeight()) / 2;
                    bounds.bottom = centerY + DPIUtil.px2dip(context, mMagnifierDrawable.getIntrinsicHeight()) / 2;
                    
                    //画放大后的图
                    maskCanvas.translate(RADIUS - mCurrentX * FACTOR, RADIUS - mCurrentY * FACTOR);
                    maskCanvas.drawBitmap(mMaskBitmap, mMagnifierMatrix, null);
                    maskCanvas.restore();
                    
                    mMagnifierDrawable.setBounds(bounds);
                    mMagnifierDrawable.draw(maskCanvas);
                }
                
                canvas.drawBitmap(mMaskBitmap, 0, 0, null);
                
                // 画微调放大镜
                if (isActionMoving && (isLeftSelectHandleDisplayed || isRightSelectHandleDisplayed)
                        && !usingPopupWindow4TuningMagnifier) {
                    
//                    // 动态调正微调放大镜的高度
//                    if (selections != null && !selections.isEmpty()) {
//
//                        if (isLeftSelectHandlePressed) {
//                            mTextHeight = selections.get(0).hashCode();
//                        } else {
//                            mTextHeight = selections.get(selections.size() - 1).hashCode();
//                        }
//                        
//                        mTextHeight = DPIUtil.px2dip(mTextHeight);
//                        
//                        float scaleHeightRadio = (mTextHeight) / mTuningMagnifierContentInitHeight;
//                        float scaleHeight = scaleHeightRadio * mTuningMagnifierDrawable.getIntrinsicHeight();
//                        
//                        if (mTuningMagnifierBitmapDrawable != null && mTuningMagnifierBitmapDrawable.getBitmap().getHeight() != scaleHeight) {
//                            try {
//                                if (mTuningMagnifierBitmapDrawable.getBitmap() != null && !mTuningMagnifierBitmapDrawable.getBitmap().isRecycled())
//                                    mTuningMagnifierBitmapDrawable.getBitmap().recycle();
//                                
//                                float scaleWidth = scaleHeightRadio * mTuningMagnifierDrawable.getIntrinsicWidth();
//                                
//                                mTuningMagnifierHorizontalMargin = (int)(scaleHeightRadio * mTuningMagnifierHorizontalInitMargin);
//                                mTuningMagnifierVerticalMargin = (int)(scaleHeightRadio * mTuningMagnifierVerticalInitMargin);
//                                
//                                Bitmap bmp = ImageTool.drawableToBitmap(mTuningMagnifierDrawable);
//                                Bitmap scaleBmp = ImageTool.getScaleImage(bmp, (int)scaleWidth, (int)scaleHeight, true, true, true);
//                                
//                                mTuningMagnifierBitmapDrawable = new BitmapDrawable(scaleBmp);
//                                bmp.recycle();
//                            } catch(Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
                    
                    Rect bounds = new Rect();
                    bounds.left = mCurrentX - mTuningMagnifierBitmapDrawable.getBitmap().getWidth() / 2;
                    bounds.right = bounds.left + mTuningMagnifierBitmapDrawable.getBitmap().getWidth();
                    bounds.top = (int) (mCurrentY - mTuningMagnifierBitmapDrawable.getBitmap().getHeight() - TUNING_OFFSET_Y);
                    if (mCurrentY < TUNING_OFFSET_Y + mTuningMagnifierBitmapDrawable.getBitmap().getHeight())
                        bounds.top = (int) (mCurrentY + TUNING_OFFSET_Y);
                    bounds.bottom = bounds.top + mTuningMagnifierBitmapDrawable.getBitmap().getHeight();

                    if (bounds.left < 0) {
                        bounds.left = 0;
                        bounds.right = bounds.left + mTuningMagnifierBitmapDrawable.getBitmap().getWidth();
                    }
                    if (bounds.right > getWidth()) {
                        bounds.right = getWidth();
                        bounds.left = bounds.right - mTuningMagnifierBitmapDrawable.getBitmap().getWidth();
                    }
                    canvas.drawRect(bounds, mMagnifierPaint);
                    
                    mTuningMagnifierBitmapDrawable.setBounds(bounds);
                    mTuningMagnifierBitmapDrawable.draw(canvas);

                    int width = mTuningMagnifierBitmapDrawable.getBitmap().getWidth() - 2 * mTuningMagnifierHorizontalMargin;
                    int height = mTuningMagnifierBitmapDrawable.getBitmap().getHeight() - 2 * mTuningMagnifierVerticalMargin;
                    mTuningSelectPath.reset();
                    mTuningSelectPath.addRoundRect(new RectF(0, 0, width, height), 15, 15, Direction.CW);

                    canvas.save();
                    int dx = bounds.left + mTuningMagnifierHorizontalMargin;
//                    if (dx < mTuningMagnifierHorizontalMargin)
//                        dx = mTuningMagnifierHorizontalMargin;
//                    if (dx > getWidth() - width - mTuningMagnifierHorizontalMargin)
//                        dx = getWidth() - width - 2 * mTuningMagnifierHorizontalMargin;
                    
//                    int dy = (int) (mCurrentY - (mTuningMagnifierBitmapDrawable.getBitmap().getHeight() - mTuningMagnifierVerticalMargin) - OFFSET_Y);
//                    if (mCurrentY < OFFSET_Y + mTuningMagnifierBitmapDrawable.getBitmap().getHeight())
//                        dy = (int) (mCurrentY + OFFSET_Y + mTuningMagnifierVerticalMargin);
                    int dy = bounds.top + mTuningMagnifierVerticalMargin;
                    canvas.translate(dx, dy);
                    canvas.clipPath(mTuningSelectPath);
                    
                    dx = (int) (width / 2 - mCurrentX * FACTOR);
                    if (selections != null && !selections.isEmpty()) {
                        if (isLeftSelectHandlePressed) {
                            dy = (int) (height / 2 - (selections.get(0).top - (selections.get(0).top - selections.get(0).bottom) / 2) * FACTOR);
                        } else {
                            dy = (int) (height / 2 - (selections.get(selections.size() - 1).top - (selections.get(selections.size() - 1).top - selections.get(selections.size() - 1).bottom) / 2) * FACTOR);
                        }
                    } else {
                        dy = (int) (height / 2 - mCurrentY * FACTOR);
                    }

                    canvas.translate(dx, dy);
                    canvas.drawBitmap(mMaskBitmap, mTuningSelectMatrix, null);
                    canvas.restore();
                    
//                    canvas.save();
//                    canvas.translate(mCurrentX - width / 2, mCurrentY - height / 2);
//                    canvas.clipPath(mTuningSelectPath);
//                    canvas.translate(-mCurrentX * FACTOR + width / 2, -mCurrentY * FACTOR + height/ 2);
//                    canvas.drawBitmap(mMaskBitmap, mTuningSelectMatrix, null);
//                    canvas.restore();
                }
            }

        }
        // 画左微调把手
        if (isLeftSelectHandleDisplayed && !mLeftSelectHandleRect.isEmpty()) {
            mLeftSelectHandleDrawable.setBounds(mLeftSelectHandleRect);
            mLeftSelectHandleDrawable.draw(canvas);
        }
        
        // 画右微调把手
        if (isRightSelectHandleDisplayed && !mRightSelectHandleRect.isEmpty()) {
            mRightSelectHandleDrawable.setBounds(mRightSelectHandleRect);
            mRightSelectHandleDrawable.draw(canvas);
        }
    }

    public boolean isMagnifier() {
        return isMagnifier;
    }

    public void setMagnifier(boolean isMagnifier) {
        this.isMagnifier = isMagnifier;
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    public void setCurrentIndex(int mCurrentIndex) {
        this.mCurrentIndex = mCurrentIndex;
    }
    
    public void initOperatorWindow() {
//        if (mTopOperatorBackgroundDrawable == null)            
//            mTopOperatorBackgroundDrawable = context.getResources().getDrawable(R.drawable.select_btn_down);
//        
//        if (mBottomOperatorBackgroundDrawable == null)
//            mBottomOperatorBackgroundDrawable = context.getResources().getDrawable(R.drawable.select_btn_up);
        
        if (mTopOperatorBackgroundDrawable == null) {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.select_btn_down);
            byte[] chunk = bitmap.getNinePatchChunk();
            mTopOperatorBackgroundDrawable = new NinePatchDrawable(getResources(), bitmap, chunk, new Rect(), null);
            
//            crash at 2.1
//            NinePatch ninePatch = new NinePatch(bitmap, bitmap.getNinePatchChunk(),
//                    null);
//            mTopOperatorBackgroundDrawable = new NinePatchDrawable(context.getResources(), ninePatch);
        }

        if (mBottomOperatorBackgroundDrawable == null) {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.select_btn_up);
            byte[] chunk = bitmap.getNinePatchChunk();
            mBottomOperatorBackgroundDrawable = new NinePatchDrawable(getResources(), bitmap, chunk, new Rect(), null);
//              crash at 2.1
//            NinePatch ninePatch = new NinePatch(bitmap, bitmap.getNinePatchChunk(),
//                    null);
//            mBottomOperatorBackgroundDrawable = new NinePatchDrawable(context.getResources(), ninePatch);
        }
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        RelativeLayout layout = new RelativeLayout(context);
        layout.setBackgroundDrawable(mTopOperatorBackgroundDrawable);
        layout.setLayoutParams(params);
        layout.setPadding(0, 0, 0, mOperatorBackgroundMargin);
        
//        Button copy = new Button(context);
//        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
//        copy.setLayoutParams(params);
//        copy.setBackgroundColor(Color.TRANSPARENT);
//        copy.setText("复制");
//        copy.setTextSize(context.getResources().getDimension(R.dimen.tSize14));
//        copy.setTextColor(Color.WHITE);
        
//        Button comment = new Button(context);
//        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//        comment.setLayoutParams(params);
//        comment.setBackgroundColor(Color.TRANSPARENT);
//        comment.setText("批注");
//        comment.setOnClickListener(new View.OnClickListener() {
//            
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(context, "批注", Toast.LENGTH_SHORT).show();
//            }
//        });
//        
//        Button share = new Button(context);
//        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//        share.setLayoutParams(params);
//        share.setBackgroundColor(Color.TRANSPARENT);
//        share.setText("分享");
//        share.setOnClickListener(new View.OnClickListener() {
//            
//            @Override
//            public void onClick(View v) {
//                SelectInfo select_info = new SelectInfo();
//                NativeJeb.JEBGetSelectedTextInfo(select_info);
//                //Toast.makeText(context, select_info.content, Toast.LENGTH_SHORT).show();
//                
//                Intent intent = new Intent(Intent.ACTION_SEND);
//                intent.setType("text/plain");
//                //intent.putExtra(Intent.EXTRA_SUBJECT, "京东商城");
//                
//                String shareAddress = Configuration.getProperty(Configuration.EBOOK_SHARE_ADDRESS);
//                
//                String extraText = (select_info.content.length() > MAXIMUM_COPY_TEXT_COUNT ? select_info.content.substring(0, MAXIMUM_COPY_TEXT_COUNT) : select_info.content) + ((BookProperties.BOOK_ID > 0) ? ("("+ shareAddress + BookProperties.BOOK_ID + ".html)") : "");
//                intent.putExtra(Intent.EXTRA_TEXT, extraText);
//                context.startActivity(Intent.createChooser(intent, "分享到："));
//            }
//        });
//
//        layout.addView(copy);
//        layout.addView(comment);   
//        layout.addView(share);

        operatorWindow = new PopupWindow(layout, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        //operatorWindow.setBackgroundDrawable(mTopOperatorBackgroundDrawable);
        //opertorWindow.setAnimationStyle(R.style.Animation_Popup);
        operatorWindow.setHeight(LayoutParams.WRAP_CONTENT);
        operatorWindow.setFocusable(false);
        operatorWindow.setOutsideTouchable(true);
        operatorWindow.update();
    }
    
    void hideOperatorWindow() {
        if (operatorWindow != null)
            operatorWindow.dismiss();
        operatorWindow = null;
    }
    
    void updateOperatorWindow() {
        
        int positionX;
        int positionY;
        boolean bottom = false;
        if (mStartY > (mTopOperatorBackgroundDrawable.getIntrinsicHeight() + mLeftSelectHandleDrawable.getIntrinsicHeight())) {
            positionY = (int) (mStartY - mTopOperatorBackgroundDrawable.getIntrinsicHeight() - mLeftSelectHandleDrawable.getIntrinsicHeight());
            positionX = mStartX;
        } else if (mEndY < (getHeight() - mTopOperatorBackgroundDrawable.getIntrinsicHeight() - mLeftSelectHandleDrawable.getIntrinsicHeight())) {
            positionY = (int) (mEndY + mLeftSelectHandleDrawable.getIntrinsicHeight());
            positionX = mEndX;
            bottom = true;
        } else {
            positionY = (int) (getHeight() / 2 - mTopOperatorBackgroundDrawable.getIntrinsicHeight() / 2);
            positionX = getWidth() / 2 - mTopOperatorBackgroundDrawable.getIntrinsicWidth() / 2;
        }
        

        // 复制、批注、分享面板通过PopupWindow来实现
        if (operatorWindow == null)
            initOperatorWindow();
        if (operatorWindow != null) {
            //operatorWindow.getContentView()
            operatorWindow.getContentView().setBackgroundDrawable(bottom ? mBottomOperatorBackgroundDrawable : mTopOperatorBackgroundDrawable);
            operatorWindow.getContentView().setPadding(0, bottom ? mOperatorBackgroundMargin : 0, 0, bottom ? 0 : mOperatorBackgroundMargin);
            if (!operatorWindow.isShowing())
                operatorWindow.showAtLocation(this, Gravity.NO_GRAVITY, positionX, positionY);
            else
                operatorWindow.update(positionX, positionY, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
    }
    
    
    public class TuningMagnifierView extends View {
        
        private RectF rect = new RectF();
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public TuningMagnifierView(Context context) {
            super(context);

            paint.setAntiAlias(true);
            paint.setStrokeWidth(1);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mMaskBitmap != null && !mMaskBitmap.isRecycled()) {
                if (rect.isEmpty()) {
                    rect.left = rect.top = 0.0f;
                    rect.right = getWidth();
                    rect.bottom = getHeight();
                }
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(rect, paint);
                
                Rect src = new Rect();
                src.left = (int) (mCurrentX - rect.width() / 4);
                src.right = (int) (src.left + rect.width() / 2);
                src.top = (int) (mCurrentY - rect.height() / 4);
                src.bottom = (int) (src.top + rect.height() / 2);
                
                if (src.left < 0) {
                    src.left = 0;
                    src.right = src.left + getWidth() / 2;
                }
                
                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                if (src.right > width) {
                    src.right = height;
                    src.left = src.right - getWidth() / 2;
                }
                canvas.drawBitmap(mMaskBitmap, src, rect, paint);
            }
        }
    }
    
    void initTuningMagnifierWindow() {
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.HORIZONTAL);

        if (mTuningMagnifierDrawable == null) {
            mTextHeight = (int)mReaderSettings.mTextSize;
            if (mTextHeight < 25)
                mTextHeight = 25;
            mTuningMagnifierDrawable = context.getResources().getDrawable(R.drawable.select_magnifier);
            float scaleHeightRadio = (FACTOR * mTextHeight) / mTuningMagnifierContentInitHeight;
            float scaleHeight = scaleHeightRadio * mTuningMagnifierDrawable.getIntrinsicHeight();
            
            float scaleWidth = scaleHeightRadio * mTuningMagnifierDrawable.getIntrinsicWidth();
            
            mTuningMagnifierHorizontalMargin = (int)(scaleHeightRadio * mTuningMagnifierHorizontalInitMargin);
            mTuningMagnifierVerticalMargin = (int)(scaleHeightRadio * mTuningMagnifierVerticalInitMargin);
            
            if (mTuningMagnifierBitmapDrawable == null) {
                try {
                    Bitmap bmp = ImageTool.drawableToBitmap(mTuningMagnifierDrawable);
                    Bitmap scaleBmp = ImageTool.getScaleImage(bmp, (int)scaleWidth, (int)scaleHeight, true, true, true);
                    //Log.e("MaskView", "width = " + scaleWidth + " height = " + scaleHeight);
                    
                    mTuningMagnifierBitmapDrawable = new BitmapDrawable(scaleBmp);
                    bmp.recycle();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        //params.setMargins(mTuningMagnifierHorizontalMargin,mTuningMagnifierVerticalMargin,mTuningMagnifierHorizontalMargin,mTuningMagnifierVerticalMargin);
        TuningMagnifierView tuningMagnifierView = new TuningMagnifierView(context);
        tuningMagnifierView.setLayoutParams(params);
        layout.addView(tuningMagnifierView);

        mTuningMagnifierWindow = new PopupWindow(layout, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
        mTuningMagnifierWindow.setBackgroundDrawable(mTuningMagnifierBitmapDrawable);
        mTuningMagnifierWindow.setHeight(LayoutParams.WRAP_CONTENT);
        mTuningMagnifierWindow.update();
    }
    
    void hideTuningMagnifierWindow() {
        if (mTuningMagnifierWindow != null)
            mTuningMagnifierWindow.dismiss();
        mTuningMagnifierWindow = null;
    }
    
    void updateTuningMagnifierWindow(int x, int y) {
        if (usingPopupWindow4TuningMagnifier) {
            if (mTuningMagnifierWindow == null)
                initTuningMagnifierWindow();
            
            int positionY = (int)(y - TUNING_OFFSET_Y - mTuningMagnifierBitmapDrawable.getBitmap().getHeight());
            if (y < TUNING_OFFSET_Y + mTuningMagnifierBitmapDrawable.getBitmap().getHeight())
                positionY = (int) (y + TUNING_OFFSET_Y);
            if (mTuningMagnifierWindow != null) {
                if (!mTuningMagnifierWindow.isShowing())
                    mTuningMagnifierWindow.showAtLocation(this, Gravity.NO_GRAVITY, (int)(x - mTuningMagnifierBitmapDrawable.getBitmap().getWidth() / 2), positionY);
                else
                    mTuningMagnifierWindow.update((int)(x - mTuningMagnifierBitmapDrawable.getBitmap().getWidth() / 2), positionY, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                mTuningMagnifierWindow.getContentView().invalidate();
            }
        }
    }
    
    void updateLeftSelectHandlePosition(int x, int y) {
        if (!isMagnifier)
            isLeftSelectHandleDisplayed = true;
        mLeftSelectHandleRect.left = x - DPIUtil.px2dip(context, mLeftSelectHandleDrawable.getIntrinsicWidth() / 2);
        mLeftSelectHandleRect.top = y - DPIUtil.px2dip(context, mLeftSelectHandleDrawable.getIntrinsicHeight());
        mLeftSelectHandleRect.right = mLeftSelectHandleRect.left + DPIUtil.px2dip(context, mLeftSelectHandleDrawable.getIntrinsicWidth());
        mLeftSelectHandleRect.bottom = mLeftSelectHandleRect.top + DPIUtil.px2dip(context, mLeftSelectHandleDrawable.getIntrinsicHeight());
    }
    
    void updateRightSelectHandlePosition(int x, int y) {
        if (!isMagnifier)
            isRightSelectHandleDisplayed = true;
        mRightSelectHandleRect.left = x - DPIUtil.px2dip(context, mRightSelectHandleDrawable.getIntrinsicWidth() / 2);
        mRightSelectHandleRect.top = y;
        mRightSelectHandleRect.right = mRightSelectHandleRect.left + DPIUtil.px2dip(context, mRightSelectHandleDrawable.getIntrinsicWidth());
        mRightSelectHandleRect.bottom = mRightSelectHandleRect.top + DPIUtil.px2dip(context, mRightSelectHandleDrawable.getIntrinsicHeight());
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
        mWebReaderView.endSelectionMode();
        return true;
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
}
