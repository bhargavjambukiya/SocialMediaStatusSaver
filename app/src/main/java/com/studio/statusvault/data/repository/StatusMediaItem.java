package com.studio.statusvault.data.repository;

import android.net.Uri;

import java.io.File;

public class StatusMediaItem {
    private final File file;
    private final Uri uri;
    private final String name;
    private final long lastModified;
    private final boolean video;
    private final boolean fromSaf;

    public StatusMediaItem(File file, boolean video) {
        this.file = file;
        this.uri = null;
        this.name = file.getName();
        this.lastModified = file.lastModified();
        this.video = video;
        this.fromSaf = false;
    }

    /** File-backed row with an explicit timestamp (e.g. mirrored copy keeps original WhatsApp mtime). */
    public StatusMediaItem(File file, boolean video, long lastModifiedOverride) {
        this.file = file;
        this.uri = null;
        this.name = file.getName();
        this.lastModified = lastModifiedOverride;
        this.video = video;
        this.fromSaf = false;
    }

    public StatusMediaItem(Uri uri, String name, long lastModified, boolean video) {
        this.file = null;
        this.uri = uri;
        this.name = name;
        this.lastModified = lastModified;
        this.video = video;
        this.fromSaf = true;
    }

    public File getFile() {
        return file;
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isVideo() {
        return video;
    }

    public boolean isFromSaf() {
        return fromSaf;
    }
}