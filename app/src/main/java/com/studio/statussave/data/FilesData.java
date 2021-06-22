package com.studio.statussave.data;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FilesData {
    private static final String WHATSAPP_STATUSES_LOCATION = "/WhatsApp/Media/.Statuses";
    public static final String SAVED_FILES_LOCATION = "/WhatsAppStatusDownloader";
    public static final String SAVED_FILES_AUDIO_LOCATION = "/WhatsAppStatusDownloader/audio";
    public static final String SAVED_FILES_SPLIT_VIDEO = "/WhatsAppStatusDownloader/convertedVideo";
    private static ArrayList<File> whatsAppFilesImages = new ArrayList<>();
    private static ArrayList<File> whatsAppFilesVideos = new ArrayList<>();
    private static ArrayList<File> savedFilesImages = new ArrayList<>();
    private static ArrayList<File> savedFilesVideos = new ArrayList<>();
    private static ArrayList<File> savedSplitFilesVideos = new ArrayList<>();
    private static ArrayList<File> savedAudioFiles = new ArrayList<>();
    private static String recentOrSaved;

    public static void scrapWhatsAppFiles() {

        whatsAppFilesImages.clear();
        whatsAppFilesVideos.clear();

        File[] files;
        File parentDir = new File(Environment.getExternalStorageDirectory().toString() + WHATSAPP_STATUSES_LOCATION);

        if (!parentDir.exists())
            parentDir.mkdirs();


        files = parentDir.listFiles();

        if (files != null && files.length > 1) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File object1, File object2) {
                    return (int) (Math.max(object1.lastModified(), object2.lastModified()));
                }
            });
        }
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".jpg")) {
                    if (!whatsAppFilesImages.contains(file))
                        whatsAppFilesImages.add(file);
                } else if (file.getName().endsWith(".gif") ||
                        file.getName().endsWith(".mp4")) {
                    if (!whatsAppFilesVideos.contains(file))
                        whatsAppFilesVideos.add(file);
                }
            }


        }
    }

    public static void scrapSavedFiles() {
        savedFilesImages.clear();
        savedFilesVideos.clear();
        File[] files;
        File parentDir = new File(Environment.getExternalStorageDirectory().toString() + SAVED_FILES_LOCATION);

        if (!parentDir.exists())
            parentDir.mkdirs();

        files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {

                if (file.getName().endsWith(".jpg")) {
                    if (!savedFilesImages.contains(file))
                        savedFilesImages.add(file);
                } else if (file.getName().endsWith(".gif") ||
                        file.getName().endsWith(".mp4")) {

                    if (!savedFilesVideos.contains(file))
                        savedFilesVideos.add(file);
                }
            }
        }
        splatSavedFiles();
    }

    public static void audioSavedFiles() {
        savedAudioFiles.clear();
        File[] files;
        File parentDir = new File(Environment.getExternalStorageDirectory().toString() + SAVED_FILES_AUDIO_LOCATION);

        if (!parentDir.exists())
            parentDir.mkdirs();

        files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".mp3")) {
                    savedAudioFiles.add(file);
                }
            }
        }
    }

    public static void splatSavedFiles() {
        savedSplitFilesVideos.clear();
        File[] files;
        File parentDir = new File(Environment.getExternalStorageDirectory().toString() + SAVED_FILES_SPLIT_VIDEO);

        if (!parentDir.exists())
            parentDir.mkdirs();

        files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".mp4")) {
                    savedSplitFilesVideos.add(file);
                }
            }
        }
    }

    public static ArrayList<File> getSavedFilesImages() {
        return savedFilesImages;
    }

    public static ArrayList<File> getSavedAudioFiles() {
        return savedAudioFiles;
    }

    public static ArrayList<File> getSavedFilesVideos() {
        return savedFilesVideos;
    }

    public static ArrayList<File> getSplittedFilesVideos() {
        return savedSplitFilesVideos;
    }

    public static ArrayList<File> getWhatsAppFilesImages() {
        return whatsAppFilesImages;
    }

    public static ArrayList<File> getWhatsAppFilesVideos() {
        return whatsAppFilesVideos;
    }

    public static String getRecentOrSaved() {

        return recentOrSaved;
    }

    public static void setRecentOrSaved(String a) {
        recentOrSaved = a;
    }

    public static String getSavedFilesLocation() {
        return SAVED_FILES_LOCATION;
    }


}



