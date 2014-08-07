package com.greenlemonmobile.app.ebook;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.greenlemonmobile.app.constant.DefaultConstant;
import com.greenlemonmobile.app.ebook.adapter.BookinfosGridAdapter;
import com.greenlemonmobile.app.ebook.adapter.BookinfosListAdapter;
import com.greenlemonmobile.app.ebook.books.reader.EpubContext;
import com.greenlemonmobile.app.ebook.books.reader.EpubReaderActivity;
import com.greenlemonmobile.app.ebook.entity.Bookmark;
import com.greenlemonmobile.app.ebook.entity.LocalBook;
import com.greenlemonmobile.app.ebook.entity.UpgradeInfo;
import com.greenlemonmobile.app.utils.Md5Encrypt;
import com.greenlemonmobile.app.views.ActionItem;
import com.greenlemonmobile.app.views.QuickAction;
import com.greenlemonmobile.app.views.QuickAction.OnActionItemClickListener;
import com.loopj.android.http.JSONArrayPoxy;
import com.loopj.android.http.JSONObjectProxy;

import org.ebookdroid.CodecType;
import org.emdev.utils.LengthUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class LibraryActivity extends Activity implements OnClickListener, OnCheckedChangeListener, OnItemClickListener, TextWatcher, ViewFactory {
    private static final String SHOW_INFO = "show_info";
	
    private static final String TAG = LibraryActivity.class.getSimpleName();
    
    private static final String VIEWTYPE = "view_type";
    private static final String ORDERCOLUMN = "order_column";
    private static final String ORDERTYPE = "order_type";
    
    private static final int ID_VIEWS_DETAILS = 1;
    private static final int ID_VIEWS_LIST = 2;
    private static final int ID_VIEWS_SMALL_ICON = 3;
    private static final int ID_VIEWS_MEDIUM_ICON = 4;
    private static final int ID_VIEWS_LARGE_ICON = 5;
    private static final int ID_VIEWS_BOOK_SHELF = 6;
    
    private static final int ID_SORT_BY_TITLE = 1;
    private static final int ID_SORT_BY_AUTHOR = 2;
    private static final int ID_SORT_BY_ADDITION_DATE = 3;
    private static final int ID_SORT_BY_LAST_ACCESS_DATE = 4;
    
    private static final int REQUEST_CODE_IMPORT_FILES = 1;
    
    public enum ViewType {
        BIG_THUMB, MEDIUM_THUMB, SMALL_THUMB, DETAILS, LIST, BOOK_SHELF
    }
    
    private static final int MESSAGE_REFRESH_LIST = 1;
    
    private LinearLayout mSplashLayout;
    private Animation mLayoutAnimation;
    
    private ImageSwitcher mImageSwitcher;
    private Animation mSlideOutLeft;
    private Animation mSlideInRight;
    private Animation mSlideOutRight;
    private Animation mSlideInLeft;
    private int mImageIndex = 0;
    private boolean  mHasReadAllInfos = true;
    
    private QuickAction mViewsAction;
    private QuickAction mSortAction;
    
    private ViewGroup mOperationPanel;
    private ViewGroup mToolbar;
    private ViewGroup mSearchPanel;
    private LinearLayout mContentContainer;
    private EditText mSearchInput;
    private Button mSortBtn;
    private CheckBox mSelectAllBtn;
    
    private TextView mItems;
    
    private LocalBook.OrderColumn mOrderColumn = LocalBook.OrderColumn.BY_LAST_ACCESS_TIME;
    private LocalBook.OrderType mOrderType = LocalBook.OrderType.DESC;
    private ViewType mViewType = ViewType.DETAILS;
    
    private AbsListView mList;
    private BaseAdapter mAdapter;
    private ArrayList<LocalBook> mLocalBooks;
    
    private boolean mPress2Exit = false;
    
    private static ArrayList<String> mReadiumFiles = new ArrayList<String>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.act_library);
        
        mOperationPanel = (ViewGroup) findViewById(R.id.operation_panel);
        mSearchPanel = (ViewGroup) findViewById(R.id.searchbar);
        mToolbar = (ViewGroup) findViewById(R.id.toolbar);
        mContentContainer = (LinearLayout) findViewById(R.id.content_container);
        mOperationPanel.setVisibility(View.GONE);
        
        mSearchInput = (EditText) findViewById(R.id.search_input);
        mSearchInput.addTextChangedListener(this);
        mSortBtn = (Button) findViewById(R.id.library_change_sort_criteria_header);
        mItems = (TextView) findViewById(R.id.items);
               
        ((CheckBox)findViewById(R.id.library_order_icon)).setOnCheckedChangeListener(this);
        mSelectAllBtn = ((CheckBox)findViewById(R.id.all_checkbox));
        mSelectAllBtn.setOnCheckedChangeListener(this);
        
        initViewsQuickAction();
        initSortQuickAction();
        
        restoreConfig();
        setSortTitle();
        
        mSplashLayout = (LinearLayout) findViewById(R.id.splash_layout);
        
        mImageSwitcher = (ImageSwitcher) findViewById(R.id.info_image_switcher);
        
        mLayoutAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        mSlideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        mSlideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        mSlideOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        mSlideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        
        mImageSwitcher.setFactory(this);
        mImageSwitcher.setInAnimation(mSlideInRight);
        mImageSwitcher.setOutAnimation(mSlideOutLeft);
        mImageSwitcher.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mGestureDetector.onTouchEvent(event))
                    return true;
                return false;
            }
            
        });
        mImageSwitcher.setImageResource(mImageIds[mImageIndex]);  
        
        mLocalBooks = new ArrayList<LocalBook>();
        LocalBook.getLocalBookList(this, mOrderColumn, mOrderType, mLocalBooks);
        ViewsChanged(mViewType);
        
        super.onCreate(savedInstanceState);
        detectEnvironment(this);
    }

    @Override
	protected void onNewIntent(Intent intent) {
    	processIntent(intent);
		super.onNewIntent(intent);
	}
    
    private boolean processIntent(Intent intent) {
    	boolean startReading = false;
        if (intent != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            CodecType codecType = null;
            final Uri data = intent.getData();
            if (data != null) {
                codecType = CodecType.getByUri(data.toString());
                if (codecType == null) {
                    final String type = intent.getType();
                    if (LengthUtils.isNotEmpty(type)) {
                        codecType = CodecType.getByMimeType(type);
                    }
                }
            }
            
            if (codecType != null) {
            	startReading = true;
            	if (codecType.getContextClass().getSimpleName().equals(EpubContext.class.getSimpleName())) {
    	    		Intent readIntent = new Intent(LibraryActivity.this, EpubReaderActivity.class);
    	    		String filePath = intent.getDataString();
    	    		if (filePath.startsWith("file://"))
    	    			filePath = filePath.substring(7);
    	    		readIntent.putExtra("BookPath", filePath);
    	    		startActivity(readIntent);
            	} else {
    	    		Intent readIntent = new Intent(LibraryActivity.this, org.ebookdroid.ui.viewer.ViewerActivity.class);
    	    		readIntent.setData(intent.getData());
    	    		readIntent.putExtra("persistent", "false");
    	    		readIntent.putExtra("nightMode", "false");
    	            startActivity(readIntent);
            	}
            }
        }
    	return startReading;
    }

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		final Intent intent = getIntent();
		if (intent != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
			mSplashLayout.setVisibility(View.GONE);
			mImageSwitcher.setVisibility(View.GONE);
			processIntent(intent);
		} else {
	    	mSplashLayout.setVisibility(View.VISIBLE);
	        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(LibraryActivity.this);
	        if (sp.getBoolean(SHOW_INFO, true))
	        	mImageSwitcher.setVisibility(View.VISIBLE);

	    	this.mUIHandler.postDelayed(new Runnable() {
	
				@Override
				public void run() {
	               mSplashLayout.setVisibility(View.GONE);

                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(LibraryActivity.this);
                    if (sp.getBoolean(SHOW_INFO, true)) {
                    	mHasReadAllInfos = false;
                        sp.edit().putBoolean(SHOW_INFO, false).commit();                    
                        mImageSwitcher.setVisibility(View.VISIBLE);
                        
                        new Thread() {

    						@Override
    						public void run() {
    		            		ArrayList<String> assets = new ArrayList<String>();
    		            		InputStream is = null;
    		            		ByteArrayOutputStream os = null;
    		            		try {
    		            			is = getAssets().open("books.json");
    		            			os = new ByteArrayOutputStream();
    		            			byte[] buffer = new byte[DefaultConstant.DEFAULT_BUFFER_SIZE];
    		            			int len = 0;
    		            			while ((len = is.read(buffer)) > 0) {
    		            				os.write(buffer, 0, len);
    		            			}
    		            			String bookString = new String(os.toByteArray(), DefaultConstant.DEFAULT_CHARSET);
    		            			try {
    		            				JSONObjectProxy json = new JSONObjectProxy(new JSONObject(bookString));
    		            				JSONArrayPoxy books = json.getJSONArray("books");
    		            				if (books != null) {
    		            					for (int index = 0; index < books.length(); ++index) {
    		            						assets.add(books.getJSONObject(index).getString("path"));
    		            					}
    		            				}
    		            			} catch (JSONException e) {
    		            				e.printStackTrace();
    		            			}
    		                    	try {
    			                        for (String file: assets) {
    			                        	if (!LocalBook.isBuildInBookHasSaved(LibraryActivity.this, file)) {
    			                        		LocalBook.importAssetLocalBook(LibraryActivity.this, file);
    			                        		LocalBook.setBuildInBookSaved(LibraryActivity.this, file);
    			                        	}
    			                        }
    		                    	} catch (Exception e) {
    		                    		e.printStackTrace();
    		                    	}
    		            		} catch (Exception e) {
    		            			e.printStackTrace();
    							} finally {
    								try {
    									if (is != null)
    										is.close();
    									if (os != null)
    										os.close();
    								} catch (IOException e) {
    									e.printStackTrace();
    								}
    		            			is = null;
    		            			os = null;
    		            		}
    		            		mUIHandler.sendEmptyMessage(MESSAGE_REFRESH_LIST);
    							super.run();
    						}
                        	
                        }.start();
                    } else {
                    	mSplashLayout.setVisibility(View.GONE);
                    	mImageSwitcher.setVisibility(View.GONE);
                    	
//                    	UpgradeInfo.checkUpdate(LibraryActivity.this, true);
                    }
        		}
	    		
	    	}, 1000);
		}
		super.onPostCreate(savedInstanceState);
	}



	@Override
    protected void onStart() {     
        super.onStart();
    }

    @Override
    protected void onResume() {
    	mSelectAllBtn.setChecked(false);
    	mUIHandler.sendEmptyMessage(MESSAGE_REFRESH_LIST);
        super.onResume();
    }

    @Override
    protected void onPause() {
        saveConfig();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (REQUEST_CODE_IMPORT_FILES == requestCode && resultCode == Activity.RESULT_OK) {
//        	mUIHandler.sendEmptyMessage(MESSAGE_REFRESH_LIST);
//        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.library_synchronize_btn:
                Intent synchronizeIntent = new Intent(LibraryActivity.this, ImportFilesActivity.class);
                LibraryActivity.this.startActivityForResult(synchronizeIntent, REQUEST_CODE_IMPORT_FILES);
                break;
            case R.id.settings:
                Intent settingIntent = new Intent(LibraryActivity.this, SettingPreference.class);
                LibraryActivity.this.startActivity(settingIntent);
                break;
            case R.id.view_btn:
                if (mViewsAction != null) {
                    int selectedId = 0;
                    switch (mViewType) {
                        case DETAILS:
                            selectedId = 0;
                            break;
                        case LIST:
                            selectedId = 1;
                            break;
                        case SMALL_THUMB:
                            selectedId = 2;
                            break;
                        case MEDIUM_THUMB:
                            selectedId = 3;
                            break;
                        case BIG_THUMB:
                            selectedId = 4;
                            break;
                        case BOOK_SHELF:
                        	selectedId = 5;
                        	break;
                    }
                    mViewsAction.setItemSelected(selectedId);
                    mViewsAction.show(v);
                }
                break;
            case R.id.library_change_sort_criteria_header:
                if (mSortAction != null) {
                    int selectedId = 0;
                    switch (mOrderColumn) {
                        case BY_TITLE:
                            selectedId = 0;
                            break;
                        case BY_AUTHOR:
                            selectedId = 1;
                            break;
                        case BY_ADDITION_DATE:
                            selectedId = 2;
                            break;
                        case BY_LAST_ACCESS_TIME:
                            selectedId = 3;
                            break;
                    }
                    mSortAction.setItemSelected(selectedId);
                    mSortAction.show(v);
                    mSortAction.setAnimStyle(QuickAction.ANIM_REFLECT);
                }
            case R.id.search_dialog_close:
                closeSearchPanel();
                mUIHandler.sendEmptyMessage(MESSAGE_REFRESH_LIST);
                break;
            case R.id.search_btn:
                showSearchPanel();
                break;
            case R.id.delete_btn:
            	final ArrayList<LocalBook> deletedBooks = new ArrayList<LocalBook>();
            	
            	if (mLocalBooks != null) {
            		for (LocalBook book : mLocalBooks) {
            			if (book.selected)
            				deletedBooks.add(book);
            		}
            		
            		final ProgressDialog deleteProgress = new ProgressDialog(this);
            		deleteProgress.setMessage(getResources().getString(R.string.deleting_book));
            		deleteProgress.setIndeterminate(true);
            		deleteProgress.setCancelable(false);
            		deleteProgress.show();
            		
            		new Thread() {

						@Override
						public void run() {
							for (LocalBook book : deletedBooks) {
								book.delete();
							}
							
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									mUIHandler.sendEmptyMessage(MESSAGE_REFRESH_LIST);
								}
								
							});
							if (deleteProgress != null)
								deleteProgress.dismiss();
							super.run();
						}
            			
            		}.start();
            	}
            	break;
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mHasReadAllInfos)
            return true;
        
        if (mSearchPanel.getVisibility() == View.VISIBLE) {
            closeSearchPanel();
            return true;
        }

        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (!mPress2Exit) {
                mPress2Exit = true;
                Toast.makeText(this, R.string.press_to_exit, Toast.LENGTH_SHORT).show();
                return true;
            } else {
                super.onKeyDown(keyCode, event);
                finish();                
                return true;
            }
        } else {
            mPress2Exit = false;
        }
        
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
    	for (String path : mReadiumFiles) {
    		File file = new File(path);
    		file.delete();
    	}
    	mReadiumFiles.clear();
        if (mViewsAction != null) {
            mViewsAction.setOnActionItemClickListener(null);
            mViewsAction = null;
        }
        
        if (mSortAction != null) {
            mSortAction.setOnActionItemClickListener(null);
            mSortAction = null;
        }
        
        if (mSplashLayout != null) {
            mSplashLayout.clearAnimation();
            mSplashLayout = null;
        }
        if (mLayoutAnimation != null) {
            mLayoutAnimation.setAnimationListener(null);
            mLayoutAnimation = null;
        }
        
        if (mImageSwitcher != null) {
            mImageSwitcher.setInAnimation(null);
            mImageSwitcher.setOutAnimation(null);
            mImageSwitcher.setOnTouchListener(null);
            mImageSwitcher = null;
        }
        mGestureDetector = null;
        mGestureListener = null;
        
        mSlideOutLeft = null;
        mSlideInRight = null;
        mSlideOutRight = null;
        mSlideInLeft = null;
        
        super.onDestroy();
        
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mPress2Exit = false;
        if (buttonView.getId() == R.id.library_order_icon) {
            LocalBook.OrderType type = isChecked ? LocalBook.OrderType.DESC : LocalBook.OrderType.ASC;
            
            if (type != mOrderType) {
                mOrderType = type;
                mUIHandler.sendEmptyMessage(MESSAGE_REFRESH_LIST);
            }
        } else if (buttonView.getId() == R.id.all_checkbox) {
        	for (LocalBook book : mLocalBooks)
        		book.selected = isChecked;
    		if (mAdapter != null)
    			mAdapter.notifyDataSetChanged();
    		updateOperationPanel();
        } else
        	updateOperationPanel();
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mPress2Exit = false;
        
        if (mLocalBooks != null && mLocalBooks.size() > position) {
        	LocalBook book = mLocalBooks.get(position);
        	
        	final File file = new File(book.file);
            final Uri data = Uri.fromFile(file);
            CodecType codecType = CodecType.getByUri(data.toString());

        	if (codecType.getContextClass().getSimpleName().equals(EpubContext.class.getSimpleName())) {
	    		Intent intent = new Intent(this, EpubReaderActivity.class);
	    		intent.putExtra("BookID", book.id);
	    		startActivity(intent);
        	} else {

                int currentPage = 0;
                Bookmark bookmark = Bookmark.getBookmark(this, Md5Encrypt.md5(book.file));
                if (bookmark != null)
                    currentPage = (int) bookmark.current_page;
                if (currentPage < 0)
                    currentPage = 0;
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(file));
                intent.setClass(this, org.ebookdroid.ui.viewer.ViewerActivity.class);
                intent.putExtra("persistent", "false");
                intent.putExtra("nightMode", "false");
                intent.putExtra("pageIndex", Integer.toString(currentPage));
                intent.putExtra("BookID", book.id);
                startActivity(intent);
        	}
        }
    }
    
    private void ViewsChanged(ViewType type) {
        mViewType = type;
        if (mViewType == ViewType.DETAILS || mViewType == ViewType.LIST)
            ((CheckBox)findViewById(R.id.all_checkbox)).setVisibility(View.VISIBLE);
        else
            ((CheckBox)findViewById(R.id.all_checkbox)).setVisibility(View.GONE);
        
        if (mList != null)
            mContentContainer.removeView(mList);

        ListView listView = null;
        GridView gridView = null;
        LayoutInflater inflater = LayoutInflater.from(this);
        switch (mViewType) {
            case LIST:
                listView = (ListView) inflater.inflate(R.layout.library_list, null);
                mAdapter = new BookinfosListAdapter(this, ViewType.LIST, mLocalBooks, this);
                break;
            case DETAILS:
                listView = (ListView) inflater.inflate(R.layout.library_list, null);
                mAdapter = new BookinfosListAdapter(this, ViewType.DETAILS, mLocalBooks, this);
                break;
            case SMALL_THUMB:
                gridView = (GridView) inflater.inflate(R.layout.library_grid_small, null);
                mAdapter = new BookinfosGridAdapter(this, ViewType.SMALL_THUMB, mLocalBooks);
                break;
            case MEDIUM_THUMB:
                gridView = (GridView) inflater.inflate(R.layout.library_grid_medium, null);
                mAdapter = new BookinfosGridAdapter(this, ViewType.MEDIUM_THUMB, mLocalBooks);
                break;
            case BIG_THUMB:
                gridView = (GridView) inflater.inflate(R.layout.library_grid_big, null);
                mAdapter = new BookinfosGridAdapter(this, ViewType.BIG_THUMB, mLocalBooks);
                break;
            case BOOK_SHELF:
            	gridView = (GridView) inflater.inflate(R.layout.library_grid_shelf, null);
            	mAdapter = new BookinfosGridAdapter(this, ViewType.BOOK_SHELF, mLocalBooks);
            	break;
        }
        if (gridView != null) {
            mList = gridView;
            gridView.setAdapter(mAdapter);
            gridView.setOnItemClickListener(this);
            mContentContainer.addView(gridView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        } else if (listView != null) {
            mList = listView;
            listView.setAdapter(mAdapter);
            listView.setOnItemClickListener(this);
            mContentContainer.addView(listView);
        }
    }
    
    private void updateOperationPanel() {
    	boolean showOperationPanel = false;
    	if (mLocalBooks != null) {
    		for (LocalBook book : mLocalBooks) {
    			if (book.selected) {
    				showOperationPanel = true;
    				break;
    			}
    		}
    	} else
    	    showOperationPanel = false;
    	
    	if (showOperationPanel) {
    		mOperationPanel.setVisibility(View.VISIBLE);
    		mToolbar.setVisibility(View.GONE);
    	} else {
    		mToolbar.setVisibility(View.VISIBLE);
    		mOperationPanel.setVisibility(View.GONE);
    	}
    }
    
    private void showSearchPanel() {
        mSearchPanel.setVisibility(View.VISIBLE);
        mToolbar.setVisibility(View.GONE);
    }
    
    private void closeSearchPanel() {
        mSearchPanel.setVisibility(View.GONE);
        mToolbar.setVisibility(View.VISIBLE);
    }
    
    private void initSortQuickAction() {
        ActionItem by_title = new ActionItem(ID_SORT_BY_TITLE, getResources().getString(R.string.by_title));
        ActionItem by_author = new ActionItem(ID_SORT_BY_AUTHOR, getResources().getString(R.string.by_author));
        ActionItem by_addition_date = new ActionItem(ID_SORT_BY_ADDITION_DATE, getResources().getString(R.string.by_addition_date));
        ActionItem last_access_date = new ActionItem(ID_SORT_BY_LAST_ACCESS_DATE, getResources().getString(R.string.last_access_date));
        mSortAction = new QuickAction(this);
        
        mSortAction.addActionItem(by_title);
        mSortAction.addActionItem(by_author);
        mSortAction.addActionItem(by_addition_date);
        mSortAction.addActionItem(last_access_date);
        
        mSortAction.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                mPress2Exit = false;
                LocalBook.OrderColumn column = mOrderColumn;
                switch (actionId) {
                    case ID_SORT_BY_TITLE:
                        column = LocalBook.OrderColumn.BY_TITLE;
                        break;
                    case ID_SORT_BY_AUTHOR:
                        column = LocalBook.OrderColumn.BY_AUTHOR;
                        break;
                    case ID_SORT_BY_ADDITION_DATE:
                        column = LocalBook.OrderColumn.BY_ADDITION_DATE;
                        break;
                    case ID_SORT_BY_LAST_ACCESS_DATE:
                        column = LocalBook.OrderColumn.BY_LAST_ACCESS_TIME;
                        break;
                }
                
                if (column != mOrderColumn) {
                    mOrderColumn = column;
                    setSortTitle();
                    mUIHandler.sendEmptyMessage(MESSAGE_REFRESH_LIST);
                }
            }
            
        });
    }

    private void initViewsQuickAction() {
        ActionItem details = new ActionItem(ID_VIEWS_DETAILS, getResources().getString(R.string.details));
        ActionItem list = new ActionItem(ID_VIEWS_LIST, getResources().getString(R.string.list));
        ActionItem small = new ActionItem(ID_VIEWS_SMALL_ICON, getResources().getString(R.string.small_thumbnail));
        ActionItem medium = new ActionItem(ID_VIEWS_MEDIUM_ICON, getResources().getString(R.string.medium_thumbnail));
        ActionItem large = new ActionItem(ID_VIEWS_LARGE_ICON, getResources().getString(R.string.big_thumbnail));
        ActionItem shelf = new ActionItem(ID_VIEWS_BOOK_SHELF, getResources().getString(R.string.shelf));
        mViewsAction = new QuickAction(this);
        
        mViewsAction.addActionItem(details);
        mViewsAction.addActionItem(list);
        mViewsAction.addActionItem(small);
        mViewsAction.addActionItem(medium);
        mViewsAction.addActionItem(large);
        mViewsAction.addActionItem(shelf);
        
        mViewsAction.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
//                ActionItem actionItem = mViewsActions.getActionItem(pos);
                mPress2Exit = false;
                ViewType type = ViewType.LIST;
                switch (actionId) {
                    case ID_VIEWS_DETAILS:
                        type = ViewType.DETAILS;
                        break;
                    case ID_VIEWS_LIST:
                        type = ViewType.LIST;
                        break;
                    case ID_VIEWS_SMALL_ICON:
                        type = ViewType.SMALL_THUMB;
                        break;
                    case ID_VIEWS_MEDIUM_ICON:
                        type = ViewType.MEDIUM_THUMB;
                        break;
                    case ID_VIEWS_LARGE_ICON:
                        type = ViewType.BIG_THUMB;
                        break;
                    case ID_VIEWS_BOOK_SHELF:
                    	type = ViewType.BOOK_SHELF;
                    	break;
                }
                
                if (type != mViewType)
                    ViewsChanged(type);
            }
            
        });
        
        mViewsAction.setOnDismissListener(new QuickAction.OnDismissListener() {

            @Override
            public void onDismiss() {
            }
            
        });
    }
    
    
    private void restoreConfig() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);        
        int viewType = sp.getInt(VIEWTYPE, 0);
        switch (viewType) {
            case 0:
                mViewType = ViewType.DETAILS;
                break;
            case 1:
                mViewType = ViewType.LIST;
                break;
            case 2:
                mViewType = ViewType.SMALL_THUMB;
                break;
            case 3:
                mViewType = ViewType.MEDIUM_THUMB;
                break;
            case 4:
                mViewType = ViewType.BIG_THUMB;
                break;
            case 5:
            	mViewType = ViewType.BOOK_SHELF;
            	break;
        }
        if (mViewType == ViewType.DETAILS || mViewType == ViewType.LIST)
            ((CheckBox)findViewById(R.id.all_checkbox)).setVisibility(View.VISIBLE);
        else
            ((CheckBox)findViewById(R.id.all_checkbox)).setVisibility(View.GONE);
        
        int orderColumn = sp.getInt(ORDERCOLUMN, 3);
        switch (orderColumn) {
            case 0:
                mOrderColumn = LocalBook.OrderColumn.BY_TITLE;
                break;
            case 1:
                mOrderColumn = LocalBook.OrderColumn.BY_AUTHOR;
                break;
            case 2:
                mOrderColumn = LocalBook.OrderColumn.BY_ADDITION_DATE;
                break;
            case 3:
                mOrderColumn = LocalBook.OrderColumn.BY_LAST_ACCESS_TIME;
                break;
        }
        
        int orderType = sp.getInt(ORDERTYPE, 1);
        switch (orderType) {
            case 0:
                mOrderType = LocalBook.OrderType.ASC;
                break;
            case 1:
                mOrderType = LocalBook.OrderType.DESC;
                break;
        }
    }
    
    private void saveConfig() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        switch (mViewType) {
            case DETAILS:
                editor.putInt(VIEWTYPE, 0);
                break;
            case LIST:
                editor.putInt(VIEWTYPE, 1);
                break;
            case SMALL_THUMB:
                editor.putInt(VIEWTYPE, 2);
                break;
            case MEDIUM_THUMB:
                editor.putInt(VIEWTYPE, 3);
                break;
            case BIG_THUMB:
                editor.putInt(VIEWTYPE, 4);
                break;
            case BOOK_SHELF:
            	editor.putInt(VIEWTYPE, 5);
            	break;
        }
        
        switch (mOrderColumn) {
            case BY_TITLE:
                editor.putInt(ORDERCOLUMN, 0);
                break;
            case BY_AUTHOR:
                mOrderColumn = LocalBook.OrderColumn.BY_AUTHOR;
                editor.putInt(ORDERCOLUMN, 1);
                break;
            case BY_ADDITION_DATE:
                editor.putInt(ORDERCOLUMN, 2);
                break;
            case BY_LAST_ACCESS_TIME:
                editor.putInt(ORDERCOLUMN, 3);
                break;
        }
        
        switch (mOrderType) {
            case ASC:
                editor.putInt(ORDERTYPE, 0);
                break;
            case DESC:
                editor.putInt(ORDERTYPE, 1);
                break;
        }
        editor.commit();
    }
    
    private void setSortTitle() {
        switch (mOrderColumn) {
            case BY_TITLE:
                mSortBtn.setText(R.string.by_title);
                break;
            case BY_AUTHOR:
                mSortBtn.setText(R.string.by_author);
                break;
            case BY_ADDITION_DATE:
                mSortBtn.setText(R.string.by_addition_date);
                break;
            case BY_LAST_ACCESS_TIME:
                mSortBtn.setText(R.string.last_access_date);
                break;
        }
    }

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void afterTextChanged(Editable s) {
		if (mSearchPanel.getVisibility() == View.VISIBLE) {
            LocalBook.getLocalBookList(LibraryActivity.this, mOrderColumn, mOrderType, mLocalBooks);
			String searchFor = mSearchInput.getText().toString();
			if (!TextUtils.isEmpty(searchFor)) {
				ArrayList<LocalBook> books = new ArrayList<LocalBook>();
				for (LocalBook book : mLocalBooks) {
					if (book.title.toLowerCase().contains(searchFor.toLowerCase()))
						books.add(book);
				}
				mLocalBooks.clear();
				if (books.size() > 0) {					
					for (LocalBook book : books)
						mLocalBooks.add(book);
				}
			}
            if (mAdapter != null)
                mAdapter.notifyDataSetChanged();
			String format = getResources().getString(R.string.items);
            if (mLocalBooks.size() > 0)
            	mItems.setText(String.format(format, Integer.toString(mLocalBooks.size())));
            else
            	mItems.setText(mSearchPanel.getVisibility() == View.VISIBLE ? R.string.library_noresult_message : R.string.library_noitems_message);
		}
	}
	
    private Handler mUIHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_REFRESH_LIST:
                LocalBook.getLocalBookList(LibraryActivity.this, mOrderColumn, mOrderType, mLocalBooks);
                if (mAdapter != null)
                    mAdapter.notifyDataSetChanged();
                String format = getResources().getString(R.string.items);
                if (mLocalBooks.size() > 0)
                	mItems.setText(String.format(format, Integer.toString(mLocalBooks.size())));
                else
                	mItems.setText(mSearchPanel.getVisibility() == View.VISIBLE ? R.string.library_noresult_message : R.string.library_noitems_message);
                updateOperationPanel();
				break;
			}
			super.handleMessage(msg);
		}
    	
    };
    
    @SuppressWarnings("deprecation")
    @Override
    public View makeView() {
        ImageView i = new ImageView(this);
        i.setBackgroundColor(0xFF000000);
        i.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        return i;
    }
    
    private OnGestureListener mGestureListener = new OnGestureListener() {

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
            if (e1.getX() > e2.getX()) {
                if (mImageIndex < mImageIds.length - 1) {
                    mImageSwitcher.setInAnimation(mSlideInRight);
                    mImageSwitcher.setOutAnimation(mSlideOutLeft);
                    ++mImageIndex;
                    mImageSwitcher.setImageResource(mImageIds[mImageIndex]);
                    
                    if (mImageIndex == mImageIds.length - 1)
                        mHasReadAllInfos = true;
                } else {
                	mImageSwitcher.setVisibility(View.GONE);
//                	UpgradeInfo.checkUpdate(LibraryActivity.this, true);
//                	mImageSwitcher.startAnimation(mLayoutAnimation);
//                	mLayoutAnimation.setAnimationListener(new AnimationListener() {
//
//						@Override
//						public void onAnimationEnd(Animation animation) {
//		                	mImageSwitcher.setVisibility(View.GONE);
//						}
//
//						@Override
//						public void onAnimationRepeat(Animation animation) {
//						}
//
//						@Override
//						public void onAnimationStart(Animation animation) {
//						}
//                		
//                	});
                }
            } else {
                if (mImageIndex > 0) {
                    mImageSwitcher.setInAnimation(mSlideInLeft);
                    mImageSwitcher.setOutAnimation(mSlideOutRight);
                    --mImageIndex;
                    mImageSwitcher.setImageResource(mImageIds[mImageIndex]);
                }
            }
            return false;
        }
        
    };
    
    @SuppressWarnings("deprecation")
    private GestureDetector mGestureDetector = new GestureDetector(mGestureListener);
    
    private Integer[] mImageIds = {
           R.drawable.info_1, R.drawable.info_2,
           R.drawable.info_3, R.drawable.info_4,
           R.drawable.info_5
    };
    
    public static void detectEnvironment(Activity zthis) {
//        InputStream is = null;
//        ByteArrayOutputStream baos = null;
//        
////        // construct the libreadium.so
////        try {
////	        is = zthis.getAssets().open("readium.zip");
////	        baos = new ByteArrayOutputStream();
////	        if (is != null) {
////	        	byte[] buf = new byte[1024*4];
////	        	int characters = 0;
////	        	while((characters = is.read(buf)) > 0) {
////	        		baos.write(buf, 0, characters);
////	        	}
////	        }
////	        byte[] buffers = NDKInitialize.constructEnvironment(zthis, baos.toByteArray(), baos.size());
////	        if (buffers != null) {
////	        	// encode the default zip file into libreadium.so
////				FileGuider savePath = new FileGuider(zthis, FileGuider.SPACE_PRIORITY_EXTERNAL);
////				savePath.setSpace(FileGuider.SPACE_PRIORITY_EXTERNAL);
////				savePath.setImmutable(true);
////				savePath.setChildDirName("/readium");
////				savePath.setFileName("libreadium.so");
////				savePath.setMode(Context.MODE_WORLD_WRITEABLE);
////				
////				FileOutputStream fos = new FileOutputStream(savePath.getFile());
////				fos.write(buffers);
////				fos.flush();
////				fos.close();
////        		System.gc();
////	        }
////        } catch (Exception e) {
////        	e.printStackTrace();
////        } finally {
////        	if (is != null) {
////				try {
////					is.close();
////				} catch (IOException e) {
////					e.printStackTrace();
////				}
////        	}
////        	
////        	if (baos != null) {
////        		try {
////					baos.close();
////				} catch (IOException e) {
////					e.printStackTrace();
////				}
////        	}
////        }
//        
//        // decode the libreadium.so to zip format
//        try {
//	        is = zthis.getResources().openRawResource(R.raw.libreadium);
//	        baos = new ByteArrayOutputStream();
//	        if (is != null) {
//	        	byte[] buf = new byte[DefaultConstant.DEFAULT_BUFFER_SIZE];
//	        	int characters = 0;
//	        	while((characters = is.read(buf)) > 0) {
//	        		baos.write(buf, 0, characters);
//	        	}
//	        	buf = null;
//	        	System.gc();
//	        }
//	        byte[] buffers = NDKInitialize.environmentDetect(zthis, baos.toByteArray(), baos.size(), isDebug(zthis));
//	        if (buffers != null) {
//	        	// encode the default zip file into libreadium.so
////				FileGuider savePath = new FileGuider(zthis, FileGuider.SPACE_PRIORITY_EXTERNAL);
////				savePath.setSpace(FileGuider.SPACE_PRIORITY_EXTERNAL);
////				savePath.setImmutable(true);
////				savePath.setChildDirName("/readium");
////				savePath.setFileName("libreadium.zip");
////				savePath.setMode(Context.MODE_WORLD_WRITEABLE);
////				
////				FileOutputStream fos = new FileOutputStream(savePath.getFile());
////				fos.write(buffers);
////				fos.flush();
////				fos.close();
////				System.gc();
//	        	String readiumZipPath = zthis.getFilesDir() + File.separator + FileUtil.READIUM_FOLDER + "readium.zip";
//	        	{
//	        		File file = new File(readiumZipPath);
//	        		file.getParentFile().mkdirs();
//	        	}
//				FileOutputStream fos = new FileOutputStream(readiumZipPath);
//				fos.write(buffers);
//				fos.flush();
//				fos.close();
//				System.gc();
//	        	ZipFile zipFile = new ZipFile(readiumZipPath);
//	            Enumeration<? extends ZipEntry> entries = zipFile.entries();
//	            while (entries != null && entries.hasMoreElements()) {
//	                ZipEntry entry = entries.nextElement();
//	                if (entry.isDirectory())
//	                    continue;
//	                try {
//	                    is = zipFile.getInputStream(entry);
//	                    String outFilePath = zthis.getFilesDir() + File.separator + FileUtil.READIUM_FOLDER + entry.getName();
//	                    File file = new File(outFilePath);
//	                    file.getParentFile().mkdirs();
//	                    fos = new FileOutputStream(outFilePath);
//	                    mReadiumFiles.add(outFilePath);
//	                    int count = DefaultConstant.DEFAULT_BUFFER_SIZE;
//	                    byte[] buffer = new byte[count];
//	                    int read = 0;
//	                    while ((read = is.read(buffer, 0, count)) != -1) {
//	                    	fos.write(buffer, 0, read);
//	                    }
//	                    fos.flush();
//	                } catch (IOException e) {
//	                    e.printStackTrace();
//	                } finally {
//	                    try {
//	                        if (is != null)
//	                            is.close();
//	                        if (fos != null)
//	                        	fos.close();
//	                    } catch (IOException e) {
//	                        e.printStackTrace();
//	                    }
//	                }
//	            }
//	            if (zipFile != null) {
//	            	zipFile.close();
//	            	File file = new File(readiumZipPath);
//	            	file.delete();
//	            }
//	        }
//        } catch (Exception e) {
//        	e.printStackTrace();
//        } finally {
//        	if (is != null) {
//				try {
//					is.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//        	}
//        	
//        	if (baos != null) {
//        		try {
//					baos.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//        	}
//        }
    }

    private static boolean isDebug(Activity zthis) {
        PackageManager mgr = zthis.getPackageManager();
        try {
            ApplicationInfo info = mgr.getApplicationInfo(zthis.getPackageName(), 0);
            Log.d(TAG, "ApplicationInfo.FLAG_DEBUGGABLE: "
                    + ApplicationInfo.FLAG_DEBUGGABLE);
            Log.d(TAG, "applicationInfo.flags: " + info.flags);
            Log.d(TAG, "info.flags & ApplicationInfo.FLAG_DEBUGGABLE: "
                    + (info.flags & ApplicationInfo.FLAG_DEBUGGABLE));
            Log.d(TAG, "debug: " + ((info.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE));
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE;
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return false;
    }
    
	public static boolean RootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
			}
		}
		Log.d("*** DEBUG ***", "Root SUC ");
		return true;
	}
}