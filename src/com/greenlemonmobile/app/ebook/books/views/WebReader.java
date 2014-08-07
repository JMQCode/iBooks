package com.greenlemonmobile.app.ebook.books.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.greenlemonmobile.app.ebook.R;
import com.greenlemonmobile.app.ebook.books.httpd.MyHTTPD;
import com.greenlemonmobile.app.ebook.books.imagezoom.ImageViewTouch;
import com.greenlemonmobile.app.ebook.books.model.Book;
import com.greenlemonmobile.app.ebook.books.model.Chapter;
import com.greenlemonmobile.app.ebook.books.model.Chapter.ChapterState;
import com.greenlemonmobile.app.ebook.books.model.PaginationResult;
import com.greenlemonmobile.app.ebook.books.model.PendingTask;
import com.greenlemonmobile.app.ebook.books.model.ReaderSettings;
import com.greenlemonmobile.app.ebook.books.model.TouchableItem;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser.NavPoint;
import com.greenlemonmobile.app.ebook.books.parser.IParser;
import com.greenlemonmobile.app.ebook.books.parser.ParserFactory;
import com.greenlemonmobile.app.ebook.books.utils.ParallelTask;
import com.greenlemonmobile.app.ebook.entity.Bookmark;
import com.greenlemonmobile.app.utils.Md5Encrypt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.PriorityBlockingQueue;

@SuppressLint("SetJavaScriptEnabled")
public class WebReader extends RelativeLayout implements ReaderController {
    //private static final String JAVASCRIPT_CLEAR_DOCUMENT = "javascript:document.body.innerHTML = \"\"";
    private static final String JAVASCRIPT_CLEAR_DOCUMENT = "javascript:open('about:blank');";
    
    private static final int HANDLER_TAKE_SNAPSHOT = 1;
    
    private static final int HANDLER_THEME_CHANGED = 2;
    
    private static final int HANDLER_SHOW = 3;
    private static final int HANDLER_HIDE = 4;
    
    private static final int TAG_PAGINATION = 0xFF;
    
    public static class ThemeMode {
        public int bgColor;
        public int textColor;
        public String bgImage;
        public float fontSize;
        public float lineHeight;
        public String textAlign;
    }
    
    public enum ViewMode {
        RawViewMode, ImageViewMode
    }
    
    private Activity mActivity;
    
    private ViewMode mViewMode = ViewMode.RawViewMode;
    
    private boolean mUsingInternalGestureDetector = true;
    
    private Book mBook = new Book();
    private String mEncryptionKey = null;
    private String mRandom = null;
    
    /**
     * Paginating
     */
    private boolean mPaginating = true;
    private boolean mPaginationStart = false;
    
    private SparseArray<Chapter> mChapterList = new SparseArray<Chapter>();
    private SparseArray<PaginationResult> mPagePagination = new SparseArray<PaginationResult>();
    
    
    /*
     * RenderTask related parameters
     */
    private boolean mLocalHTTPServer = true;
    private boolean mUsingiFrame = true;
    private boolean mUsingChapterCache = true;
    private boolean mUsingContinuallyDiv = true;
    private boolean mTakeSnapshot = false;
    private PriorityBlockingQueue<PendingTask> mRenderingQueue = new PriorityBlockingQueue<PendingTask>();
    private PriorityBlockingQueue<PendingTask> mPaginatingQueue = new PriorityBlockingQueue<PendingTask>();
    private PendingTask mCurPendingTask;
//    private ReentrantReadWriteLock mRenderQueueLock = new ReentrantReadWriteLock();
    
    /**
     * Operation panel area
     */
    private Rect[] mNextPageArea = {
            new Rect(), new Rect()
    };
    private Rect[] mPreviousPageArea = {
            new Rect(), new Rect()
    };
    private Rect mCallToolbarArea = new Rect();
    
    private boolean mPageNavigationUnReachableAlerted = false;

    private IParser mParser = null;
    private MyHTTPD mHTTPD = null;
    
    private WebReaderView mWebViewPaginating;
    private WebReaderView mWebView;
    
    private ImageView mImageView;
    
    /**
     * Number of WebReaderView, currently used in modular fashion
     * @type {int}
     */
    private static final int mWebReaderViewSlots = 3;
    
    /**
     * Current Chapters in the three WebReaderViews - indexed by 0 <= slotIndex < mWebReaderViewSlots
     * @type {Array.<int>}
     */
    private WebReaderView[] mWebViews = new WebReaderView[mWebReaderViewSlots];
    private int[] mWebViewIndices = new int[mWebReaderViewSlots];
     
    private ViewGroup mImagePreviewContainer;
    private ImageView mImagePreviewView;
    private ViewGroup mTablePreviewContainer;
    private WebView mTablePreviewWebView;
    
    private Bitmap mImagePreviewBitmap;
    
    /**
     * Chapter Index, start from zero
     */
    private int mCurChapter = 0;
    
    /**
     * Page Index, start from zero
     */
    private int mCurPage = 0;
    private int mMaxPage = 1;
    private boolean mPageNavigating = false;
    private boolean mCurChapterReady = false;
    private boolean mBookReady = false;
    
    /**
     * AsyncTask
     */
    private RenderingTask mRenderingTask;
    private PaginatingTask mPaginatingTask;

    /**
     * Support video for WebChromeClient
     */
    private View mCustomView;
    private FrameLayout mCustomViewContainer;
    // the default <video> poster
    private Bitmap mDefaultVideoPoster;
    // the video progress view
    private View mVideoProgressView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    
    private LinkedList<String> mNavigationCommand = new LinkedList<String>();
    private HashMap<Integer, Bitmap> mCachedBitmaps = new HashMap<Integer, Bitmap>();
    
    private ArrayList<TouchableItem> mTouchableItems = new ArrayList<TouchableItem>();
    private ArrayList<PaginationResult.TableInfo> mTableRects = new ArrayList<PaginationResult.TableInfo>();
    private ArrayList<Rect> mSearchResults = new ArrayList<Rect>();
    
    // http://code.google.com/p/android/issues/detail?id=12987
    private boolean mJavascriptInterfaceBroken = false;
    private WebReaderBridge mRenderingJavascriptInterface;
    private WebReaderBridge mPaginatingJavascriptInterface;
    
    private GestureDetector mGestureDetector;
    
    private ReaderListener mReaderListener;
    
    /**
     * book setting
     */
    private ReaderSettings mReaderSettings;
    
    public WebReader(Context context) {
        this(context, null);
    }
    
    public WebReader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public WebReader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        WebReaderView view = new WebReaderView(context);
        mWebViewPaginating = view;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        addView(view, params);
        mWebViewPaginating.setVisibility(View.INVISIBLE);
        
        view = new WebReaderView(context);
        mWebViews[0] = view;
        addView(view, params);
        view.setVisibility(View.INVISIBLE);
        
        view = new WebReaderView(context);
        mWebViews[1] = view;
        addView(view, params);
        view.setVisibility(View.INVISIBLE);
        
        view = new WebReaderView(context);
        mWebViews[2] = view;
        addView(view, params);
        view.setVisibility(View.INVISIBLE);
        
        mImageView = new ImageView(context);
        addView(mImageView, params);
        mImageView.setVisibility(View.INVISIBLE);
        
        RelativeLayout relativeView = new RelativeLayout(context);
        mImagePreviewContainer = relativeView;
        addView(relativeView, params);
        relativeView.setVisibility(View.INVISIBLE);
        
        relativeView = new RelativeLayout(context);
        WebView webView = new WebView(context);
        relativeView.addView(webView, params);
        addView(relativeView, params);
        mTablePreviewContainer = relativeView;
        mTablePreviewWebView = webView;
        relativeView.setVisibility(View.INVISIBLE);
        
        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(Color.BLACK);
        addView(frame, params);
        mCustomViewContainer = frame;
        frame.setVisibility(View.INVISIBLE);
        
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
            mUsingiFrame = false;
        
        if (mUsingContinuallyDiv) {
            mUsingiFrame = false;
            mUsingChapterCache = false;
        }
    }
    
    private void changeTheme() {
        if (mReaderSettings.isThemeNight) {
            mImagePreviewContainer.setBackgroundColor(Color.BLACK);
            mTablePreviewContainer.setBackgroundColor(Color.BLACK);
        } else {
            if (mImagePreviewContainer.getBackground() != null && mImagePreviewContainer.getBackground() instanceof BitmapDrawable) {
                mImagePreviewContainer.getBackground().setCallback(null);
                if (mTablePreviewContainer.getBackground() != null)
                    mTablePreviewContainer.getBackground().setCallback(null);
                mImagePreviewContainer.setBackgroundDrawable(null);
                mTablePreviewContainer.setBackgroundDrawable(null);
                BitmapDrawable drawable = (BitmapDrawable) mImagePreviewContainer.getBackground();
                if (drawable != null && drawable.getBitmap() != null && !drawable.getBitmap().isRecycled())
                    drawable.getBitmap().recycle();
            }
            BitmapDrawable drawable = null;
            switch (mReaderSettings.mTheme) {
                case 0:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_1));
                    break;
                case 1:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_2));
                    break;
                case 2:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_3));
                    break;
                case 3:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_4));
                    break;
                case 4:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_5));
                    break;
                case 5:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_6));
                    break;
                case 6:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_7));
                    break;
                case 7:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_8));
                    break;
                case 8:
                    drawable = new BitmapDrawable(getResources().openRawResource(R.drawable.read_bg_9));
                    break;
            }
            mImagePreviewContainer.setBackgroundDrawable(drawable);
            mTablePreviewContainer.setBackgroundDrawable(drawable);
        }
    }
    
    private final Handler mCurrentUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLER_SHOW:
                switch (mViewMode) {
                    case RawViewMode:
                        mWebView.setVisibility(View.VISIBLE);
                        break;
                    case ImageViewMode:
                        mImageView.setVisibility(View.VISIBLE);
                        Bitmap bitmap = getPageBitmapAsync(mCurPage);
                        if (bitmap != null && !bitmap.isRecycled())
                            mImageView.setImageBitmap(bitmap);
                        break;
                }
                setVisibility(View.VISIBLE);
                break;
            case HANDLER_HIDE:
                mCurChapterReady = false;
                setVisibility(View.INVISIBLE);
                mImageView.setImageBitmap(null);
                if (mViewMode == ViewMode.ImageViewMode)
                    resetCachedBitmaps();
                for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex)
                    mWebViews[slotIndex].setVisibility(View.INVISIBLE);
                if (mReaderListener != null)
                    mReaderListener.onChapterLoading(msg.arg1);
                break;
            case HANDLER_TAKE_SNAPSHOT:
                if (mWebView != null && mViewMode == ViewMode.ImageViewMode) {
                    if (mCachedBitmaps.containsKey(Integer.valueOf(msg.arg1)))
                        mWebView.takeSnapshot(mCurChapter, msg.arg1, mCachedBitmaps.get(msg.arg1), false);
                    
                    if (!mNavigationCommand.isEmpty()) {
                        String command = mNavigationCommand.poll();
                        if (command != null)
                            mWebView.loadUrl(command);
                    }
                }
                synchronized(WebReader.this) {
                    WebReader.this.notify();
                }
                break;
            case HANDLER_THEME_CHANGED:
                changeTheme();
                break;
            }
        }
    };
    
    /**
     * Gesture Event Handler
     */
    private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent event) {
            if (handleSingleTap(event))
                return true;
//            if (mViewMode == ViewMode.ImageViewMode) {
//                if (handleSingleTap(event))
//                    return true;
//            }
            return super.onDown(event);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
//            if (mViewMode == ViewMode.RawViewMode && handleSingleTap(event))
//                return true;
            return super.onSingleTapConfirmed(event);
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
//            if (handleDoubleTag(event))
//                return true;
            return false;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            final int DECISION_FLING_HORIZONTAL = 100;
            
            float x1 = event1.getX();
            float x2 = event2.getX();
            int movement = (int) (x2 - x1) / DECISION_FLING_HORIZONTAL;

            if (movement > 0) {
                previousPageImpl();
            }
            else if (movement < 0) {
                nextPageImpl();
            }
            return super.onFling(event1, event2, velocityX, velocityY);
        }

        @Override
        public void onShowPress(MotionEvent event) {
            super.onShowPress(event);
        }

        @Override
        public void onLongPress(MotionEvent event) {
            super.onLongPress(event);
        }
    };
    
    private OnTouchListener mTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mGestureDetector.onTouchEvent(event))
                return true;
            return false;
        }

    };
    
    private WebViewClient mWebViewClient = new WebViewClient() {

//      @Override
//      public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
//          WebResourceResponse response = null;
//          if (mParser != null) {
//              synchronized (mParser) {
//                  try {
//                      if (url.startsWith("file:///android_asset/"))
//                          url = url.substring("file:///android_asset/".length());
//                      url = url.startsWith("/") ? url.substring(1) :url;
//
//                      // Get MIME type from file name extension, if possible
//                      String mime = NanoHTTPD.getMimeType(url);
//      
//                      Pair<? extends InputStream, Long> resouce = mParser.getFileStream(url);
//                      if (resouce.first != null)
//                          response = new WebResourceResponse(mime, "UTF-8", resouce.first);
//                  } catch (Exception e) {
//                  } finally {
//                  }
//              }
//          }
//          return (response != null) ? response : super.shouldInterceptRequest(view, url);
//      }

      @Override
      public void onLoadResource(WebView view, String url) {
          if (!TextUtils.isEmpty(url) && (url.toLowerCase().contains(".html") || url.toLowerCase().contains(".htm") || url.toLowerCase().contains(".xml") || url.toLowerCase().contains(".xhtml"))) {
              if (overrideUrlLoading(view, url))
                  return;
          } else if (!TextUtils.isEmpty(url) && (url.startsWith(MyHTTPD.HTTPD_URL_BASE) || url.startsWith("file:///android_asset/"))) {
              super.onLoadResource(view, url);
          }
          return;
      }

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
          super.onPageStarted(view, url, favicon);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
          super.onPageFinished(view, url);
      }

      @Override
      public void onReceivedError(WebView view, int errorCode, String description,
              String failingUrl) {
          super.onReceivedError(view, errorCode, description, failingUrl);
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
          if (overrideUrlLoading(view, url))
              return true;
          
          return true;//super.shouldOverrideUrlLoading(view, url);
      }
      
  };

  private WebChromeClient mWebChromeClient = new WebChromeClient() {

      @Override
      public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
          result.confirm();
          if (!TextUtils.isEmpty(message)) {
              String jsIntefaceTag = "window.bridge.";
              boolean paginatingInterface = false;
              Object tag = view.getTag();
              if (tag != null && tag instanceof Integer && (TAG_PAGINATION == (Integer)tag))
                  paginatingInterface = true;
              WebReaderBridge javascriptInterface = paginatingInterface ? mPaginatingJavascriptInterface : mRenderingJavascriptInterface;
              
              if (message.startsWith(jsIntefaceTag)) {
                  int bracketsStartIndex = message.indexOf("(");
                  int bracketsEndIndex = message.indexOf(")");
                  
                  String functionName = message.substring(0, bracketsStartIndex);
                  functionName = functionName.substring(jsIntefaceTag.length());
                  String parameters = (bracketsStartIndex + 1 < bracketsEndIndex) ? message.substring(bracketsStartIndex + 1, bracketsEndIndex) : "";
                  String[] parametersArray = parameters.split(",");
                  
                  if ("d".equals(functionName)) {
                      javascriptInterface.d(parametersArray[0]);
                  } else if ("e".equals(functionName)) {
                      javascriptInterface.e(parametersArray[0]);
                  } else if ("onReaderInitialized".equals(functionName)) {
                      javascriptInterface.onReaderInitialized();
                  } else if ("onReaderunInitialized".equals(functionName)) {
                      javascriptInterface.onReaderunInitialized();
                  } else if ("onPreferencesApplied".equals(functionName)) {
                      javascriptInterface.onPreferencesApplied();
                  } else if ("onChapterLoading".equals(functionName)) {
                      int chapterIndex = Integer.parseInt(parametersArray[0]);
                      javascriptInterface.onChapterLoading(chapterIndex);
                  } else if ("onChapterReady".equals(functionName)) {
                      int chapterindex = Integer.parseInt(parametersArray[0]);
                      String pageBounds = Uri.decode(parametersArray[1]);
                      String pageOffsets = Uri.decode(parametersArray[2]);
                      String touchables = Uri.decode(parametersArray[3]);
                      String tableBounds = parametersArray.length == 5 ? Uri.decode(parametersArray[4]) : "";
                      javascriptInterface.onChapterReady(chapterindex, pageBounds, pageOffsets, touchables, tableBounds);
                  } else if ("onSearchFinished".equals(functionName)) {
                      String searchResults = Uri.decode(parametersArray[0]);
                      javascriptInterface.onSearchFinished(searchResults);
                  } else if ("onPageNavigationFinished".equals(functionName)) {
                      int chapterIndex = Integer.parseInt(parametersArray[0]);
                      javascriptInterface.onPageNavigationFinished(chapterIndex);
                  } else if ("onJump2Node".equals(functionName)) {
                      int chapterIndex = Integer.parseInt(parametersArray[0]);
                      String result1 = parametersArray.length == 2 ? Uri.decode(parametersArray[1]) : "";
                      javascriptInterface.onJump2Node(chapterIndex, result1);
                  } else if ("onGetNodeRect".equals(functionName)){
                	  int chapterIndex = Integer.parseInt(parametersArray[0]);
                	  int nodeIdIndex = Integer.parseInt(parametersArray[1]);
                	  String result2 = parametersArray.length == 3 ? Uri.decode(parametersArray[2]) : "";
                	  javascriptInterface.onGetNodeRect(chapterIndex, nodeIdIndex, result2);
                  }else if ("onAlignApplied".equals(functionName)) {
                      javascriptInterface.onAlignApplied();
                  } else if ("shouldOverrideUrlLoading".equals(functionName)) {
                      String href = parametersArray[0];
                      javascriptInterface.shouldOverrideUrlLoading(href);
                  } else if ("onThemeApplied".equals(functionName)) {
                      javascriptInterface.onThemeApplied();
                  }
              }
          }
          return true; // super.onJsAlert(view, url, message, result);
      }

      @Override
      public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
          result.confirm();
          return true; // super.onJsConfirm(view, url, message, result);
      }

      @Override
      public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
              JsPromptResult result) {
          result.confirm();
          return true; // super.onJsPrompt(view, url, message, defaultValue, result);
      }

      @Override
      public void onShowCustomView(View view, CustomViewCallback callback) {
          final FrameLayout.LayoutParams COVER_SCREEN_PARAMS =
                  new FrameLayout.LayoutParams(
                          ViewGroup.LayoutParams.FILL_PARENT,
                          ViewGroup.LayoutParams.FILL_PARENT);

          final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER =
                  new FrameLayout.LayoutParams(
                          ViewGroup.LayoutParams.FILL_PARENT,
                          ViewGroup.LayoutParams.FILL_PARENT,
                          Gravity.CENTER);

          // if a view already exists then immediately terminate the new one
          if (mCustomView != null) {
              callback.onCustomViewHidden();
              return;
          }

          // Add the custom view to its container.
          mCustomViewContainer.addView(view, COVER_SCREEN_GRAVITY_CENTER);
          mCustomView = view;
          mCustomViewCallback = callback;
          mCustomViewContainer.setVisibility(View.VISIBLE);
          mCustomViewContainer.bringToFront();
      }

      @Override
      public Bitmap getDefaultVideoPoster() {
          if (mDefaultVideoPoster == null) {
              mDefaultVideoPoster = BitmapFactory.decodeResource(
                      getResources(), R.drawable.default_video_poster);
          }
          return mDefaultVideoPoster;
      }

      @Override
      public void onHideCustomView() {
          if (mCustomView == null)
              return;
          // Hide the custom view.
          mCustomView.setVisibility(View.GONE);
          // Remove the custom view from its container.
          mCustomViewContainer.removeView(mCustomView);
          mCustomView = null;
          mCustomViewContainer.setVisibility(View.GONE);
          mCustomViewCallback.onCustomViewHidden();
      }

      @Override
      public View getVideoLoadingProgressView() {
          if (mVideoProgressView == null) {
              LayoutInflater inflater = LayoutInflater.from(getContext());
              mVideoProgressView = inflater.inflate(R.layout.video_loading_progress, null);
          }
          return mVideoProgressView;
      }

  };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
    
    private synchronized void resetCachedBitmaps() {
        try {
            boolean sizeChanged = false;
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            if (!mCachedBitmaps.isEmpty() && mCachedBitmaps.values() != null) {
                Bitmap[] bitmaps = new Bitmap[3];
                mCachedBitmaps.values().toArray(bitmaps);
                if (bitmaps[0].getWidth() != width || bitmaps[0].getHeight() != height) {
                    sizeChanged = true;
                    for (Bitmap bitmap : bitmaps)
                        if (bitmap != null && !bitmap.isRecycled())
                            bitmap.recycle();
                }
            }
            
            if (sizeChanged)
                mCachedBitmaps.clear();
            
            if (mCachedBitmaps.isEmpty()) {
                for (int index = 0; index < 3; ++index) {
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    mCachedBitmaps.put(Integer.valueOf(index), bitmap);
                }
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
    }
    
    private synchronized void recycleCachedBitmaps() {
        if (!mCachedBitmaps.isEmpty() && mCachedBitmaps.values() != null) {
            Bitmap[] bitmaps = new Bitmap[3];
            mCachedBitmaps.values().toArray(bitmaps);
            for (Bitmap bitmap : bitmaps)
                if (bitmap != null && !bitmap.isRecycled())
                    bitmap.recycle();
        }
    }
    
    /**
     * Initializes Touch Areas
     */
    private void initTouchAreas() {

        mCallToolbarArea.setEmpty();
        mPreviousPageArea[0].setEmpty();
        mPreviousPageArea[1].setEmpty();
        mNextPageArea[0].setEmpty();
        mNextPageArea[1].setEmpty();
        int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;

        int areaWidth = screenWidth / 3;
        int areaHeight = screenHeight / 3;

        mCallToolbarArea.left = areaWidth;
        mCallToolbarArea.right = mCallToolbarArea.left + areaWidth;
        mCallToolbarArea.top = areaHeight;
        mCallToolbarArea.bottom = mCallToolbarArea.top + areaHeight;

        mPreviousPageArea[0].left = 0;
        mPreviousPageArea[0].top = 0;
        mPreviousPageArea[0].right = mPreviousPageArea[0].left + areaWidth;
        mPreviousPageArea[0].bottom = screenHeight;

        mPreviousPageArea[1].left = 0;
        mPreviousPageArea[1].top = 0;
        mPreviousPageArea[1].right = screenWidth - areaWidth;
        mPreviousPageArea[1].bottom = areaHeight;

        mNextPageArea[0].left = screenWidth - areaWidth;
        mNextPageArea[0].right = screenWidth;
        mNextPageArea[0].top = 0;
        mNextPageArea[0].bottom = screenHeight;

        mNextPageArea[1].left = areaWidth;
        mNextPageArea[1].top = screenHeight - areaHeight;
        mNextPageArea[1].right = screenWidth;
        mNextPageArea[1].bottom = screenHeight;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mRenderingTask = new RenderingTask();
        
        initTouchAreas();
        
        mReaderSettings = new ReaderSettings(getContext());
        
//        mImagePreviewContainer = (ViewGroup) findViewById(R.id.imagePreviewContainer);
        
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ECLAIR_MR1)
            mImagePreviewView = new ImageViewTouch(getContext());
        else
            mImagePreviewView = new TouchImageView(getContext());
        mImagePreviewContainer.addView(mImagePreviewView, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        mImagePreviewView.setClickable(true);
        
//        mCustomViewContainer = (FrameLayout) findViewById(R.id.fullscreen_custom_content);
        
//        mTablePreviewContainer = (ViewGroup) findViewById(R.id.imagePreviewContainer2);
//        mTablePreviewWebView = (WebView) findViewById(R.id.imagePreviewWebview);
        mTablePreviewWebView.setClickable(true);
        mTablePreviewWebView.setFocusable(true);
        mTablePreviewWebView.setFocusableInTouchMode(true);
        mTablePreviewWebView.setHorizontalScrollBarEnabled(true);
        mTablePreviewWebView.setVerticalScrollBarEnabled(true);
        WebSettings tableSettings = mTablePreviewWebView.getSettings();
        tableSettings.setSupportZoom(true);
        tableSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        tableSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        tableSettings.setBuiltInZoomControls(true);
        tableSettings.setJavaScriptEnabled(true);
        tableSettings.setAllowFileAccess(true);
        tableSettings.setPluginsEnabled(true);
        tableSettings.setJavaScriptCanOpenWindowsAutomatically(true);
//        tableSettings.setPluginState(WebSettings.PluginState.ON);
//        mTablePreviewWebView.setBackgroundColor(Color.TRANSPARENT);
        
//        mWebViewPaging = (WebReaderView) findViewById(R.id.webViewPaging);
        mWebViewPaginating.setTag(TAG_PAGINATION);
        mWebViewPaginating.setVisibility(View.INVISIBLE);
        mWebViewPaginating.setClickable(true);
        mWebViewPaginating.setFocusable(false);
//      if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB)
//      mWebViewPaging.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
//        mWebViewPaging.setInitialScale(100);
        mWebViewPaginating.setHorizontalScrollBarEnabled(false);
        mWebViewPaginating.setVerticalScrollBarEnabled(false);
//        mWebViewPaging.setBackgroundColor(Color.TRANSPARENT);
        mWebViewPaginating.clearFormData();
        mWebViewPaginating.clearCache(true);
        mWebViewPaginating.clearHistory();
        WebSettings pagingSettings = mWebViewPaginating.getSettings();
        pagingSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        pagingSettings.setJavaScriptEnabled(true);
        pagingSettings.setAllowFileAccess(true);
        pagingSettings.setPluginsEnabled(true);
        pagingSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO)
            pagingSettings.setPluginState(WebSettings.PluginState.ON); // Since: API Level 8
        pagingSettings.setRenderPriority(RenderPriority.LOW);
        pagingSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        pagingSettings.setSupportZoom(false);
//        pagingSettings.setDefaultFontSize((int)mReaderSettings.mTextSize);
//        pagingSettings.setDefaultFixedFontSize((int)mReaderSettings.mTextSize);
        mWebViewPaginating.setReaderSettings(mReaderSettings);
        
//        mWebViews[0] = (WebReaderView) findViewById(R.id.webViewRendering0);
//        mWebViews[1] = (WebReaderView) findViewById(R.id.webViewRendering1);
//        mWebViews[2] = (WebReaderView) findViewById(R.id.webViewRendering2);
        for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex) {
            WebReaderView webView = mWebViews[slotIndex];
            webView.setVisibility(View.INVISIBLE);
            webView.setTag(0);
            webView.setClickable(true);
            webView.setFocusable(false);
    //        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB)
    //            mWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
//            webView.setInitialScale(100);
            webView.setHorizontalScrollBarEnabled(false);
            webView.setVerticalScrollBarEnabled(false);
//            webView.setBackgroundColor(Color.TRANSPARENT);
            webView.clearFormData();
            webView.clearCache(true);
            webView.clearHistory();
            WebSettings websettings = webView.getSettings();
            websettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    //      websettings.setBlockNetworkImage(true);
    //      websettings.setBlockNetworkLoads(true);
            websettings.setJavaScriptEnabled(true);
            websettings.setAllowFileAccess(true);
            websettings.setPluginsEnabled(true);
            websettings.setUseWideViewPort(true);
            websettings.setJavaScriptCanOpenWindowsAutomatically(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO)
                websettings.setPluginState(WebSettings.PluginState.ON); // Since: API Level 8
            websettings.setRenderPriority(RenderPriority.HIGH);
            websettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            websettings.setSupportZoom(false);
//            websettings.setDefaultFontSize((int)mReaderSettings.mTextSize);
    //      websettings.setLoadWithOverviewMode(false);
//            websettings.setDefaultFixedFontSize((int)mReaderSettings.mTextSize);
    //      websettings.setUseWideViewPort(true);
    //      websettings.setLoadsImagesAutomatically(false);
            webView.setReaderSettings(mReaderSettings);
        }
        
        for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex) {
            mWebViewIndices[slotIndex] = -1;
            mWebViews[slotIndex].setJavascriptInjected(false);
        }
        
        mGestureDetector = new GestureDetector(getContext(), mGestureListener);
        
        mRenderingJavascriptInterface = new WebReaderBridge(false);
        for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex) {
            WebReaderView webView = mWebViews[slotIndex];
            webView.addJavascriptInterface(mRenderingJavascriptInterface, mRenderingJavascriptInterface.getInterfaceName());
            
            webView.setWebChromeClient(mWebChromeClient);
            webView.setWebViewClient(mWebViewClient);
            
            if (mUsingInternalGestureDetector)
                webView.setExternalOnTouchListener(mTouchListener);
        }
        
        mPaginatingJavascriptInterface = new WebReaderBridge(true);
        mWebViewPaginating.addJavascriptInterface(mPaginatingJavascriptInterface, mPaginatingJavascriptInterface.getInterfaceName());
        
        mImageView.setOnTouchListener(mTouchListener);
        
        changeTheme();
    }
    
    private synchronized void stopBackgroundTask() {
        final RenderingTask openTask = mRenderingTask;
        if (openTask != null && openTask.getStatus() != ParallelTask.Status.FINISHED) {
            PendingTask task = new PendingTask(PendingTask.PendingTaskType.Kill);
            offerRendingTask(task);
            
            openTask.cancel();
            mRenderingTask = null;
        }
        
        final PaginatingTask pagingTask = mPaginatingTask;
        if (pagingTask != null && pagingTask.getStatus() != ParallelTask.Status.FINISHED) {
            PendingTask task = new PendingTask(PendingTask.PendingTaskType.Kill);
            offerPaginatingTask(task);
            
            pagingTask.cancel();
            mPaginatingTask = null;
        }
    }
    
    private void resetWebWebView(int chapterIndex) {
        int slotIndex = getSlot(chapterIndex);
        mWebView = mWebViews[slotIndex];
    }
    
    private int getSlot(int chapterIndex) {
        if (!mUsingChapterCache)
            return 0;
        return chapterIndex % mWebReaderViewSlots;
    }
    
    private void applyRuntimeSetting(boolean background) {
        if (!background) {
            mCurrentUIHandler.obtainMessage(HANDLER_HIDE, mCurChapter, 0).sendToTarget();
        }
        
        final float density = getResources().getDisplayMetrics().density;
        final int leftMargin = (int) mReaderSettings.mLeftMargin;
        final int topMargin = (int) mReaderSettings.mTopMargin;
        final int rightMargin = (int) mReaderSettings.mRightMargin;
        final int bottomMargin = (int) mReaderSettings.mBottomMargin;
        
        final int screenWidth = getResources().getDisplayMetrics().widthPixels;
        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        final int width = screenWidth - leftMargin - rightMargin;
        final int height = screenHeight - topMargin - bottomMargin;
        
        mActivity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex) {
                    if (mWebViewIndices[slotIndex] >= 0) {
                        WebReaderView webView = mWebViews[slotIndex];
                        if (mUsingContinuallyDiv)
                            webView.loadUrl(String.format("javascript:applyRuntimeSetting(%d, %d, %d, %d, %d, \"%s\", %f, %f );", mWebViewIndices[slotIndex], (int)(screenWidth / density), (int)(screenHeight / density), (int)(width / density), (int)(height / density), mReaderSettings.mFontFamily, mReaderSettings.mTextSize, mReaderSettings.mLineHeight));
                        else
                            webView.loadUrl(String.format("javascript:applyRuntimeSetting(%d, %d, %d, %d, %d, \"%s\", %f, %f );", mWebViewIndices[slotIndex], (int)(width / density), (int)(height / density), (int)(width / density), (int)(height / density), mReaderSettings.mFontFamily, mReaderSettings.mTextSize, mReaderSettings.mLineHeight));
                    }
                }
                //mWebView.loadUrl(String.format("javascript:applyRuntimeSetting(%d, %d, %d, %d, %d, \"%s\", %f, %f );", mCurChapter, (int)(width / density), (int)(height / density), (int)(width / density), (int)(height / density), mReaderSettings.mFontFamily, mReaderSettings.mTextSize, mReaderSettings.mLineHeight));
            }
        });
    }
    
    /**
     * Open the given chapter in given WebView
     * @param chapterIndex chapter index
     * @param webView webview
     */
    private synchronized void loadChapter(final int chapterIndex, final WebReaderView webView, boolean backgroundRunning) {
        // check the parameters
        if (mChapterList == null || webView == null || chapterIndex < 0 || chapterIndex >= mChapterList.size())
            return;
        
        if (!backgroundRunning) {
            if (!mUsingContinuallyDiv || mPaginating)
                mCurrentUIHandler.obtainMessage(HANDLER_HIDE, chapterIndex, 0).sendToTarget();
        }

        Chapter chapter = mChapterList.get(chapterIndex);
        
        String html = "";
        if (!webView.isJavascriptInjected()) {
            String content = null;
            if (mParser != null) {
                synchronized (mParser) {
                    try {
                        content = mParser.getChapterContent(chapter.src);
                    } catch (Exception e) {
                    } finally {
                    }
                }
            }
            if (content == null)
                return;
            if (mUsingiFrame)
                html = preProcessingIFrameContainerHtmlContent(chapterIndex, content, true);
            else
                html = preProcessDivContainerHtmlContent(chapterIndex, content, true);
            webView.setJavascriptInjected(true);
        } else {
            mActivity.runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    webView.loadUrl(String.format("javascript:loadChapter(%d);", chapterIndex));
                }
            });
            return;
        }

        // load data on webView
        final String baseUrl = mLocalHTTPServer ? MyHTTPD.HTTPD_URL_BASE: "file:///android_asset/";
        final String data = html;
        mActivity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                webView.loadDataWithBaseURL(baseUrl, data, "text/html", "UTF-8", null);
            }
        });
    }
    
    private synchronized void paginatingChapter(final int chapterIndex, final WebReaderView webView, boolean backgroundRunning) {
        // check the parameters
        if (mChapterList == null || webView == null || chapterIndex < 0 || chapterIndex >= mChapterList.size())
            return;
        
        if (!backgroundRunning) {
            if (!mUsingContinuallyDiv || mPaginating)
                mCurrentUIHandler.obtainMessage(HANDLER_HIDE, chapterIndex, 0).sendToTarget();
        }

        Chapter chapter = mChapterList.get(chapterIndex);
        
        String html = "";
        if (!webView.isJavascriptInjected()) {
            String content = null;
            if (mParser != null) {
                synchronized (mParser) {
                    try {
                        content = mParser.getChapterContent(chapter.src);
                    } catch (Exception e) {
                    } finally {
                    }
                }
            }
            if (content == null)
                return;
            if (mUsingiFrame)
                html = preProcessingIFrameContainerHtmlContent(chapterIndex, content, true);
            else
                html = preProcessDivContainerHtmlContent(chapterIndex, content, true);
            webView.setJavascriptInjected(true);
        } else {
            mActivity.runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    webView.loadUrl(String.format("javascript:loadChapter(%d);", chapterIndex));
                }
            });
            return;
        }

        // load data on webView
        final String baseUrl = mLocalHTTPServer ? MyHTTPD.HTTPD_URL_BASE: "file:///android_asset/";
        final String data = html;
        mActivity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                webView.loadDataWithBaseURL(baseUrl, data, "text/html", "UTF-8", null);
            }
        });
    }
    

    /**
     * Preprocess the iframe html container
     * @param chapterIndex
     * @param content
     * @param injectJavascript
     * @return
     */
    private String preProcessingIFrameContainerHtmlContent(int chapterIndex, String content, boolean injectJavascript) {
        String html = "";

        StringBuilder htmlContentBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(content)) {
            float density = getResources().getDisplayMetrics().density;
            int leftMargin = (int) mReaderSettings.mLeftMargin;
            int topMargin = (int) mReaderSettings.mTopMargin;
            int rightMargin = (int) mReaderSettings.mRightMargin;
            int bottomMargin = (int) mReaderSettings.mBottomMargin;

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int width = getResources().getDisplayMetrics().widthPixels - leftMargin - rightMargin;
            int height = getResources().getDisplayMetrics().heightPixels - topMargin - bottomMargin;
            
            int sdkVersion = android.os.Build.VERSION.SDK_INT;
            
            htmlContentBuilder.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
            
            // process HEAD element
            /*
             * <head> Element
             * 
             * 1. append common library
             * 2. append paginator library
             * 3. append selection library
             * 4. set font face
             */
            {
                if (mLocalHTTPServer)
                    htmlContentBuilder.append("<base href=\"" + MyHTTPD.HTTPD_URL_BASE + "\" target=\"_blank\" />\n");
                htmlContentBuilder.append("<meta id=\"viewport_id\" name=\"viewport\" content=\"initial-scale=1.0,user-scalable=no\" />\n");
                
                // 1. append common library
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/jquery/jquery.js\"></script>\n");
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/util_common.js\"></script>\n");
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/entity.js\"></script>\n");
                
                // 2. append paginator library
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/paginator_v2.js\"></script>\n");
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/reader_v2_extra.js\"></script>\n");
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/offset.js\"></script>\n");
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/paginator_v2_extra.js\"></script>\n");
                
                // 3. append selection library
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/rangy/rangy-core.js\"></script>\n");
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/rangy/rangy-serializer.js\"></script>\n");
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/rangy/rangy-cssclassapplier.js\"></script>\n");
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/selection.js\"></script>\n");
                
                // 4. set the css file
                /*
                */
                htmlContentBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/css/content.css\" />\n");
                
                // 5. set font face
                // set css style for font-type
                if (!TextUtils.isEmpty(mReaderSettings.mFontFamily)) {
                    htmlContentBuilder.append("<style type=\"text/css\">\n"
                            + "@font-face {\n"
                            + "font-family: \'custom_font\' ; \n"
                            + "src: url('"
                            + "file://" + mReaderSettings.mFontUrl
                            + "'); }\n"
                            + "</style>\n");
                }
                
                // 6. set the div p max width
                htmlContentBuilder.append("<style type=\"text/css\">\n"
                        + "p {\n"
                        + "max-width: " + (int)(width / density) + "px !important; \n"
                        + "}\n"
                        + "div {\n"
                        + "max-width: " + (int)(width / density) + "px !important; \n"
                        + "}\n"
                        + "img {\n"
                        + "max-width: " + (int)(width / density) + "px !important; \n"
                        + "max-height: " + (int)(height / density) + "px !important; \n"
                        + "}\n"
                        + "table {\n"
                        + "max-width: " + (int)(width / density) + "px !important; \n"
                        + "}\n"
                        + "</style>\n");
                
                // 7. insert the theme css
                htmlContentBuilder.append("<style type=\"text/css\">\n"
                        + "body.day-mode1 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_1.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        //+ "    background-repeat:no-repeat;\n"
                        + "}\n"
                        + "body.day-mode2 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_2.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode3 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_3.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode4 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_4.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        //+ "    background-repeat:no-repeat;\n"
                        + "}\n"
                        + "body.day-mode5 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_5.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode6 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_6.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode7 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_7.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        //+ "    background-repeat:no-repeat;\n"
                        + "}\n"
                        + "body.day-mode8 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_8.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode9 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_9.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "</style>\n");
                
                // 8. Insert running Javascript
                if (injectJavascript) {
                    htmlContentBuilder.append("<script type=\"text/javascript\">\n"
                            + "function injectPagination() {\n"
                            + "initializeReader(" + mChapterList.size() + ");\n"
                            + "setDebugInfo(false, false, false, false);\n"
                            + "applyPreferences(" + (int)(screenWidth / density) + ","
                            + (int)(screenHeight / density) + "," 
                            + (int)(width / density) + ","
                            + (int)(height / density) + ",\""
                            + mReaderSettings.mFontFamily + "\","
                            + mReaderSettings.mTextZoom + ","
                            + mReaderSettings.mLineHeight + ",\""
                            + mReaderSettings.mTextAlign + "\","
                            + mReaderSettings.isThemeNight + ","
                            + mReaderSettings.mTheme + ","
                            + density + ","
                            + sdkVersion
                            + ");\n"
                            + "loadChapter(" + chapterIndex + ");\n"
                            + "}\n"
                            + (injectJavascript ? ("window.addEventListener('load', injectPagination, false);\n") : "")
                            + "</script>\n");
                }
                htmlContentBuilder.append("</head>\n");
            }
            
            // process BODY element
            /*
             * <body> Element
             * 
             * 1. insert iframe container div
             */
            {
                // 1. insert body start tag
                htmlContentBuilder.append("<body ");
                htmlContentBuilder.append("style=\"" + "margin:0 0 0 0; "
                        + "padding:0 0 0 0; border:none; overflow:hidden; \" "
                        //+ (injectJavascript ? "onload=\"injectPagination()\" " : "")
                        + ">\n");
                
                // 2. insert div for iframe container
                htmlContentBuilder.append("<div id=\"iframe_container\" style=\""
                                + "margin:0 0 0 0; position: absolute !important; "
                                + "padding:" + (int)(topMargin / density) + "px " + (int)(rightMargin / density) + "px " + (int)(bottomMargin / density) + "px " + (int)(leftMargin / density) + "px " + "; "
                                + "top: 0px; left: 0px; overflow:hidden; zoom: 1; border:none;  "//border:dashed red;
                                + "\">\n"
                                );
                
                htmlContentBuilder.append("</div>\n");
                
                htmlContentBuilder.append("<!-- offscreen div to keep custom fonts warm in webcore cache -->\n"
                        + "<div style=\"position: absolute; top: -1024px; left: -1024px;\" id=\"android_books_font_holder\">\n"
                        + "<!--<div style=\"font-family:'Merriweather';\">Lorem Ipsum</div>-->\n"
                        + "<div style='height: 1in; width: 1in;' id=\"android_books_inch_ruler\">Lorem Ipsum</div>\n"
                        + "</div>\n");
                
                // 3. insert body end tag
                htmlContentBuilder.append("</body>\n");
            }
            htmlContentBuilder.append("</html>");
        }
        html = htmlContentBuilder.toString();
        return html;
    }
    
    private String preProcessDIVChapterContent(int chapterIndex) {        
        Chapter chapter = mChapterList.get(chapterIndex);
        String content = "";
        StringBuilder chapterContentBuilder = new StringBuilder();
        chapterContentBuilder.append(content);
        if (mParser != null) {
            synchronized (mParser) {
                try {
                    content = mParser.getChapterContent(chapter.src);
                    
                    int bodyEndTagIndex = content.lastIndexOf("</body>");
                    int bodyStartTagIndex = content.indexOf("<body");
                    if (mParser.getParserType() == IParser.ParserType.UMD) {
                        if (bodyEndTagIndex != -1 && bodyStartTagIndex != -1) {
                            chapterContentBuilder.append("<div id=\'chapter_content\' ");
                            chapterContentBuilder.append(content.subSequence(
                                    bodyStartTagIndex + 5, bodyEndTagIndex));
                            chapterContentBuilder.append("</div>");
                        } else {
                            chapterContentBuilder.append("<div id=\'chapter_content\' >\n");
                            String[] sigments = content.split("###");
                            chapterContentBuilder.append("<div>\n");
                            for (String object : sigments) {
                                String[] subSigments = object.split("\n");
                                chapterContentBuilder.append("<p>\n");
                                for (String subObject : subSigments) {
                                    chapterContentBuilder.append(subObject);
                                    chapterContentBuilder.append("</p>\n");
                                    chapterContentBuilder.append("<p>\n");
                                }
                                chapterContentBuilder.append("</p>\n");
        
                                chapterContentBuilder.append("</div>\n");
                                chapterContentBuilder.append("<div>\n");
                            }
                            chapterContentBuilder.append("</div>\n");
                            chapterContentBuilder.append("</div>\n");
                        }
                    } else if (mParser.getParserType() == IParser.ParserType.Epub) {
                        chapterContentBuilder.append(content);
                    } else if (mParser.getParserType() == IParser.ParserType.Html) {
                        if (bodyEndTagIndex != -1 && bodyStartTagIndex != -1) {
                            chapterContentBuilder.append("<div id=\'chapter_content\'  ");
                            chapterContentBuilder.append(content.subSequence(
                                    bodyStartTagIndex + 5, bodyEndTagIndex));
                            chapterContentBuilder.append("</div>");
                        } else {
                            chapterContentBuilder.append("<div id=\'chapter_content\'  >\n");
                            chapterContentBuilder.append(content);
                            chapterContentBuilder.append("</div>");
                        }
                    } else if (mParser.getParserType() == IParser.ParserType.CHM) {
                        if (bodyEndTagIndex != -1 && bodyStartTagIndex != -1) {
                            chapterContentBuilder.append("<div id=\'chapter_content\'  ");
                            chapterContentBuilder.append(content.subSequence(
                                    bodyStartTagIndex + 5, bodyEndTagIndex));
                            chapterContentBuilder.append("</div>");
                        } else {
                            chapterContentBuilder.append("<div id=\'chapter_content\'  >\n");
                            chapterContentBuilder.append(content);
                            chapterContentBuilder.append("</div>");
                        }
                    } else if (mParser.getParserType() == IParser.ParserType.EBK2) {
                        chapterContentBuilder.append("<div id=\'chapter_content\'>\n");
                        String[] subSigments = content.split("\n");
                        chapterContentBuilder.append("<p>\n");
                        for (String subObject : subSigments) {
                            chapterContentBuilder.append(subObject);
                            chapterContentBuilder.append("</p>\n");
                            chapterContentBuilder.append("<p>\n");
                        }
                        chapterContentBuilder.append("</p>\n");

                        chapterContentBuilder.append("</div>\n");
                    }
                } catch (Exception e) {
                } finally {
                }
            }
        }
        
        return chapterContentBuilder.toString();
    }
    
    private String preProcessIFrameChapterContent(int chapterIndex) {
        float density = getResources().getDisplayMetrics().density;
        int leftMargin = (int) mReaderSettings.mLeftMargin;
        int topMargin = (int) mReaderSettings.mTopMargin;
        int rightMargin = (int) mReaderSettings.mRightMargin;
        int bottomMargin = (int) mReaderSettings.mBottomMargin;

        int width = getResources().getDisplayMetrics().widthPixels - leftMargin - rightMargin;
        int height = getResources().getDisplayMetrics().heightPixels - topMargin - bottomMargin;
        
        Chapter chapter = mChapterList.get(chapterIndex);
        String content = null;
        if (mParser != null) {
            synchronized (mParser) {
                try {
                    content = mParser.getChapterContent(chapter.src);
                } catch (Exception e) {
                } finally {
                }
            }
        }
        if (content == null)
            return "";
        
        StringBuilder iFrameContentBuilder = new StringBuilder();
        
        // process IFRAME element
        /*
         * <iframe> Element
         * 
         * 1. insert pagination div
         * 2. append chapter content
         */
        
        // 1. contruct the head element
        int headEndTagIndex = content.indexOf("</head>");
        if (headEndTagIndex > 0) {
            iFrameContentBuilder.append(content.subSequence(0, headEndTagIndex));
            iFrameContentBuilder.append("\n");
        } else {
            // there is no head tag in html string
            iFrameContentBuilder.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
        }
        
        // 2. set the day or night css file
        iFrameContentBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/css/content.css\" />\n");
        
        // 3. set the div p max width
        iFrameContentBuilder.append("<style type=\"text/css\">\n"
                + "p {\n"
                + "max-width: " + (int)(width / density) + "px !important; \n"
                + " }\n"
                + "div {\n"
                + "max-width: " + (int)(width / density) + "px !important; \n"
                + " }\n"
                + "img {\n"
                + "max-width: " + (int)(width / density) + "px !important; \n"
                + "max-height: " + (int)(height / density) + "px !important; \n"
                + " }\n"
                + "table {\n"
                + "max-width: " + (int)(width / density) + "px !important; \n"
                + " }\n"
                + "</style>\n");
        
        iFrameContentBuilder.append("</head>\n");
        
        // 4. contruct body element
        iFrameContentBuilder.append("<body ");
        String fontSize = Float.toString(mReaderSettings.mTextSize);
        String dayNightOption = "";
        iFrameContentBuilder.append("style=\"" + "margin:0 0 0 0; "
                + "padding:0 0 0 0; border:none; overflow:hidden; "
                + "line-height: " + mReaderSettings.mLineHeight + "em !important; "
                + (!TextUtils.isEmpty(mReaderSettings.mFontFamily) ? "font-family:custom_font !important; " : " ")
                + "font-size:" + fontSize + "px !important" + "; " + dayNightOption 
                + "text-align: " + mReaderSettings.mTextAlign + " ; \" "
                + ">\n");
        
        // 4.1. insert div for debug
        iFrameContentBuilder.append("<div id=\"rects_layer\">\n");
        iFrameContentBuilder.append("</div>\n");
        
        // 4.2. insert div for paginator
        iFrameContentBuilder.append("<div id=\"book_container\" style=\""
                 + "width:" + (int)(width / density) + "px; "
                 + "border:none; overflow:hidden; margin:0 0 0 0; "
                 + "padding:0 0 0 0; "
                 + "position: absolute !important; top: 0px; left: 0px; "
                 + "\">\n"
                 );
        
        // 4.3. get the html body content
        // 4.3. get the html body content
        StringBuilder chapterContentBuilder = new StringBuilder();
        int bodyEndTagIndex = content.lastIndexOf("</body>");
        int bodyStartTagIndex = content.indexOf("<body");
        if (mParser.getParserType() == IParser.ParserType.UMD) {
            if (bodyEndTagIndex != -1 && bodyStartTagIndex != -1) {
                chapterContentBuilder.append("<div id=\'chapter_content\' ");
                chapterContentBuilder.append(content.subSequence(
                        bodyStartTagIndex + 5, bodyEndTagIndex));
                chapterContentBuilder.append("</div>");
            } else {
                chapterContentBuilder.append("<div id=\'chapter_content\' >\n");
                String[] sigments = content.split("###");
                chapterContentBuilder.append("<div>\n");
                for (String object : sigments) {
                    String[] subSigments = object.split("\n");
                    chapterContentBuilder.append("<p>\n");
                    for (String subObject : subSigments) {
                        chapterContentBuilder.append(subObject);
                        chapterContentBuilder.append("</p>\n");
                        chapterContentBuilder.append("<p>\n");
                    }
                    chapterContentBuilder.append("</p>\n");

                    chapterContentBuilder.append("</div>\n");
                    chapterContentBuilder.append("<div>\n");
                }
                chapterContentBuilder.append("</div>\n");
                chapterContentBuilder.append("</div>\n");
            }
        } else if (mParser.getParserType() == IParser.ParserType.Epub) {
            chapterContentBuilder.append("<div id=\'chapter_content\'  ");
            if (bodyEndTagIndex != -1 && bodyStartTagIndex != -1) {
                chapterContentBuilder.append(content.subSequence(
                        bodyStartTagIndex + 5, bodyEndTagIndex));
            } else if (bodyEndTagIndex != -1) {
                chapterContentBuilder.append(content.substring(bodyStartTagIndex + 5));
            } else {
                chapterContentBuilder.append(content);
            }
            chapterContentBuilder.append("</div>");

        } else if (mParser.getParserType() == IParser.ParserType.Html) {
            if (bodyEndTagIndex != -1 && bodyStartTagIndex != -1) {
                chapterContentBuilder.append("<div id=\'chapter_content\'  ");
                chapterContentBuilder.append(content.subSequence(
                        bodyStartTagIndex + 5, bodyEndTagIndex));
                chapterContentBuilder.append("</div>");
            } else {
                chapterContentBuilder.append("<div id=\'chapter_content\' > \n");
                chapterContentBuilder.append(content);
                chapterContentBuilder.append("</div>");
            }
        } else if (mParser.getParserType() == IParser.ParserType.CHM) {
            if (bodyEndTagIndex != -1 && bodyStartTagIndex != -1) {
                chapterContentBuilder.append("<div id=\'chapter_content\'  ");
                chapterContentBuilder.append(content.subSequence(
                        bodyStartTagIndex + 5, bodyEndTagIndex));
                chapterContentBuilder.append("</div>");
            } else {
                chapterContentBuilder.append("<div id=\'chapter_content\' >\n ");
                chapterContentBuilder.append(content);
                chapterContentBuilder.append("</div>");
            }
        } else if (mParser.getParserType() == IParser.ParserType.EBK2) {
            chapterContentBuilder.append("<div id=\'chapter_content\'>\n");
            String[] subSigments = content.split("\n");
            chapterContentBuilder.append("<p>\n");
            for (String subObject : subSigments) {
                chapterContentBuilder.append(subObject);
                chapterContentBuilder.append("</p>\n");
                chapterContentBuilder.append("<p>\n");
            }
            chapterContentBuilder.append("</p>\n");

            chapterContentBuilder.append("</div>\n");
        }
        
        // 4.4. insert the book_content div for pagination
        iFrameContentBuilder.append("<div id=\"book_content\" style=\""
                        + "width:" + (int)(width / density) + "px; "
                        //+ "height:" + height / density + "px; "
                        + "overflow:hidden; zoom: 1; top: 0px; left: 0px; border:none; "
                        + "\">\n"
                        );
        
        // 4.5. append the chapter content
        iFrameContentBuilder.append(chapterContentBuilder.toString());
        
        // 4.6. append book_content end tag
        iFrameContentBuilder.append("</div>\n");
        
        // 4.7. insert book_container end tag
        iFrameContentBuilder.append("</div>\n");
        
        iFrameContentBuilder.append("</body>\n");
        
        iFrameContentBuilder.append("</html>");
        
        return iFrameContentBuilder.toString();
    }
    
    /**
     * Preprocess the div html file content
     * 
     * @param chapterIndex
     * @param content
     * @param injectJavascript
     * @return return the generated HTML includes javascript
     */
    private String preProcessDivContainerHtmlContent(int chapterIndex, String content, boolean injectJavascript) {
        String html = "";

        StringBuilder htmlContentBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(content)) {
            float density = getResources().getDisplayMetrics().density;
            int leftMargin = (int) mReaderSettings.mLeftMargin;
            int topMargin = (int) mReaderSettings.mTopMargin;
            int rightMargin = (int) mReaderSettings.mRightMargin;
            int bottomMargin = (int) mReaderSettings.mBottomMargin;

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int width = getResources().getDisplayMetrics().widthPixels - leftMargin - rightMargin;
            int height = getResources().getDisplayMetrics().heightPixels - topMargin - bottomMargin;
            
            int sdkVersion = android.os.Build.VERSION.SDK_INT;
            
            int headEndTagIndex = content.indexOf("</head>");
            if (headEndTagIndex > 0) {
                htmlContentBuilder.append(content.subSequence(0, headEndTagIndex));
                htmlContentBuilder.append("\n");
            } else {
                // there is no head tag in html string
                htmlContentBuilder.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n");
            }
            
            // process HEAD element
            /*
             * <head> Element
             * 
             * 1. append common library
             * 2. append paginator library
             * 3. append selection library
             * 4. set font face
             */
            {
                if (mLocalHTTPServer)
                    htmlContentBuilder.append("<base href=\"" + MyHTTPD.HTTPD_URL_BASE + "\" target=\"_blank\" />\n");
                htmlContentBuilder.append("<meta id=\"viewport_id\" name=\"viewport\" content=\"initial-scale=1.0,user-scalable=no\" />\n");
                
                // 1. append common library
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/jquery/jquery.js\"></script>\n");
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/util_common.js\"></script>\n");
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/entity.js\"></script>\n");
                
                // 2. append paginator library
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/paginator_v2.js\"></script>\n");
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/reader_common.js\"></script>\n");
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/offset.js\"></script>\n");
                htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/paginator.js\"></script>\n");
                
                // 3. append selection library
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/rangy/rangy-core.js\"></script>\n");
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/rangy/rangy-serializer.js\"></script>\n");
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/rangy/rangy-cssclassapplier.js\"></script>\n");
                //htmlContentBuilder.append("<script type=\"text/javascript\" src=\"file:///android_asset/selection.js\"></script>\n");
                
                // 4. set the css file
                /*
                */
                htmlContentBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/css/content.css\" />\n");
                
                // 5. set font face
                // set css style for font-type
                if (!TextUtils.isEmpty(mReaderSettings.mFontFamily)) {
                    htmlContentBuilder.append("<style type=\"text/css\">\n"
                            + "@font-face {\n"
                            + "font-family: \'custom_font\' ; \n"
                            + "src: url('"
                            + "file://" + mReaderSettings.mFontUrl
                            + "'); }\n"
                            + "</style>\n");
                }
                
                // 6. set the div p max width
                htmlContentBuilder.append("<style type=\"text/css\">\n"
                        + "p {\n"
                        + "max-width: " + (int)(width / density) + "px !important; \n"
                        + " }\n"
                        + "div {\n"
                        + "max-width: " + (int)(width / density) + "px !important; \n"
                        + " }\n"
                        + "img {\n"
                        + "max-width: " + (int)(width / density) + "px !important; \n"
                        + "max-height: " + (int)(height / density - 50) + "px !important; \n"
                        + " }\n"
                        + "table {\n"
                        + "max-width: " + (int)(width / density) + "px !important; \n"
                        + " }\n"
                        + "</style>\n");
                
                // 7. insert the theme css
                htmlContentBuilder.append("<style type=\"text/css\">\n"
                        + "body.day-mode1 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_1.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        //+ "    background-repeat:no-repeat;\n"
                        + "}\n"
                        + "body.day-mode2 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_2.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode3 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_3.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode4 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_4.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        //+ "    background-repeat:no-repeat;\n"
                        + "}\n"
                        + "body.day-mode5 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_5.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode6 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_6.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode7 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_7.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        //+ "    background-repeat:no-repeat;\n"
                        + "}\n"
                        + "body.day-mode8 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_8.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "body.day-mode9 {\n"
                        + "    color:  #181619 !important;\n"
                        + "    background-image: url('file:///android_asset/read_bg_9.png');\n"
                        + "    background-size:" + (int)(screenWidth / density) + "px " + (int)(screenHeight / density) + "px;\n"
                        + "    background-repeat:repeat;\n"
                        + "}\n"
                        + "</style>\n");
                
                // 8. Insert running Javascript
                if (injectJavascript) {
                    htmlContentBuilder.append("<script type=\"text/javascript\">\n"
                            + "function injectPagination() {\n"
                            + "initializeReader(" + mChapterList.size() + ");\n"
                            + "setDebugInfo(false, false, false, false);\n"
                            + "applyPreferences(" + (int)(screenWidth / density) + ","
                            + (int)(screenHeight / density) + "," 
                            + (int)(width / density) + ","
                            + (int)(height / density) + ",\""
                            + mReaderSettings.mFontFamily + "\","
                            + mReaderSettings.mTextZoom + ","
                            + mReaderSettings.mLineHeight + ",\""
                            + mReaderSettings.mTextAlign + "\","
                            + mReaderSettings.isThemeNight + ","
                            + mReaderSettings.mTheme + ","
                            + density + ","
                            + sdkVersion
                            + (mUsingContinuallyDiv ? ",true" : "")
                            + ");\n"
                            + "loadChapter(" + chapterIndex + ");\n"
                            + "}\n"
                            + (injectJavascript ? ("window.addEventListener('load', injectPagination, false);\n") : "")
                            + "</script>\n");
                }
                htmlContentBuilder.append("</head>\n");
            }
            
            // process BODY element
            /*
             * <body> Element
             * 
             * 1. insert div for paginator
             * 2. set font size
             */
            {
                htmlContentBuilder.append("<body ");
//                StringBuilder chapterContentBuilder = new StringBuilder();
//                
//                int bodyEndTagIndex = content.lastIndexOf("</body>");
//                int bodyStartTagIndex = content.indexOf("<body");
//                chapterContentBuilder.append("<div id=\"chapter_content\" ");
//                if (bodyStartTagIndex != -1 && bodyEndTagIndex > 0)
//                    chapterContentBuilder.append(content.subSequence(bodyStartTagIndex + 5, bodyEndTagIndex));
//                else if (bodyStartTagIndex != -1)
//                    chapterContentBuilder.append(content.substring(bodyStartTagIndex + 5));
//                else
//                    chapterContentBuilder.append(content);
//                chapterContentBuilder.append("</div>");
                
                // 1. set body style
                String fontSize = Float.toString(mReaderSettings.mTextSize);
                String dayNightOption = "";
//                if (mReaderSettings.isThemeNight) {
//                    dayNightOption = "background:#4B4A48 !important; color: #FFFFFF !important; background-color: rgba(255,255,255,0.0) !important; ";
//                }
//                else {
//                    dayNightOption = "background: #4B4A48 !important; color: #181619 !important; background-color: rgba(255,255,255,0.0) !important; ";
//                }
                htmlContentBuilder.append("style=\"" + "margin:0 0 0 0; "
                        + "padding:0 0 0 0; "
                        + "line-height: " + mReaderSettings.mLineHeight + "em !important; "
                        + (!TextUtils.isEmpty(mReaderSettings.mFontFamily) ? "font-family:custom_font !important; " : " ")
                        + "font-size:" + fontSize + "px !important" + "; " + dayNightOption 
                        + "text-align: " + mReaderSettings.mTextAlign + " ; \" "
                        //+ (injectJavascript ? "onload=\"injectPagination()\" " : "")
                        + ">\n");
                
                // 2. insert div for debug
                htmlContentBuilder.append("<div id=\"rects_layer\">\n");
                htmlContentBuilder.append("</div>\n");
                
                // 3. insert div for paginator
                htmlContentBuilder.append("<div id=\"book_container\" style=\""
                         //+ "width:" + width / density + "px; "
                         //+ "height:" + height / density + "px; "
                         + "border:none; overflow:hidden; zoom: 1; "
                         + "margin:" + (int)(topMargin / density) + "px " + (int)(rightMargin / density) + "px " + (int)(bottomMargin / density) + "px " + (int)(leftMargin / density) + "px " + "; "
                         + "top: 0px; left: 0px; "
                         + "position: absolute !important; "
                         + "\">\n"
                         );
                 
                htmlContentBuilder.append("<div id=\"book_content\" style=\""
                                //+ "width:" + width / density + "px; "
                                //+ "height:" + height / density + "px; "
                                + "border:none; overflow:hidden; zoom: 1; top: 0px; left: 0px; "
                                + "\">\n"
                                );
                
                // append the chapter content
                if (mUsingContinuallyDiv) {
                    htmlContentBuilder.append("<div id=\"chapter_content\" style=\""
                            + "position: absolute !important; "
                            + "\">\n");
                    
                    for (int index = 0; index < mChapterList.size(); ++index) {
                        htmlContentBuilder.append("<div id=\"chapter_id_" + Integer.toString(index) + "_container\" style=\""
                                + "position: absolute !important; "
                                + "top: 0px; left: " + Integer.toString(index * (int)(screenWidth / density)) + "px; "
                                + "\">\n"
                                );
                        htmlContentBuilder.append("<div id=\"chapter_id_" + Integer.toString(index) + "\"></div>\n");
                        htmlContentBuilder.append("</div>\n");
                    }
                    htmlContentBuilder.append("</div>");
                }
                else {
                    htmlContentBuilder.append("<div id=\"chapter_content\" >");
                    htmlContentBuilder.append("</div>");
                }
                
                // book_content end tag
                htmlContentBuilder.append("</div>\n");
                
                // book_container end tag
                htmlContentBuilder.append("</div>\n");
                
                htmlContentBuilder.append("<!-- offscreen div to keep custom fonts warm in webcore cache -->\n"
                                + "<div style=\"position: absolute; top: -1024px; left: -1024px;\" id=\"android_books_font_holder\">\n"
                                + "<!--<div style=\"font-family:'Merriweather';\">Lorem Ipsum</div>-->\n"
                                + "<div style='height: 1in; width: 1in;' id=\"android_books_inch_ruler\">Lorem Ipsum</div>\n"
                                + "</div>\n");
                
                // body end tag
                htmlContentBuilder.append("</body>\n");
            }
            htmlContentBuilder.append("</html>");
        }
        html = htmlContentBuilder.toString();
        return html;
    }
    
    /**
     * Open Previous Page
     */
    private boolean previousPageImpl() {
        if (!mCurChapterReady)
            return true;
        
        if (mCurPage == 0) {
            if (mCurChapter == 0) {
                if (!mPageNavigationUnReachableAlerted) {
                    Toast.makeText(getContext(), R.string.already_arrived_first_page, Toast.LENGTH_SHORT).show();
                    mPageNavigationUnReachableAlerted = true;
                }
                return false;
            }
            mCurPage = 0;
            mMaxPage = 1;
            int chapterIndex = mCurChapter - 1;
            loadChapter(chapterIndex, -1);
        } else {
            pageNavigation(mCurPage - 1, mCurPage);
        }
        
        return true;
    }
    
    /**
     * Open Next Page
     */
    private boolean nextPageImpl() {
        if (!mCurChapterReady)
            return true;
        
//        new Thread() {
//
//            @Override
//            public void run() {
//                getPageBitmap(0, 0, null);
//                getPageBitmap(1, 0, null);
//                getPageBitmap(2, 0, null);
//                getPageBitmap(3, 0, null);
//                getPageBitmap(4, 0, null);
//                getPageBitmap(4, 1, null);
//                getPageBitmap(4, 2, null);
//                getPageBitmap(4, 3, null);
//                getPageBitmap(5, 0, null);
//                super.run();
//            }
//            
//        }.start();

        if (mCurPage == mMaxPage - 1) {
            if (mCurChapter == mChapterList.size() - 1) {
                if (!mPageNavigationUnReachableAlerted) {
                    Toast.makeText(getContext(), R.string.already_arrived_last_page, Toast.LENGTH_SHORT).show();
                    mPageNavigationUnReachableAlerted = true;
                }
                return false;
            }
            mCurPage = 0;
            mMaxPage = 1;
            int chapterIndex = mCurChapter + 1;
            loadChapter(chapterIndex, 0);
        } else {
            pageNavigation(mCurPage + 1, mCurPage);
        }
        return true;
    }
    
    private void jump2Anchor(int chapterIndex, String nodeId) {
        mWebView.loadUrl(String.format("javascript:jump2Node(%d, \'%s\');", chapterIndex, nodeId));
    }
    
    private void getNodeRect(final WebReaderView webView, final int chapterIndex, final int nodeIdIndex, final String nodeId)
    {
    	mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				webView.loadUrl(String.format("javascript:getNodeRect(%d,  %d, \'%s\');", chapterIndex, nodeIdIndex, nodeId));
			}
    		
    	});
    }
    
    private void pageNavigation(int chapterIndex, int pageIndex, int previousPageIndex) {
        if (mPageNavigating || mWebView == null)
            return;
        
        mPageNavigationUnReachableAlerted = false;
        
        mPageNavigating = true;
        int height = getResources().getDisplayMetrics().heightPixels;
        float density = getResources().getDisplayMetrics().density;
        
        PaginationResult pagination = mPagePagination.get(chapterIndex);
        if (pagination == null)
            return;
        mMaxPage = pagination.getPagesCount();
        
        if (pageIndex < 0)
            pageIndex = pagination.getPagesCount() - 1;
        if (pageIndex > pagination.getPagesCount() - 1)
            pageIndex = pagination.getPagesCount() - 1;
        mCurPage = pageIndex;
        
        PaginationResult.PageBounds pageBound = pagination.getPageBounds(pageIndex);
        float scrollPosition = pageBound.pageTop;
        float ajdustHeight = pageBound.pageBottom - pageBound.pageTop;
        
        if (scrollPosition > 0 && !mUsingiFrame)
            scrollPosition -= (int)(mReaderSettings.mTopMargin / density);
        
        mWebView.loadUrl(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", chapterIndex, pageIndex, scrollPosition, ajdustHeight, Boolean.toString(false)));
        
        getClickableRect();
    }
    
    private void pageNavigation(int pageIndex, int previousPageIndex) {
        if (mPageNavigating || mWebView == null)
            return;
        
        mPageNavigationUnReachableAlerted = false;
        
        mPageNavigating = true;
        int height = getResources().getDisplayMetrics().heightPixels;
        float density = getResources().getDisplayMetrics().density;
        
        PaginationResult pagination = mPagePagination.get(mCurChapter);
        if (pagination == null)
            return;
        mMaxPage = pagination.getPagesCount();
        
        if (pageIndex < 0)
            pageIndex = pagination.getPagesCount() - 1;
        if (pageIndex > pagination.getPagesCount() - 1)
            pageIndex = pagination.getPagesCount() - 1;
        mCurPage = pageIndex;
        
        if (mTakeSnapshot && mViewMode == ViewMode.ImageViewMode) {
            if (previousPageIndex >= 0) {
                if (pageIndex > previousPageIndex) {
                    if (mCachedBitmaps.containsKey(previousPageIndex - 1)) {
                        Bitmap bitmap = mCachedBitmaps.get(previousPageIndex - 1);
                        mCachedBitmaps.remove(previousPageIndex - 1);
                        mCachedBitmaps.put(Integer.valueOf(pageIndex + 1), bitmap);
                    }
                } else {
                    if (mCachedBitmaps.containsKey(previousPageIndex + 1)) {
                        Bitmap bitmap = mCachedBitmaps.get(previousPageIndex + 1);
                        mCachedBitmaps.remove(previousPageIndex + 1);
                        mCachedBitmaps.put(Integer.valueOf(pageIndex - 1), bitmap);
                    }
                }
            } else {
                if (!mCachedBitmaps.isEmpty() && mCachedBitmaps.values() != null) {
                    Bitmap[] bitmaps = new Bitmap[3];
                    mCachedBitmaps.values().toArray(bitmaps);
                    mCachedBitmaps.clear();
                    
                    mCachedBitmaps.put(Integer.valueOf(pageIndex - 1), bitmaps[0]);
                    mCachedBitmaps.put(Integer.valueOf(pageIndex), bitmaps[1]);
                    mCachedBitmaps.put(Integer.valueOf(pageIndex + 1), bitmaps[2]);
                }
            }
        
            if (pageIndex > 0 && previousPageIndex != pageIndex - 1) {
                int index = pageIndex - 1;
                PaginationResult.PageBounds pageBound = pagination.getPageBounds(index);
                float scrollPosition = pageBound.pageTop;
                float ajdustHeight = pageBound.pageBottom - pageBound.pageTop;
                
                if (scrollPosition > 0 && !mUsingiFrame)
                    scrollPosition -= (int)(mReaderSettings.mTopMargin / density);
                mNavigationCommand.add(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", mCurChapter, index, scrollPosition, ajdustHeight, Boolean.toString(true)));
                //mWebView.loadUrl(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", mCurChapter, index, scrollPosition, ajdustHeight, Boolean.toString(true)));
            }
        
            if (pageIndex < mMaxPage - 1 && previousPageIndex != pageIndex + 1) {
                int index = pageIndex + 1;
                PaginationResult.PageBounds pageBound = pagination.getPageBounds(index);
                float scrollPosition = pageBound.pageTop;
                float ajdustHeight = pageBound.pageBottom - pageBound.pageTop;
                
                if (scrollPosition > 0 && !mUsingiFrame)
                    scrollPosition -= (int)(mReaderSettings.mTopMargin / density);
                mNavigationCommand.add(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", mCurChapter, index, scrollPosition, ajdustHeight, Boolean.toString(true)));
                //mWebView.loadUrl(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", mCurChapter, index, scrollPosition, ajdustHeight, Boolean.toString(true)));
            }
        }
        
        PaginationResult.PageBounds pageBound = pagination.getPageBounds(pageIndex);
        float scrollPosition = pageBound.pageTop;
        float ajdustHeight = pageBound.pageBottom - pageBound.pageTop;
        
        if (scrollPosition > 0 && !mUsingiFrame)
            scrollPosition -= (int)(mReaderSettings.mTopMargin / density);
        //mNavigationCommand.add(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", mCurChapter, pageIndex, scrollPosition, ajdustHeight, Boolean.toString(true)));
        //mWebView.loadUrl(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", mCurChapter, pageIndex, scrollPosition, ajdustHeight, Boolean.toString(true)));
        
        mNavigationCommand.add(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", mCurChapter, pageIndex, scrollPosition, ajdustHeight, Boolean.toString(false)));
        //mWebView.loadUrl(String.format("javascript:pageNavigation(%d, %d, %f, %f, %s);", mCurChapter, pageIndex, scrollPosition, ajdustHeight, Boolean.toString(false)));
        
        if (!mNavigationCommand.isEmpty()) {
            String command = mNavigationCommand.poll();
            mWebView.loadUrl(command);
        }
        
        getClickableRect();
    }
    
    private boolean handleSingleTap(MotionEvent event) {
        TouchableItem touchableItem = getClickedTouchableItem(event);
        if (touchableItem != null && touchableItem.type == TouchableItem.ItemType.VIDEO) {
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
//                return false;
        }
        
        if (touchableItem != null && (touchableItem.type == TouchableItem.ItemType.BUTTON || touchableItem.type == TouchableItem.ItemType.AUDIO)) {
            return false;
        }
        
        PaginationResult.TableInfo tableInfo = getClickedTable(event);
        if (touchableItem != null && touchableItem.type == TouchableItem.ItemType.VIDEO) {
            
            if (!TextUtils.isEmpty(touchableItem.id)) {
                String javascript = "javascript:playVideo(\'" + touchableItem.id + "\')";
                mWebView.loadUrl(javascript);
            }
            return true;
        }
        
        if (touchableItem != null || tableInfo != null)
            return false;
        
        if (touchableItem == null && tableInfo == null) {        
            if (mCallToolbarArea.contains((int)event.getX(), (int)event.getY())) {
                if (mReaderListener != null)
                    mReaderListener.onShowToolbar();
                return true;
            } else {
                for (int index = 0; index < mPreviousPageArea.length; ++index)
                    if (mPreviousPageArea[index].contains((int)event.getX(), (int)event.getY())) {
                        previousPageImpl();
                        return true;
                    }
                
                for (int index = 0; index < mNextPageArea.length; ++index)
                    if (mNextPageArea[index].contains((int)event.getX(), (int)event.getY())) {
                        nextPageImpl();
                        return true;
                    }
            }
        }
        
        return true;
    }
    
    private boolean handleDoubleTag(MotionEvent event) {
        TouchableItem touchableItem = getClickedTouchableItem(event);
        if (touchableItem != null && touchableItem.type == TouchableItem.ItemType.IMAGE) {
            if (mImagePreviewView != null)
                mImagePreviewView.setImageBitmap(null);
            
            if (mImagePreviewBitmap != null && !mImagePreviewBitmap.isRecycled())
                mImagePreviewBitmap.recycle();
            mImagePreviewBitmap = null;
            String src = Uri.decode(touchableItem.source);
            if (src.startsWith("file:///"))
                src = src.substring(7);
            else if (!src.startsWith("http"))
                src = mParser.getFileLocalPath(src);
            File file = new File(src);
            if (file.exists() && file.isFile()) {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(src, options);
                    int leftMargin = (int) mReaderSettings.mLeftMargin;
                    int topMargin = (int) mReaderSettings.mTopMargin;
                    int rightMargin = (int) mReaderSettings.mRightMargin;
                    int bottomMargin = (int) mReaderSettings.mBottomMargin;

                    int width = getResources().getDisplayMetrics().widthPixels - leftMargin - rightMargin;
                    int height = getResources().getDisplayMetrics().heightPixels - topMargin - bottomMargin;
                    
                    int w = options.outWidth;
                    int h = options.outHeight;
                    if (options.outWidth > width && options.outHeight > height) {
                        w = width;
                        h = (int) (height * (options.outWidth * 1.0f / options.outHeight));
                        
                        options.inSampleSize = (w * h) / (height * width);
                        if (((w * h) % (height * width)) > 0) {
                            options.inSampleSize++;
                        }
                        if (options.inSampleSize == 0)
                            options.inSampleSize = 1;
                    }
                    
                    options.inJustDecodeBounds = false;
                    options.inDither = false;
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    mImagePreviewBitmap = BitmapFactory.decodeFile(src, options);
                    mImagePreviewView.setImageBitmap(mImagePreviewBitmap);

                    mImagePreviewContainer.setVisibility(View.VISIBLE);
                    mImagePreviewView.invalidate();
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                }
            }
            
            return true;
        }
        
        if (getClickedTable(event) != null) {
            PaginationResult.TableInfo tableInfo = getClickedTable(event);
            
            Chapter chapter = mChapterList.get(mCurChapter);
            String content = null;
            if (mParser != null) {
                synchronized (mParser) {
                    try {
                        content = mParser.getChapterContent(chapter.src);
                    } catch (Exception e) {
                    } finally {
                    }
                }
            }
            StringBuilder htmlContentBuilder = new StringBuilder();
            
            int headEndTagIndex = content.indexOf("</head>");
            if (headEndTagIndex > 0) {
                htmlContentBuilder.append(content.subSequence(0, headEndTagIndex));
                htmlContentBuilder.append("\n</head>\n");
            } else {
                // there is no head tag in html string
                htmlContentBuilder.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n");
            }
            htmlContentBuilder.append("<body><div>"
                    + Uri.decode(tableInfo.html) + "</div></body></html>");
                
            String html = preProcessDivContainerHtmlContent(mCurChapter, htmlContentBuilder.toString(), false);

            // load data on webView
            String baseUrl = mLocalHTTPServer ? MyHTTPD.HTTPD_URL_BASE: "file:///android_asset/";
            mTablePreviewWebView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
            
            mTablePreviewWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                    result.confirm();
                    return true;
                    // return super.onJsAlert(view, url, message, result);
                }
            });
            
            mTablePreviewContainer.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }
    
    private boolean overrideUrlLoading(WebView view, String url) {
        String anchor = "";
        String chapterName;
        if (url.startsWith(MyHTTPD.HTTPD_URL_BASE)) {
            int idIndex = url.indexOf("#");
            if (idIndex >= 0) {
                anchor = url.substring(idIndex + 1);
                chapterName = url.substring(MyHTTPD.HTTPD_URL_BASE.length(), idIndex);
            } else 
                chapterName = url.substring(MyHTTPD.HTTPD_URL_BASE.length());
            
            int chapterIndex = 0;
            for (int index = 0; index < mChapterList.size(); ++index) {
                mChapterList.get(index);
                if (mChapterList.get(index).src.toLowerCase().contains(chapterName.toLowerCase())) {
                    chapterIndex = index;
                    break;
                }
            }
            if (chapterIndex != mCurChapter) {
                if (!TextUtils.isEmpty(anchor)) {
                    PendingTask nextCacheTask = null;
                    PendingTask prevCacheTask = null;
                    if (chapterIndex > 0) {
                        prevCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
                        prevCacheTask.chapterIndex = chapterIndex - 1;
                    }
                    if (chapterIndex < mChapterList.size() - 1) {
                        nextCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
                        nextCacheTask.chapterIndex = chapterIndex + 1;
                    }
                    PendingTask task = new PendingTask(PendingTask.PendingTaskType.JumpNode);
                    task.chapterIndex = chapterIndex;
                    task.anchor = anchor;
                    offerRendingTask(task);
                    if (nextCacheTask != null)
                        offerRendingTask(nextCacheTask);
                    if (prevCacheTask != null)
                        offerRendingTask(prevCacheTask);
                } else {
                    mMaxPage = 1;
                    loadChapter(chapterIndex, 0);
                }
            } else if (!TextUtils.isEmpty(anchor)) {
                // jump node
                jump2Anchor(mCurChapter, anchor);
            }
            return true;
        } else if (url.startsWith("file://") )  {                       
            int idIndex = url.indexOf("#");
            if (idIndex >= 0) {
                anchor = url.substring(idIndex + 1);
                chapterName = url.substring(0, idIndex);
            } else
                chapterName = url;
            if (chapterName.lastIndexOf("\\") > 0)
                chapterName = chapterName.substring(chapterName.lastIndexOf("\\") + 1);
            else if (chapterName.lastIndexOf("/") > 0)
                chapterName = chapterName.substring(chapterName.lastIndexOf("/") + 1);
            
            int chapterIndex = 0;
            for (int index = 0; index < mChapterList.size(); ++index) {
                mChapterList.get(index);
                if (mChapterList.get(index).src.toLowerCase().contains(chapterName.toLowerCase())) {
                    chapterIndex = index;
                    break;
                }
            }
            if (chapterIndex != mCurChapter) {
                if (!TextUtils.isEmpty(anchor)) {
                    PendingTask nextCacheTask = null;
                    PendingTask prevCacheTask = null;
                    if (chapterIndex > 0) {
                        prevCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
                        prevCacheTask.chapterIndex = chapterIndex - 1;
                    }
                    if (chapterIndex < mChapterList.size() - 1) {
                        nextCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
                        nextCacheTask.chapterIndex = chapterIndex + 1;
                    }
                    PendingTask task = new PendingTask(PendingTask.PendingTaskType.JumpNode);
                    task.chapterIndex = chapterIndex;
                    task.anchor = anchor;
                    offerRendingTask(task);
                    if (nextCacheTask != null)
                        offerRendingTask(nextCacheTask);
                    if (prevCacheTask != null)
                        offerRendingTask(prevCacheTask);
                } else {
                    mMaxPage = 1;
                    loadChapter(chapterIndex, 0);
                }
            } else if (!TextUtils.isEmpty(anchor)) {
                // jump node
                jump2Anchor(mCurChapter, anchor);
            }
            return true;
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                getContext().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    
    private void getClickableRect() {
        if (mPagePagination.get(mCurChapter) != null && mPagePagination.get(mCurChapter).getPageBounds(mCurPage) != null) {
            PaginationResult pagination = mPagePagination.get(mCurChapter);
            
            ArrayList<TouchableItem> touchableItems = pagination.getTouchableItems(mCurPage);                
            if (mTouchableItems == null)
                mTouchableItems = new ArrayList<TouchableItem>(touchableItems.size());
            else
                mTouchableItems.clear();
//            float scale = WebReaderView.getDensityIndependentValue(mWebView.getScale(), getContext());
            
            for (int index = 0; index < touchableItems.size(); ++index) {
                Rect touchableItemRect = new Rect();
                
                touchableItemRect.left = touchableItems.get(index).bounds.left;
                touchableItemRect.top = touchableItems.get(index).bounds.top;
                touchableItemRect.right = touchableItems.get(index).bounds.right;
                touchableItemRect.bottom = touchableItems.get(index).bounds.bottom;
                
//                Rect touchableItemRect = new Rect(touchableItems.get(index).bounds.left
//                        , touchableItems.get(index).bounds.top - pageBound.pageTop
//                        , touchableItems.get(index).bounds.right
//                        , touchableItems.get(index).bounds.bottom - pageBound.pageTop);
                
                //touchableItemRect.offset(BookProperties.BOOK_MARGIN_LEFT, BookProperties.BOOK_MARGIN_TOP);
                
                mTouchableItems.add(new TouchableItem(touchableItems.get(index).id, touchableItems.get(index).type, touchableItemRect, touchableItems.get(index).hasControls, touchableItems.get(index).source));
            }
            
            ArrayList<PaginationResult.TableInfo> tableInfos = pagination.getTableInfo(mCurPage);
            if (tableInfos != null && tableInfos.size() > 0) {
                if (mTableRects == null)
                    mTableRects = new ArrayList<PaginationResult.TableInfo>(tableInfos.size());
                else
                    mTableRects.clear();
                
                for (int index = 0; index < tableInfos.size(); ++index) {
                    Rect tableRect = new Rect();
                    
                    tableRect.left = tableInfos.get(index).bounds.left;
                    tableRect.top = tableInfos.get(index).bounds.top;
                    tableRect.right = tableInfos.get(index).bounds.right;
                    tableRect.bottom = tableInfos.get(index).bounds.bottom;
//                    Rect tableRect = new Rect(tableInfos.get(index).bound.left
//                            , tableInfos.get(index).bound.top - pageBound.pageTop
//                            , tableInfos.get(index).bound.right
//                            , tableInfos.get(index).bound.bottom - pageBound.pageTop);
                    
                    //tableRect.offset(BookProperties.BOOK_MARGIN_LEFT, BookProperties.BOOK_MARGIN_TOP);
                    
                    mTableRects.add(new PaginationResult.TableInfo(tableRect.left, tableRect.top, tableRect.right, tableRect.bottom, tableInfos.get(index).html));
                }
            }
        }
    }
    
    private TouchableItem getClickedTouchableItem(MotionEvent event) {
        int xPoint = (int) (event.getX());
        int yPoint = (int) (event.getY());
        
        int width = getResources().getDisplayMetrics().widthPixels;
        float scale = WebReaderView.getDensityIndependentValue(mWebView.getScale(), getContext());
        PaginationResult pagination = mPagePagination.get(mCurChapter);
        
        if (pagination != null && pagination.getPageBounds(mCurPage) != null) {
            PaginationResult.PageBounds pageBound = pagination.getPageBounds(mCurPage);
            Rect pageRect = new Rect(0, pageBound.pageTop, width, pageBound.pageBottom);
            if (mTouchableItems != null && mTouchableItems.size() > 0) {
                for (int index = 0; index < mTouchableItems.size(); ++index) {
                    if (mTouchableItems.get(index) != null) {
                        Rect bound = mTouchableItems.get(index).bounds;
                        if (pageRect.contains(bound)) {
                            bound.top -= pageBound.pageTop;
                            bound.bottom -= pageBound.pageTop;
                            bound.left = (int) (WebReaderView.getDensityDependentValue(bound.left,
                                    getContext()) * scale);
                            bound.top = (int) (WebReaderView.getDensityDependentValue(bound.top,
                                    getContext()) * scale);
                            bound.right = (int) (WebReaderView.getDensityDependentValue(bound.right,
                                    getContext()) * scale);
                            bound.bottom = (int) (WebReaderView.getDensityDependentValue(bound.bottom,
                                    getContext()) * scale);
                            if (bound.contains(xPoint, yPoint))
                                return mTouchableItems.get(index);
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private PaginationResult.TableInfo getClickedTable(MotionEvent event) {
        int xPoint = (int) (event.getX());
        int yPoint = (int) (event.getY());
        int width = getResources().getDisplayMetrics().widthPixels;
        float scale = WebReaderView.getDensityIndependentValue(mWebView.getScale(), getContext());
        PaginationResult pagination = mPagePagination.get(mCurChapter);

        if (pagination != null && pagination.getPageBounds(mCurPage) != null) {
            PaginationResult.PageBounds pageBound = pagination.getPageBounds(mCurPage);
            Rect pageRect = new Rect(0, pageBound.pageTop, width, pageBound.pageBottom);
            if (mTableRects != null && mTableRects.size() > 0) {
                for (int index = 0; index < mTableRects.size(); ++index) {
                    if (mTableRects.get(index) != null) {
                        Rect bound = mTableRects.get(index).bounds;
                        if (pageRect.contains(bound)) {
                            bound.top -= pageBound.pageTop;
                            bound.bottom -= pageBound.pageTop;
                            
                            bound.left = (int) (WebReaderView.getDensityDependentValue(bound.left,
                                    getContext()) * scale);
                            bound.top = (int) (WebReaderView.getDensityDependentValue(bound.top,
                                    getContext()) * scale);
                            bound.right = (int) (WebReaderView.getDensityDependentValue(bound.right,
                                    getContext()) * scale);
                            bound.bottom = (int) (WebReaderView.getDensityDependentValue(bound.bottom,
                                    getContext()) * scale);
                            if (bound.contains(xPoint, yPoint))
                                return mTableRects.get(index);
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private void clearRendingTask() {
        mRenderingQueue.clear();
    }
    
    private void offerRendingTask(PendingTask task) {
        mRenderingQueue.offer(task);
    }
    
    private PendingTask pollRenderingTask() {
        PendingTask task = null;
        try {
            task = mRenderingQueue.take();
        } catch (InterruptedException e) {
        }
        return task;
    }
    
    private void clearPaginatingTask() {
        mPaginatingQueue.clear();
    }
    
    private void offerPaginatingTask(PendingTask task) {
        mPaginatingQueue.offer(task);
    }
    
    private PendingTask pollPaginatingTask() {
        PendingTask task = null;
        try {
            task = mPaginatingQueue.take();
        } catch (InterruptedException e) {
        }
        return task;
    }
    
    
    public boolean triggerPagination(int chapterIndex) {
        boolean paginationFinished = false;
        
        if (chapterIndex >= 0 && chapterIndex < mChapterList.size()) {
            if (!mUsingContinuallyDiv) {
                while (chapterIndex < mChapterList.size()) {
                    Chapter chapter = mChapterList.get(chapterIndex);
                    if (chapter.chapterState == Chapter.ChapterState.READY) {
                        if (chapterIndex == 0) {
                            chapter.previousPageCount = 0;
                        } else {
                            Chapter prevChapter = mChapterList.get(chapterIndex - 1);
                            chapter.previousPageCount = prevChapter.previousPageCount + prevChapter.pageCount;
                        }
                        chapter.paginationState = Chapter.ChapterState.READY;
                    } else
                        break;
                    ++chapterIndex;
                }
            }
            if (chapterIndex < mChapterList.size()) {
                PendingTask paginatingTask = new PendingTask(PendingTask.PendingTaskType.Paginating);
                paginatingTask.chapterIndex = chapterIndex;
                offerPaginatingTask(paginatingTask);
            } else 
                paginationFinished = true;
        } else
            paginationFinished = true;
        
        return paginationFinished;
    }
    
    private synchronized void paginationReady(int total, int index) {
        if (mReaderListener != null)
            mReaderListener.onPaginationReady(total, index);
        if (mWebViewPaginating != null) {
            if (mUsingContinuallyDiv) {
                changeForegroundWebReaderView();
            } else {
                mWebViewPaginating.stopLoading();
                mWebViewPaginating.loadUrl(JAVASCRIPT_CLEAR_DOCUMENT);
            }
        }
    }
    
    private synchronized void changeForegroundWebReaderView() {
        WebReaderView webView = mWebViewPaginating;
        final int slotIndex = getSlot(mCurChapter);
        mWebViews[slotIndex].setVisibility(View.GONE);
        mWebViewPaginating = mWebViews[slotIndex];
        mWebViews[slotIndex] = webView;

        mWebViews[slotIndex].setWebChromeClient(mWebChromeClient);
        mWebViews[slotIndex].setWebViewClient(mWebViewClient);
        
        if (mUsingInternalGestureDetector)
            mWebViews[slotIndex].setExternalOnTouchListener(mTouchListener);

        mWebViewPaginating.setExternalOnTouchListener(null);
        mWebViewPaginating.setWebChromeClient(null);
        mWebViewPaginating.setWebViewClient(null);
        
        boolean paginatingInterface = false;
        Object tag = mWebViewPaginating.getTag();
        if (tag != null && tag instanceof Integer && (TAG_PAGINATION == (Integer)tag))
            paginatingInterface = true;
        mRenderingJavascriptInterface.changeRenderType(paginatingInterface ? false : true);
        mPaginatingJavascriptInterface.changeRenderType(paginatingInterface ? true : false);
        
        for (int index = 0; index < mChapterList.size(); ++index) {
            mChapterList.get(index).chapterState = Chapter.ChapterState.READY;
            mChapterList.get(index).paginationState = Chapter.ChapterState.READY;
        }
        resetWebWebView(mCurChapter);
        pageNavigation(mCurPage, -1);
    }
    
    private synchronized void rePaginating(boolean viewportChanged) {
        mCurChapterReady = true;
        mPageNavigating = false;
        clearRendingTask();
        clearPaginatingTask();
        
        mPagePagination.clear();
        
        for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex) {
            mWebViewIndices[slotIndex] = -1;
            mWebViews[slotIndex].setJavascriptInjected(false);
            if (viewportChanged) {
                mWebViews[slotIndex].stopLoading();
                mWebViews[slotIndex].loadUrl(JAVASCRIPT_CLEAR_DOCUMENT);
            }
        }
        
        if (mWebViewPaginating != null) {
            mWebViewPaginating.setJavascriptInjected(false);
            if (viewportChanged) {
                mWebViewPaginating.stopLoading();
                mWebViewPaginating.loadUrl(JAVASCRIPT_CLEAR_DOCUMENT);
            }
        }
        
        for (int index = 0; index < mChapterList.size(); ++index) {
            mChapterList.get(index).chapterState = Chapter.ChapterState.NOT_LOADED;
            mChapterList.get(index).paginationState = Chapter.ChapterState.NOT_LOADED;
            mChapterList.get(index).pageCount = 1;
            mChapterList.get(index).previousPageCount = 1;
        }
        
        mBook.pageCount = 1;
        mPaginating = true;
        int chapterIndex = mCurChapter;
        PendingTask nextCacheTask = null;
        PendingTask prevCacheTask = null;
        if (chapterIndex > 0) {
            prevCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
            prevCacheTask.chapterIndex = chapterIndex - 1;
        }
        if (chapterIndex < mChapterList.size() - 1) {
            nextCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
            nextCacheTask.chapterIndex = chapterIndex + 1;
        }
        PendingTask task = new PendingTask(viewportChanged ? PendingTask.PendingTaskType.NextChapter : PendingTask.PendingTaskType.ApplyRuntimeSetting);
        task.chapterIndex = mCurChapter;
        task.pageIndex = mCurPage;
        offerRendingTask(task);
//        if (nextCacheTask != null)
//            offerRendingTask(nextCacheTask);
//        if (prevCacheTask != null)
//            offerRendingTask(prevCacheTask);
        mPaginationStart = false;
    }
    
    private void responseChapterReady(final int chapterIndex) {
        mActivity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                if (mCurPendingTask != null) {
                    final PendingTask task = mCurPendingTask;
                    if (task.type == PendingTask.PendingTaskType.Cache || mPagePagination.get(chapterIndex) == null)
                        return;
                    mCurChapter = chapterIndex;
                    mMaxPage = mPagePagination.get(chapterIndex).getPagesCount();
                    mPageNavigating = false;
                    
                    switch (task.type) {
                    case NextChapter:
                        resetWebWebView(chapterIndex);
                        mCurPage = task.pageIndex;
                        pageNavigation(mCurPage, -1);
                        break;
                    case PreviousChapter:
                        resetWebWebView(chapterIndex);
                        mCurPage = mMaxPage - 1;
                        pageNavigation(mCurPage, -1);
                        break;
                    case GotoBookmark:
                        break;
                    case JumpNode:
                        resetWebWebView(chapterIndex);
                        if (!TextUtils.isEmpty(task.anchor)) {
                            jump2Anchor(chapterIndex, task.anchor);
                        } else {
                            mCurPage = task.pageIndex;
                            pageNavigation(mCurPage, -1);
                        }
                        break;
                    case ApplyRuntimeSetting:
                        resetWebWebView(chapterIndex);
                        pageNavigation(mCurPage, -1);
                        break;
                    }
                    mCurPendingTask = null;
                } else {
                    resetWebWebView(chapterIndex);
                    pageNavigation(mCurPage, -1);
                }
                if (mReaderListener != null)
                    mReaderListener.onChapterReady(mCurChapter);
            }
        });
    }
    
    public class WebReaderBridge {
        /** The javascript interface name for adding to web view. */
        private final String interfaceName = "bridge";
        private boolean mPaginatingBridge;
        
        public WebReaderBridge(boolean paginating) {
            this.mPaginatingBridge = paginating;
        }
        
        public void changeRenderType(boolean paginating) {
            this.mPaginatingBridge = paginating;
        }
        
        public boolean isForeground() {
            return !mPaginatingBridge;
        }
        
        /**
         * Gets the interface name
         * 
         * @return
         */
        public String getInterfaceName() {
            return this.interfaceName;
        }

        public void d(String warning) {
            if (mReaderListener != null)
                mReaderListener.d(warning);
        }

        public void e(String error) {
            if (mReaderListener != null)
                mReaderListener.e(error);
        }
        
        public void onReaderInitialized() {
            
        }
        
        public void onReaderunInitialized() {
            
        }
        
        public void onPreferencesApplied() {
            
        }
        
        public synchronized String getResourceContentUri(String resId) {
            String resourceUri = resId;
            if (!TextUtils.isEmpty(resId) && (!resId.startsWith(MyHTTPD.HTTPD_URL_BASE) && !resId.startsWith("file:///"))) {
                if (mParser != null) {
                    synchronized(mParser) {
                        try {
                            String path  = mParser.getFileLocalPath(resId);
                            File file = new File(path);
                            resourceUri = Uri.fromFile(file).toString();
                        } catch (NullPointerException e) {
                            
                        } finally {
                            
                        }
                    }
                }
            }
            return resourceUri;
        }
        
        public synchronized String getChapterContent(int chapterIndex) {
            if (mUsingiFrame)
                return preProcessIFrameChapterContent(chapterIndex);
            return preProcessDIVChapterContent(chapterIndex);
        }
        
        public void onChapterLoading(int chapterIndex) {
            
        }
        
        public synchronized void onChapterReady(final int chapterIndex, String pageBounds, String pageOffsets, String touchables, String tableBounds) {
            if (mPaginatingBridge && mParser != null) {
                String boundsArray[] = pageBounds.split(",");
                Chapter chapter = mChapterList.get(chapterIndex);
                chapter.paginationState = Chapter.ChapterState.READY;
                chapter.pageCount = boundsArray.length / 2;

                if (mUsingContinuallyDiv) {
                    ArrayList<TouchableItem> touchableItems = TouchableItem.parseTouchableItems(touchables);
                    final PaginationResult paginationResult = new PaginationResult(pageBounds, pageOffsets, tableBounds, touchableItems);
//                    if (mPagePagination.get(chapterIndex) != null) {
//                        mPagePagination.get(chapterIndex).merge(paginationResult);
//                    } else
//                        mPagePagination.put(chapterIndex, paginationResult);
                    mPagePagination.put(chapterIndex, paginationResult);
                }
                
                if (chapterIndex == 0) {
                    chapter.previousPageCount = 0;
                }
                else {
                    Chapter prevChapter = mChapterList.get(chapterIndex - 1);
                    chapter.previousPageCount = prevChapter.previousPageCount + prevChapter.pageCount;
                }
                
                if (mParser != null && mParser.getParserType() == IParser.ParserType.Epub) {
                    EpubParser epubParser = (EpubParser) mParser;
                    if (epubParser.getSpine() != null && !epubParser.getSpine().isEmpty()) {
                        ArrayList<EpubParser.NavPoint> navlist = epubParser.getSpine().get(chapterIndex).navcontent;
                        if (navlist != null)
                        {
                        	for(int i=0; i<navlist.size(); i++)
                            {
                        		NavPoint nav = navlist.get(i);
                            	String anchor = nav.anchor;
                            	if (anchor != null && !TextUtils.isEmpty(anchor))
                            		getNodeRect(mWebViewPaginating, chapterIndex, i, anchor);
                            	nav.pageIndex = chapter.previousPageCount + 1;
                            }
                        }
                    }
                }
                
                int progress = (int) (((chapterIndex + 1.0f) / mChapterList.size()) * 100.0f);
                if (mReaderListener != null)
                    mReaderListener.onPaginationProgressChanged(progress);
                
                if (chapterIndex == mChapterList.size() - 1 || triggerPagination(chapterIndex + 1)) {
                    Chapter curChapter = mChapterList.get(mCurChapter);
                    if (chapterIndex == mChapterList.size() - 1) {
                        mBook.pageCount = chapter.pageCount + chapter.previousPageCount;
                    }
                    else {
                        chapter = mChapterList.get(chapterIndex + 1);
                        mBook.pageCount = chapter.pageCount + chapter.previousPageCount;
                    }
                    
                    if (mCurPage > curChapter.pageCount - 1)
                        mCurPage = curChapter.pageCount - 1;
                    
                    mPaginating = false;
                    final int totalCount = mBook.pageCount;
                    final int currentIndex = curChapter.previousPageCount + mCurPage + 1;
                    
                    mActivity.runOnUiThread(new Runnable() {
                        
                        @Override
                        public void run() {
                            paginationReady(totalCount, currentIndex);
                        }
                    });
                } else {
//                    int progress = (int) (((chapterIndex + 1.0f) / mChapterList.size()) * 100.0f);
//                    mCurrentUIHandler.obtainMessage(HANDLER_PAGINATION_PROGRESS_UPDATE, progress, 0).sendToTarget();
//                    
//                    PendingTask paginatingTask = new PendingTask(PendingTask.PendingTaskType.Paginating);
//                    paginatingTask.chapterIndex = chapterIndex + 1;
//                    offerPaginatingTask(paginatingTask);
                }
            } else if (!mPaginatingBridge) {
                Chapter chapter = mChapterList.get(chapterIndex);
                ArrayList<TouchableItem> touchableItems = TouchableItem.parseTouchableItems(touchables);
                final PaginationResult paginationResult = new PaginationResult(pageBounds, pageOffsets, tableBounds, touchableItems);
//                if (mPagePagination.get(chapterIndex) != null)
//                    mPagePagination.get(chapterIndex).merge(paginationResult);
//                else
//                    mPagePagination.put(chapterIndex, paginationResult);
                mPagePagination.put(chapterIndex, paginationResult);
                
                String boundsArray[] = pageBounds.split(",");
                chapter.pageCount = boundsArray.length / 2;
                chapter.chapterState = Chapter.ChapterState.READY;
                
                if (mCurPendingTask != null && mCurPendingTask.type != PendingTask.PendingTaskType.Cache && mCurPendingTask.chapterIndex == chapterIndex)
                    responseChapterReady(chapterIndex);
            }
        }

        public synchronized void selectionChanged(final String text, final String handleBounds, final String rectString) {
            mActivity.runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    mWebView.selectionChanged(text, handleBounds, rectString);
                }
            });
        }
        
        public synchronized void onSearchFinished(String searchResults) {
            if (TextUtils.isEmpty(searchResults))
                return;
            String searchArray[] = searchResults.split(",");
            
            mSearchResults.clear();
            for (int i = 0; i + 1 < searchArray.length; i += 4) {
                int left = Integer.valueOf(searchArray[i]).intValue();
                int top = Integer.valueOf(searchArray[i + 1]).intValue();
                int right = Integer.valueOf(searchArray[i + 2]).intValue();
                int bottom = Integer.valueOf(searchArray[i + 3]).intValue();
                mSearchResults.add(new Rect(left, top, right, bottom));
            }
        }
        
        public void onTakeSnapshot(int pageIndex) {
            if (mTakeSnapshot)
                mCurrentUIHandler.obtainMessage(HANDLER_TAKE_SNAPSHOT, pageIndex, 0).sendToTarget();
        }
        
        public synchronized void onPageNavigationFinished(int pageIndex) {
            mCurChapterReady = true;
            mPageNavigating = false;
            
            if (!mPaginationStart) {
                mPaginationStart = true;
                if (mReaderListener != null)
                    mReaderListener.onPaginationStarting();
                if (triggerPagination(0)) {
                    Chapter chapter = mChapterList.get(0);
                    mBook.pageCount = chapter.pageCount + chapter.previousPageCount;
                    Chapter curChapter = mChapterList.get(mCurChapter);
                    mPaginating = false;
                    
                    if (mReaderListener != null)
                        mReaderListener.onPaginationReady(mBook.pageCount, curChapter.previousPageCount + mCurPage + 1);
                }
            }
            mCurrentUIHandler.obtainMessage(HANDLER_TAKE_SNAPSHOT, pageIndex, 0).sendToTarget();
            
            Chapter chapter = mChapterList.get(mCurChapter);
            if (mReaderListener != null)
                mReaderListener.onCurrentPageChanged(mCurChapter, chapter.previousPageCount + mCurPage, mBook.pageCount);
            if (mReaderListener != null)
                mReaderListener.onPageNavigationFinish(mCurChapter, pageIndex);
            mCurrentUIHandler.obtainMessage(HANDLER_SHOW).sendToTarget();
        }
        
        public synchronized void onJump2Node(int chapterIndex, String result) {
            String rectArray[] = result.split(",");
            Rect nodeRect = new Rect();
            int index = 0;
            mMaxPage = 1;
            boolean anchorFound = false;
            if (!TextUtils.isEmpty(result)) {
                nodeRect.left = Integer.valueOf(rectArray[0]).intValue();
                nodeRect.top = Integer.valueOf(rectArray[1]).intValue();
                nodeRect.right = Integer.valueOf(rectArray[2]).intValue();
                nodeRect.bottom = Integer.valueOf(rectArray[3]).intValue();
                
                PaginationResult pagination = mPagePagination.get(chapterIndex);
                if (pagination == null)
                    return;
                for (; index < pagination.getPagesCount(); ++index) {
                    PaginationResult.PageBounds pageBound = pagination.getPageBounds(index);
                    if (nodeRect.top >= pageBound.pageTop && nodeRect.bottom <= pageBound.pageBottom) {
                        anchorFound = true;
                        break;
                    }
                }
            }
            final int jumpChapterIndex = chapterIndex;
            final int pageIndex = anchorFound ? index : 0;
            mActivity.runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    if (jumpChapterIndex != mCurChapter) {
                        loadChapter(jumpChapterIndex, pageIndex);
                    } else {
                        mMaxPage = 1;
                        PaginationResult pagination = mPagePagination.get(mCurChapter);
                        if (pagination != null)
                            mMaxPage = pagination.getPagesCount();
                        pageNavigation(pageIndex, -1);
                    }
                }
            });
        }
        
        public synchronized void onGetNodeRect(int chapterIndex, int nodeIdIndex, String result) {
            if (mParser != null && mParser.getParserType() == IParser.ParserType.Epub) {
                EpubParser epubParser = (EpubParser) mParser;
            	String rectArray[] = result.split(",");
                Rect nodeRect = new Rect();
                int index = 0;
                if (!TextUtils.isEmpty(result)) {
                    nodeRect.left = Integer.valueOf(rectArray[0]).intValue();
                    nodeRect.top = Integer.valueOf(rectArray[1]).intValue();
                    nodeRect.right = Integer.valueOf(rectArray[2]).intValue();
                    nodeRect.bottom = Integer.valueOf(rectArray[3]).intValue();
                    
                    PaginationResult pagination = mPagePagination.get(chapterIndex);
                    if (pagination == null)
                        return;
                    for (; index < pagination.getPagesCount(); ++index) {
                        PaginationResult.PageBounds pageBound = pagination.getPageBounds(index);
                        Rect pageRect = new Rect(nodeRect.left, pageBound.pageTop, nodeRect.right, pageBound.pageBottom);
                        if (nodeRect.intersect(pageRect)) {
    //                    if (nodeRect.top >= pageBound.pageTop && nodeRect.top <= pageBound.pageBottom) {
                        	NavPoint nav = epubParser.getSpine().get(chapterIndex).navcontent.get(nodeIdIndex);
                        	nav.pageIndex = index + mChapterList.get(chapterIndex).previousPageCount + 1;
                            break;
                        }
                    }
                }
            }
        }
        
        public void onAlignApplied() {
            
        }
        
        public void shouldOverrideUrlLoading(String url) {
            if (mWebView != null && !TextUtils.isEmpty(url))
                overrideUrlLoading(mWebView, url);
        }
        
        public void onThemeApplied() {
            mCurrentUIHandler.obtainMessage(HANDLER_THEME_CHANGED).sendToTarget();
            if (mReaderListener != null)
                mReaderListener.onThemeApplied();
        }
    }
    
    private class RenderingTask extends ParallelTask<Bundle, Integer, Boolean> {
        private boolean runAsDaemon = true;
        
        public void cancel() {
            runAsDaemon = false;
            clearRendingTask();
            cancel(true);
        }

        @Override
        protected Boolean doInBackground(final Bundle... params) {
            Boolean result = Boolean.TRUE;
            Bundle savedInstanceState = params[0];
            
            if (mParser == null) {
                mPaginatingTask = new PaginatingTask();
                mPaginatingTask.execute(params);

                mParser = new EpubParser(getContext(), mBook.path, mEncryptionKey, mRandom);
                
                try {
                    mParser = ParserFactory.getParserInstance(mActivity, mBook.path);
                    if (mParser != null) {
                        mParser.constructChapterList(mChapterList, mBook);
                        
                        File wwwroot = new File(".");
                        try {
                            mHTTPD = new MyHTTPD(MyHTTPD.HTTPD_PORT, wwwroot, mParser);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        
                        // rotated or something
                        if (savedInstanceState != null) {
                            mCurChapter = savedInstanceState.getInt("mCurChapter");
                            mCurPage = savedInstanceState.getInt("mCurPage");
                        } else {
                            Bookmark bookmark = Bookmark.getBookmark(mActivity, Md5Encrypt.md5(mBook.path));
                            if (bookmark != null) {
                                mCurPage = (int) bookmark.current_page;
                                mCurChapter = (int) bookmark.chapter;
                            }
                            if (mCurPage < 0)
                                mCurPage = 0;
                        }
                    }
                } catch (NullPointerException e) {
                }
                
                if (!mBookReady) {
                    mBookReady = true;
                    if (mReaderListener != null) {
                        if (mChapterList.size() > 0) {
                            loadChapter(mCurChapter, mCurPage);
                            
                            mReaderListener.onBookReady();
                        }
                        else
                            mReaderListener.onBookError();
                    }
                }
                
                while (runAsDaemon) {
                    final PendingTask pendingTask = pollRenderingTask();
                    if (null != pendingTask) {
                        if (pendingTask.type == PendingTask.PendingTaskType.Kill) {
                            runAsDaemon = false;
                            break;
                        }
                        
                        if (pendingTask.type == PendingTask.PendingTaskType.Cache) {
                            if (!mUsingChapterCache)
                                continue;
                        }
                        else {
                            mCurPendingTask = pendingTask;
                        }

                        final int chapterIndex = pendingTask.chapterIndex;
                        final int slotIndex = getSlot(chapterIndex);
                        if (mUsingiFrame || mUsingChapterCache) {
                            if (mWebViewIndices[slotIndex] >= 0 && chapterIndex != mWebViewIndices[slotIndex])
                                mChapterList.get(mWebViewIndices[slotIndex]).chapterState = ChapterState.NOT_LOADED;
                        }
                        mWebViewIndices[slotIndex] = chapterIndex;
                        
                        if (mChapterList.get(chapterIndex).chapterState == ChapterState.READY) {
                            if (pendingTask.type == PendingTask.PendingTaskType.ApplyRuntimeSetting) {
                                applyRuntimeSetting(false);
                            }
                            else if (pendingTask.type != PendingTask.PendingTaskType.Cache) {
                                if (!mUsingContinuallyDiv || mPaginating)
                                    mCurrentUIHandler.obtainMessage(HANDLER_HIDE, chapterIndex, 0).sendToTarget();
                                responseChapterReady(chapterIndex);
                            }
                        } else {
                            if (pendingTask.type == PendingTask.PendingTaskType.ApplyRuntimeSetting)
                                applyRuntimeSetting(false);
                            else
                                loadChapter(chapterIndex, mWebViews[slotIndex], (pendingTask.type == PendingTask.PendingTaskType.Cache ? true : false));
                        }
                    }
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
        }
    }
    
    private class PaginatingTask extends ParallelTask<Bundle, Integer, Boolean> {
        private boolean runAsDaemon = true;
        
        public void cancel() {
            runAsDaemon = false;
            clearPaginatingTask();
            cancel(true);
        }
        
        @Override
        protected void onPreExecute() {
            mPaginating = true;
            if (mReaderListener != null)
                mReaderListener.onPaginationStarting();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Bundle... params) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
            
            Boolean result = Boolean.TRUE;
            if (mChapterList != null) {
                while (runAsDaemon) {
                    final PendingTask pendingTask = pollPaginatingTask();
                    if (null != pendingTask) {
                        if (pendingTask.type == PendingTask.PendingTaskType.Kill) {
                            runAsDaemon = false;
                            break;
                        }
                        paginatingChapter(pendingTask.chapterIndex, mWebViewPaginating, true);
                     }
                }
            }
            return result;
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            mPaginating = false;
            super.onPostExecute(result);
        }
    }
    
    public ReaderListener getReaderListener() {
        return mReaderListener;
    }

    public void setReaderListener(ReaderListener mReaderListener) {
        this.mReaderListener = mReaderListener;
    }

    @Override
    public void initializeTheme(ArrayList<ThemeMode> themes) {
    }
    
    @Override
    public void initializeReader(Activity activity) {
        mActivity = activity;
        mViewMode = ViewMode.RawViewMode;
        mUsingInternalGestureDetector = true;
        
        if (mViewMode == ViewMode.ImageViewMode)
            mTakeSnapshot = true;
        else
            mTakeSnapshot = false;
        
        mTakeSnapshot = true;
    }
    
    @Override
    public void openBook(String bookUrl, String encryptionKey, String random) {
        mBook.path = bookUrl;
        mEncryptionKey = encryptionKey;
        mRandom = random;
        
        if (mRenderingTask != null) {
            Bundle savedInstanceState = null;
            mRenderingTask.execute(savedInstanceState);
        }
    }
    
    @Override
    public SparseArray<Chapter> getSequenceReadingChapterList() {
        return mChapterList;
    }
    
    public int getChapterPreviousPageCount(int chapterIndex) {
    	if (chapterIndex < 0 || chapterIndex >= mChapterList.size())
    		return -1;
    	return mChapterList.get(chapterIndex).previousPageCount;
    }
    
    @Override
    public ArrayList<NavPoint> getTOC() {
        if (mParser != null)
            return mParser.getNavMap();
        return null;
    }
    
    @Override
    public void closeBook() {
        synchronized(WebReader.this) {
            WebReader.this.notify();
        }
        stopBackgroundTask();
        
        recycleCachedBitmaps();
        
        if (mHTTPD != null)
            mHTTPD.stop();
        
        for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex) {
            mWebViewIndices[slotIndex] = -1;
            mWebViews[slotIndex].setJavascriptInjected(false);
            mWebViews[slotIndex].stopLoading();
            mWebViews[slotIndex].loadUrl(JAVASCRIPT_CLEAR_DOCUMENT);
        }
        
        if (mWebViewPaginating != null) {
            mWebViewPaginating.stopLoading();
            mWebViewPaginating.loadUrl(JAVASCRIPT_CLEAR_DOCUMENT);
        }
        
        if (mImagePreviewView != null)
            mImagePreviewView.setImageBitmap(null);
        mImagePreviewView = null;
        
        if (mImagePreviewBitmap != null && !mImagePreviewBitmap.isRecycled())
            mImagePreviewBitmap.recycle();
        mImagePreviewBitmap = null;
        
        if (mParser != null)
            mParser.close();
        mParser = null;
    }
    
    @Override
    public void onLowMemory() {
        if (mWebViewPaginating != null)
            mWebViewPaginating.freeMemory();
        for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex)
            mWebViews[slotIndex].freeMemory();
        
        if (mUsingContinuallyDiv) {
            mUsingContinuallyDiv = false;
            mUsingiFrame = false;
            mUsingChapterCache = false;
            
            for (int index = 0; index < mChapterList.size(); ++index) {
                if (index == mCurChapter)
                    continue;
                mChapterList.get(index).chapterState = Chapter.ChapterState.NOT_LOADED;
                mChapterList.get(index).paginationState = Chapter.ChapterState.NOT_LOADED;
                mChapterList.get(index).pageCount = 1;
                mChapterList.get(index).previousPageCount = 1;
            }
        } if (mUsingiFrame) {
            mUsingiFrame = false;
            mUsingChapterCache = false;
        }
    }
    
    @Override
    public boolean onBackKeyPressed() {
        if (mImagePreviewContainer.getVisibility() == View.VISIBLE || mTablePreviewContainer.getVisibility() == View.VISIBLE) {
            mTablePreviewContainer.setVisibility(View.GONE);
            mImagePreviewContainer.setVisibility(View.GONE);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void applyRuntimeSettings(ReaderSettings settings) {
        boolean fontSizeChanged = false;
        boolean alignChanged = false;
        boolean themeChanged = false;
        boolean fontFaceChanged = false;
        boolean lineHeightChanged = false;
        boolean columnNumsChanged = false;
        
        if (settings.mFontFamily == null)
            settings.mFontFamily = "";
        if (settings.mFontUrl == null)
            settings.mFontUrl = "";
        
        if (!settings.mTextAlign.equalsIgnoreCase(mReaderSettings.mTextAlign)) {
            mReaderSettings.mTextAlign = settings.mTextAlign;
            alignChanged = true;
        } else if (settings.mTextSize != mReaderSettings.mTextSize) {
            mReaderSettings.mTextSize = settings.mTextSize;
            fontSizeChanged = true;
        } else if (settings.mLineHeight != mReaderSettings.mLineHeight) {
            mReaderSettings.mLineHeight = settings.mLineHeight;
            lineHeightChanged = true;
        } else if (mReaderSettings.isThemeNight != settings.isThemeNight || mReaderSettings.mTheme != settings.mTheme) {
            mReaderSettings.isThemeNight = settings.isThemeNight;
            mReaderSettings.mTheme = settings.mTheme;
            themeChanged = true;
        } else if (!settings.mFontFamily.equalsIgnoreCase(mReaderSettings.mFontFamily) || !settings.mFontUrl.equalsIgnoreCase(mReaderSettings.mFontUrl)) {
            mReaderSettings.mFontFamily = settings.mFontFamily;
            mReaderSettings.mFontUrl = settings.mFontUrl;
            fontFaceChanged = true;
        } else if (settings.mColumnNums != mReaderSettings.mColumnNums) {
            mReaderSettings.mColumnNums = settings.mColumnNums;
            columnNumsChanged = true;
        }
        
        if ((fontSizeChanged || lineHeightChanged || fontFaceChanged || columnNumsChanged) && mCurChapterReady) {
            rePaginating(false);
        } else if (themeChanged) {
            for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex) {
                if (mWebViewIndices[slotIndex] >= 0) {
                    WebReaderView webView = mWebViews[slotIndex];
                    webView.loadUrl(String.format("javascript:setThemeMode(%d, %s, %d);", mWebViewIndices[slotIndex], Boolean.toString(mReaderSettings.isThemeNight), mReaderSettings.mTheme));
                }
            }
            //mWebView.loadUrl(String.format("javascript:setThemeMode(%d, %s, %d);", mCurChapter, Boolean.toString(mReaderSettings.isThemeNight), mReaderSettings.mTheme));
            
            if (mViewMode == ViewMode.ImageViewMode)
                loadChapter(mCurChapter, mCurPage);
        } else if (alignChanged) {
            for (int slotIndex = 0; slotIndex < mWebReaderViewSlots; ++slotIndex) {
                if (mWebViewIndices[slotIndex] >= 0) {
                    WebReaderView webView = mWebViews[slotIndex];
                    webView.loadUrl(String.format("javascript:setAlignMode(%d, \"%s\");", mWebViewIndices[slotIndex], mReaderSettings.mTextAlign));
                }
            }
            //mWebView.loadUrl(String.format("javascript:setAlignMode(%d, \"%s\");", mCurChapter, mReaderSettings.mTextAlign));
            
            if (mViewMode == ViewMode.ImageViewMode)
                loadChapter(mCurChapter, mCurPage);
        }
    }

    @Override
    public void viewSizeChanged() {
        initTouchAreas();
        rePaginating(true);
    }

    @Override
    public void gotoBookmark() {
    }
    
    @Override
    public void gotoPercent(float percent) {
        if (!mPaginating && mChapterList.size() > 0) {
            int gotoPageIndex = (int) (mBook.pageCount * percent);
            int pageCount = mBook.pageCount;
            if (gotoPageIndex >= pageCount)
                gotoPageIndex = pageCount - 1;
            gotoPage(gotoPageIndex);
        }
    }
    
    @Override
    public void gotoPage(int pageIndex) {
        if (!mPaginating && mChapterList.size() > 0) {
            int chapterIndex = 0;
            int pageCount = mBook.pageCount;
            if (pageIndex >= pageCount) {
                chapterIndex = mChapterList.size() - 1;
                pageIndex = mChapterList.get(chapterIndex).pageCount - 1;
            } else {
                for (int index = 0; index < mChapterList.size(); ++index) {
                    Chapter chapter = mChapterList.get(index);
                    if (pageIndex >= chapter.previousPageCount && pageIndex < (chapter.previousPageCount + chapter.pageCount)) {
                        chapterIndex = index;
                        pageIndex = pageIndex - chapter.previousPageCount;
                        break;
                    }
                }
            }
            
            mMaxPage = 1;
            loadChapter(chapterIndex, pageIndex);
        }
    }
    
    @Override
    public Pair<Integer, Integer> getCorrespondChapterPage(int globalPageIndex) {
        Pair<Integer, Integer> result = null;
        
        int chapterIndex = 0;
        int pageIndex = 0;
        if (!mPaginating && mChapterList.size() > 0) {
            int pageCount = mBook.pageCount;
            if (globalPageIndex >= pageCount) {
                chapterIndex = mChapterList.size() - 1;
                pageIndex = mChapterList.get(chapterIndex).pageCount - 1;
            } else {
                for (int index = 0; index < mChapterList.size(); ++index) {
                    Chapter chapter = mChapterList.get(index);
                    if (globalPageIndex >= chapter.previousPageCount && globalPageIndex < (chapter.previousPageCount + chapter.pageCount)) {
                        chapterIndex = index;
                        pageIndex = globalPageIndex - chapter.previousPageCount;
                        break;
                    }
                }
            }
            result = new Pair<Integer, Integer>(chapterIndex, pageIndex);
        }
        return result;
    }

    @Override
    public void loadChapter(int chapterIndex, int pageIndex) {
        if (chapterIndex < 0 || chapterIndex >= mChapterList.size())
            return;
        if (isChapterReady(chapterIndex)) {
            mCurChapter = chapterIndex;
            pageNavigation(chapterIndex, pageIndex, -1);
        } else if (mCurChapter == chapterIndex && isChapterReady(mCurChapter)) {
            pageNavigation(pageIndex, -1);
        } else {
            PendingTask nextCacheTask = null;
            PendingTask prevCacheTask = null;
            if (chapterIndex > 0) {
                prevCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
                prevCacheTask.chapterIndex = chapterIndex - 1;
            }
            if (chapterIndex < mChapterList.size() - 1) {
                nextCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
                nextCacheTask.chapterIndex = chapterIndex + 1;
            }
            PendingTask task = new PendingTask(pageIndex == -1 ? PendingTask.PendingTaskType.PreviousChapter : PendingTask.PendingTaskType.NextChapter);
            task.chapterIndex = chapterIndex;
            task.pageIndex = pageIndex;
            offerRendingTask(task);
            if (nextCacheTask != null)
                offerRendingTask(nextCacheTask);
            if (prevCacheTask != null)
                offerRendingTask(prevCacheTask);
        }
    }
    
    @Override
    public void loadChapter(Chapter chapter) {
        int chapterIndex = 0;
        
        for (int index = 0; index < mChapterList.size(); ++index) {
            if (chapter.id.equals(mChapterList.get(index).id) || chapter.src.equals(mChapterList.get(index).src)) {
                chapterIndex = index;
                break;
            }
        }
        PendingTask nextCacheTask = null;
        PendingTask prevCacheTask = null;
        if (chapterIndex > 0) {
            prevCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
            prevCacheTask.chapterIndex = chapterIndex - 1;
        }
        if (chapterIndex < mChapterList.size() - 1) {
            nextCacheTask = new PendingTask(PendingTask.PendingTaskType.Cache);
            nextCacheTask.chapterIndex = chapterIndex + 1;
        }
        PendingTask task = new PendingTask(PendingTask.PendingTaskType.NextChapter);
        task.chapterIndex = chapterIndex;
        task.anchor = chapter.anchor;
        task.pageIndex = 0;
        offerRendingTask(task);
        if (nextCacheTask != null)
            offerRendingTask(nextCacheTask);
        if (prevCacheTask != null)
            offerRendingTask(prevCacheTask);
    }
    
    @Override
    public boolean isChapterReady(int chapterIndex) {
        return (mChapterList.get(chapterIndex) != null 
                && mPagePagination.get(chapterIndex) != null 
                && mChapterList.get(chapterIndex).chapterState == Chapter.ChapterState.READY);
    }
    
    @Override
    public boolean isChapterLoading() {
        return mCurChapterReady;
    }
    
    @Override
    public boolean isPaginatingFinish() {
        return mPaginating;
    }
    
    @Override
    public int getCurrentChapterIndex() {
        return mCurChapter;
    }
    
    @Override
    public String getChapterTitle(int chapterIndex) {
        String title = "";
        if (chapterIndex < 0 || chapterIndex >= mChapterList.size())
            return title;
        Chapter chapter = mChapterList.get(chapterIndex);
        if (chapter != null)
            title = chapter.title;
        return title;
    }

    @Override
    public int getChapterPageCount(int chapterIndex) {
        if (chapterIndex < 0 || chapterIndex >= mChapterList.size())
            return 1;
        Chapter chapter = mChapterList.get(chapterIndex);
        if (chapter != null && mPagePagination.get(chapterIndex) != null && chapter.chapterState == Chapter.ChapterState.READY)
            return mPagePagination.get(chapterIndex).getPagesCount();
        return 1;
    }
    
    @Override
    public int getPreviousChaptersPageCount(int chapterIndex) {
        int pageCount = 1;
        if (chapterIndex < 0 || chapterIndex >= mChapterList.size())
            return pageCount;
        Chapter chapter = mChapterList.get(chapterIndex);
        if (chapter != null && mPagePagination.get(chapterIndex) != null && chapter.chapterState == Chapter.ChapterState.READY)
            pageCount = chapter.previousPageCount;
        return pageCount;
    }
    
    @Override
    public int getTotalPageCount() {
        int pageCount = 1;
        if (mPaginating)
            return pageCount;
        
        return mBook.pageCount;
    }
    
    @Override
    public String getCurrentChapterTitle() {
        Chapter chapter = mChapterList.get(mCurChapter);
        return chapter.title;
    }
    
    @Override
    public int getCurrentPageIndex() {
        return mCurPage;
    }
    
    @Override
    public int getCurrentChapterPageCount() {
        return mMaxPage;
    }

    @Override
    public boolean nextPage() {
        return nextPageImpl();
    }

    @Override
    public boolean previousPage() {
        return previousPageImpl();
    }
    
    @Override
    public Bitmap getPageBitmapAsync(int pageIndex) {
        if (mCachedBitmaps.containsKey(Integer.valueOf(pageIndex)))
            return mCachedBitmaps.get(Integer.valueOf(pageIndex));
        
        return null;
    }
    
    @Override
    public synchronized boolean getPageBitmap(int chapterIndex, int pageIndex, BitmapInfor bitmapInfo) {
        if (chapterIndex < 0 || chapterIndex >= mChapterList.size()
                || bitmapInfo == null || bitmapInfo.mBitmap == null
                || bitmapInfo.mBitmap.isRecycled())
            return false;
        
        if (Thread.currentThread() == Looper.getMainLooper().getThread())
            throw new RuntimeException("method must be called from no ui thread, was: " + Thread.currentThread());
        
        bitmapInfo.issucced = false;
        loadChapter(chapterIndex, pageIndex);
        
        synchronized(this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        if (mWebView == null)
            return false;
        PaginationResult pagination = mPagePagination.get(chapterIndex);
        if (pagination == null)
            return false;
        
        Canvas canvas = new Canvas(bitmapInfo.mBitmap);
        int count = canvas.save(1);
        mWebView.draw(canvas);
        canvas.restoreToCount(count);
        
        bitmapInfo.mPageCount = pagination.getPagesCount();
        bitmapInfo.isOverLast = (pageIndex == -1 || pageIndex == bitmapInfo.mPageCount - 1) ? true : false;
        bitmapInfo.issucced = true;
        
//        Bitmap snapshotBitmap = null;
//        FileOutputStream fos = null;
//        try {
//            int snapshotWidth = getResources().getDisplayMetrics().widthPixels;
//            int snapshotHeight = getResources().getDisplayMetrics().heightPixels;
//            snapshotBitmap = Bitmap.createBitmap(snapshotWidth, snapshotHeight, Bitmap.Config.ARGB_8888);
//            if (snapshotBitmap != null && !snapshotBitmap.isRecycled()) {
//                Canvas canvas = new Canvas(snapshotBitmap);
//                
//                int count = canvas.save(1);
//                mWebView.draw(canvas);
//                canvas.restoreToCount(count);
//                fos = new FileOutputStream("/mnt/sdcard/lebook_crack/chapter_" + chapterIndex + "_page_" + pageIndex + ".jpg");
//                snapshotBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
//                fos.flush();
//            }
//        } catch (OutOfMemoryError e) {
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (fos != null)
//                    fos.close();
//            } catch (IOException e) {
//                
//            }
//        }
        
        return true;
    }

    @Override
    public void switchTextSelectMode(boolean selectionMode) {
    }
}
