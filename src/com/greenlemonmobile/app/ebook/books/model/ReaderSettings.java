package com.greenlemonmobile.app.ebook.books.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class ReaderSettings {
	// "justify"
	// "left"
	// "center"
	// "right"
    
	public static final int MAX_BRIGHTNESS = 255;
	public static final int MIN_BRIGHTNESS = 10;
	public static final int BRIGHTNESS_DELTA = 5;
	
	public static final float MAX_FONT_SIZE = 40.0f;
	public static final float MIN_FONT_SIZE = 10.0f;
	public static final float FONT_SIZE_DELTA = 2.0f;
	
	public static final float MAX_LINE_HEIGHT = 3.0f;
	public static final float MIN_LINE_HEIGHT = 1.3f;
	public static final float LINE_HEIGHT_DELTA = 0.2f;
	
	public ReaderSettings(Context context) {
		SharedPreferences sf = PreferenceManager.getDefaultSharedPreferences(context);
		
		isThemeNight = sf.getBoolean("isThemeNight", false);
		mFontFamily = sf.getString("mFontFamily", "");
		mFontUrl = sf.getString("mFontUrl", mFontUrl);
		mTextZoom = sf.getFloat("mTextZoom", 1.0f);
		mLineHeight = sf.getFloat("mLineHeight", 1.5f);
		mTextAlign = sf.getString("mTextAlign", "left");
		mAutoAdjustBrightness = sf.getBoolean("mAutoAdjustBrightness", false);
		mVolumeKeysNavigation = sf.getBoolean("mVolumeKeysNavigation", true);
		mKeekReaderScreenOn = sf.getBoolean("mKeekReaderScreenOn", false);

		int brightnessValue = 128;
		ContentResolver resolver = context.getContentResolver();
		try {
			brightnessValue = android.provider.Settings.System.getInt(resolver,
					Settings.System.SCREEN_BRIGHTNESS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		mBrightness = sf.getInt("mBrightness", brightnessValue);
		
		mTextSize = sf.getFloat("mTextSize", 20.0f);
		mLeftMargin = sf.getFloat("mLeftMargin", 20.0f);
		mTopMargin = sf.getFloat("mTopMargin", 60.0f);
		mRightMargin = sf.getFloat("mRightMargin", 20.0f);
		mBottomMargin = sf.getFloat("mBottomMargin", 60.0f);
		mColumnNums = sf.getInt("mColumnNums", 1);
		
		mTheme = sf.getInt("mTheme", 0);
	}

	public void save(Context context) {
		SharedPreferences sf = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sf.edit();

		editor.putBoolean("isThemeNight", isThemeNight);
		editor.putString("mFontFamily", mFontFamily);
		editor.putString("mFontUrl", mFontUrl);
		editor.putFloat("mTextZoom", mTextZoom);
		editor.putFloat("mLineHeight", mLineHeight);
		editor.putString("mTextAlign", mTextAlign);
		editor.putInt("mBrightness", mBrightness);
		editor.putFloat("mTextSize", mTextSize);
		editor.putFloat("mLeftMargin", mLeftMargin);
		editor.putFloat("mTopMargin", mTopMargin);
		editor.putFloat("mRightMargin", mRightMargin);
		editor.putFloat("mBottomMargin", mBottomMargin);
		editor.putInt("mTheme", mTheme);
		editor.putBoolean("mAutoAdjustBrightness", mAutoAdjustBrightness);
		editor.putBoolean("mVolumeKeysNavigation", mVolumeKeysNavigation);
		editor.putBoolean("mKeekReaderScreenOn", mKeekReaderScreenOn);
		editor.putInt("mColumnNums", mColumnNums);
		editor.commit();
	}

	public String mFontFamily;
	public String mFontUrl;
	public boolean isThemeNight;
	public float mLineHeight;
	public float mTextZoom;
	public float mTextSize;
	public String mTextAlign;
	public int mBrightness;	
	public float mLeftMargin;
	public float mTopMargin;
	public float mRightMargin;
	public float mBottomMargin;
	public int mTheme;
	public boolean mAutoAdjustBrightness;
	public boolean mVolumeKeysNavigation;
	public boolean mKeekReaderScreenOn;
	public int mColumnNums;
}
