package com.greenlemonmobile.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

import com.greenlemonmobile.app.ebook.R;

public class BookShelf extends GridView {
	
	private static Drawable shelfLeft;
	private static Drawable shelfRight;
	private static Drawable shelfCenter;
	private static Drawable shelfEmptyLeft;
	private static Drawable shelfEmptyRight;
	private static int itemHeight;
	
	public BookShelf(Context context) {
		super(context);
	}

	public BookShelf(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0);
	}
	
	public BookShelf(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}
	
	private void init(Context context, AttributeSet attrs, int defStyle) {
		TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.BookShelf, defStyle, 0);
		
		if (shelfLeft == null) {
			int id = typeArray.getResourceId(R.styleable.BookShelf_shelfLayerLeft, R.drawable.bookshelf_layer_left);
			shelfLeft = context.getResources().getDrawable(id);
		}
		if (shelfRight == null) {
			int id = typeArray.getResourceId(R.styleable.BookShelf_shelfLayerRight, R.drawable.bookshelf_layer_right);
			shelfRight = context.getResources().getDrawable(id);
		}
		if (shelfCenter == null) {
			int id = typeArray.getResourceId(R.styleable.BookShelf_shelfLayerCenter, R.drawable.bookshelf_layer_center);
			shelfCenter = context.getResources().getDrawable(id);
		}
		if (shelfEmptyLeft == null) {
			int id = typeArray.getResourceId(R.styleable.BookShelf_shelfWebLeft, R.drawable.bookshelf_web_left);
			shelfEmptyLeft = context.getResources().getDrawable(id);
		}
		if (shelfEmptyRight == null) {
			int id = typeArray.getResourceId(R.styleable.BookShelf_shelfWebRight, R.drawable.bookshelf_web_right);
			shelfEmptyRight = context.getResources().getDrawable(id);
		}
		typeArray.recycle();
		
		itemHeight = (int) getContext().getResources().getDimension(R.dimen.bookShelfHeight);
	}

	@Override
	public void draw(Canvas canvas) {
		int height = getHeight();
		int drawableHeight = (int) getContext().getResources().getDimension(R.dimen.bookShelfHeight);
		
		int childsCount = this.getChildCount();
		int topAdjust = 0;
		if (childsCount > 0) {
			View child = getChildAt(0);
			topAdjust = getTop() - child.getTop();
		}
		
		if (topAdjust > 0)
			height += topAdjust;

		int counts = (height / drawableHeight) + ((height % drawableHeight > 0) ? 1 : 0);
		Rect bounds = new Rect();
		for (int index = 0; index < counts; ++index) {
			bounds.left = getLeft();
			bounds.right = (int) (bounds.left + getContext().getResources().getDimension(R.dimen.bookShelfLeftMargin));
			bounds.top = getTop() - topAdjust + index * drawableHeight;
			bounds.bottom = bounds.top + drawableHeight;				
			shelfLeft.setBounds(bounds);
			shelfLeft.draw(canvas);
			
			bounds.left = (int) (getRight() - getContext().getResources().getDimension(R.dimen.bookShelfRightMargin));
			bounds.right = getRight();
			bounds.top = getTop() - topAdjust + index * drawableHeight;
			bounds.bottom = bounds.top + drawableHeight;
			shelfRight.setBounds(bounds);
			shelfRight.draw(canvas);
			
			bounds.left = (int) (getLeft() + getContext().getResources().getDimension(R.dimen.bookShelfLeftMargin));
			bounds.right = (int) (getRight() - getContext().getResources().getDimension(R.dimen.bookShelfRightMargin));
			bounds.top = getTop() - topAdjust + index * drawableHeight;
			bounds.bottom = bounds.top + drawableHeight;				
			shelfCenter.setBounds(bounds);
			shelfCenter.draw(canvas);
		}
		
		if (childsCount == 0) {
			bounds.left = getLeft();
			bounds.right = bounds.left + shelfEmptyLeft.getIntrinsicWidth();
			bounds.top = getTop() - topAdjust;
			bounds.bottom = bounds.top + shelfEmptyLeft.getIntrinsicHeight();				
			shelfEmptyLeft.setBounds(bounds);
			shelfEmptyLeft.draw(canvas);
			
			bounds.left = getRight() - shelfEmptyRight.getIntrinsicWidth();
			bounds.right = getRight();
			bounds.top = getTop() - topAdjust + drawableHeight;
			bounds.bottom = bounds.top + shelfEmptyRight.getIntrinsicHeight();				
			shelfEmptyRight.setBounds(bounds);
			shelfEmptyRight.draw(canvas);
		}
		
		super.draw(canvas);
	}
}
