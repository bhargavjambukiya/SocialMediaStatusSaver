package com.studio.statusvault.data;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.studio.statusvault.data.repository.StatusMediaItem;
import com.studio.statusvault.utils.WhatsAppAccessHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FilesData {

    private static final String TAG = "FilesData";

    private static Context appContext;

    /**
     * Call from {@link com.studio.statusvault.MyApplication#onCreate()} so app-specific save dirs work.
     */
    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    /**
     * Trim / converted videos — app-private external dir (writable on Android 10+ without broad storage permission).
     */
    public static File getAppConvertedVideoDir() {
        if (appContext == null) {
            throw new IllegalStateException("FilesData.init(Application) must be called before using storage");
        }
        File base = appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (base == null) {
            base = appContext.getExternalFilesDir(null);
        }
        if (base == null) {
            base = appContext.getFilesDir();
        }
        File dir = new File(base, "convertedVideo");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Could not create " + dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * Extracted audio — app-private external dir.
     */
    public static File getAppAudioDir() {
        if (appContext == null) {
            throw new IllegalStateException("FilesData.init(Application) must be called before using storage");
        }
        File base = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (base == null) {
            base = appContext.getExternalFilesDir(null);
        }
        if (base == null) {
            base = appContext.getFilesDir();
        }
        File dir = new File(base, "extracted_audio");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Could not create " + dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * Saved images/videos — app-specific external dir for reliable writes on Android 10+.
     */
    public static File getAppSavedStatusesDir() {
        if (appContext == null) {
            throw new IllegalStateException("FilesData.init(Application) must be called before using storage");
        }
        File base = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (base == null) {
            base = appContext.getExternalFilesDir(null);
        }
        if (base == null) {
            base = appContext.getFilesDir();
        }
        File dir = new File(base, "saved_statuses");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Could not create " + dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * WhatsApp and WhatsApp Business status folders (classic + Android/media scoped layout).
     */
    private static final String[] WHATSAPP_STATUS_DIRECTORIES = {
            "/WhatsApp/Media/.Statuses",
            "/WhatsApp Business/Media/.Statuses",
            "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
    };

    public static final String SAVED_FILES_LOCATION = "/WhatsAppStatusDownloader";
    /** @deprecated Legacy public path; audio is stored under {@link #getAppAudioDir()}. */
    public static final String SAVED_FILES_AUDIO_LOCATION = "/WhatsAppStatusDownloader/audio";
    /** @deprecated Legacy public path; converted videos use {@link #getAppConvertedVideoDir()}. */
    public static final String SAVED_FILES_SPLIT_VIDEO = "/WhatsAppStatusDownloader/convertedVideo";

    private static ArrayList<StatusMediaItem> whatsAppFilesImages = new ArrayList<>();
    private static ArrayList<StatusMediaItem> whatsAppFilesVideos = new ArrayList<>();
    private static ArrayList<File> savedFilesImages = new ArrayList<>();
    private static ArrayList<File> savedFilesVideos = new ArrayList<>();
    private static ArrayList<File> savedSplitFilesVideos = new ArrayList<>();
    private static ArrayList<File> savedAudioFiles = new ArrayList<>();
    private static String recentOrSaved;

    public static void scrapWhatsAppFilesLegacy() {
        whatsAppFilesImages.clear();
        whatsAppFilesVideos.clear();

        File root = Environment.getExternalStorageDirectory();
        ArrayList<File> candidates = new ArrayList<>();

        for (String relative : WHATSAPP_STATUS_DIRECTORIES) {
            File parentDir = new File(root, relative.startsWith("/") ? relative.substring(1) : relative);
            collectFilesFromDirectory(parentDir, candidates);
        }

        Set<String> seen = new HashSet<>();
        ArrayList<File> unique = new ArrayList<>();

        for (File file : candidates) {
            String key;
            try {
                key = file.getCanonicalPath();
            } catch (IOException e) {
                key = file.getAbsolutePath();
            }
            if (seen.add(key)) {
                unique.add(file);
            }
        }

        Collections.sort(unique, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        for (File file : unique) {
            String lower = file.getName().toLowerCase(Locale.US);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
                whatsAppFilesImages.add(new StatusMediaItem(file, false));
            } else if (lower.endsWith(".gif") || lower.endsWith(".mp4")) {
                whatsAppFilesVideos.add(new StatusMediaItem(file, true));
            }
        }
    }

    public static void scrapWhatsAppFilesScoped(Context context) {
        whatsAppFilesImages.clear();
        whatsAppFilesVideos.clear();

        Uri treeUri = WhatsAppAccessHelper.getSavedTreeUri(context);
        if (treeUri == null) {
            return;
        }

        DocumentFile parentDir = DocumentFile.fromTreeUri(context, treeUri);
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            return;
        }

        ArrayList<StatusMediaItem> all = new ArrayList<>();

        for (DocumentFile file : parentDir.listFiles()) {
            if (!file.isFile()) continue;

            String name = file.getName();
            if (name == null) continue;

            String lower = name.toLowerCase(Locale.US);
            long lastModified = file.lastModified();

            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
                all.add(new StatusMediaItem(file.getUri(), name, lastModified, false));
            } else if (lower.endsWith(".gif") || lower.endsWith(".mp4")) {
                all.add(new StatusMediaItem(file.getUri(), name, lastModified, true));
            }
        }

        Collections.sort(all, (a, b) -> Long.compare(b.getLastModified(), a.getLastModified()));

        for (StatusMediaItem item : all) {
            if (item.isVideo()) {
                whatsAppFilesVideos.add(item);
            } else {
                whatsAppFilesImages.add(item);
            }
        }
    }

    public static void scrapWhatsAppFiles(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            scrapWhatsAppFilesScoped(context);
        } else {
            scrapWhatsAppFilesLegacy();
        }
        RecentStatusMirror.syncMirrorAndAugmentLists(context);
        applyRecentDateRangeFilter(context);
    }

    /**
     * When the user chooses &quot;last 7 days&quot; in settings, drop older items from the in-memory lists.
     * {@link StatusMediaItem#getLastModified()} must stay aligned with grid and fullscreen viewers.
     * Lenient policy: {@code lastModified} less than or equal to 0 (unknown, sometimes from SAF) is kept so lists do not empty on devices that omit timestamps.
     * Mirrored copies under app storage are kept for their retention window separately — do not drop here.
     */
    private static void applyRecentDateRangeFilter(Context context) {
        if (RecentStatusRangePreference.getRange(context) != RecentStatusRangePreference.Range.LAST_7_DAYS) {
            return;
        }
        final long cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        whatsAppFilesImages.removeIf(item -> !keepStatusItemForLast7Days(context, item, cutoff));
        whatsAppFilesVideos.removeIf(item -> !keepStatusItemForLast7Days(context, item, cutoff));
    }

    private static boolean keepStatusItemForLast7Days(Context context, StatusMediaItem item, long cutoff) {
        if (item.getFile() != null && RecentStatusMirror.isMirroredStoredFile(item.getFile(), context)) {
            return true;
        }
        long lm = item.getLastModified();
        if (lm <= 0L) {
            return true;
        }
        return lm >= cutoff;
    }

    static void replaceWhatsAppRecentLists(
            ArrayList<StatusMediaItem> images, ArrayList<StatusMediaItem> videos) {
        whatsAppFilesImages.clear();
        whatsAppFilesImages.addAll(images);
        whatsAppFilesVideos.clear();
        whatsAppFilesVideos.addAll(videos);
    }

    private static void collectFilesFromDirectory(File parentDir, ArrayList<File> out) {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            return;
        }

        File[] files = parentDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                out.add(file);
            }
        }
    }

    public static void scrapSavedFiles() {
        savedFilesImages.clear();
        savedFilesVideos.clear();

        File[] dirsToScan = new File[]{
                getAppSavedStatusesDir(),
                new File(Environment.getExternalStorageDirectory().toString() + SAVED_FILES_LOCATION) // legacy path
        };

        for (File parentDir : dirsToScan) {
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            File[] files = parentDir.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                String lower = file.getName().toLowerCase(Locale.US);

                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
                    if (!savedFilesImages.contains(file)) {
                        savedFilesImages.add(file);
                    }
                } else if (lower.endsWith(".gif") || lower.endsWith(".mp4")) {
                    if (!savedFilesVideos.contains(file)) {
                        savedFilesVideos.add(file);
                    }
                }
            }
        }

        splatSavedFiles();
    }

    public static void audioSavedFiles() {
        savedAudioFiles.clear();

        File parentDir = getAppAudioDir();

        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase(Locale.US).endsWith(".mp3")) {
                    savedAudioFiles.add(file);
                }
            }
        }
    }

    public static void splatSavedFiles() {
        savedSplitFilesVideos.clear();

        File parentDir = getAppConvertedVideoDir();

        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase(Locale.US).endsWith(".mp4")) {
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

    public static ArrayList<StatusMediaItem> getWhatsAppFilesImages() {
        return whatsAppFilesImages;
    }

    public static ArrayList<StatusMediaItem> getWhatsAppFilesVideos() {
        return whatsAppFilesVideos;
    }

    /**
     * Rows that still correspond to WhatsApp’s hidden status folder (SAF tree or legacy filesystem path).
     * Excludes app-mirrored files used so the combined “All” tab can keep items for seven days after
     * WhatsApp removes them from {@code .Statuses}.
     */
    public static boolean isCurrentlyInWhatsAppStatusFolder(StatusMediaItem item, Context context) {
        if (item.isFromSaf()) {
            return true;
        }
        File f = item.getFile();
        if (f == null) {
            return false;
        }
        if (RecentStatusMirror.isMirroredStoredFile(f, context)) {
            return false;
        }
        return isLegacyPathLikelyWhatsAppStatuses(f);
    }

    /** Filtered snapshot; does not mutate the backing lists returned by {@link #getWhatsAppFilesImages()} / {@link #getWhatsAppFilesVideos()}. */
    public static ArrayList<StatusMediaItem> copyFilteredToWhatsAppStatusFolderOnly(
            ArrayList<StatusMediaItem> source, Context context) {
        ArrayList<StatusMediaItem> out = new ArrayList<>();
        for (StatusMediaItem item : source) {
            if (isCurrentlyInWhatsAppStatusFolder(item, context)) {
                out.add(item);
            }
        }
        return out;
    }

    private static boolean isLegacyPathLikelyWhatsAppStatuses(File file) {
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            path = file.getAbsolutePath();
        }
        String lower = path.toLowerCase(Locale.US);
        return lower.contains("/.statuses");
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