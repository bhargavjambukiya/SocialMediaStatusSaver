package com.studio.statusvault.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.studio.statusvault.R;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.filesoperations.FileOperations;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Extracted MP3 files list with delete.
 */
public class AudioFileListAdapter extends ListAdapter<File, AudioFileListAdapter.VH> {

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
    private final Runnable onEmpty;

    public AudioFileListAdapter(Context context, Runnable onEmpty) {
        super(DIFF);
        this.context = context;
        this.onEmpty = onEmpty;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView v = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.st_recyclerview_audio_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        File file = getItem(position);
        Date lastModDate = new Date(file.lastModified());
        holder.textViewAudio.setText(formatDate(lastModDate));
        holder.textViewSize.setText(fileSizeKb(file));

        View.OnClickListener open = v -> openAudio(file);
        holder.imageView.setOnClickListener(open);
        holder.textViewAudio.setOnClickListener(open);
        holder.textViewSize.setOnClickListener(open);

        holder.imageViewDelete.setOnClickListener(v -> confirmDelete(file));
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.delete_audio_title)
                .setMessage(R.string.delete_audio_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    FileOperations.deleteAndRefreshFiles(file);
                    ArrayList<File> next = new ArrayList<>(FilesData.getSavedAudioFiles());
                    submitList(next);
                    Toast.makeText(context, R.string.audio_deleted, Toast.LENGTH_SHORT).show();
                    if (next.isEmpty() && onEmpty != null) {
                        onEmpty.run();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void openAudio(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
        intent.setDataAndType(uri, "audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, null));
    }

    private static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        return sdf.format(date);
    }

    private static String fileSizeKb(File file) {
        long size = file.length() / 1024;
        if (size >= 1024) {
            return (size / 1024) + " Mb";
        }
        return size + " Kb";
    }

    static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final ImageView imageView;
        final ImageView imageViewDelete;
        final TextView textViewAudio;
        final TextView textViewSize;

        VH(CardView v) {
            super(v);
            imageViewDelete = v.findViewById(R.id.imageViewDelete);
            imageView = v.findViewById(R.id.vvr);
            textViewAudio = v.findViewById(R.id.textViewAudio);
            textViewSize = v.findViewById(R.id.textViewSize);
        }
    }
}
