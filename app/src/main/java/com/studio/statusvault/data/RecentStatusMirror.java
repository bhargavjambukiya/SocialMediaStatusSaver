package com.studio.statusvault.data;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import androidx.annotation.Nullable;

import com.studio.statusvault.data.repository.StatusMediaItem;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Copies statuses from WhatsApp’s .Statuses scrape into app-private storage and keeps them up to
 * 7 days after first seen, so items can still be listed after WhatsApp removes them from the folder.
 */
public final class RecentStatusMirror {

    private static final String SUBDIR = "recent_status_mirror";
    private static final String INDEX_JSON = "recent_status_mirror_entries.json";

    /** Retention counted from first time we mirrored the file while it was visible in WhatsApp. */
    private static final long RETENTION_MS = 7L * 24 * 60 * 60 * 1000;

    private RecentStatusMirror() {
    }

    public static File getMirrorRoot(Context context) {
        Context app = context.getApplicationContext();
        File base = app.getExternalFilesDir(null);
        if (base == null) {
            base = app.getFilesDir();
        }
        File root = new File(base, SUBDIR);
        File img = new File(root, "images");
        File vid = new File(root, "videos");
        if (!img.exists()) {
            //noinspection ResultOfMethodCallIgnored
            img.mkdirs();
        }
        if (!vid.exists()) {
            //noinspection ResultOfMethodCallIgnored
            vid.mkdirs();
        }
        return root;
    }

    /**
     * Unique key aligned with WhatsApp-scraped rows (URI path or filesystem path).
     */
    static String stableKey(StatusMediaItem item) {
        if (item.isFromSaf()) {
            return "u:" + item.getUri().toString();
        }
        if (item.getFile() != null) {
            try {
                return "f:" + item.getFile().getCanonicalPath();
            } catch (IOException e) {
                return "f:" + item.getFile().getAbsolutePath();
            }
        }
        return "x:" + System.identityHashCode(item);
    }

    /** True if {@code path} belongs to our mirror dirs (mirror-only rows). */
    public static boolean isMirroredStoredFile(File file, Context context) {
        if (file == null || !file.isFile()) {
            return false;
        }
        try {
            String mirror = getMirrorRoot(context).getCanonicalPath();
            return file.getCanonicalPath().startsWith(mirror + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * After scrape loaded {@link FilesData} lists from WhatsApp, mirror new/changed URLs/files and merge
     * rows that vanished from WhatsApp but are still retained.
     */
    public static void syncMirrorAndAugmentLists(Context context) {
        synchronized (RecentStatusMirror.class) {
            HashMap<String, Entry> registry = loadIndex(context);

            pruneExpired(registry);

            Collection<StatusMediaItem> scrapedImages =
                    new ArrayList<>(FilesData.getWhatsAppFilesImages());
            Collection<StatusMediaItem> scrapedVideos =
                    new ArrayList<>(FilesData.getWhatsAppFilesVideos());

            long now = System.currentTimeMillis();
            mirrorFromScrape(context, registry, scrapedImages, false, now);
            mirrorFromScrape(context, registry, scrapedVideos, true, now);

            pruneExpired(registry);
            persistIndex(context, registry);

            Set<String> presentKeys = new HashSet<>();
            for (StatusMediaItem i : scrapedImages) {
                presentKeys.add(stableKey(i));
            }
            for (StatusMediaItem i : scrapedVideos) {
                presentKeys.add(stableKey(i));
            }

            ArrayList<StatusMediaItem> outImg = new ArrayList<>(scrapedImages);
            ArrayList<StatusMediaItem> outVid = new ArrayList<>(scrapedVideos);

            for (Entry en : registry.values()) {
                if (now - en.firstSeenMillis > RETENTION_MS) {
                    continue;
                }
                File mirrored = new File(en.mirrorAbsolutePath);
                if (!mirrored.isFile()) {
                    continue;
                }
                if (presentKeys.contains(en.stableKey)) {
                    continue;
                }
                long lm = Math.max(en.originLastModified, mirrored.lastModified());
                StatusMediaItem row =
                        lm > 0L ? new StatusMediaItem(mirrored, en.video, lm)
                                : new StatusMediaItem(mirrored, en.video);
                if (en.video) {
                    outVid.add(row);
                } else {
                    outImg.add(row);
                }
            }

            Collections.sort(outImg, (a, b) -> Long.compare(b.getLastModified(), a.getLastModified()));
            Collections.sort(outVid, (a, b) -> Long.compare(b.getLastModified(), a.getLastModified()));

            FilesData.replaceWhatsAppRecentLists(outImg, outVid);
        }
    }

    private static void mirrorFromScrape(
            Context context,
            Map<String, Entry> registry,
            Iterable<StatusMediaItem> scraped,
            boolean video,
            long nowMillis) {

        Context app = context.getApplicationContext();

        File dir = new File(getMirrorRoot(app), video ? "videos" : "images");

        for (StatusMediaItem item : scraped) {
            String key = stableKey(item);
            Entry existing = registry.get(key);
            if (existing != null) {
                File m = new File(existing.mirrorAbsolutePath);
                if (m.isFile()) {
                    if (existing.video != video) {
                        existing.video = video;
                    }
                    existing.originLastModified = Math.max(existing.originLastModified, item.getLastModified());
                    continue;
                }
            }

            File dest = new File(dir, mirroredFileName(key, video, item));
            try {
                if (item.isFromSaf()) {
                    try (java.io.InputStream in =
                                 app.getContentResolver().openInputStream(item.getUri())) {
                        if (in != null) {
                            FileUtils.copyInputStreamToFile(in, dest);
                        }
                    }
                } else if (item.getFile() != null) {
                    FileUtils.copyFile(item.getFile(), dest);
                }
            } catch (IOException e) {
                if (dest.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    dest.delete();
                }
                continue;
            }

            if (!dest.isFile()) {
                continue;
            }

            Entry en = new Entry();
            en.stableKey = key;
            en.mirrorAbsolutePath = dest.getAbsolutePath();
            en.video = video;
            en.firstSeenMillis = nowMillis;
            en.originLastModified = Math.max(item.getLastModified(), dest.lastModified());

            Entry oldSameKey = registry.put(key, en);

            cleanupOldMirrored(oldSameKey, en);
        }
    }

    private static void cleanupOldMirrored(@Nullable Entry removed, Entry kept) {
        if (removed == null || removed == kept) {
            return;
        }
        if (!Objects.equals(removed.mirrorAbsolutePath, kept.mirrorAbsolutePath)) {
            File f = new File(removed.mirrorAbsolutePath);
            if (f.isFile()) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    private static String mirroredFileName(String key, boolean video, StatusMediaItem hint) {
        int h = Math.abs(key.hashCode());
        String ext = inferExt(hint, video);
        return h + "_" + System.nanoTime() + "." + ext;
    }

    private static String inferExt(StatusMediaItem item, boolean video) {
        String name = item.getName();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.US);
        }
        return video ? "mp4" : "jpg";
    }

    private static void pruneExpired(Map<String, Entry> registry) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = registry.entrySet().iterator();
        while (it.hasNext()) {
            Entry en = it.next().getValue();
            if (now - en.firstSeenMillis > RETENTION_MS) {
                File f = new File(en.mirrorAbsolutePath);
                if (f.isFile()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
                it.remove();
            }
        }
    }

    private static File indexFile(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), INDEX_JSON);
    }

    private static HashMap<String, Entry> loadIndex(Context context) {
        File f = indexFile(context);
        if (!f.isFile()) {
            return new HashMap<>();
        }

        HashMap<String, Entry> map = new HashMap<>();
        try (FileInputStream in = new FileInputStream(f);
             JsonReader jr = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            jr.beginArray();
            while (jr.hasNext()) {
                Entry en = Entry.read(jr);
                if (en != null && en.stableKey != null && en.mirrorAbsolutePath != null) {
                    map.put(en.stableKey, en);
                }
            }
            jr.endArray();
        } catch (IOException e) {
            return new HashMap<>();
        }

        Iterator<Map.Entry<String, Entry>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry en = it.next().getValue();
            if (!(new File(en.mirrorAbsolutePath)).isFile()) {
                it.remove();
            }
        }
        return map;
    }

    private static void persistIndex(Context context, Map<String, Entry> registry) {
        File f = indexFile(context);
        try (FileOutputStream out = new FileOutputStream(f);
             JsonWriter jw = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            jw.beginArray();
            for (Entry en : registry.values()) {
                en.write(jw);
            }
            jw.endArray();
        } catch (IOException ignored) {
        }
    }

    private static final class Entry {
        String stableKey;
        String mirrorAbsolutePath;
        boolean video;
        long firstSeenMillis;
        long originLastModified;

        @Nullable
        static Entry read(JsonReader jr) throws IOException {
            Entry en = new Entry();
            jr.beginObject();
            while (jr.hasNext()) {
                String n = jr.nextName();
                switch (n) {
                    case "k":
                        en.stableKey = jr.nextString();
                        break;
                    case "p":
                        en.mirrorAbsolutePath = jr.nextString();
                        break;
                    case "v":
                        en.video = jr.nextBoolean();
                        break;
                    case "fs":
                        en.firstSeenMillis = jr.nextLong();
                        break;
                    case "ol":
                        en.originLastModified = jr.nextLong();
                        break;
                    default:
                        jr.skipValue();
                        break;
                }
            }
            jr.endObject();
            return en;
        }

        void write(JsonWriter jw) throws IOException {
            jw.beginObject();
            jw.name("k").value(stableKey);
            jw.name("p").value(mirrorAbsolutePath);
            jw.name("v").value(video);
            jw.name("fs").value(firstSeenMillis);
            jw.name("ol").value(originLastModified);
            jw.endObject();
        }
    }
}
