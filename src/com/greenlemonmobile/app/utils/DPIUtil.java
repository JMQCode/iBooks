package com.greenlemonmobile.app.utils;

import android.content.Context;

public class DPIUtil {
    
	public static int dip2px(Context context, float dipValue) {
		return (int) (dipValue * context.getResources().getDisplayMetrics().density + 0.5f);
	}

	public static int px2dip(Context context, float pxValue) {
		return (int) (pxValue / context.getResources().getDisplayMetrics().density + 0.5f);
	}
}
