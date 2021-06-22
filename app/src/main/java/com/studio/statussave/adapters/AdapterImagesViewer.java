package com.studio.statussave.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.github.chrisbanes.photoview.PhotoView;
import com.studio.statussave.R;

import java.io.File;
import java.util.ArrayList;

public class AdapterImagesViewer extends PagerAdapter {
    Context mContext;
    private ArrayList<File> mImagesArray;

    LayoutInflater layoutInflater;

    public AdapterImagesViewer(Context context, ArrayList<File> mDataset) {
        mContext = context;
        mImagesArray = mDataset;
        layoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mImagesArray.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((ConstraintLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View itemView = layoutInflater.inflate(R.layout.item_view_images, container, false);
        PhotoView imageViewer = itemView.findViewById(R.id.imageViewer);
        imageViewer.setImageURI(Uri.parse(mImagesArray.get(position).getAbsolutePath()));
        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ConstraintLayout) object);
    }
}
