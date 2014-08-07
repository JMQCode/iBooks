
package com.greenlemonmobile.app.ebook.books.reader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmobile.app.ebook.R;
import com.greenlemonmobile.app.ebook.books.adapter.TOCAdapter;
import com.greenlemonmobile.app.ebook.books.model.Book;
import com.greenlemonmobile.app.ebook.books.model.ReaderSettings;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser.NavPoint;
import com.greenlemonmobile.app.ebook.books.views.ReaderListener;
import com.greenlemonmobile.app.ebook.books.views.WebReader;
import com.greenlemonmobile.app.ebook.entity.Bookmark;
import com.greenlemonmobile.app.ebook.entity.FileInfo;
import com.greenlemonmobile.app.ebook.entity.LocalBook;
import com.greenlemonmobile.app.utils.FileUtil;
import com.greenlemonmobile.app.views.ActionItem;
import com.greenlemonmobile.app.views.QuickAction;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class EpubReaderActivity extends Activity implements OnClickListener, ReaderListener {
    /** The TAG for logging. */
    private static final String TAG = "EpubWebReaderActivity";
    
    private static final String INTENT_ACTION_TIME_TICK = "android.intent.action.TIME_TICK";
    private static final String INTENT_ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";
    
    
    /**
     * current ui handler message
     */
    private static final int HANDLER_BOOK_READY = 1;
    private static final int HANDLER_BOOK_ERROR = 2;
    
    private static final int HANDLER_SHOW_TOOLBAR = 3;
    private static final int HANDLER_HIDE_TOOLBAR = 4;
    
    private static final int HANDLER_CHAPTER_LOADING = 5;
    private static final int HANDLER_THEME_CHANGED = 6;
    private static final int HANDLER_PAGE_NAVIGATION_FINISHED = 7;
    
    private static final int HANDLER_PAGINATION_BEGIN = 8;
    private static final int HANDLER_PAGINATION_PROGRESS_UPDATE = 9;
    private static final int HANDLER_PAGINATION_FINISH = 10;
    private static final int HANDLER_CURRENT_PAGE_CHANGED = 13;
    
    private Book mBook = new Book();
    private String mBookId;
    private String mEncryptionKey = null;
    private String mRandom = null;
    
    private WebReader mWebReader;
    
    private GestureDetector mSingleTapUpDetector;
    
    /**
     * Layout Views
     */
    private RelativeLayout mMainContainer;

    private ProgressBar mLoadingBar;
    private View mToolbar;
    private SeekBar mPageSeekBar;
    private ProgressBar mPaginatingProgress;
    private TextView mSeekPageNumView;
    private TextView mBookNameView;
    private TextView mAuthorNameView;
    private TextView mChapterNameView;
    private TextView mPageIndexView;
    private ImageView mBatteryView;
    private TextView mTimeView;
    private ImageView mReadInfo;
    
    private QuickAction mDisplaySettings;
    private QuickAction mThemeSettings;
    private QuickAction mBrightnessSettings;
    
    /**
     * TOC Bookmark related Views
     */
    private View mTocContainer;
    private TabHost mTabHost;
    private ListView mTocList;
    private ListView mBookmarkList;
    private TOCAdapter mTOCAdapter;
    
    /**
     * book setting
     */
    private ReaderSettings mReaderSettings;
    
    /**
     * current configuration
     */
    private Configuration mCurConfig = new Configuration();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	//android.os.Debug.getNativeHeapAllocatedSize();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        //MyBookManager.init(this);
        
        this.setContentView(R.layout.act_epubreader);

        super.onCreate(savedInstanceState);  
        
        Intent intent = getIntent();
		if (intent != null) {
			if (!TextUtils.isEmpty(intent.getStringExtra("BookID"))) {
				mBookId = intent.getStringExtra("BookID");
				LocalBook book = new LocalBook(EpubReaderActivity.this);
				book.id = mBookId;
				book.load();
				mBook.path = book.file;
				mBook.name = book.title;
				mBook.author = book.author;
			} else if (!TextUtils.isEmpty(intent.getStringExtra("BookPath"))) {
				mBook.path = intent.getStringExtra("BookPath");
				FileInfo fileInfo = FileUtil.GetFileInfo(mBook.path);
				if (fileInfo != null)
				    mBook.name = FileUtil.getNameFromFilename(fileInfo.fileName);
			}
			mEncryptionKey = null;
			mRandom = null;
		}
        
        mReaderSettings = new ReaderSettings(this);
        // initialize
        this.initViews();
        
            mWebReader.openBook(mBook.path, mEncryptionKey, mRandom);
        
        this.initListeners();
        
        if (mReaderSettings.mAutoAdjustBrightness) {
        } else {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = (float)(mReaderSettings.mBrightness / (255 * 1.0f));
            getWindow().setAttributes(lp);
        }
        
        Configuration config = getResources().getConfiguration();
        //mCurConfig.setTo(config); // Since: API Level 8
        mCurConfig.setToDefaults();
        mCurConfig.orientation = config.orientation;
        
        Calendar calendar = Calendar.getInstance();
        String time = (new SimpleDateFormat("HH:mm")).format(calendar.getTime());
        mTimeView.setText(time);
        changeTheme();
        
        boolean hasRead = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("has_read", false);
        if (!hasRead) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("has_read", true).commit();
            mReadInfo.setVisibility(View.VISIBLE);
        }
    }
    
    private void saveConfig() {
    	mReaderSettings.save(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(INTENT_ACTION_TIME_TICK);
        intentfilter.addAction(INTENT_ACTION_BATTERY_CHANGED);
        registerReceiver(mReceiver, intentfilter);
        
        mDisplaySettings.dismiss();
        mThemeSettings.dismiss();
        mBrightnessSettings.dismiss();
        super.onResume();
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
    	if (mCurConfig.orientation != newConfig.orientation) {
    	    //mCurConfig.setTo(newConfig);
    	    mCurConfig.orientation = newConfig.orientation;
    		
    		//rePaginating(true);
    		mWebReader.viewSizeChanged();
    	}
		super.onConfigurationChanged(newConfig);
	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiver);
    	saveConfig();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mWebReader.closeBook();

        if (!TextUtils.isEmpty(mBookId) && mWebReader != null && mWebReader.isChapterReady(mWebReader.getCurrentChapterIndex())) {
            LocalBook book = new LocalBook(EpubReaderActivity.this);
            book.id = mBookId;
            book.load();
            
            book.current_page = mWebReader.getCurrentPageIndex();
            book.total_page = mBook.pageCount;
            book.last_access_date = System.currentTimeMillis();
            book.save();
            
            Bookmark bookmark = new Bookmark(this);
            bookmark.bookid = book.id;
            bookmark.author = book.author;
            bookmark.file = book.file;
            bookmark.title = book.title;
            bookmark.big_image = book.big_image;
            bookmark.medium_image = book.medium_image;
            bookmark.small_image = book.small_image;
            bookmark.detail_image = book.detail_image;
            bookmark.list_image = book.list_image;
            bookmark.chapter = mWebReader.getCurrentChapterIndex();
            bookmark.chapter_title = mWebReader.getCurrentChapterTitle();
            bookmark.current_offset = book.current_offset;
            bookmark.total_offset = book.total_offset;
            bookmark.current_page = book.current_page;
            bookmark.total_page = book.total_page;
            bookmark.modified_date = book.last_access_date;
            bookmark.save();
        }
        
        super.onDestroy();
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP:
            if (mReaderSettings.mVolumeKeysNavigation) {
                return true;
            }
            break;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (mReaderSettings.mVolumeKeysNavigation) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	switch (keyCode) {
    	case KeyEvent.KEYCODE_MENU:
    	case KeyEvent.KEYCODE_DPAD_CENTER:
            if (mToolbar.getVisibility() == View.GONE)
                mCurrentUIHandler.obtainMessage(HANDLER_SHOW_TOOLBAR).sendToTarget();
            else
                mCurrentUIHandler.obtainMessage(HANDLER_HIDE_TOOLBAR).sendToTarget();
    		break;
    	case KeyEvent.KEYCODE_BACK:
    	    if (mTocContainer.getVisibility() == View.VISIBLE) {
                mTocContainer.clearAnimation();
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.push_right_out);
                anim.setAnimationListener(new AnimationListener() {
                    
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }
                    
                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                    
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mTocContainer.setVisibility(View.GONE);
                        mTocContainer.clearAnimation();
                    }
                });
                mTocContainer.startAnimation(anim);
                return true;
            } else if (mToolbar.getVisibility() == View.VISIBLE) {
    	        mCurrentUIHandler.obtainMessage(HANDLER_HIDE_TOOLBAR).sendToTarget();
    	        return true;
    	    } else if (mWebReader.onBackKeyPressed()) {
    	        return true;
    	    }
    	    break;
    	case KeyEvent.KEYCODE_VOLUME_UP:
    	    if (mReaderSettings.mVolumeKeysNavigation) {
    	        mWebReader.previousPage();
    	        return true;
    	    }
    	    break;
    	case KeyEvent.KEYCODE_VOLUME_DOWN:
    	    if (mReaderSettings.mVolumeKeysNavigation) {
    	        mWebReader.nextPage();
    	        return true;
    	    }
    	    break;
    	}
		return super.onKeyDown(keyCode, event);
	}
    
	@Override
	public void onClick(View v) {
		boolean rePaginating = false;
		switch (v.getId()) {
        case R.id.settings_dialog_ok:
            mDisplaySettings.dismiss();
            mThemeSettings.dismiss();
            mBrightnessSettings.dismiss();
            break;
		case R.id.open_navigation:
		    if (mTocContainer.getVisibility() != View.VISIBLE && mTOCAdapter != null) {
		        TextView tocBookName = (TextView) mTocContainer.findViewById(R.id.toc_book_name);
		        if (mReaderSettings.isThemeNight) {
		            tocBookName.setTextColor(Color.parseColor("#AAAAAA"));
		            for (int index = 0; index < mTabHost.getTabWidget().getChildCount(); ++index) {
		                View childView = mTabHost.getTabWidget().getChildTabViewAt(index);
		                if (childView instanceof TextView)
		                    ((TextView)childView).setTextColor(Color.parseColor("#AAAAAA"));
		            }
		        } else {
		            tocBookName.setTextColor(Color.BLACK);
	                  for (int index = 0; index < mTabHost.getTabWidget().getChildCount(); ++index) {
	                        View childView = mTabHost.getTabWidget().getChildTabViewAt(index);
	                        if (childView instanceof TextView)
	                            ((TextView)childView).setTextColor(Color.BLACK);
	                    }
		        }
		        mTOCAdapter.notifyDataSetChanged();
		        
		        mTocContainer.setVisibility(View.VISIBLE);
		        mToolbar.setVisibility(View.GONE);
		        mTocContainer.clearAnimation();
		        mTocContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_in));
		    }
			break;
			
        case R.id.toolbar_theme:
            mThemeSettings.show(v);
            break;
            
        case R.id.toolbar_display_settings:
            mDisplaySettings.show(v);
            break;
            
        case R.id.toolbar_brightness:
            mBrightnessSettings.show(v);
            break;
			
        case R.id.theme_mode1:
        case R.id.theme_mode2:
        case R.id.theme_mode3:
        case R.id.theme_mode4:
        case R.id.theme_mode5:
        case R.id.theme_mode6:
        case R.id.theme_mode7:
        case R.id.theme_mode8:
        case R.id.theme_mode9:
		    if (mWebReader.isChapterLoading()) {
    		    switch (v.getId()) {
    	        case R.id.theme_mode1:
    	        	mReaderSettings.mTheme = 0;
    	        	break;
    	        case R.id.theme_mode2:
    	        	mReaderSettings.mTheme = 1;
    	        	break;
    	        case R.id.theme_mode3:
    	        	mReaderSettings.mTheme = 2;
    	        	break;
    	        case R.id.theme_mode4:
    	        	mReaderSettings.mTheme = 3;
    	        	break;
    	        case R.id.theme_mode5:
    	        	mReaderSettings.mTheme = 4;
    	        	break;
    	        case R.id.theme_mode6:
    	        	mReaderSettings.mTheme = 5;
    	        	break;
    	        case R.id.theme_mode7:
    	        	mReaderSettings.mTheme = 6;
    	        	break;
    	        case R.id.theme_mode8:
    	        	mReaderSettings.mTheme = 7;
    	        	break;
    	        case R.id.theme_mode9:
    	        	mReaderSettings.mTheme = 8;
    	        	break;
    		    }
    		    mReaderSettings.isThemeNight = false;
    		    
		        mWebReader.applyRuntimeSettings(mReaderSettings);
    			mCurrentUIHandler.obtainMessage(HANDLER_THEME_CHANGED).sendToTarget();
		    }
			break;
		case R.id.bookreader_bright_up_btn_epub:
            if (mReaderSettings.mBrightness < ReaderSettings.MAX_BRIGHTNESS) {
            	mReaderSettings.mBrightness += ReaderSettings.BRIGHTNESS_DELTA;
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = (float)(mReaderSettings.mBrightness / (255 * 1.0f));
                getWindow().setAttributes(lp);
            }
            break;
		case R.id.bookreader_bright_down_btn_epub:
            if (mReaderSettings.mBrightness > ReaderSettings.MIN_BRIGHTNESS) {
            	mReaderSettings.mBrightness -= ReaderSettings.BRIGHTNESS_DELTA;
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = (float)(mReaderSettings.mBrightness / (255 * 1.0f));
                getWindow().setAttributes(lp);
            }
            break;
            
        case R.id.screen_orientation_epub:
        	Configuration config = getResources().getConfiguration();
        	int requestedOrientation = (config.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ?
        			ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        	setRequestedOrientation(requestedOrientation);
            break;
            
//        case R.id.book_nav_minus:
//        	prevPage();
//        	break;
//        case R.id.book_nav_plus:
//        	nextPage();
//        	break;
			
        case R.id.bookreader_align_left:
        case R.id.bookreader_align_justified:
        case R.id.bookreader_align_right:
        case R.id.bookreader_align_center:
		    if (mWebReader.isChapterLoading()) {
		        switch (v.getId()) {
		            case R.id.bookreader_align_left:
		                mReaderSettings.mTextAlign = "left";
		                break;
		            case R.id.bookreader_align_justified:
		                mReaderSettings.mTextAlign = "justify";
		                break;
		            case R.id.bookreader_align_right:
		                mReaderSettings.mTextAlign = "right";
		                break;
		            case R.id.bookreader_align_center:
		                mReaderSettings.mTextAlign = "center";
		                break;
		        }
    			
		        mWebReader.applyRuntimeSettings(mReaderSettings);
		    }
			break;
		case R.id.night_mode:
            if (mWebReader.isChapterLoading()) {
                mReaderSettings.isThemeNight = !mReaderSettings.isThemeNight;
                mWebReader.applyRuntimeSettings(mReaderSettings);
            }
            break;
            
		case R.id.bookreader_lineheight_larger:
			if (mReaderSettings.mLineHeight < ReaderSettings.MAX_LINE_HEIGHT) {
				mReaderSettings.mLineHeight += ReaderSettings.LINE_HEIGHT_DELTA;
				rePaginating = true;
			}
			break;
		case R.id.bookreader_lineheight_smaller:
			if (mReaderSettings.mLineHeight > ReaderSettings.MIN_LINE_HEIGHT) {
				mReaderSettings.mLineHeight -= ReaderSettings.LINE_HEIGHT_DELTA;
				rePaginating = true;
			}
			break;
		case R.id.bookreader_smaller_font_btn_epub:
            if (mReaderSettings.mTextSize > ReaderSettings.MIN_FONT_SIZE) {
            	mReaderSettings.mTextSize -= ReaderSettings.FONT_SIZE_DELTA;
            	rePaginating = true;
            }
			break;
		case R.id.bookreader_larger_font_btn_epub:
            if (mReaderSettings.mTextSize < ReaderSettings.MAX_FONT_SIZE) {
            	mReaderSettings.mTextSize += ReaderSettings.FONT_SIZE_DELTA;
            	rePaginating = true;
            }
			break;
		}
		
		if (rePaginating)
		    mWebReader.applyRuntimeSettings(mReaderSettings);
	}
    
    private void changeTheme() {
        if (mReaderSettings.isThemeNight) {
            mMainContainer.setBackgroundColor(Color.BLACK);
            mTocContainer.setBackgroundColor(Color.BLACK);
        } else {
            if (mMainContainer.getBackground() != null && mMainContainer.getBackground() instanceof BitmapDrawable) {
                mMainContainer.getBackground().setCallback(null);
                if (mTocContainer.getBackground() != null)
                    mTocContainer.getBackground().setCallback(null);
                
                mMainContainer.setBackgroundDrawable(null);
                mTocContainer.setBackgroundDrawable(null);
                BitmapDrawable drawable = (BitmapDrawable) mMainContainer.getBackground();
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
            mMainContainer.setBackgroundDrawable(drawable);
            mTocContainer.setBackgroundDrawable(drawable);
        }
    }
    
    private void updateReadingProgress(int pageIndex, int pageCount) {
    	if (!mWebReader.isPaginatingFinish()) {
    	    mBook.pageCount = pageCount;
            String pageNum = String.format("%d/%d", pageIndex + 1, pageCount);
    		mPageIndexView.setText(pageNum);
    		mSeekPageNumView.setText(pageNum);
    		mPageSeekBar.setProgress(pageIndex);
    	}
    }
    
    private void updateViews() {
        if (mWebReader.isChapterReady(mWebReader.getCurrentChapterIndex())) {
            String title = mWebReader.getCurrentChapterTitle();
            if (TextUtils.isEmpty(title))
                mChapterNameView.setText("Chapter: " + (mWebReader.getCurrentChapterIndex() + 1));
            else
                mChapterNameView.setText(title);
        } else {
            mChapterNameView.setText("");
            mPageIndexView.setText("");
        }
        
        LinearLayout topInfo = (LinearLayout) findViewById(R.id.top_infoview);
        LinearLayout bottomInfo = (LinearLayout) findViewById(R.id.bottom_infoview);
        RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) topInfo.getLayoutParams();
        params.height = (int) mReaderSettings.mTopMargin;
        RelativeLayout.LayoutParams params2 = (android.widget.RelativeLayout.LayoutParams) bottomInfo.getLayoutParams();
        params2.height = (int) mReaderSettings.mBottomMargin;
    }
    
    /**
     * Initializes Views
     */
    private void initViews() {
        mMainContainer = (RelativeLayout) findViewById(R.id.main_layout);
        
        mWebReader = (WebReader) findViewById(R.id.webreader);
        mWebReader.initializeReader(this);
        mWebReader.setReaderListener(this);
        
        mToolbar = findViewById(R.id.bookreader_toolbar);
        mToolbar.setVisibility(View.GONE);
        mToolbar.setClickable(true);
        
        mLoadingBar = (ProgressBar)findViewById(R.id.chapter_loading_bar);
        mLoadingBar.setVisibility(View.GONE);
        
        mBookNameView = (TextView) findViewById(R.id.bookName);
        mAuthorNameView = (TextView) findViewById(R.id.authorName);
        mChapterNameView = (TextView) findViewById(R.id.chapterName);
        mPageIndexView = (TextView) findViewById(R.id.pageIndex);
        mBatteryView = (ImageView) findViewById(R.id.text_battery);
        mTimeView = (TextView) findViewById(R.id.text_time);
        
        mReadInfo = (ImageView) findViewById(R.id.read_info);
        
        /**
         * setup the toolbar
         */
        {
            ActionItem displayAction = new ActionItem(0, null, R.layout.bookreader_display_settings);
            mDisplaySettings = new QuickAction(this);
            mDisplaySettings.setTitle(getResources().getString(R.string.popup_display_title));
            mDisplaySettings.addActionItem(displayAction);
            
            mThemeSettings = new QuickAction(this);
            ActionItem themeAction = new ActionItem(0, null, R.layout.bookreader_theme_settings);
            mThemeSettings.setTitle(getResources().getString(R.string.themes_settings));
            mThemeSettings.addActionItem(themeAction);
            
            mBrightnessSettings = new QuickAction(this);
            ActionItem brightnessAction = new ActionItem(0, null, R.layout.bookreader_brightness_settings);
            mBrightnessSettings.setTitle(getResources().getString(R.string.brightness_settings));
            mBrightnessSettings.addActionItem(brightnessAction);
            
            mPageSeekBar = (SeekBar) findViewById(R.id.seek);
            mPaginatingProgress = (ProgressBar) findViewById(R.id.paginating_progress);
            mSeekPageNumView = (TextView) findViewById(R.id.bookreader_pagenumber);
        }
        
        /**
         * TOC Bookmark Views
         */
        {
            mTocContainer = findViewById(R.id.toc_tab_row);
            mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            mTabHost.setup();
            
            mTocList = (ListView) findViewById(R.id.toc_list);
            mBookmarkList = (ListView) findViewById(R.id.bookmark_list);        
            
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
            TextView topText = (TextView) inflater.inflate(R.layout.toc_tab_layout, null);
            topText.setText(R.string.toc_label);
            mTabHost.addTab(mTabHost.newTabSpec("tab1")
                    .setIndicator("tab1").setIndicator(topText)
                    .setContent(R.id.toc_list));
            
//            TextView bookmarkText = (TextView) inflater.inflate(R.layout.toc_tab_layout, null);
//            bookmarkText.setText(R.string.bookmark_label);
//            mTabHost.addTab(mTabHost.newTabSpec("tab2")
//                    .setIndicator("tab1").setIndicator(bookmarkText)
//                    .setContent(R.id.bookmark_list));
            View view = mTocContainer.findViewById(R.id.bookmark_list);
            view.setVisibility(View.GONE);
        }
        
        TextView bookName = (TextView) findViewById(R.id.title);
        bookName.setText(mBook.name);
        
        TextView tocBookName = (TextView) findViewById(R.id.toc_book_name);
        tocBookName.setText(mBook.name);
        
        mBookNameView.setText(mBook.name);
        mAuthorNameView.setText(mBook.author);
    }

    /**
     * Initializes Event Listeners
     */
    private void initListeners() {
        mSingleTapUpDetector = new GestureDetector(this, mSingleTapUpDetectorListener);
        
        mReadInfo.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mSingleTapUpDetector.onTouchEvent(event))
                    return true;
                return false;
            }
            
        });
        mToolbar.findViewById(R.id.middle_toolbar).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCurrentUIHandler.obtainMessage(HANDLER_HIDE_TOOLBAR).sendToTarget();
            }
            
        });
        mPageSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            	if (progress == seekBar.getMax())
            		progress = seekBar.getMax() - 1;
                String pageNum = String.format("%d/%d", progress + 1, seekBar.getMax());
                mSeekPageNumView.setText(pageNum);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mWebReader.gotoPercent(seekBar.getProgress() * 1.0F / seekBar.getMax());
            	}
        });
        
        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
            
            @Override
            public void onTabChanged(String tabId) {
            }
        });
        
        mTocList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mTocContainer.setVisibility(View.GONE);
                NavPoint nav = mTOCAdapter.getItem(position);
                int prePageCount = mWebReader.getChapterPreviousPageCount(nav.chapterIndex);
                int targetpageindex = nav.pageIndex - prePageCount -1;
                if (mWebReader.getCurrentChapterIndex() != nav.chapterIndex
                        || (mWebReader.getCurrentChapterIndex() == nav.chapterIndex && mWebReader.getCurrentPageIndex() != targetpageindex)) {
                    mWebReader.loadChapter(nav.chapterIndex, targetpageindex);
                } else if (mWebReader.isChapterReady(nav.chapterIndex)) {
                    mWebReader.loadChapter(nav.chapterIndex, targetpageindex);
                }
            }
        });
    }
    
    private SimpleOnGestureListener mSingleTapUpDetectorListener = new SimpleOnGestureListener() {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            mReadInfo.setVisibility(View.GONE);
            return false;
        }
        
    };
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_ACTION_TIME_TICK)) {
                Calendar calendar = Calendar.getInstance();
                String time = (new SimpleDateFormat("HH:mm")).format(calendar.getTime());
                mTimeView.setText(time);
            } else if (intent.getAction().equals(INTENT_ACTION_BATTERY_CHANGED)) {
                int i = intent.getIntExtra("level", 0);
                int j = intent.getIntExtra("scale", 100);
                int batteryLevel = (i * 100) / j;
                int batteryIcon = R.drawable.icon_statusbar_battery_5;
                if (batteryLevel <= 15)
                    batteryIcon = R.drawable.icon_statusbar_battery_0;
                else if (batteryLevel <= 28)
                    batteryIcon = R.drawable.icon_statusbar_battery_1;
                else if (batteryLevel <= 43)
                    batteryIcon = R.drawable.icon_statusbar_battery_2;
                else if (batteryLevel <= 57)
                    batteryIcon = R.drawable.icon_statusbar_battery_3;
                else if (batteryLevel <= 71)
                    batteryIcon = R.drawable.icon_statusbar_battery_4;
                else if (batteryLevel <= 85)
                    batteryIcon = R.drawable.icon_statusbar_battery_5;

                mBatteryView.setImageResource(batteryIcon);
            }
        }
        
    };
    
    private final Handler mCurrentUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLER_SHOW_TOOLBAR:
                if (mToolbar.getVisibility() == View.GONE) {
                    mToolbar.setVisibility(View.VISIBLE);
                    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                }
                break;
            case HANDLER_HIDE_TOOLBAR:
                if (mToolbar.getVisibility() == View.VISIBLE) {
                    mToolbar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                break;
            case HANDLER_CHAPTER_LOADING:
                mLoadingBar.setVisibility(View.VISIBLE);
            	mChapterNameView.setText("");
            	mPageIndexView.setText("");
                break;
            case HANDLER_THEME_CHANGED:
                changeTheme();
                break;
            case HANDLER_PAGE_NAVIGATION_FINISHED:
                mLoadingBar.setVisibility(View.GONE);
                updateViews();
                break;
            case HANDLER_CURRENT_PAGE_CHANGED:
                updateReadingProgress(msg.arg1, msg.arg2);
                break;
            case HANDLER_PAGINATION_BEGIN:
                mChapterNameView.setText("");
                mPageIndexView.setText("");
                mPageSeekBar.setVisibility(View.GONE);
                mPaginatingProgress.setVisibility(View.VISIBLE);
                mPaginatingProgress.setProgress(0);
                mSeekPageNumView.setText(R.string.paginating);
                break;
            case HANDLER_PAGINATION_PROGRESS_UPDATE:
                mPaginatingProgress.setProgress(msg.arg1);
                break;
            case HANDLER_PAGINATION_FINISH:
                mPageSeekBar.setVisibility(View.VISIBLE);
                mPaginatingProgress.setVisibility(View.GONE);
                mPageSeekBar.setMax(msg.arg1);
                mPageSeekBar.setProgress(msg.arg2);
                String pageNum = String.format("%d/%d", msg.arg2, msg.arg1);
                mSeekPageNumView.setText(pageNum);
                mPageIndexView.setText(pageNum);
                break;
            case HANDLER_BOOK_ERROR:
                Toast.makeText(mTocContainer.getContext(), R.string.fail_to_open_book, Toast.LENGTH_SHORT).show();
                finish();
                return;
            case HANDLER_BOOK_READY:
                mTOCAdapter = new TOCAdapter(mTocContainer.getContext(), mReaderSettings, mWebReader.getSequenceReadingChapterList(), mWebReader.getTOC(), mWebReader.getCurrentChapterIndex());
                mTocList.setAdapter(mTOCAdapter);
                break;
            }
            mCurrentUIHandler.removeMessages(msg.what);
        }
    };

    @Override
    public void d(String warning) {
        Log.d(TAG, warning);
    }

    @Override
    public void e(String error) {
        Log.e(TAG, error);
    }

    @Override
    public void onBookReady() {
        mCurrentUIHandler.obtainMessage(HANDLER_BOOK_READY).sendToTarget();
    }

    @Override
    public void onBookError() {
        mCurrentUIHandler.obtainMessage(HANDLER_BOOK_ERROR).sendToTarget();
    }

    @Override
    public void onPaginationStarting() {
        mCurrentUIHandler.obtainMessage(HANDLER_PAGINATION_BEGIN).sendToTarget();
    }
    
    @Override
    public void onPaginationProgressChanged(int progress) {
        mCurrentUIHandler.obtainMessage(HANDLER_PAGINATION_PROGRESS_UPDATE, progress, 0).sendToTarget();
    }

    @Override
    public void onPaginationReady(int pageCount, int pageIndex) {
        mCurrentUIHandler.obtainMessage(HANDLER_PAGINATION_FINISH, pageCount, pageIndex).sendToTarget();
    }

    @Override
    public void onChapterLoading(int chapterIndex) {
        mCurrentUIHandler.obtainMessage(HANDLER_CHAPTER_LOADING).sendToTarget();
    }

    @Override
    public void onChapterReady(int chapterIndex) {
    }

    @Override
    public void onCurrentPageChanged(int chapterIndex, int pageIndex, int pageCount) {
        mCurrentUIHandler.obtainMessage(HANDLER_CURRENT_PAGE_CHANGED, pageIndex, pageCount).sendToTarget();
    }
    
    @Override
    public void onPageNavigationFinish(int chapterIndex, int pageIndex) {
        mCurrentUIHandler.obtainMessage(HANDLER_PAGE_NAVIGATION_FINISHED).sendToTarget();
    }
    
    @Override
    public void onThemeApplied() {
        mCurrentUIHandler.obtainMessage(HANDLER_THEME_CHANGED).sendToTarget();
    }
    
    @Override
    public void onShowToolbar() {
        if (mToolbar.getVisibility() == View.GONE)
            mCurrentUIHandler.obtainMessage(HANDLER_SHOW_TOOLBAR).sendToTarget();
        else
            mCurrentUIHandler.obtainMessage(HANDLER_HIDE_TOOLBAR).sendToTarget();
    }
}
