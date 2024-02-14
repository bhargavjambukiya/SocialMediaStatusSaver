package com.studio.statussave.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.studio.statussave.R;
import com.studio.statussave.adapters.RecyclerAdapter;
import com.studio.statussave.adapters.RecyclerInstances;
import com.studio.statussave.data.FilesData;

public class ImageFragmentOld extends Fragment {
    LinearLayout layoutNoRecordFound;
    ImageView imageViewNoRecord;
    TextView textViewNoRecord;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.st_video_image_fragment, container, false);

        layoutNoRecordFound = v.findViewById(R.id.layoutNoRecordFound);
        imageViewNoRecord = v.findViewById(R.id.imageViewNoRecord);
        textViewNoRecord = v.findViewById(R.id.textViewNoRecord);


        imageViewNoRecord.setImageResource(R.drawable.ic_no_image);
        textViewNoRecord.setText(getResources().getString(R.string.no_images_found));


        if (FilesData.getRecentOrSaved().equals("recent")) {
            RecyclerInstances.recentImageRecyclerview = v.findViewById(R.id.videoImageRecyclerView);
            RecyclerInstances.recentImageRecyclerview.setHasFixedSize(true);

            GridLayoutManager mLayoutManager = new GridLayoutManager(getContext(), 2);
            RecyclerInstances.recentImageRecyclerview.setLayoutManager(mLayoutManager);

            if (FilesData.getWhatsAppFilesImages().isEmpty()) {
                FilesData.scrapWhatsAppFiles();

            }
            if (FilesData.getWhatsAppFilesImages().size() > 0) {
                RecyclerInstances.recentImageAdapter = new RecyclerAdapter(FilesData.getWhatsAppFilesImages(), getContext(), 'i');
                RecyclerInstances.recentImageRecyclerview.setAdapter(RecyclerInstances.recentImageAdapter);
            } else {
                layoutNoRecordFound.setVisibility(View.VISIBLE);
            }
        } else {
            RecyclerInstances.savedImageRecyclerview = v.findViewById(R.id.videoImageRecyclerView);
            RecyclerInstances.savedImageRecyclerview.setHasFixedSize(true);

            GridLayoutManager mLayoutManager = new GridLayoutManager(getContext(), 2);
            RecyclerInstances.savedImageRecyclerview.setLayoutManager(mLayoutManager);


            if (FilesData.getSavedFilesImages().isEmpty()) {
                FilesData.scrapSavedFiles();
            }
            if (FilesData.getSavedFilesImages().size() > 0) {
                RecyclerInstances.savedImageAdapter = new RecyclerAdapter(FilesData.getSavedFilesImages(), getContext(), 'i');
                RecyclerInstances.savedImageRecyclerview.setAdapter(RecyclerInstances.savedImageAdapter);
            } else {
                layoutNoRecordFound.setVisibility(View.VISIBLE);
            }
        }
        return v;
    }
}







