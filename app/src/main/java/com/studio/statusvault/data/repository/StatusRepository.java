package com.studio.statusvault.data.repository;

import android.content.Context;

import com.studio.statusvault.data.FilesData;

import java.io.File;
import java.util.ArrayList;

public class StatusRepository {

    private final Context context;

    public StatusRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void refreshListsForCurrentMode() {
        String mode = FilesData.getRecentOrSaved();
        if (mode == null) {
            mode = "recent";
        }

        switch (mode) {
            case "recent":
                FilesData.scrapWhatsAppFiles(context);
                break;
            case "audio":
                FilesData.audioSavedFiles();
                break;
            case "videoSplitter":
            case "offline":
            default:
                FilesData.scrapSavedFiles();
                break;
        }
    }

    public ArrayList<StatusMediaItem> getRecentImages() {
        return FilesData.getWhatsAppFilesImages();
    }

    public ArrayList<StatusMediaItem> getRecentVideos() {
        return FilesData.getWhatsAppFilesVideos();
    }

    /** Images still under WhatsApp’s {@code .Statuses} folder only (Images tab / folder-only viewers). */
    public ArrayList<StatusMediaItem> getRecentImagesFromStatusFolderOnly() {
        return FilesData.copyFilteredToWhatsAppStatusFolderOnly(
                FilesData.getWhatsAppFilesImages(), context);
    }

    /** Videos still under WhatsApp’s {@code .Statuses} folder only (Videos tab / folder-only viewers). */
    public ArrayList<StatusMediaItem> getRecentVideosFromStatusFolderOnly() {
        return FilesData.copyFilteredToWhatsAppStatusFolderOnly(
                FilesData.getWhatsAppFilesVideos(), context);
    }

    public ArrayList<File> getSavedImages() {
        return FilesData.getSavedFilesImages();
    }

    public ArrayList<File> getSavedVideos() {
        String mode = FilesData.getRecentOrSaved();
        if ("videoSplitter".equals(mode)) {
            return FilesData.getSplittedFilesVideos();
        }
        return FilesData.getSavedFilesVideos();
    }
}