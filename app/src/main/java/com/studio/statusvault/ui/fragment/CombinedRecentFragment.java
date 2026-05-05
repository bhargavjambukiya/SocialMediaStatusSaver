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
import com.studio.statusvault.adapters.MixedRecentStatusMediaListAdapter;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.data.repository.StatusMediaItem;
import com.studio.statusvault.ui.activity.StoriesActivity;
import com.studio.statusvault.ui.viewmodel.StoriesViewModel;
import com.studio.statusvault.utils.WhatsAppAccessHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Recent images and videos together (last 7 days filter applied in {@link com.studio.statusvault.data.FilesData}),
 * sorted newest first.
 */
public class CombinedRecentFragment extends Fragment {

    private LinearLayout layoutNoRecordFound;
    private ImageView imageViewNoRecord;
    private TextView textViewNoRecord;
    private RecyclerView recyclerView;
    private MixedRecentStatusMediaListAdapter adapter;

    private ArrayList<StatusMediaItem> lastImages = new ArrayList<>();
    private ArrayList<StatusMediaItem> lastVideos = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.st_video_image_fragment, container, false);

        layoutNoRecordFound = v.findViewById(R.id.layoutNoRecordFound);
        imageViewNoRecord = v.findViewById(R.id.imageViewNoRecord);
        textViewNoRecord = v.findViewById(R.id.textViewNoRecord);
        recyclerView = v.findViewById(R.id.videoImageRecyclerView);

        imageViewNoRecord.setImageResource(R.drawable.ic_image);
        textViewNoRecord.setText(R.string.empty_combined_recent_hint);

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

        adapter = new MixedRecentStatusMediaListAdapter(requireContext(), () -> {
            if (getActivity() instanceof StoriesActivity) {
                ((StoriesActivity) getActivity()).requestWhatsAppFolderAccess();
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel.getRecentImages().observe(getViewLifecycleOwner(), this::onImages);
        viewModel.getRecentVideos().observe(getViewLifecycleOwner(), this::onVideos);

        return v;
    }

    private void onImages(ArrayList<StatusMediaItem> files) {
        lastImages = files != null ? new ArrayList<>(files) : new ArrayList<>();
        mergeAndBind();
    }

    private void onVideos(ArrayList<StatusMediaItem> files) {
        lastVideos = files != null ? new ArrayList<>(files) : new ArrayList<>();
        mergeAndBind();
    }

    private void mergeAndBind() {
        if (adapter == null) {
            return;
        }
        ArrayList<StatusMediaItem> merged = new ArrayList<>();
        if (lastImages != null) {
            merged.addAll(lastImages);
        }
        if (lastVideos != null) {
            merged.addAll(lastVideos);
        }
        Collections.sort(merged, Comparator.comparingLong(StatusMediaItem::getLastModified).reversed());

        adapter.setSourceLists(lastImages != null ? lastImages : new ArrayList<>(),
                lastVideos != null ? lastVideos : new ArrayList<>());
        adapter.submitList(new ArrayList<>(merged));

        if (merged.isEmpty()) {
            layoutNoRecordFound.setVisibility(View.VISIBLE);
            textViewNoRecord.setText(R.string.empty_combined_recent_hint);
        } else {
            layoutNoRecordFound.setVisibility(View.GONE);
        }
    }
}
