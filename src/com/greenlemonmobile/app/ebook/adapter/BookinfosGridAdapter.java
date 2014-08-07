package com.greenlemonmobile.app.ebook.adapter;

import java.io.File;
import java.util.ArrayList;

import org.ebookdroid.CodecType;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenlemonmobile.app.constant.DefaultConstant;
import com.greenlemonmobile.app.ebook.LibraryActivity.ViewType;
import com.greenlemonmobile.app.ebook.R;
import com.greenlemonmobile.app.ebook.books.reader.EpubContext;
import com.greenlemonmobile.app.ebook.books.reader.EpubReaderActivity;
import com.greenlemonmobile.app.ebook.entity.LocalBook;
import com.greenlemonmobile.app.utils.ImageBuffer;

public class BookinfosGridAdapter extends BaseAdapter {
    
    private final Context mContext;
    private final ArrayList<LocalBook> mLocalBooks;
    private final LayoutInflater mInflater;
    private final ViewType mItemType;
    
    public BookinfosGridAdapter(Context context, ViewType type, ArrayList<LocalBook> books) {
        mContext = context;
        mLocalBooks = books;
        mInflater = LayoutInflater.from(context);
        mItemType = type;
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
        BaseViewHolder holder = null;
        if (convertView == null) {
            switch (mItemType) {
                case BIG_THUMB:
                    convertView = mInflater.inflate(R.layout.library_bookinfos_big, null);
                    break;
                case MEDIUM_THUMB:
                    convertView = mInflater.inflate(R.layout.library_bookinfos_medium, null);
                    break;
                case SMALL_THUMB:
                    convertView = mInflater.inflate(R.layout.library_bookinfos_small, null);
                    break;
                case BOOK_SHELF:
                	convertView = mInflater.inflate(R.layout.library_bookinfos_shelf, null);
                	break;
            }
            holder = new BaseViewHolder();
            convertView.setTag(holder);
            
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.format = (ImageView) convertView.findViewById(R.id.format);
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.position = position;
        } else {
            holder = (BaseViewHolder) convertView.getTag();
        }
        
        final LocalBook book = mLocalBooks.get(position);
        holder.title.setText(book.title);
        holder.icon.setImageBitmap(null);
        holder.icon.setImageResource(R.drawable.no_cover);
        holder.format.setImageResource(BaseViewHolder.getFormatDrawable(book.file));
        Bitmap bitmap = ImageBuffer.getBitmap(mContext, (mItemType == ViewType.DETAILS) ? book.detail_image : book.list_image);
        if (bitmap != null)
        	holder.icon.setImageBitmap(bitmap);
//        // Using an AsyncTask to load the slow images in a background thread
//        new AsyncTask<BaseViewHolder, Void, Bitmap>() {
//            private BaseViewHolder v;
//
//            @Override
//            protected Bitmap doInBackground(BaseViewHolder... params) {
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
        
        return convertView;
    }

    public static class BaseViewHolder {
        public ImageView icon;
        public ImageView format;
        public TextView title;
        public int position;
        
        public static int getFormatDrawable(String fileName) {
        	final File file = new File(fileName);
            final Uri data = Uri.fromFile(file);
            CodecType codecType = CodecType.getByUri(data.toString());
            
            if (codecType != null) {
                switch (codecType) {
                case EPUB:
                	return R.drawable.format_epub;
                case TXT:
                	return R.drawable.format_txt;
                case PDF:
                	return R.drawable.format_pdf;
                case DJVU:
                	return R.drawable.format_djvu;
                case XPS:
                	return R.drawable.format_xps;
                case CBZ:
                	break;
                case CBR:
                	break;
                case FB2:
                	break;
                }
            }
        	
            return R.drawable.format_default;
        }
    }
}
