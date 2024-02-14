package com.studio.statussave.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.studio.statussave.Constant;
import com.studio.statussave.Models.Status;
import com.studio.statussave.R;
import com.studio.statussave.ui.activity.VideoViewerActivity;
import com.studio.statussave.utils.Common;

import java.util.List;


public class VideoAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private final List<Status> videoList;
    private Context context;
    private final ConstraintLayout container;

    public VideoAdapter(List<Status> videoList, ConstraintLayout container) {
        this.videoList = videoList;
        this.container = container;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_status, parent, false);
        return new ItemViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {

        final Status status = videoList.get(position);

        if (status.isApi30()) {
//            holder.save.setVisibility(View.GONE);
            Glide.with(context).load(status.getDocumentFile().getUri()).into(holder.imageView);
        } else {
//            holder.save.setVisibility(View.VISIBLE);
            Glide.with(context).load(status.getFile()).into(holder.imageView);
        }

        holder.share.setOnClickListener(v -> {

            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            shareIntent.setType("image/mp4");
            if (status.isApi30()) {
                shareIntent.putExtra(Intent.EXTRA_STREAM, status.getDocumentFile().getUri());
            } else {
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + status.getFile().getAbsolutePath()));
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share image"));

        });
        holder.imageView.setOnClickListener(view -> {
            Constant.SelectedFile = status;
            Intent i = new Intent(context, VideoViewerActivity.class);
            context.startActivity(i);
        });

       /* LayoutInflater inflater = LayoutInflater.from(context);
        final View view1 = inflater.inflate(R.layout.view_video_full_screen, null);

        holder.imageView.setOnClickListener(v -> {

            final AlertDialog.Builder alertDg = new AlertDialog.Builder(context);

            FrameLayout mediaControls = view1.findViewById(R.id.videoViewWrapper);

            if (view1.getParent() != null) {
                ((ViewGroup) view1.getParent()).removeView(view1);
            }

            alertDg.setView(view1);

            final VideoView videoView = view1.findViewById(R.id.video_full);

            final MediaController mediaController = new MediaController(context, false);

            videoView.setOnPreparedListener(mp -> {

                mp.start();
                mediaController.show(0);
                mp.setLooping(true);
            });

            videoView.setMediaController(mediaController);
            mediaController.setMediaPlayer(videoView);

            if (status.isApi30()) {
                videoView.setVideoURI(status.getDocumentFile().getUri());
            } else {
                videoView.setVideoURI(Uri.fromFile(status.getFile()));
            }
            videoView.requestFocus();

            ((ViewGroup) mediaController.getParent()).removeView(mediaController);

            if (mediaControls.getParent() != null) {
                mediaControls.removeView(mediaController);
            }

            mediaControls.addView(mediaController);

            final AlertDialog alert2 = alertDg.create();

            alert2.getWindow().getAttributes().windowAnimations = R.style.SlidingDialogAnimation;
            alert2.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alert2.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            alert2.show();

        });*/

        holder.save.setOnClickListener(v -> Common.copyFile(status, context, container));

    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

}
