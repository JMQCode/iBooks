package com.greenlemonmobile.app.ebook.books.imagezoom;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ViewConfiguration;

import com.greenlemonmobile.app.ebook.books.imagezoom.graphics.FastBitmapDrawable;

public class ImageViewTouch extends ImageViewTouchBase {

	static final float MIN_ZOOM = 0.9f;
	protected ScaleGestureDetector mScaleDetector;
	protected GestureDetector mGestureDetector;
	protected int mTouchSlop;
	protected float mCurrentScaleFactor;
	protected float mScaleFactor;
	protected int mDoubleTapDirection;
	protected OnGestureListener mGestureListener;
	protected OnScaleGestureListener mScaleListener;
	protected GestureDetector mOuterGestureDetector;

	protected boolean mDoubleTapEnabled = true;
	protected boolean mScaleEnabled = true;
	protected boolean mScrollEnabled = true;
	
	protected boolean mThumbnailEnabled = true;
	protected boolean mThumbnailShow = true;
	protected Rect mThumbnailRect;
	protected Rect mTouchArea;
	protected Paint mThumbnailMask;
	protected Paint mLinePaint;
	protected Paint mTouchAreaPaint;
	
    private int mCurrentX, mCurrentY;
    private int mStartX, mStartY;
    private int mEndX, mEndY;
    private boolean isActionDown = false;
    private boolean isActionMoving = false;
	
	protected final static int LINE_WIDTH = 2;
	protected final static int MARGION = 20;

	public ImageViewTouch( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}
	
	public ImageViewTouch( Context context ) {
		super( context );
	}

	@Override
	protected void init() {
		super.init();
		mTouchSlop = ViewConfiguration.getTouchSlop();
		mGestureListener = getGestureListener();
		mScaleListener = getScaleListener();

		mScaleDetector = new ScaleGestureDetector( getContext(), mScaleListener );
		mGestureDetector = new GestureDetector( getContext(), mGestureListener, null, true );

		mCurrentScaleFactor = 1f;
		mDoubleTapDirection = 1;
		
		mTouchArea = new Rect();
		mThumbnailRect = new Rect();
		
		mThumbnailMask = new Paint();		
		mThumbnailMask.setAntiAlias(true);
		mThumbnailMask.setColor(Color.GRAY);
		mThumbnailMask.setAlpha(150);
		
		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLinePaint.setColor(Color.BLACK);
		mLinePaint.setStyle(Paint.Style.STROKE);
		mLinePaint.setStrokeWidth(LINE_WIDTH);
		
		mTouchAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTouchAreaPaint.setColorFilter(new ColorFilter());
	}

	public void setDoubleTapEnabled( boolean value ) {
		mDoubleTapEnabled = value;
	}

	public void setScaleEnabled( boolean value ) {
		mScaleEnabled = value;
	}

	public void setScrollEnabled( boolean value ) {
		mScrollEnabled = value;
	}

	public boolean getDoubleTapEnabled() {
		return mDoubleTapEnabled;
	}

	protected OnGestureListener getGestureListener() {
		return new GestureListener();
	}

	protected OnScaleGestureListener getScaleListener() {
		return new ScaleListener();
	}

	@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        RectF bitmapRectF = getBitmapRect();
        RectF scrollRectF = mScrollRect;
        
        int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        if (bitmapRectF.left < 0 || bitmapRectF.top < 0 || bitmapRectF.right > screenWidth || bitmapRectF.bottom > screenHeight)
            mThumbnailShow = true;
        else
            mThumbnailShow = false;
        
        if (mThumbnailShow && mThumbnailEnabled) {
            FastBitmapDrawable drawable = null;
            try {
                drawable = (FastBitmapDrawable)getDrawable();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            float radio = 1.0f;
            if (mThumbnailRect.isEmpty() && drawable != null) {
                mThumbnailRect.left = getLeft() + MARGION;
                mThumbnailRect.top = getTop() + MARGION;
                
                radio = drawable.getBitmap().getHeight() / (1.0f * drawable.getBitmap().getWidth());
                
                int thumbnailWidth = (int) (((screenWidth > screenHeight) ? screenHeight : screenWidth) / 3.0f);
                int thumbnailHeight = (int) (radio * thumbnailWidth);
                
                mThumbnailRect.right = mThumbnailRect.left + thumbnailWidth;
                mThumbnailRect.bottom = mThumbnailRect.top + thumbnailHeight;
                
                radio = mThumbnailRect.width() / drawable.getBitmap().getWidth();
            }
            
            if (drawable != null) {
                RectF borderRectF = new RectF(mThumbnailRect);
                borderRectF.left -= LINE_WIDTH;
                borderRectF.top -= LINE_WIDTH;
                borderRectF.bottom += LINE_WIDTH;
                borderRectF.right += LINE_WIDTH;
                canvas.drawRect(borderRectF, mLinePaint);
                
                Rect src = new Rect(0, 0, drawable.getBitmap().getWidth(), drawable.getBitmap().getHeight());
                canvas.drawBitmap(drawable.getBitmap(), src, mThumbnailRect, null);
                
                int xOffset = (int) (scrollRectF.left - bitmapRectF.left);
                int yOffset = (int) (scrollRectF.top - bitmapRectF.top);
                
                radio = xOffset / bitmapRectF.width();
                mTouchArea.left = (int) (mThumbnailRect.left + radio * mThumbnailRect.width());
                
                radio = yOffset / bitmapRectF.height();
                mTouchArea.top = (int) (mThumbnailRect.top + radio * mThumbnailRect.height());
                
                if (bitmapRectF.right > screenWidth) {
                    radio = screenWidth / bitmapRectF.width();
                    mTouchArea.right = (int) (mTouchArea.left + radio * mThumbnailRect.width());
                } else {
                	mTouchArea.right = mThumbnailRect.right;
                }
                
                if (bitmapRectF.bottom > screenHeight) {
                    radio = screenHeight / bitmapRectF.height();
                    mTouchArea.bottom = (int) (mTouchArea.top + radio * mThumbnailRect.height());
                } else {
                	mTouchArea.bottom = mThumbnailRect.bottom;
                }
                
                if (mTouchArea.left < mThumbnailRect.left)
                	mTouchArea.left = mThumbnailRect.left;
                
                if (mTouchArea.top < mThumbnailRect.top)
                	mTouchArea.top = mThumbnailRect.top;
                
                Rect topArea = new Rect(mThumbnailRect.left, mThumbnailRect.top, mThumbnailRect.right, mTouchArea.top);            
                canvas.drawRect(topArea, mThumbnailMask);
                
                Rect bottomArea = new Rect(mThumbnailRect.left, mTouchArea.bottom, mThumbnailRect.right, mThumbnailRect.bottom);
                canvas.drawRect(bottomArea, mThumbnailMask);
                
                Rect leftArea = new Rect(mThumbnailRect.left, mTouchArea.top, mTouchArea.left, mTouchArea.bottom);
                canvas.drawRect(leftArea, mThumbnailMask);
                
                Rect rightArea = new Rect(mTouchArea.right, mTouchArea.top, mThumbnailRect.right, mTouchArea.bottom);
                canvas.drawRect(rightArea, mThumbnailMask);
                
                canvas.drawRect(mTouchArea, mLinePaint);
            }
        }
    }

    @Override
	protected void onBitmapChanged( Drawable drawable ) {
		super.onBitmapChanged( drawable );

		mThumbnailRect.setEmpty();
		
		float v[] = new float[9];
		mSuppMatrix.getValues( v );
		mCurrentScaleFactor = v[Matrix.MSCALE_X];
	}

	@Override
	protected void _setImageDrawable( final Drawable drawable, final boolean reset, final Matrix initial_matrix, final float maxZoom ) {
		super._setImageDrawable( drawable, reset, initial_matrix, maxZoom );
		mScaleFactor = getMaxZoom() / 3;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		mCurrentX = (int) event.getX();
		mCurrentY = (int) event.getY();
		
		if (!mTouchArea.isEmpty() && !mThumbnailRect.isEmpty() && (isActionMoving || mThumbnailRect.contains(mCurrentX, mCurrentY))) {
			float distanceX = 0.0f;
			float distanceY = 0.0f;
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (mTouchArea.contains(mCurrentX, mCurrentY)) {
					mStartX = mCurrentX;
					mStartY = mCurrentY;
					isActionDown = true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				RectF bitmapRectF = getBitmapRect();
				if (isActionDown && bitmapRectF != null && !bitmapRectF.isEmpty()) {
					isActionMoving = true;
					
					distanceX = mCurrentX - mStartX;
					distanceY = mCurrentY - mStartY;

	                distanceX = (distanceX / mThumbnailRect.width()) * bitmapRectF.width();
	                distanceY = (distanceY / mThumbnailRect.height()) * bitmapRectF.height();
	                
	    			scrollBy( -distanceX, -distanceY );
	    			invalidate();
				}
				break;
			case MotionEvent.ACTION_UP:
				isActionDown = false;
				isActionMoving = false;
				mEndX = mCurrentX;
				mEndY = mCurrentY;
				break;
			}
			
			return true;
		}
		
		mScaleDetector.onTouchEvent( event );
		if ( !mScaleDetector.isInProgress() ) mGestureDetector.onTouchEvent( event );
		int action = event.getAction();
		switch ( action & MotionEvent.ACTION_MASK ) {
			case MotionEvent.ACTION_UP:
				if ( getScale() < 1f ) {
					zoomTo( 1f, 50 );
				}
				break;
		}
		
		if (mOuterGestureDetector != null && mOuterGestureDetector.onTouchEvent(event))
			return true;
		return true;
	}

	@Override
	protected void onZoom( float scale ) {
		super.onZoom( scale );
		if ( !mScaleDetector.isInProgress() ) mCurrentScaleFactor = scale;
	}

	protected float onDoubleTapPost( float scale, float maxZoom ) {
		if ( mDoubleTapDirection == 1 ) {
			if ( ( scale + ( mScaleFactor * 2 ) ) <= maxZoom ) {
				return scale + mScaleFactor;
			} else {
				mDoubleTapDirection = -1;
				return maxZoom;
			}
		} else {
			mDoubleTapDirection = 1;
			return 1f;
		}
	}

	public class GestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap( MotionEvent e ) {
			Log.i( LOG_TAG, "onDoubleTap. double tap enabled? " + mDoubleTapEnabled );
			if ( mDoubleTapEnabled ) {
			    mThumbnailShow = !mThumbnailShow;
			    
//				float scale = getScale();
//				float targetScale = scale;
//				targetScale = onDoubleTapPost( scale, getMaxZoom() );
//				targetScale = Math.min( getMaxZoom(), Math.max( targetScale, MIN_ZOOM ) );
//				mCurrentScaleFactor = targetScale;
//				zoomTo( targetScale, e.getX(), e.getY(), 200 );
				invalidate();
			}
			return super.onDoubleTap( e );
		}

		@Override
		public void onLongPress( MotionEvent e ) {
			if ( isLongClickable() ) {
				if ( !mScaleDetector.isInProgress() ) {
					setPressed( true );
					performLongClick();
				}
			}
		}

		@Override
		public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
			if ( !mScrollEnabled ) return false;

			if ( e1 == null || e2 == null ) return false;
			if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
			if ( mScaleDetector.isInProgress() ) return false;
			if ( getScale() == 1f ) return false;
			scrollBy( -distanceX, -distanceY );
			invalidate();
			return super.onScroll( e1, e2, distanceX, distanceY );
		}

		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
			if ( !mScrollEnabled ) return false;

			if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
			if ( mScaleDetector.isInProgress() ) return false;

			float diffX = e2.getX() - e1.getX();
			float diffY = e2.getY() - e1.getY();

			if ( Math.abs( velocityX ) > 800 || Math.abs( velocityY ) > 800 ) {
				scrollBy( diffX / 2, diffY / 2, 300 );
				invalidate();
			}
			return super.onFling( e1, e2, velocityX, velocityY );
		}
	}

	public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@SuppressWarnings("unused")
		@Override
		public boolean onScale( ScaleGestureDetector detector ) {
			float span = detector.getCurrentSpan() - detector.getPreviousSpan();
			float targetScale = mCurrentScaleFactor * detector.getScaleFactor();
			if ( mScaleEnabled ) {
				targetScale = Math.min( getMaxZoom(), Math.max( targetScale, MIN_ZOOM ) );
				zoomTo( targetScale, detector.getFocusX(), detector.getFocusY() );
				mCurrentScaleFactor = Math.min( getMaxZoom(), Math.max( targetScale, MIN_ZOOM ) );
				mDoubleTapDirection = 1;
				invalidate();
				return true;
			}
			return false;
		}
	}
	
	public GestureDetector getOuterGestureDetector() {
		return mOuterGestureDetector;
	}

	public void setOuterGestureDetector(GestureDetector outerGestureDetector) {
		this.mOuterGestureDetector = outerGestureDetector;
	}
}
