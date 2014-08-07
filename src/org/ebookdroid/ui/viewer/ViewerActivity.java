package org.ebookdroid.ui.viewer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;

import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.ui.viewer.dialogs.GoToPageDialog;
import org.ebookdroid.ui.viewer.views.PageViewZoomControls;
import org.ebookdroid.ui.viewer.views.SearchControls;
import org.ebookdroid.ui.viewer.views.ViewEffects;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.LayoutUtils;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmobile.app.ebook.R;
import com.greenlemonmobile.app.ebook.entity.Bookmark;
import com.greenlemonmobile.app.ebook.entity.LocalBook;
import com.greenlemonmobile.app.views.ActionItem;
import com.greenlemonmobile.app.views.QuickAction;

public class ViewerActivity extends AbstractActionActivity<ViewerActivity, ViewerActivityController> implements OnClickListener{
    private static final String HAS_USED = "pdf_has_used";
    
    private static final String NIGHT_THEME = "pdf_night_theme";
    private static final String DAY_THEME = "pdf_day_theme";
    
    private static final String ORIENTATION = "pdf_orientation";
    
    private static final String BRIGHTNESS = "pdf_brightness";
	
    // current ui handler message
    private static final int MESSAGE_REFRESH_CURRENT_PAGE = 100;
    public static final int MESSAGE_SHOW_LOADINGBAR = 101;
    public static final int MESSAGE_HIDE_LOADINGBAR = 102;
    private static final int MESSAGE_UPDATE_TIME = 103;
    private static final int MESSAGE_UPDATE_BATTERY_LEVEL = 104;
    
    private static final String INTENT_ACTION_TIME_TICK = "android.intent.action.TIME_TICK";
    private static final String INTENT_ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";

    private static final int DIALOG_GOTO = 0;

    public static final DisplayMetrics DM = new DisplayMetrics();

    private static final AtomicLong SEQ = new AtomicLong();

    final LogContext LCTX;

    IView view;

    private Toast pageNumberToast;

    private Toast zoomToast;

    private PageViewZoomControls zoomControls;

    private SearchControls searchControls;

    private FrameLayout frameLayout;

    private boolean menuClosedCalled;
    
    private QuickAction mDisplaySettings;
    private QuickAction mThemeSettings;
    private QuickAction mBrightnessSettings;
    
    private ViewGroup mLoadingBar;
    
    private ViewGroup mTitlebar;
    private TextView mTextTime;
    private TextView mTextBattery;
    private int mBatteryLevel;
    private TextView mTitle;
    
    private ViewGroup mToolbar;
    private TextView mReadPageNumber;
    private SeekBar mReadPageProgress;
    
    private int mCurrentPage = 1;
    private int mTotalPage = 1;
    
    private String m_BookId;
    
    private Rect[] mNextPageArea = {new Rect(), new Rect()};
    private Rect[] mPreviousPageArea = {new Rect(), new Rect()};
    private Rect mCallToolbarArea = new Rect();
    
    private boolean mIsNightMode = false;
    private int mDayMode = 1;
    private int mOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    private int mBrightness;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_ACTION_TIME_TICK)) {
                updateTime();
            } else if (intent.getAction().equals(INTENT_ACTION_BATTERY_CHANGED)) {
                int i = intent.getIntExtra("level", 0);
                int j = intent.getIntExtra("scale", 100);
                LCTX.d((new StringBuilder()).append("level:").append(i).append(" scale:").append(j).toString());
                updateBattery((i * 100) / j);
            }
        }
        
    };

    /**
     * Instantiates a new base viewer activity.
     */
    public ViewerActivity() {
        super();
        LCTX = LogContext.ROOT.lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement(), true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected ViewerActivityController createController() {
        return new ViewerActivityController(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onNewIntent(): " + intent);
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onCreate(): " + getIntent());
        }
        restoreConfig();
        requestWindowFeature(mOrientation);
        
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = (float)(mBrightness / (255 * 1.0f));
        getWindow().setAttributes(lp);

        restoreController();
        getController().beforeCreate(this);
        
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(INTENT_ACTION_TIME_TICK);
        intentfilter.addAction(INTENT_ACTION_BATTERY_CHANGED);
        registerReceiver(mReceiver, intentfilter);

        super.onCreate(savedInstanceState);

        getWindowManager().getDefaultDisplay().getMetrics(DM);
        LCTX.i("XDPI=" + DM.xdpi + ", YDPI=" + DM.ydpi);

        view = AppSettings.current().viewType.create(getController());
        this.registerForContextMenu(view.getView());

        final AppSettings appSettings = AppSettings.current();

        IUIManager.instance.setFullScreenMode(this, view.getView(), appSettings.fullScreen);
        IUIManager.instance.setTitleVisible(this, appSettings.showTitle);
        
        setContentView(R.layout.act_pdfreader);
        
        frameLayout = (FrameLayout) findViewById(R.id.pageViewContainer);
        LayoutUtils.fillInParent(frameLayout, view.getView());

        frameLayout.addView(view.getView());
        frameLayout.addView(getZoomControls());
        frameLayout.addView(getSearchControls());
        
        initPanel();
        initTouchArea();
        
        
        if (getIntent() != null) 
            m_BookId = getIntent().getStringExtra("BookID");    
        
        getController().afterCreate();
    }

    @Override
    protected void onResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onResume()");
        }

        getController().beforeResume();

        super.onResume();
        IUIManager.instance.onResume(this);

        getController().afterResume();
        
        if (isPanelShowing()) {
            hideAllPanels();
            mDisplaySettings.dismiss();
            mThemeSettings.dismiss();
            mBrightnessSettings.dismiss();
        }
    }

    @Override
    protected void onPause() {
    	saveConfig();
    	
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPause(): " + isFinishing());
        }

        getController().beforePause();

        super.onPause();
        IUIManager.instance.onPause(this);

        getController().afterPause();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && this.view != null) {
            IUIManager.instance.setFullScreenMode(this, this.view.getView(),
                    AppSettings.current().fullScreen);
        }
    }

    @Override
    protected void onDestroy() {
    	unregisterReceiver(mReceiver);
    	
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy(): " + isFinishing());
        }
        if (!TextUtils.isEmpty(m_BookId)) {
            LocalBook book = new LocalBook(this);
            book.id = m_BookId;
            book.load();

            book.current_page = mCurrentPage;
            book.total_page = mTotalPage;
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
            bookmark.current_offset = book.current_offset;
            bookmark.total_offset = book.total_offset;
            bookmark.current_page = book.current_page;
            bookmark.total_page = book.total_page;
            bookmark.modified_date = book.last_access_date;
            bookmark.save();
        }

        getController().beforeDestroy();
        super.onDestroy();
        getController().afterDestroy();
    }

    protected IView createView() {
        return AppSettings.current().viewType.create(getController());
    }
    
    public void currentPageChanged(int pageIndex, int pageCount, final String bookTitle) {
        String pageText = pageIndex + "/" + pageCount;
        mCurrentPage = pageIndex;
        mTotalPage = pageCount;

        AppSettings app = AppSettings.current();
        if (app.showTitle && app.pageInTitle) {
            getWindow().setTitle("(" + pageText + ") " + bookTitle);
            return;
        }
        mTitle.setText(bookTitle);
        mReadPageNumber.setText(pageText);
        mReadPageProgress.setMax(mTotalPage);
        mReadPageProgress.setProgress(mCurrentPage);

        if (app.pageNumberToastPosition == ToastPosition.Invisible) {
            return;
        }
        if (pageNumberToast != null) {
            pageNumberToast.setText(pageText);
        } else {
            pageNumberToast = Toast.makeText(this, pageText, 0);
        }

        pageNumberToast.setGravity(app.pageNumberToastPosition.position, 0, 0);
        //pageNumberToast.show();
    }

    public void zoomChanged(final float zoom) {
        if (getZoomControls().isShown()) {
            return;
        }

        AppSettings app = AppSettings.current();

        if (app.zoomToastPosition == ToastPosition.Invisible) {
            return;
        }

        String zoomText = String.format("%.2f", zoom) + "x";

        if (zoomToast != null) {
            zoomToast.setText(zoomText);
        } else {
            zoomToast = Toast.makeText(this, zoomText, 0);
        }

        zoomToast.setGravity(app.zoomToastPosition.position, 0, 0);
        zoomToast.show();
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        getController().beforePostCreate();
        super.onPostCreate(savedInstanceState);
        getController().afterPostCreate();
    }

    public PageViewZoomControls getZoomControls() {
        if (zoomControls == null) {
            zoomControls = new PageViewZoomControls(this, getController().getZoomModel());
            zoomControls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        }
        return zoomControls;
    }

    public SearchControls getSearchControls() {
        if (searchControls == null) {
            searchControls = new SearchControls(this);
        }
        return searchControls;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
//        menu.setHeaderTitle(R.string.app_name);
//        menu.setHeaderIcon(R.drawable.ic_launcher);
//        final MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.mainmenu, menu);
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
//        final MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onMenuOpened(int, android.view.Menu)
     */
    @Override
    public boolean onMenuOpened(final int featureId, final Menu menu) {
//        view.changeLayoutLock(true);
//        IUIManager.instance.onMenuOpened(this);
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onPanelClosed(int, android.view.Menu)
     */
    @Override
    public void onPanelClosed(final int featureId, final Menu menu) {
//        menuClosedCalled = false;
        super.onPanelClosed(featureId, menu);
//        if (!menuClosedCalled) {
//            onOptionsMenuClosed(menu);
//        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onOptionsMenuClosed(android.view.Menu)
     */
    @Override
    public void onOptionsMenuClosed(final Menu menu) {
//        menuClosedCalled = true;
//        IUIManager.instance.onMenuClosed(this);
//        view.changeLayoutLock(false);
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        switch (id) {
            case DIALOG_GOTO:
                return new GoToPageDialog(getController());
        }
        return null;
    }

    @ActionMethod(ids = { R.id.mainmenu_zoom, R.id.actions_toggleTouchManagerView })
    public void toggleControls(final ActionEx action) {
        final View view = action.getParameter("view");
        ViewEffects.toggleControls(view);
    }

    @Override
    public final boolean dispatchKeyEvent(final KeyEvent event) {
        if (getController().dispatchKeyEvent(event)) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }
    
    @Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
    	if (pageViewGestureDetector.onTouchEvent(ev))
    		return true;
		return super.dispatchTouchEvent(ev);
	}
    
	@Override
	public void onClick(View v) {
        switch (v.getId()) {
            case R.id.open_navigation:
            	getController().showOutline(null);
                break;
            case R.id.search_btn:
                hideAllPanels();
                break;
            case R.id.toolbar_theme:
                mThemeSettings.show(v);
                break;
            case R.id.night_mode:
            	SettingsManager.toggleNightMode();
                break;
            case R.id.toolbar_display_settings:
                mDisplaySettings.show(v);
                break;
            case R.id.toolbar_brightness:
                mBrightnessSettings.show(v);
                break;
            case R.id.bookreader_align_justified:
                mDisplaySettings.dismiss();
                break;
            case R.id.bookreader_align_left:
                mDisplaySettings.dismiss();
                break;
            case R.id.bookreader_align_right:
                mDisplaySettings.dismiss();
                break;
            case R.id.bookreader_align_center:
                mDisplaySettings.dismiss();
                break;
            case R.id.bookreader_lineheight_larger:
                break;
            case R.id.bookreader_lineheight_smaller:
                break;
            case R.id.bookreader_smaller_font_btn_epub:
                break;
            case R.id.bookreader_larger_font_btn_epub:
                break;
            case R.id.screen_orientation_epub:
                mDisplaySettings.dismiss();
                mThemeSettings.dismiss();
                mBrightnessSettings.dismiss();
            	mOrientation = (mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            	setRequestedOrientation(mOrientation);
                break;
            case R.id.settings_dialog_ok:
                mDisplaySettings.dismiss();
                mThemeSettings.dismiss();
                mBrightnessSettings.dismiss();
                break;
            case R.id.bookreader_bright_up_btn_epub:
                if (mBrightness < 255) {
                    mBrightness += 5;
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = (float)(mBrightness / (255 * 1.0f));
                    getWindow().setAttributes(lp);
                }
                break;
            case R.id.bookreader_bright_down_btn_epub:
                if (mBrightness > 10) {
                    mBrightness -= 5;
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = (float)(mBrightness / (255 * 1.0f));
                    getWindow().setAttributes(lp);
                }
                break;
        }
	}

    public void showToastText(int duration, int resId, Object... args) {
        Toast.makeText(getApplicationContext(), getResources().getString(resId, args), duration).show();
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isPanelShowing())
            hideAllPanels();
        else
            showPanel();
        return true;//super.onPrepareOptionsMenu(menu);
    }
    
    private void updateTime() {
        mCurrentUIHandler.sendEmptyMessage(MESSAGE_UPDATE_TIME);
    }
    private void updateBattery(int level) {        
        mCurrentUIHandler.sendMessage(mCurrentUIHandler.obtainMessage(MESSAGE_UPDATE_BATTERY_LEVEL, level, 0));
    }
    
    private void setBatteryIcon(TextView textview) {
        switch (mBatteryLevel / 10) {
            case 0:
                textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_statusbar_battery_0, 0, 0, 0);
                break;
            case 1:
            case 2:
                textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_statusbar_battery_1, 0, 0, 0);
                break;
            case 3:
            case 4:
                textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_statusbar_battery_2, 0, 0, 0);
                break;
            case 5:
            case 6:
                textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_statusbar_battery_3, 0, 0, 0);
                break;
            case 7:
            case 8:
                textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_statusbar_battery_4, 0, 0, 0);
                break;
            case 9:
            case 10:
                textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_statusbar_battery_5, 0, 0, 0);
                break;
        }
    }
    
    private boolean isPanelShowing() {
        return (mTitlebar.getVisibility() == View.VISIBLE) && (mToolbar.getVisibility() == View.VISIBLE) /*&& (mSearchPanel.getVisibility() == View.VISIBLE)*/;
    }
    
    private void hideAllPanels() {
        mTitlebar.setVisibility(View.GONE);
        mToolbar.setVisibility(View.GONE);
    }
    
    private void showPanel() {
        mTitlebar.setVisibility(View.VISIBLE);
        mToolbar.setVisibility(View.VISIBLE);
    }
    
    private void initPanel() {
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
        
        mLoadingBar = (ViewGroup) findViewById(R.id.progress);
        mTitlebar = (ViewGroup) findViewById(R.id.top_toolbar);
        mTextTime = (TextView) findViewById(R.id.text_time);
        mTextBattery = (TextView) findViewById(R.id.text_battery);
        mTitle = (TextView) findViewById(R.id.title);
        
        mToolbar = (ViewGroup) findViewById(R.id.bottom_toolbar);
        mReadPageNumber = (TextView) findViewById(R.id.bookreader_pagenumber);
        mReadPageProgress = (SeekBar) findViewById(R.id.seek);
        mReadPageProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mReadPageNumber.setText(Integer.toString(progress) + " / " + Integer.toString(mTotalPage));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            	int pageNumber = seekBar.getProgress();
            	if (pageNumber < 1)
            		pageNumber = 1;
                final int pageCount = getController().getDocumentModel().getPageCount();
                if (pageNumber < 1 || pageNumber > pageCount) {
                    Toast.makeText(ViewerActivity.this, getResources().getString(R.string.bookmark_invalid_page) + pageCount, 2000)
                            .show();
                    return;
                }
                getController().jumpToPage(pageNumber - 1, 0, 0, false);
            }
            
        });
    }
    
    private void restoreConfig() {
        mIsNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(NIGHT_THEME, false);
        mDayMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(DAY_THEME, 5);
        mOrientation = PreferenceManager.getDefaultSharedPreferences(this).getInt(ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        int brightnessValue = 128;
        ContentResolver resolver = getContentResolver();
        try {
            brightnessValue = android.provider.Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        mBrightness = PreferenceManager.getDefaultSharedPreferences(this).getInt(BRIGHTNESS, brightnessValue);
    }
    
    private void saveConfig() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(NIGHT_THEME, mIsNightMode);
        editor.putInt(DAY_THEME, mDayMode);
        editor.putInt(ORIENTATION, mOrientation);
        editor.putInt(BRIGHTNESS, mBrightness);
        editor.commit();
    } 
    
    private void initTouchArea() {
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        
        if (screenWidth > screenHeight) {
            int temp = screenWidth;
            screenWidth = screenHeight;
            screenHeight = temp;
        }
        
        int areaWidth = screenWidth / 3;
        
        mCallToolbarArea.left = areaWidth;
        mCallToolbarArea.right = mCallToolbarArea.left + areaWidth;
        mCallToolbarArea.top = areaWidth;
        mCallToolbarArea.bottom = mCallToolbarArea.top + screenHeight - 2 * areaWidth; 
        
        mPreviousPageArea[0].left = 0;
        mPreviousPageArea[0].top = 0;
        mPreviousPageArea[0].right = mPreviousPageArea[0].left + areaWidth;
        mPreviousPageArea[0].bottom = screenHeight;
        
        mPreviousPageArea[1].left = 0;
        mPreviousPageArea[1].top = 0;
        mPreviousPageArea[1].right = screenWidth - areaWidth;
        mPreviousPageArea[1].bottom = areaWidth;
        
        mNextPageArea[0].left = screenWidth - areaWidth;
        mNextPageArea[0].right = screenWidth;
        mNextPageArea[0].top = 0;
        mNextPageArea[0].bottom = screenHeight;
        
        mNextPageArea[1].left = areaWidth;
        mNextPageArea[1].top = screenHeight - areaWidth;
        mNextPageArea[1].right = screenWidth;
        mNextPageArea[1].bottom = screenHeight;
    }
    
    public final Handler mCurrentUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_REFRESH_CURRENT_PAGE:
                break;
            case MESSAGE_SHOW_LOADINGBAR:                
                mLoadingBar.setVisibility(View.VISIBLE);
                break;
            case MESSAGE_HIDE_LOADINGBAR:
                mLoadingBar.setVisibility(View.GONE);
                break;
            case MESSAGE_UPDATE_TIME:
                Calendar calendar = Calendar.getInstance();
                String time = (new SimpleDateFormat("HH:mm")).format(calendar.getTime());
                mTextTime.setText(time);
                break;
            case MESSAGE_UPDATE_BATTERY_LEVEL:
                mBatteryLevel = msg.arg1;
                String level = (new StringBuilder()).append("").append(mBatteryLevel).append("%").toString();
                mTextBattery.setText(level);
                setBatteryIcon(mTextBattery);
                break;
            }
        }
    };

	private OnGestureListener pageViewGestureDectectorListener = new OnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			if (mTitlebar.getVisibility() == View.VISIBLE) {
				Rect rect = new Rect(mTitlebar.getLeft(), mTitlebar.getTop(), mTitlebar.getRight(), mTitlebar.getBottom());
				if (rect.contains((int)e.getX(), (int)e.getY()))
					return false;
			}
			if (mToolbar.getVisibility() == View.VISIBLE) {
				Rect rect = new Rect(mToolbar.getLeft(), mToolbar.getTop(), mToolbar.getRight(), mToolbar.getBottom());
				if (rect.contains((int)e.getX(), (int)e.getY()))
					return false;
			}
			
            if (mCallToolbarArea.contains((int)e.getX(), (int)e.getY())) 
            {
                if (isPanelShowing())
                    hideAllPanels();
                else
                    showPanel();
                return true;
            }  else {
                for (int index = 0; index < mPreviousPageArea.length; ++index) {
                    if (mPreviousPageArea[index].contains((int)e.getX(), (int)e.getY())) {
                        if (isPanelShowing())
                            hideAllPanels();
                        else
                            showPanel();
                        return true;
                    }
                }
                
                for (int index = 0; index < mNextPageArea.length; ++index) {
                    if (mNextPageArea[index].contains((int)e.getX(), (int)e.getY())) {
                        if (isPanelShowing())
                            hideAllPanels();
                        else
                            showPanel();
                        return true;
                    }
                }
            }
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {			
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return false;
		}
    	
    };
    private GestureDetector pageViewGestureDetector = new GestureDetector(pageViewGestureDectectorListener);
}
