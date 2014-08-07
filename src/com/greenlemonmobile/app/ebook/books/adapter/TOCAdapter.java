package com.greenlemonmobile.app.ebook.books.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.greenlemonmobile.app.ebook.R;
import com.greenlemonmobile.app.ebook.books.model.Chapter;
import com.greenlemonmobile.app.ebook.books.model.ReaderSettings;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser.NavPoint;

import java.util.ArrayList;

public class TOCAdapter extends BaseAdapter {
	
    private int spaceWidth;

    private final Drawable background;
    private final Drawable selected;

    private final VoidListener voidListener = new VoidListener();
    private final ItemListener itemListener = new ItemListener();
    private final CollapseListener collapseListener = new CollapseListener();

    private final Context context;
    private final SparseArray<Chapter> objects;
    private final ArrayList<NavPoint> navMap;
    private final TOCItemState[] states;
    private final SparseIntArray mapping = new SparseIntArray();
    private final int currentId;
    private final ReaderSettings settings;

    public TOCAdapter(final Context context, ReaderSettings settings, final SparseArray<Chapter> objects, ArrayList<NavPoint> navMap, final int curChapterIndex) {
        this.context = context;
        this.settings = settings;
        final Resources resources = context.getResources();
        background = resources.getDrawable(R.drawable.item_background_holo_dark);
        selected = resources.getDrawable(R.drawable.list_selector_holo_dark);

        this.navMap = navMap;
        this.objects = objects;
        this.states = new TOCItemState[this.navMap.size()];

        boolean treeFound = false;
        for (int i = 0; i < this.navMap.size(); i++) {
            mapping.put(i, i);
            final int next = i + 1;
            if (next < this.navMap.size() && this.navMap.get(i).navLevel < this.navMap.get(next).navLevel) {
                states[i] = TOCItemState.COLLAPSED;
                treeFound = true;
            } else {
                states[i] = TOCItemState.LEAF;
            }
        }

        currentId = curChapterIndex;

        if (treeFound) {
            for (int parent = getParentId(currentId); parent != -1; parent = getParentId(parent)) {
                states[parent] = TOCItemState.EXPANDED;
            }
            rebuild();
            if (getCount() == 1 && states[0] == TOCItemState.COLLAPSED) {
                states[0] = TOCItemState.EXPANDED;
                rebuild();
            }
        }
    }

    public int getParentId(final int id) {
        final int level = (int) objects.get(id).navLevel;
        Chapter chapter = objects.get(id);
        int index = 0;
        for (; index < navMap.size(); ++index) {
            if (chapter.id != null && chapter.id.equals(navMap.get(index))) {
                break;
            }
        }
        for (int i = index - 1; i >= 0; i--) {
            if (navMap.get(i).navLevel < level) {
                return i;
            }
        }
        return -1;
    }

    protected void rebuild() {
        mapping.clear();
        int pos = 0;
        int level = Integer.MAX_VALUE;
        for (int cid = 0; cid < navMap.size(); cid++) {
            if (navMap.get(cid).navLevel <= level) {
                mapping.put(pos++, cid);
                if (states[cid] == TOCItemState.COLLAPSED) {
                    level = (int) navMap.get(cid).navLevel;
                } else {
                    level = Integer.MAX_VALUE;
                }
            }
        }
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public int getCount() {
        return mapping.size();
    }

    @Override
    public NavPoint getItem(final int position)
    {
    	final int id = mapping.get(position, -1);
    	if (id >=0 && id < navMap.size())
    	{
    		return navMap.get(id);
    	}
    	return null;
    }

    @Override
    public long getItemId(final int position) {
        return mapping.get(position, -1);
    }

//    public int getItemPosition(final Chapter item) {
//        for (int i = 0, n = getCount(); i < n; i++) {
//            if (item == getItem(i)) {
//                return i;
//            }
//        }
//        return -1;
//    }
    public int getItemPosition(final NavPoint item)
    {
    	for (int i=0, n=getCount(); i<n; i++)
    	{
    		if(item == getItem(i))
    		{
    			return i;
    		}
    	}
    	return -1;
    }

    public int getItemId(final Chapter item) {
        for (int i = 0, n = navMap.size(); i < n; i++) {
            if (item.id != null && item.id.equals(navMap.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final int id = (int) getItemId(position);
        View container = null;
        ViewHolder holder = null;
        boolean firstTime = false;
        if (convertView == null) {
            container = LayoutInflater.from(context).inflate(R.layout.toc_list_item_view, parent, false);
            holder = new ViewHolder();
            container.setTag(holder);
            holder.title = (TextView) container.findViewById(R.id.treeview_item_title);
            holder.pageNum = (TextView) container.findViewById(R.id.treeview_item_sub_title);
            holder.collapse = container.findViewById(R.id.treeview_collapse);
            holder.space = container.findViewById(R.id.treeview_space);
            firstTime = true;
        } else {
            container = convertView;
            holder = (ViewHolder) container.getTag();
        }

        final NavPoint item = getItem(position);
        if (!TextUtils.isEmpty(item.navLabel))
        	holder.title.setText(item.navLabel.trim());
        else
        	holder.title.setText(item.src.trim());
        holder.title.setTag(position);
        holder.collapse.setTag(position);
        if (((Chapter)objects.get(item.chapterIndex)).chapterState == Chapter.ChapterState.READY)
        	holder.pageNum.setText(Integer.toString(item.pageIndex));
        else
        	holder.pageNum.setText("");

//        container.setBackgroundDrawable(id == currentId ? this.selected : this.background);
//        if (id == currentId)
//        	container.setBackgroundDrawable(this.selected);

        if (firstTime) {
            final LinearLayout.LayoutParams btnParams = (LayoutParams) holder.collapse.getLayoutParams();
            spaceWidth = btnParams.width;
            //container.setOnClickListener(voidListener);
            //holder.title.setOnClickListener(itemListener);
            container.setClickable(false);
            holder.title.setClickable(false);
        }
        
        if (this.settings.isThemeNight) {
            holder.title.setTextColor(Color.parseColor("#AAAAAA"));
            holder.pageNum.setTextColor(Color.parseColor("#AAAAAA"));
        } else {
            holder.title.setTextColor(Color.BLACK);
            holder.pageNum.setTextColor(Color.BLACK);
        }

        final LinearLayout.LayoutParams sl = (LayoutParams) holder.space.getLayoutParams();
        sl.leftMargin = (int) (Math.min(3, item.navLevel) * spaceWidth);
        holder.space.setLayoutParams(sl);

        if (states[id] == TOCItemState.LEAF) {
            holder.space.setVisibility(View.VISIBLE);
            holder.collapse.setOnClickListener(voidListener);
            holder.collapse.setBackgroundColor(Color.TRANSPARENT);
        } else {
            holder.space.setVisibility(View.INVISIBLE);
            holder.collapse.setOnClickListener(collapseListener);
            holder.collapse.setBackgroundResource(states[id] == TOCItemState.EXPANDED ? R.drawable.treeview_unexpand_popup
                    : R.drawable.treeview_expand_popup);
        }

        return container;
    }
    
    private static class ViewHolder {
        TextView title;
        TextView pageNum;
        View collapse;
        View space;
    }

    private static enum TOCItemState {
        LEAF, EXPANDED, COLLAPSED;
    }

    private final class CollapseListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            //System.out.println("btn.OnClickListener()");
            {
                final int position = ((Integer) v.getTag()).intValue();
                final int id = (int) getItemId(position);
                final TOCItemState newState = states[id] == TOCItemState.EXPANDED ? TOCItemState.COLLAPSED
                        : TOCItemState.EXPANDED;
                states[id] = newState;
            }
            rebuild();

            v.post(new Runnable() {

                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    private static final class ItemListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            for (ViewParent p = v.getParent(); p != null; p = p.getParent()) {
                if (p instanceof ListView) {
                    final ListView list = (ListView) p;
                    final OnItemClickListener l = list.getOnItemClickListener();
                    if (l != null) {
                        l.onItemClick(list, v, ((Integer) v.getTag()).intValue(), 0);
                    }
                    return;
                }
            }

        }
    }

    private static final class VoidListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
        }
    }
}
