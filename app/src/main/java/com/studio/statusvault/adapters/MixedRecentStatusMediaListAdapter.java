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
import com.studio.statusvault.R;
import com.studio.statusvault.data.repository.StatusMediaItem;
import com.studio.statusvault.ui.activity.ImageViewerActivity;
import com.studio.statusvault.ui.activity.VideoViewerActivity;
import com.studio.statusvault.utils.WhatsAppAccessHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Recent grid mixing images and videos (sorted elsewhere). Clicks map to the correct index in the
 * underlying image-only or video-only lists so viewers stay compatible.
 */
public final class MixedRecentStatusMediaListAdapter
        extends ListAdapter<StatusMediaItem, androidx.recyclerview.widget.RecyclerView.ViewHolder> {

    private static final int TYPE_IMAGE = 0;
    private static final int TYPE_VIDEO = 1;

    private final Context context;
    @Nullable
    private final Runnable onNeedWhatsAppAccess;

    private ArrayList<StatusMediaItem> imageSource = new ArrayList<>();
    private ArrayList<StatusMediaItem> videoSource = new ArrayList<>();

    private static final DiffUtil.ItemCallback<StatusMediaItem> DIFF =
            new DiffUtil.ItemCallback<StatusMediaItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull StatusMediaItem oldItem, @NonNull StatusMediaItem newItem) {
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
                public boolean areContentsTheSame(
                        @NonNull StatusMediaItem oldItem, @NonNull StatusMediaItem newItem) {
                    return oldItem.getLastModified() == newItem.getLastModified()
                            && oldItem.isVideo() == newItem.isVideo();
                }
            };

    public MixedRecentStatusMediaListAdapter(
            Context context, @Nullable Runnable onNeedWhatsAppAccess) {
        super(DIFF);
        this.context = context;
        this.onNeedWhatsAppAccess = onNeedWhatsAppAccess;
    }

    public void setSourceLists(@Nullable List<StatusMediaItem> images, @Nullable List<StatusMediaItem> videos) {
        imageSource = images != null ? new ArrayList<>(images) : new ArrayList<>();
        videoSource = videos != null ? new ArrayList<>(videos) : new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        StatusMediaItem item = getItem(position);
        return item.isVideo() ? TYPE_VIDEO : TYPE_IMAGE;
    }

    @NonNull
    @Override
    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        CardView card = (CardView) LayoutInflater.from(parent.getContext()).inflate(
                viewType == TYPE_VIDEO ? R.layout.st_recyclerview_video_item : R.layout.st_recyclerview_image_item,
                parent,
                false);
        return new MediaVH(card);
    }

    @Override
    public void onBindViewHolder(
            @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
        StatusMediaItem item = getItem(position);
        MediaVH vh = (MediaVH) holder;

        if (item.isFromSaf()) {
            Glide.with(context)
                    .load(item.getUri())
                    .override(600, 400)
                    .centerCrop()
                    .into(vh.imageView);
        } else if (item.getFile() != null) {
            Glide.with(context)
                    .load(item.getFile())
                    .override(600, 400)
                    .centerCrop()
                    .into(vh.imageView);
        }

        vh.imageView.setOnClickListener(v -> openViewer(item));
    }

    private void openViewer(@NonNull StatusMediaItem item) {
        if (item.isVideo()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && onNeedWhatsAppAccess != null
                    && (!WhatsAppAccessHelper.hasTreeUri(context)
                    || !WhatsAppAccessHelper.canAccessWhatsAppTree(context))) {
                onNeedWhatsAppAccess.run();
                return;
            }
            int vi = indexInList(videoSource, item);
            Intent i = new Intent(context, VideoViewerActivity.class);
            i.putExtra("position", vi);
            i.putExtra("contentType", 'v');
            context.startActivity(i);
        } else {
            int ii = indexInList(imageSource, item);
            Intent i = new Intent(context, ImageViewerActivity.class);
            i.putExtra("position", ii);
            i.putExtra("contentType", 'i');
            context.startActivity(i);
        }
    }

    private static int indexInList(ArrayList<StatusMediaItem> list, StatusMediaItem needle) {
        for (int i = 0; i < list.size(); i++) {
            StatusMediaItem it = list.get(i);
            if (needle.isFromSaf() && it.isFromSaf()) {
                if (needle.getUri().equals(it.getUri())) {
                    return i;
                }
            } else if (!needle.isFromSaf() && !it.isFromSaf()
                    && needle.getFile() != null && it.getFile() != null) {
                if (needle.getFile().getAbsolutePath().equals(it.getFile().getAbsolutePath())) {
                    return i;
                }
            }
        }
        return 0;
    }

    static final class MediaVH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final ImageView imageView;

        MediaVH(CardView itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.vvr);
        }
    }
}
