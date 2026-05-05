package com.studio.statusvault.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.bumptech.glide.Glide;
import com.studio.statusvault.R;
import com.studio.statusvault.ui.activity.ImageViewerActivity;
import com.studio.statusvault.ui.activity.VideoViewerActivity;

import java.io.File;

/**
 * Saved images or videos on disk (app download folder).
 */
public class SavedFileListAdapter extends ListAdapter<File, SavedFileListAdapter.VH> {

    private static final DiffUtil.ItemCallback<File> DIFF = new DiffUtil.ItemCallback<File>() {
        @Override
        public boolean areItemsTheSame(@NonNull File oldItem, @NonNull File newItem) {
            return oldItem.getAbsolutePath().equals(newItem.getAbsolutePath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull File oldItem, @NonNull File newItem) {
            return oldItem.lastModified() == newItem.lastModified() && oldItem.length() == newItem.length();
        }
    };

    private final Context context;
    private final char contentType;

    public SavedFileListAdapter(Context context, char contentType) {
        super(DIFF);
        this.context = context;
        this.contentType = contentType;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView view;
        if (contentType == 'i') {
            view = (CardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.st_recyclerview_image_item, parent, false);
        } else {
            view = (CardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.st_recyclerview_video_item, parent, false);
        }
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        File file = getItem(position);
        Glide.with(context)
                .load(file.getPath())
                .override(600, 400)
                .centerCrop()
                .into(holder.imageView);

        holder.imageView.setOnClickListener(v -> {
            if (contentType == 'i') {
                Intent i = new Intent(context, ImageViewerActivity.class);
                i.putExtra("path", file.getPath());
                i.putExtra("contentType", contentType);
                i.putExtra("position", position);
                context.startActivity(i);
            } else {
                Intent i = new Intent(context, VideoViewerActivity.class);
                i.putExtra("position", position);
                i.putExtra("contentType", contentType);
                context.startActivity(i);
            }
        });
    }

    static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final ImageView imageView;

        VH(CardView itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.vvr);
        }
    }
}
