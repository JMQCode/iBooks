package com.greenlemonmobile.app.ebook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.ebookdroid.CodecType;
import org.emdev.utils.LengthUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher.ViewFactory;

import com.greenlemonmobile.app.constant.DefaultConstant;
import com.greenlemonmobile.app.ebook.books.reader.EpubContext;
import com.greenlemonmobile.app.ebook.books.reader.EpubReaderActivity;
import com.greenlemonmobile.app.ebook.entity.LocalBook;
import com.loopj.android.http.JSONArrayPoxy;
import com.loopj.android.http.JSONObjectProxy;

public class SplashActivity extends Activity implements ViewFactory {
    private static final String SHOW_INFO = "show_info";
    
    private LinearLayout mSplashLayout;
    private Animation mLayoutAnimation;
    
    private ImageSwitcher mImageSwitcher;
    private Animation mSlideOutLeft;
    private Animation mSlideInRight;
    private Animation mSlideOutRight;
    private Animation mSlideInLeft;
    private int mImageIndex = 0;
    private boolean  mHasReadAllInfos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.act_splash);        
        
        mSplashLayout = (LinearLayout) findViewById(R.id.splash_layout);
        
        mImageSwitcher = (ImageSwitcher) findViewById(R.id.info_image_switcher);
        
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
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLayoutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        mLayoutAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mSplashLayout.setVisibility(View.GONE);
                
                boolean startReading = false;
                Intent intent = getIntent();
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
            	    		Intent readIntent = new Intent(SplashActivity.this, EpubReaderActivity.class);
            	    		String filePath = intent.getDataString();
            	    		if (filePath.startsWith("file://"))
            	    			filePath = filePath.substring(7);
            	    		readIntent.putExtra("BookPath", filePath);
            	    		startActivity(readIntent);
                    	} else {
            	    		Intent readIntent = new Intent(SplashActivity.this, org.ebookdroid.ui.viewer.ViewerActivity.class);
            	    		readIntent.setData(intent.getData());
            	    		readIntent.putExtra("persistent", "false");
            	    		readIntent.putExtra("nightMode", "false");
            	            startActivity(readIntent);
                    	}
                    }
                }
                
                if (!startReading)
                {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SplashActivity.this);
                    if (sp.getBoolean(SHOW_INFO, true)) {
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
    			                        	if (!LocalBook.isBuildInBookHasSaved(SplashActivity.this, file)) {
    			                        		LocalBook.importAssetLocalBook(SplashActivity.this, file);
    			                        		LocalBook.setBuildInBookSaved(SplashActivity.this, file);
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
    							super.run();
    						}
                        	
                        }.start();
                    } else
                        finishSplash();
        		}
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            
        });
        mSplashLayout.startAnimation(mLayoutAnimation);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mHasReadAllInfos)
            return true;
        return super.onKeyDown(keyCode, event);
    }

	@Override
    protected void onDestroy() {
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
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public View makeView() {
        ImageView i = new ImageView(this);
        i.setBackgroundColor(0xFF000000);
        i.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        return i;
    }
    
    private void finishSplash() {
        SplashActivity.this.finish();
        Intent intent = new Intent(SplashActivity.this, LibraryActivity.class);
        SplashActivity.this.startActivity(intent);
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
                } else
                    finishSplash();
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
}
