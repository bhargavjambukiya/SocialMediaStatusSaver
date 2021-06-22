package com.studio.statussave.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.studio.statussave.R;
import com.studio.statussave.data.FilesData;
import com.studio.statussave.filesoperations.FileOperations;
import com.studio.statussave.ui.activity.AudioActivity;
import com.studio.statussave.ui.activity.ImageViewerActivity;
import com.studio.statussave.ui.activity.VideoViewerActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private ArrayList<File> mDataset;
    private Context mContext;
    private char contentType;

    public RecyclerAdapter(ArrayList<File> myDataset, Context c, char k) {
        mDataset = myDataset;
        mContext = c;
        contentType = k;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        CardView v;
        if (mDataset != null && !mDataset.isEmpty()) {
            if (contentType == 'i') {
                v = (CardView) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.st_recyclerview_image_item, parent, false);
            } else if (contentType == 'a') {
                v = (CardView) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.st_recyclerview_audio_item, parent, false);
            } else {
                v = (CardView) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.st_recyclerview_video_item, parent, false);
            }
        } else {

            v = (CardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.st_empty_warning, parent, false);
        }

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (mDataset != null) {
            if (!mDataset.isEmpty()) {
                final int ourPosition = (position);
                if (contentType == 'i') {
                    Glide.with(mContext)
                            .load(mDataset.get(ourPosition).getPath())
                            .override(600, 400)
                            .centerCrop()
                            .into(holder.mImageView);

                    holder.mImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(mContext, ImageViewerActivity.class);
                            i.putExtra("path", mDataset.get(ourPosition).getPath());
                            i.putExtra("contentType", contentType);
                            i.putExtra("position", position);
                            mContext.startActivity(i);
                        }
                    });

                } else if (contentType == 'a') {
                    Date lastModDate = new Date(mDataset.get(ourPosition).lastModified());

                    holder.textViewAudio.setText(dateFormat(lastModDate));
                    holder.textViewSize.setText(fileSize(mDataset.get(position)));
                    holder.mImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openAudioPlayer(position);
                        }
                    });
                    holder.textViewAudio.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openAudioPlayer(position);
                        }
                    });
                    holder.textViewSize.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openAudioPlayer(position);
                        }
                    });

                    holder.imageViewDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            deleteAudio(position);
                        }
                    });
                } else {
                    Glide.with(mContext)
                            .load(mDataset.get(ourPosition).getPath())
                            .override(600, 400)
                            .centerCrop()
                            .into(holder.mImageView);
                    holder.mImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(mContext, VideoViewerActivity.class);
                            i.putExtra("position", position);
                            i.putExtra("contentType", contentType);
                            mContext.startActivity(i);
                        }
                    });
                }
            }
        }
    }

    private void deleteAudio(final int position) {
        new AlertDialog.Builder(mContext)
                .setTitle("Delete audio")
                .setMessage("Are you sure you want to delete this audio?")

                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        FileOperations.deleteAndRefreshFiles(FilesData.getSavedAudioFiles().get(position));
                        RecyclerInstances.audioAdapter.notifyDataSetChanged();
                        RecyclerInstances.audioRecyclerView.setAdapter(RecyclerInstances.audioAdapter);
                        Toast.makeText(mContext, "Audio deleted", Toast.LENGTH_SHORT).show();
                        if (FilesData.getSavedAudioFiles().size() == 0) {
                            AudioActivity.layoutNoRecordFound.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();

    }

    private void openAudioPlayer(int position) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(mDataset.get(position).getAbsolutePath()), "audio/*");
        mContext.startActivity(intent);
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {

        if (mDataset != null) {
            if (mDataset.isEmpty()) {

                return 1;
            } else {
                return (mDataset.size());
            }
        } else {
            return 0;
        }
    }


    //viewHolder InnerClass
    protected class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView, imageViewDelete;
        private TextView textViewAudio, textViewSize;

        public ViewHolder(CardView v) {
            super(v);
            imageViewDelete = v.findViewById(R.id.imageViewDelete);
            mImageView = v.findViewById(R.id.vvr);
            textViewAudio = v.findViewById(R.id.textViewAudio);
            textViewSize = v.findViewById(R.id.textViewSize);
        }
    }

    private static String dateFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        return sdf.format(date);
    }

    private String fileSize(File file) {
        long size = file.length() / 1024; // Get size and convert bytes into Kb.
        if (size >= 1024) {
            return (size / 1024) + " Mb";
        } else {
            return size + " Kb";
        }
    }
}






