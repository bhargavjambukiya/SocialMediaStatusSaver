package com.studio.statusvault.utils;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight pub/sub so viewers can signal that saved-library lists changed without static adapter refs.
 */
public final class MediaLibraryEvents {

    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    private MediaLibraryEvents() {}

    public static void addListener(Runnable runnable) {
        if (runnable != null) {
            listeners.add(runnable);
        }
    }

    public static void removeListener(Runnable runnable) {
        listeners.remove(runnable);
    }

    public static void notifySavedLibraryChanged() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}
