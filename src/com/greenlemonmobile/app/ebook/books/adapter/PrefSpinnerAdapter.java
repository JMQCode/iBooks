
package com.greenlemonmobile.app.ebook.books.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.greenlemonmobile.app.ebook.R;

import java.util.List;

public class PrefSpinnerAdapter extends ArrayAdapter<String> {
    private int mDropDownImageViewResourceId = 0;

    public PrefSpinnerAdapter(Context context, int resource,
            int textViewResourceId, int drawableResourceId, List<String> objects) {
        super(context, resource, textViewResourceId, objects);
        mDropDownImageViewResourceId = drawableResourceId;
    }

    public PrefSpinnerAdapter(Context context, int resource,
            int textViewResourceId, int drawableResourceId, String[] objects) {
        super(context, resource, textViewResourceId, objects);
        mDropDownImageViewResourceId = drawableResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        ImageView image = (ImageView) view.findViewById(android.R.id.icon);
        //image.setImageResource(R.drawable.ic_settings_default);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        if (mDropDownImageViewResourceId > 0) {
            ImageView image = (ImageView) view.findViewById(android.R.id.icon);
            image.setImageResource(mDropDownImageViewResourceId);
        }
        return view;
    }
}
