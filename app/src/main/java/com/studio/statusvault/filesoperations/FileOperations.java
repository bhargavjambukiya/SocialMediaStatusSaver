package com.studio.statusvault.filesoperations;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.studio.statusvault.R;
import com.studio.statusvault.data.FilesData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class FileOperations {
    private static final String TAG = "FileOperations";

    public static void deleteAndRefreshFiles(File file) {
        file.delete();
        FilesData.scrapSavedFiles();
        FilesData.audioSavedFiles();
    }

    public static void saveAndRefreshFiles(Context context, File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Source file missing: " + sourceFile);
        }
        File destDir = FilesData.getAppSavedStatusesDir();
        File destFile = new File(destDir, sourceFile.getName());

        if (destFile.exists()) {
            destFile = uniqueDestinationFile(destDir, sourceFile.getName());
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            Log.e(TAG, "saveAndRefreshFiles failed. source=" + sourceFile + ", dest=" + destFile, e);
            throw e;
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
        FilesData.scrapSavedFiles();
    }


    public static void shareFile(File file, Context c, char type) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", file);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setClipData(android.content.ClipData.newUri(c.getContentResolver(), "", uri));

        if (type == 'i') {
            shareIntent.setType("image/*");
        } else {
            shareIntent.setType("video/*");
        }
        c.startActivity(Intent.createChooser(shareIntent, c.getString(R.string.share_using)));
    }
    public static void saveAndRefreshFilesFromUri(Context context, Uri sourceUri, String fileName) throws IOException {
        File destDir = FilesData.getAppSavedStatusesDir();
        File safeName = new File(fileName == null ? "status_image" : fileName);
        File destFile = new File(destDir, safeName.getName());
        if (destFile.exists()) {
            destFile = uniqueDestinationFile(destDir, safeName.getName());
        }

        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destFile)) {

            if (inputStream == null) {
                throw new IOException("Unable to open input stream from uri");
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "saveAndRefreshFilesFromUri failed. uri=" + sourceUri + ", dest=" + destFile, e);
            throw e;
        }

        FilesData.scrapSavedFiles();
    }

    private static File uniqueDestinationFile(File parentDir, String originalName) {
        String name = originalName;
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > 0 && dot < originalName.length() - 1) {
            name = originalName.substring(0, dot);
            ext = originalName.substring(dot);
        }
        int i = 1;
        File candidate = new File(parentDir, originalName);
        while (candidate.exists()) {
            candidate = new File(parentDir, name + "_" + i + ext);
            i++;
        }
        return candidate;
    }
    public static void shareUri(Context context, Uri uri, char type) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setClipData(android.content.ClipData.newUri(context.getContentResolver(), "", uri));

        if (type == 'i') {
            shareIntent.setType("image/*");
        } else {
            shareIntent.setType("video/*");
        }

        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_using)));
    }
    public static File copyUriToCacheFile(Context context, Uri sourceUri, String fileName) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "shared_media");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File destFile = new File(cacheDir, fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destFile)) {

            if (inputStream == null) {
                throw new IOException("Unable to open input stream from uri");
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        }

        return destFile;
    }
}
