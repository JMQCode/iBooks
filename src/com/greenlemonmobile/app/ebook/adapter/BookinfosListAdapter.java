package com.greenlemonmobile.app.ebook.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.greenlemonmobile.app.constant.DefaultConstant;
import com.greenlemonmobile.app.ebook.LibraryActivity.ViewType;
import com.greenlemonmobile.app.ebook.R;
import com.greenlemonmobile.app.ebook.adapter.BookinfosGridAdapter.BaseViewHolder;
import com.greenlemonmobile.app.ebook.entity.LocalBook;
import com.greenlemonmobile.app.utils.FileUtil;
import com.greenlemonmobile.app.utils.ImageBuffer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BookinfosListAdapter extends BaseAdapter {
    
    private final Context mContext;
    private final ArrayList<LocalBook> mLocalBooks;
    private final LayoutInflater mInflater;
    private final ViewType mItemType;
    private final OnCheckedChangeListener mCheckedChangedListener;
    
    private SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public BookinfosListAdapter(Context context, ViewType type, ArrayList<LocalBook> books, OnCheckedChangeListener listener) {
        mContext = context;
        mLocalBooks = books;
        mInflater = LayoutInflater.from(context);
        mItemType = type;
        mCheckedChangedListener = listener;
    }

    @Override
    public int getCount() {
        return mLocalBooks.size();
    }

    @Override
    public Object getItem(int position) {
        return mLocalBooks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            switch (mItemType) {
                case DETAILS:
                    convertView = mInflater.inflate(R.layout.library_bookinfos_details, null);
                    break;
                case LIST:
                    convertView = mInflater.inflate(R.layout.library_bookinfos_list, null);
                    break;
            }
            holder = new ViewHolder();
            convertView.setTag(holder);
            
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.format = (ImageView) convertView.findViewById(R.id.format);
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.author = (TextView) convertView.findViewById(R.id.author);
            holder.size = (TextView) convertView.findViewById(R.id.size);
            holder.book_checkbox = (CheckBox) convertView.findViewById(R.id.book_checkbox);
            holder.creation_date = (TextView) convertView.findViewById(R.id.creation_date);
            holder.rate_info = (TextView) convertView.findViewById(R.id.rate_info);
            holder.rate_info_percent = (TextView) convertView.findViewById(R.id.rate_info_percent);
            holder.progressbar = (ProgressBar) convertView.findViewById(R.id.progressbar);
            holder.position = position;
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        final LocalBook book = mLocalBooks.get(position);
        holder.title.setText(book.title);
        holder.author.setText(book.author);
        holder.book_checkbox.setTag(book);
        holder.book_checkbox.setChecked(book.selected);
        holder.format.setImageResource(BaseViewHolder.getFormatDrawable(book.file));
        holder.icon.setImageBitmap(null);
        holder.icon.setImageResource(R.drawable.no_cover);
        Bitmap bitmap = ImageBuffer.getBitmap(mContext, (mItemType == ViewType.DETAILS) ? book.detail_image : book.list_image);
        if (bitmap != null)
        	holder.icon.setImageBitmap(bitmap);
//        // Using an AsyncTask to load the slow images in a background thread
//        new AsyncTask<ViewHolder, Void, Bitmap>() {
//            private ViewHolder v;
//
//            @Override
//            protected Bitmap doInBackground(ViewHolder... params) {
//                v = params[0];
//                return ImageBuffer.getBitmap(mContext, (mItemType == ViewType.DETAILS) ? book.detail_image : book.list_image);
//            }
//
//            @Override
//            protected void onPostExecute(Bitmap result) {
//                super.onPostExecute(result);
//                v.icon.setVisibility(View.VISIBLE);
//                if (result != null)
//                	v.icon.setImageBitmap(result);
//            }
//        }.execute(holder);
        
        holder.book_checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				
				LocalBook book = (LocalBook) buttonView.getTag();
				book.selected = isChecked;
				mCheckedChangedListener.onCheckedChanged(buttonView, isChecked);
			}
        	
        });
        
        if (holder.creation_date != null)
            holder.creation_date.setText(date.format(new Date(book.addition_date)));
        
        if (holder.size != null)
            holder.size.setText(FileUtil.convertStorage(book.size));
        
        if (holder.rate_info != null && holder.rate_info_percent != null && holder.progressbar != null && book.total_offset > 0 && book.total_page > 0) {
            holder.rate_info.setVisibility(View.VISIBLE);
            holder.rate_info_percent.setVisibility(View.VISIBLE);
            holder.progressbar.setVisibility(View.VISIBLE);
            
            holder.rate_info.setText(Long.toString(book.current_page) + "/" + Long.toString(book.total_page));
            
            int percent = (int) (100 * book.current_offset / book.total_offset);
            holder.progressbar.setProgress(percent);
            holder.rate_info_percent.setText(Integer.toString(percent) + "%");
        } else if (holder.rate_info != null && holder.rate_info_percent != null && holder.progressbar != null){
            holder.rate_info.setVisibility(View.INVISIBLE);
            holder.rate_info_percent.setVisibility(View.INVISIBLE);
            holder.progressbar.setVisibility(View.INVISIBLE);
        }
        
        if (holder.progressbar != null) {
            holder.progressbar.setMax(100);
        }
        return convertView;
    }

    class ViewHolder extends BaseViewHolder {
        TextView author;
        TextView size;
        CheckBox book_checkbox;        
        TextView creation_date;
        TextView rate_info;
        TextView rate_info_percent;
        ProgressBar progressbar;
    }
}
