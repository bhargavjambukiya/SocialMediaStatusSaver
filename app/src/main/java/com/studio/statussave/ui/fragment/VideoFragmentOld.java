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

public class VideoFragmentOld extends Fragment {
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

        imageViewNoRecord.setImageResource(R.drawable.ic_no_videos);
        textViewNoRecord.setText(getResources().getString(R.string.no_videos_found));

        if (FilesData.getRecentOrSaved().equals("recent")) {
            RecyclerInstances.recentVideoRecyclerview = v.findViewById(R.id.videoImageRecyclerView);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            RecyclerInstances.recentVideoRecyclerview.setHasFixedSize(true);

            // use a Grid layout manager
            GridLayoutManager mLayoutManager = new GridLayoutManager(getContext(), 2);
            RecyclerInstances.recentVideoRecyclerview.setLayoutManager(mLayoutManager);

            if (FilesData.getWhatsAppFilesVideos().isEmpty()) {
                FilesData.scrapWhatsAppFiles();
            }
            if (FilesData.getWhatsAppFilesVideos().size() > 0) {
                RecyclerInstances.recentVideoAdapter = new RecyclerAdapter(FilesData.getWhatsAppFilesVideos(), getContext(), 'v');
                RecyclerInstances.recentVideoRecyclerview.setAdapter(RecyclerInstances.recentVideoAdapter);
            } else {
                layoutNoRecordFound.setVisibility(View.VISIBLE);
            }
        } else if (FilesData.getRecentOrSaved().equals("videoSplitter")) {
            RecyclerInstances.savedVideoRecyclerview = v.findViewById(R.id.videoImageRecyclerView);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            RecyclerInstances.savedVideoRecyclerview.setHasFixedSize(true);

            // use a Grid layout manager
            GridLayoutManager mLayoutManager = new GridLayoutManager(getContext(), 2);
            RecyclerInstances.savedVideoRecyclerview.setLayoutManager(mLayoutManager);

            if (FilesData.getSavedFilesVideos().isEmpty()) {
                FilesData.scrapSavedFiles();
            }

            if (FilesData.getSplittedFilesVideos().size() > 0) {
                RecyclerInstances.savedVideoAdapter = new RecyclerAdapter(FilesData.getSplittedFilesVideos(), getContext(), 'v');
                RecyclerInstances.savedVideoRecyclerview.setAdapter(RecyclerInstances.savedVideoAdapter);
            } else {
                layoutNoRecordFound.setVisibility(View.VISIBLE);
            }
        } else {
            RecyclerInstances.savedVideoRecyclerview = v.findViewById(R.id.videoImageRecyclerView);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            RecyclerInstances.savedVideoRecyclerview.setHasFixedSize(true);

            // use a Grid layout manager
            GridLayoutManager mLayoutManager = new GridLayoutManager(getContext(), 2);
            RecyclerInstances.savedVideoRecyclerview.setLayoutManager(mLayoutManager);

            if (FilesData.getSavedFilesVideos().isEmpty()) {
                FilesData.scrapSavedFiles();
            }

            if (FilesData.getSavedFilesVideos().size() > 0) {
                RecyclerInstances.savedVideoAdapter = new RecyclerAdapter(FilesData.getSavedFilesVideos(), getContext(), 'v');
                RecyclerInstances.savedVideoRecyclerview.setAdapter(RecyclerInstances.savedVideoAdapter);
            } else {
                layoutNoRecordFound.setVisibility(View.VISIBLE);
            }
        }
        return v;
    }
}
