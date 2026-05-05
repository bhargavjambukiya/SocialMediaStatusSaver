package com.studio.statusvault.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * {@link Intent#ACTION_OPEN_DOCUMENT_TREE} is independent of app package name. A new install
 * (e.g. new applicationId) clears the saved tree URI, so the picker is shown again.
 * <p>
 * OEM pickers are inconsistent. We combine: (1) a {@link Document} URI to {@code .Statuses} via
 * {@link DocumentsContract#buildDocumentUri} (not {@code buildTreeDocumentUri}), which the Files
 * app usually honors for {@link DocumentsContract#EXTRA_INITIAL_URI}; and (2) on API 30+,
 * {@link StorageVolume#createOpenDocumentTreeIntent} as the base intent, which targets the
 * system DocumentsUI more reliably on some devices.
 */
public class WhatsAppAccessHelper {

    public static final String PREFS = "wa_status_prefs";
    public static final String KEY_TREE_URI = "wa_tree_uri";

    private static final String EXT_STORAGE_DOCS = "com.android.externalstorage.documents";
    /**
     * Android 10+ scoped layout (default for most Android 11+ / current WhatsApp).
     */
    private static final String WA_CONSUMER_STATUSES_DOC_ID =
            "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses";
    /** Pre-scoped: {@code /WhatsApp/Media/.Statuses} on primary storage. */
    private static final String WA_CONSUMER_STATUSES_DOC_ID_LEGACY =
            "primary:WhatsApp/Media/.Statuses";

    public static boolean isAndroid11Plus() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    /**
     * Picks a {@code primary:...} document id that matches the on-disk layout, when we can
     * cheaply {@link File#exists()} without SAF (best-effort; may be wrong on strict OEMs).
     */
    @NonNull
    private static String documentIdForWhatsappStatusesFolder() {
        File ext = Environment.getExternalStorageDirectory();
        if (ext == null) {
            return WA_CONSUMER_STATUSES_DOC_ID;
        }
        File scopedWa = new File(ext, "Android/media/com.whatsapp");
        if (scopedWa.isDirectory()) {
            return WA_CONSUMER_STATUSES_DOC_ID;
        }
        File legacyMedia = new File(ext, "WhatsApp/Media");
        if (legacyMedia.isDirectory()) {
            return WA_CONSUMER_STATUSES_DOC_ID_LEGACY;
        }
        return WA_CONSUMER_STATUSES_DOC_ID;
    }

    /**
     * @return a tree-picker intent. Use {@code addFlags} as you already do, then
     *         {@code launch(intent)}. Prefer this over a bare {@code ACTION_OPEN_DOCUMENT_TREE}.
     */
    @NonNull
    public static Intent newOpenDocumentTreeIntentForWhatsappStatuses(@NonNull Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent fromVol = null;
            try {
                StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                if (sm != null) {
                    StorageVolume vol = sm.getPrimaryStorageVolume();
                    if (vol != null) {
                        fromVol = vol.createOpenDocumentTreeIntent();
                    }
                }
            } catch (Throwable ignored) {
                // use generic intent below
            }
            intent = (fromVol != null) ? fromVol : new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Document URI (not tree URI) — required by many pickers to open *inside* this folder.
            Uri initial = DocumentsContract.buildDocumentUri(
                    EXT_STORAGE_DOCS,
                    documentIdForWhatsappStatusesFolder());
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial);
        }
        return intent;
    }

    /**
     * Legacy helper: set {@link DocumentsContract#EXTRA_INITIAL_URI} on an existing intent
     * (e.g. if you cannot use {@link #newOpenDocumentTreeIntentForWhatsappStatuses}).
     */
    public static void configureOpenDocumentTreeForWhatsAppStatuses(@NonNull Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        Uri initial = DocumentsContract.buildDocumentUri(
                EXT_STORAGE_DOCS,
                documentIdForWhatsappStatusesFolder());
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial);
    }

    public static void saveTreeUri(Context context, Uri uri, int takeFlags) {
        ContentResolver resolver = context.getContentResolver();
        resolver.takePersistableUriPermission(
                uri,
                takeFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        );

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply();
    }

    public static Uri getSavedTreeUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String uri = prefs.getString(KEY_TREE_URI, null);
        return uri == null ? null : Uri.parse(uri);
    }

    public static boolean hasTreeUri(Context context) {
        return getSavedTreeUri(context) != null;
    }

    public static boolean canAccessWhatsAppTree(Context context) {
        Uri treeUri = getSavedTreeUri(context);
        if (treeUri == null) return false;

        androidx.documentfile.provider.DocumentFile dir =
                androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri);

        return dir != null && dir.exists() && dir.isDirectory() && dir.canRead();
    }
}