package com.studio.statusvault.locale;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Persists app language and syncs with {@link AppCompatDelegate#setApplicationLocales}.
 * <p>
 * A marker under {@link Context#getNoBackupFilesDir()} is not restored by Auto Backup; together
 * with a local install id file it distinguishes this install from restored {@link #PREFS} data.
 */
public final class AppLocaleHelper {

    private static final String PREFS = "reel_vault_app_prefs";
    private static final String KEY_LANG = "selected_language_tag";
    private static final String KEY_CHOSEN = "language_chosen_by_user";

    private static final String LANG_COMMIT_FILENAME = "language_user_commit";
    private static final String INSTALL_SIG_FILENAME = "lang_install_first_ms.txt";

    /**
     * If first/last update times are this close, the package was never updated via the store on top of
     * the original install — used with missing install-signature to treat prefs as restored after reinstall.
     */
    private static final long TIGHT_FIRST_LAST_UPDATE_MS = 120_000L;

    private AppLocaleHelper() {
    }

    /**
     * Call early from {@link Application#onCreate()} before {@link #applyPersistedLocale(Application)}.
     */
    public static void reconcileLanguageStateAfterPossibleRestore(Context context) {
        Context app = context.getApplicationContext();
        File marker = langCommitMarkerFile(app);
        if (marker.exists()) {
            return;
        }
        SharedPreferences prefs = getPrefs(app);
        if (!prefs.getBoolean(KEY_CHOSEN, false)) {
            return;
        }
        long firstInstall;
        long lastUpdate;
        try {
            PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            firstInstall = pi.firstInstallTime;
            lastUpdate = pi.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        boolean tightFirstLast = Math.abs(lastUpdate - firstInstall) < TIGHT_FIRST_LAST_UPDATE_MS;
        String sig = readInstallSig(app);

        if (sig == null) {
            if (tightFirstLast) {
                scrubLanguagePrefs(app, prefs);
            } else {
                touchLangCommitMarker(app);
                writeInstallSig(app, firstInstall);
            }
            return;
        }
        if (!Long.toString(firstInstall).equals(sig.trim())) {
            scrubLanguagePrefs(app, prefs);
            return;
        }
        touchLangCommitMarker(app);
    }

    private static void scrubLanguagePrefs(Context app, SharedPreferences prefs) {
        prefs.edit().remove(KEY_CHOSEN).remove(KEY_LANG).apply();
        deleteQuietly(installSigFile(app));
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
    }

    private static void deleteQuietly(File f) {
        if (f != null && f.exists() && !f.delete()) {
            // ignore
        }
    }

    private static File installSigFile(Context app) {
        return new File(app.getNoBackupFilesDir(), INSTALL_SIG_FILENAME);
    }

    private static void writeInstallSig(Context app, long firstInstallTime) {
        File f = installSigFile(app);
        try (FileWriter w = new FileWriter(f)) {
            w.write(Long.toString(firstInstallTime));
        } catch (IOException ignored) {
        }
    }

    private static String readInstallSig(Context app) {
        File f = installSigFile(app);
        if (!f.exists()) {
            return null;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            return r.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    public static void applyPersistedLocale(Application app) {
        String tag = getSavedLanguageTag(app);
        if (tag == null || tag.isEmpty()) {
            return;
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
    }

    public static boolean hasUserSelectedLanguage(Context context) {
        Context app = context.getApplicationContext();
        return langCommitMarkerFile(app).exists()
                && getPrefs(app).getBoolean(KEY_CHOSEN, false);
    }

    public static String getSavedLanguageTag(Context context) {
        String t = getPrefs(context).getString(KEY_LANG, "");
        return t != null ? t : "";
    }

    public static void saveLanguage(Context context, String languageTag) {
        Context app = context.getApplicationContext();
        getPrefs(app).edit()
                .putString(KEY_LANG, languageTag)
                .putBoolean(KEY_CHOSEN, true)
                .apply();
        touchLangCommitMarker(app);
        try {
            long first = app.getPackageManager().getPackageInfo(app.getPackageName(), 0).firstInstallTime;
            writeInstallSig(app, first);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
    }

    private static File langCommitMarkerFile(Context app) {
        return new File(app.getNoBackupFilesDir(), LANG_COMMIT_FILENAME);
    }

    private static void touchLangCommitMarker(Context app) {
        File dir = app.getNoBackupFilesDir();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            // still try createNewFile below
        }
        File marker = langCommitMarkerFile(app);
        try {
            if (!marker.exists()) {
                marker.createNewFile();
            }
        } catch (IOException ignored) {
        }
    }

    private static SharedPreferences getPrefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
