package com.studio.statusvault.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.bumptech.glide.Glide;
import com.studio.statusvault.Constant;
import com.studio.statusvault.R;
import com.studio.statusvault.data.repository.StatusMediaItem;
import com.studio.statusvault.ui.activity.ImageViewerActivity;
import com.studio.statusvault.ui.activity.VideoViewerActivity;
import com.studio.statusvault.utils.WhatsAppAccessHelper;

public class RecentStatusMediaListAdapter extends ListAdapter<StatusMediaItem, RecentStatusMediaListAdapter.VH> {

    private final Context context;
    private final char contentType;
    @Nullable
    private final Runnable onNeedWhatsAppAccess;

    private static final DiffUtil.ItemCallback<StatusMediaItem> DIFF = new DiffUtil.ItemCallback<StatusMediaItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull StatusMediaItem oldItem, @NonNull StatusMediaItem newItem) {
            if (oldItem.isFromSaf() && newItem.isFromSaf()) {
                return oldItem.getUri().equals(newItem.getUri());
            }
            if (!oldItem.isFromSaf() && !newItem.isFromSaf()
                    && oldItem.getFile() != null && newItem.getFile() != null) {
                return oldItem.getFile().getAbsolutePath().equals(newItem.getFile().getAbsolutePath());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull StatusMediaItem oldItem, @NonNull StatusMediaItem newItem) {
            return oldItem.getLastModified() == newItem.getLastModified()
                    && oldItem.isVideo() == newItem.isVideo();
        }
    };

    public RecentStatusMediaListAdapter(Context context, char contentType, @Nullable Runnable onNeedWhatsAppAccess) {
        super(DIFF);
        this.context = context;
        this.contentType = contentType;
        this.onNeedWhatsAppAccess = onNeedWhatsAppAccess;
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
        StatusMediaItem item = getItem(position);

        if (item.isFromSaf()) {
            Glide.with(context)
                    .load(item.getUri())
                    .override(600, 400)
                    .centerCrop()
                    .into(holder.imageView);
        } else if (item.getFile() != null) {
            Glide.with(context)
                    .load(item.getFile())
                    .override(600, 400)
                    .centerCrop()
                    .into(holder.imageView);
        }

        holder.imageView.setOnClickListener(v -> {
            if (contentType == 'i') {
                Intent i = new Intent(context, ImageViewerActivity.class);
                i.putExtra("position", position);
                i.putExtra("contentType", contentType);
                i.putExtra(Constant.EXTRA_RECENT_VIEWER_STATUS_FOLDER_ONLY, true);
                context.startActivity(i);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        && onNeedWhatsAppAccess != null
                        && (!WhatsAppAccessHelper.hasTreeUri(context)
                        || !WhatsAppAccessHelper.canAccessWhatsAppTree(context))) {
                    onNeedWhatsAppAccess.run();
                    return;
                }
                Intent i = new Intent(context, VideoViewerActivity.class);
                i.putExtra("position", position);
                i.putExtra("contentType", contentType);
                i.putExtra(Constant.EXTRA_RECENT_VIEWER_STATUS_FOLDER_ONLY, true);
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
