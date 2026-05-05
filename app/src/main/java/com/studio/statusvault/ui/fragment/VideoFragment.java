package com.studio.statusvault.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.studio.statusvault.R;
import com.studio.statusvault.adapters.RecentStatusMediaListAdapter;
import com.studio.statusvault.adapters.SavedFileListAdapter;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.data.RecentStatusRangePreference;
import com.studio.statusvault.data.repository.StatusMediaItem;
import com.studio.statusvault.ui.activity.StoriesActivity;
import com.studio.statusvault.ui.viewmodel.StoriesViewModel;
import com.studio.statusvault.utils.WhatsAppAccessHelper;

import java.io.File;
import java.util.ArrayList;

public class VideoFragment extends Fragment {
    LinearLayout layoutNoRecordFound;
    ImageView imageViewNoRecord;
    TextView textViewNoRecord;
    private RecyclerView recyclerView;
    private RecentStatusMediaListAdapter recentAdapter;
    private SavedFileListAdapter savedAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.st_video_image_fragment, container, false);

        layoutNoRecordFound = v.findViewById(R.id.layoutNoRecordFound);
        imageViewNoRecord = v.findViewById(R.id.imageViewNoRecord);
        textViewNoRecord = v.findViewById(R.id.textViewNoRecord);
        recyclerView = v.findViewById(R.id.videoImageRecyclerView);

        imageViewNoRecord.setImageResource(R.drawable.ic_no_videos);
        textViewNoRecord.setText(getResources().getString(R.string.no_videos_found));
        if ("recent".equals(FilesData.getRecentOrSaved())
                && !WhatsAppAccessHelper.canAccessWhatsAppTree(requireContext())) {
            layoutNoRecordFound.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(null);
            textViewNoRecord.setText(R.string.status_permission_path_hint);
            return v;
        }
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        StoriesViewModel viewModel = new ViewModelProvider(requireActivity()).get(StoriesViewModel.class);

        String mode = FilesData.getRecentOrSaved();
        if ("recent".equals(mode)) {
            if (recentAdapter == null) {
                recentAdapter = new RecentStatusMediaListAdapter(requireContext(), 'v', () -> {
                    if (getActivity() instanceof StoriesActivity) {
                        ((StoriesActivity) getActivity()).requestWhatsAppFolderAccess();
                    }
                });
                recyclerView.setAdapter(recentAdapter);
            }
            viewModel.getRecentVideosStatusFolderOnly()
                    .observe(getViewLifecycleOwner(), this::bindRecentVideoList);
        } else {
            if (savedAdapter == null) {
                savedAdapter = new SavedFileListAdapter(requireContext(), 'v');
                recyclerView.setAdapter(savedAdapter);
            }
            viewModel.getSavedVideos().observe(getViewLifecycleOwner(), this::bindSavedVideoList);
        }

        return v;
    }

    private void bindRecentVideoList(ArrayList<StatusMediaItem> files) {
        if (files == null || files.isEmpty()) {
            layoutNoRecordFound.setVisibility(View.VISIBLE);
            if (recentAdapter != null) {
                recentAdapter.submitList(new ArrayList<>());
            }
            int emptyRes = RecentStatusRangePreference.isLast7DaysOnly(requireContext())
                    ? R.string.status_empty_recent_videos_last_7_days
                    : R.string.status_no_recent_videos;
            textViewNoRecord.setText(emptyRes);
            return;
        }

        recentAdapter.submitList(new ArrayList<>(files));
        layoutNoRecordFound.setVisibility(View.GONE);
    }

    private void bindSavedVideoList(ArrayList<File> files) {
        if (files == null || files.isEmpty()) {
            layoutNoRecordFound.setVisibility(View.VISIBLE);
            if (savedAdapter != null) {
                savedAdapter.submitList(new ArrayList<>());
            }
            if ("videoSplitter".equals(FilesData.getRecentOrSaved())) {
                textViewNoRecord.setText(R.string.empty_converted_videos_hint);
            } else {
                textViewNoRecord.setText(R.string.no_videos_found);
            }
            return;
        }

        savedAdapter.submitList(new ArrayList<>(files));
        layoutNoRecordFound.setVisibility(View.GONE);
    }
}
