package com.studio.statusvault.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.github.chrisbanes.photoview.PhotoView;
import com.studio.statusvault.R;
import com.studio.statusvault.data.repository.StatusMediaItem;

import java.io.File;
import java.util.ArrayList;

public class AdapterImagesViewer extends PagerAdapter {
    Context mContext;
    private ArrayList<StatusMediaItem> mImagesArray;

    LayoutInflater layoutInflater;

    public AdapterImagesViewer(Context context, ArrayList<StatusMediaItem> mDataset) {
        mContext = context;
        // Defensive copy: do not hold the same reference as {@link com.studio.statusvault.data.FilesData}
        // static lists; they are cleared/rebuilt on refresh and would change count without
        // {@link #notifyDataSetChanged()}, crashing {@link androidx.viewpager.widget.ViewPager}.
        mImagesArray = mDataset == null ? new ArrayList<>() : new ArrayList<>(mDataset);
        layoutInflater = LayoutInflater.from(mContext);
    }

    /**
     * Saved/offline images on disk. Uses a factory to avoid generic constructor clash with
     * {@link #AdapterImagesViewer(Context, ArrayList)} after type erasure.
     */
    public static AdapterImagesViewer fromSavedFiles(Context context, ArrayList<File> savedImageFiles) {
        ArrayList<StatusMediaItem> items = new ArrayList<>();
        if (savedImageFiles != null) {
            for (File file : savedImageFiles) {
                items.add(new StatusMediaItem(file, false));
            }
        }
        return new AdapterImagesViewer(context, items);
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
        StatusMediaItem item = mImagesArray.get(position);
        if (item.isFromSaf()) {
            imageViewer.setImageURI(item.getUri());
        } else if (item.getFile() != null) {
            imageViewer.setImageURI(Uri.fromFile(item.getFile()));
        }
        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ConstraintLayout) object);
    }
}
