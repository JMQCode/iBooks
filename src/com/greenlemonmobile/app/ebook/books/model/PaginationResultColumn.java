package com.greenlemonmobile.app.ebook.books.model;

import android.graphics.Rect;
import android.util.SparseArray;


import java.util.ArrayList;
import java.util.List;

public class PaginationResultColumn {
    private final static int MAX_SCREEN_HEIGHT = 80240;
    
	public static class PageBounds {

		public int getWidth() {
			return pageRight - pageLeft;
		}

		public boolean intersects(int top, int bottom) {
			boolean flag;
			if (pageLeft < bottom && top < pageRight)
				flag = true;
			else
				flag = false;
			return flag;
		}

		public final int pageRight;
		public final int pageLeft;
		public final int offset_from;
		public final int offset_to;

		public PageBounds(int left, int right, int from, int to) {
			super();
			pageLeft = left;
			pageRight = right;
			
			offset_from = from;
			offset_to = to;
		}
	}
	
	public static class TableInfo {
	    public final Rect bounds;
	    public final String html;
	    
	    public TableInfo(int left, int top, int right, int bottom, String html) {
	        bounds = new Rect(left, top, right, bottom);
	        this.html = html;
	    }
	}

	/**
	 * javascript will call the function to tell us the page pagination result
	 * @param pageBounds page pagination rect(top, bottom)
	 * @param pageOffsets page content text offset (from, to)
	 * @param tableBounds table information (rect, html)
	 */
	public PaginationResultColumn(String pageBounds, String pageOffsets, String tableBounds, List<TouchableItem> touchableItems) {
		String boundsArray[] = pageBounds.split(",");
		String offsetArray[] = pageOffsets.split(",");
		final String splitString = "<<>>";
		
		int index = 0;
		int arrayLength = boundsArray.length;
		mPageLists = new ArrayList<Integer>(arrayLength / 2/*boundsArray.length / 2*/);
		for (int i = 0; i + 1 < arrayLength; i += 2) {
			int left = Integer.valueOf(boundsArray[i]).intValue();
			int right = Integer.valueOf(boundsArray[i + 1]).intValue();
			int from = (arrayLength == offsetArray.length) ? Integer.valueOf(offsetArray[i]).intValue() : 0;
			int to = (arrayLength == offsetArray.length) ? Integer.valueOf(offsetArray[i + 1]).intValue() : 0;

			mPageLists.add(index);
			mPageBounds.put(index++, new PageBounds(left, right, from, to));
		}
        
        String tableBoundsArray[] = tableBounds.split(splitString);
        if (tableBoundsArray.length > 0) {
            mTableInfos = new ArrayList<TableInfo>(tableBoundsArray.length / 5);
            for (int i = 0; i + 1 < tableBoundsArray.length; i += 5) {
                int left = Integer.valueOf(tableBoundsArray[i]).intValue();
                int top = Integer.valueOf(tableBoundsArray[i + 1]).intValue();
                int right = Integer.valueOf(tableBoundsArray[i + 2]).intValue();
                int bottom = Integer.valueOf(tableBoundsArray[i + 3]).intValue();
                
                String html = tableBoundsArray[i + 4];
                
                mTableInfos.add(new TableInfo(left, top, right, bottom, html));
            }
        }
        
        if (touchableItems != null && touchableItems.size() > 0) {
            mTouchableItems = new ArrayList<TouchableItem>();
            for (TouchableItem object : touchableItems)
                mTouchableItems.add(object);
        }
	}
	
    public PaginationResultColumn(int pageIndex, String pageBounds) {
        String boundsArray[] = pageBounds.split(",");
        
        int arrayLength = boundsArray.length;
        mPageLists = new ArrayList<Integer>(arrayLength / 2);
        if (arrayLength / 2 > 1)
            pageIndex -= 1;
        for (int i = 0; i + 1 < arrayLength; i += 2) {
            int top = Integer.valueOf(boundsArray[i]).intValue();
            int bottom = Integer.valueOf(boundsArray[i + 1]).intValue();
            mPageLists.add(pageIndex);
            mPageBounds.put(pageIndex++, new PageBounds(top, bottom, 0, 0));
        }
    }

	public int getBottomOfLastPage() {
	    if (mPageBounds.size() == 0)
	        return 0;
		return ((PageBounds) mPageBounds.get(-1 + mPageBounds.size())).pageRight;
	}

	public PageBounds getPageBounds(int i) {
	    if (mPageBounds.size() - 1 < i)
	        return null;
		return (PageBounds) mPageBounds.get(i);
	}
	
    public int getPageByOffset(int textOffset) {
        int pageIndex = 1;
        if (mPageBounds != null && mPageBounds.size() > 0) {            
            for (int index = 0; index < mPageLists.size(); ++index) {
                if (mPageBounds.get(mPageLists.get(index)) != null) {
                    PageBounds pageBounds = mPageBounds.get(mPageLists.get(index));
                    if (textOffset >= pageBounds.offset_from && textOffset <= pageBounds.offset_to) {
                        break;
                    }
                    ++pageIndex;
                }
            }
        }
        
        return pageIndex;
    }
    
    public int getPageByRect(Rect rect) {
        int pageIndex = 1;
        
        if (mPageBounds != null && mPageBounds.size() > 0) {            
            for (int index = 0; index < mPageLists.size(); ++index) {
                if (mPageBounds.get(mPageLists.get(index)) != null) {
                    PageBounds pageBounds = mPageBounds.get(mPageLists.get(index));
                    Rect pageRect = new Rect(pageBounds.pageLeft, 0, pageBounds.pageRight, MAX_SCREEN_HEIGHT);
                    if (pageRect.contains(rect)) {
                        break;
                    } else if (rect.left < pageRect.right && rect.left >= pageRect.left)
                        break;
                    ++pageIndex;
                }
            }
        }
        
        return pageIndex;
    }
	
	public ArrayList<TouchableItem> getTouchableItems(int i) {
	    ArrayList<TouchableItem> touchableItems = new ArrayList<TouchableItem>();
        
        PageBounds bounds = mPageBounds.get(i);
        
        Rect pageRect = new Rect(bounds.pageLeft, 0, bounds.pageRight, MAX_SCREEN_HEIGHT);
        if (mTouchableItems != null) {
            for (int index = 0; index < mTouchableItems.size(); ++index) {
                Rect tochableItemRect = mTouchableItems.get(index).bounds;
                if (pageRect.contains(tochableItemRect)) {
                    touchableItems.add(mTouchableItems.get(index));
                } else {
                    int left = 0;
                    int right = 0;
                    if (tochableItemRect.left < pageRect.left && tochableItemRect.right > pageRect.left) {
                        left = pageRect.left;
                        right = tochableItemRect.right > pageRect.right ? pageRect.right : tochableItemRect.right;
                        TouchableItem touchableItem = new TouchableItem(mTouchableItems.get(index).id, mTouchableItems.get(index).type, new Rect(left, tochableItemRect.top, right, tochableItemRect.bottom), mTouchableItems.get(index).hasControls, mTouchableItems.get(index).source);
                        touchableItems.add(touchableItem);
                    } else if (tochableItemRect.left > pageRect.left && tochableItemRect.left < pageRect.right) {
                        left = tochableItemRect.left;
                        right = pageRect.right;
                        TouchableItem touchableItem = new TouchableItem(mTouchableItems.get(index).id, mTouchableItems.get(index).type, new Rect(left, tochableItemRect.top, right, tochableItemRect.bottom), mTouchableItems.get(index).hasControls, mTouchableItems.get(index).source);
                        touchableItems.add(touchableItem);
                    }
                }
            }
        }
        
        return touchableItems;
	};
	
	public ArrayList<TableInfo> getTableInfo(int i) {
	    ArrayList<TableInfo> tableInfos = new ArrayList<TableInfo>();
	    
	    PageBounds bounds = mPageBounds.get(i);
        Rect pageRect = new Rect(bounds.pageLeft, 0, bounds.pageRight, MAX_SCREEN_HEIGHT);
        if (mTableInfos != null) {
            for (int index = 0; index < mTableInfos.size(); ++index) {
                if (pageRect.contains(mTableInfos.get(index).bounds) || Rect.intersects(pageRect, mTableInfos.get(index).bounds)) {
                    Rect tableRect = mTableInfos.get(index).bounds;
                    
                    if (pageRect.contains(tableRect)) {
                        tableInfos.add(mTableInfos.get(index));
                    } else {
                        int left = 0;
                        int right = 0;
                        if (tableRect.left < pageRect.left && tableRect.right > pageRect.left) {
                            left = pageRect.left;
                            right = tableRect.right > pageRect.right ? pageRect.right : tableRect.right;
                            TableInfo touchableItem = new TableInfo(left, tableRect.top, right, tableRect.bottom, mTableInfos.get(index).html);
                            tableInfos.add(touchableItem);
                        } else if (tableRect.left > pageRect.left && tableRect.left < pageRect.right) {
                            left = tableRect.left;
                            right = pageRect.right;
                            TableInfo touchableItem = new TableInfo(left, tableRect.top, right, tableRect.bottom, mTableInfos.get(index).html);
                            tableInfos.add(touchableItem);
                        }
                    }
                }
            }
        }
	    return tableInfos;
	}

	public int getPagesCount() {
		return mPageBounds.size();
	}
	
	public long getTotalTextCount() {
	    return mPageBounds.get(mPageBounds.size() - 1).offset_to;
	}
	
	public void merge(final PaginationResultColumn paginationResult) {
        if (paginationResult.mPageBounds != null) {
            for (int index = 0; index < paginationResult.mPageLists.size(); ++index) {
                if (mPageBounds.get(paginationResult.mPageLists.get(index)) == null) {
                    mPageLists.add(paginationResult.mPageLists.get(index));
                    mPageBounds.put(paginationResult.mPageLists.get(index), paginationResult.mPageBounds.get(paginationResult.mPageLists.get(index)));
                }
            }
        }
	    
	    if (paginationResult.mTableInfos != null) {
	        mTableInfos = paginationResult.mTableInfos;
	    }
	    
	    if (paginationResult.mTouchableItems != null) {
	        mTouchableItems = paginationResult.mTouchableItems;
	    }
	}

	private SparseArray<PageBounds> mPageBounds = new SparseArray<PageBounds>();
	private ArrayList<Integer> mPageLists;
	private ArrayList<TouchableItem> mTouchableItems;
	private ArrayList<TableInfo> mTableInfos;
}
