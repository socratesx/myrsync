package com.linminitools.myrsync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * Stores SSH passwords outside the plain {@code configs} preferences.
 * Values are encrypted with a Keystore-backed AES key via
 * {@link EncryptedSharedPreferences}.
 */
final class SshSecretStore {

    static final String PREFS_NAME = "ssh_secrets";
    private static final String TAG = "SshSecretStore";
    private static final String KEY_PREFIX = "rs_ssh_password_";
    private static final String LEGACY_CONFIGS = "configs";

    private static final Object LOCK = new Object();
    @Nullable
    private static SharedPreferences cachedPrefs;

    private SshSecretStore() {
    }

    static void putPassword(@NonNull Context context, int configId, @Nullable String password) {
        SharedPreferences prefs = secretsPrefs(context);
        String key = KEY_PREFIX + configId;
        if (password == null || password.isEmpty()) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putString(key, password).apply();
        }
        // Ensure plaintext never remains in configs.xml.
        wipeLegacyPlaintext(context, configId);
    }

    @NonNull
    static String getPassword(@NonNull Context context, int configId) {
        SharedPreferences prefs = secretsPrefs(context);
        String key = KEY_PREFIX + configId;
        String stored = prefs.getString(key, "");
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        // One-shot migration from older plaintext configs.xml entries.
        SharedPreferences configs = context.getApplicationContext()
                .getSharedPreferences(LEGACY_CONFIGS, Context.MODE_PRIVATE);
        String legacy = configs.getString(key, "");
        if (legacy != null && !legacy.isEmpty()) {
            prefs.edit().putString(key, legacy).apply();
            configs.edit().remove(key).apply();
            return legacy;
        }
        return "";
    }

    static void removePassword(@NonNull Context context, int configId) {
        secretsPrefs(context).edit().remove(KEY_PREFIX + configId).apply();
        wipeLegacyPlaintext(context, configId);
    }

    /**
     * Move any leftover plaintext passwords from {@code configs} into the secret store.
     * Safe to call on every startup.
     */
    static void migrateAllLegacy(@NonNull Context context) {
        SharedPreferences configs = context.getApplicationContext()
                .getSharedPreferences(LEGACY_CONFIGS, Context.MODE_PRIVATE);
        Map<String, ?> all = configs.getAll();
        if (all == null || all.isEmpty()) {
            return;
        }
        SharedPreferences.Editor legacyEdit = configs.edit();
        boolean changed = false;
        for (String key : all.keySet()) {
            if (key == null || !key.startsWith(KEY_PREFIX)) {
                continue;
            }
            Object value = all.get(key);
            if (!(value instanceof String password)) {
                legacyEdit.remove(key);
                changed = true;
                continue;
            }
            String idPart = key.substring(KEY_PREFIX.length());
            try {
                int configId = Integer.parseInt(idPart);
                if (!password.isEmpty()) {
                    secretsPrefs(context).edit().putString(key, password).apply();
                }
                legacyEdit.remove(key);
                changed = true;
                Log.i(TAG, "Migrated plaintext SSH password for config " + configId);
            } catch (NumberFormatException ignored) {
                legacyEdit.remove(key);
                changed = true;
            }
        }
        if (changed) {
            legacyEdit.apply();
        }
    }

    private static void wipeLegacyPlaintext(@NonNull Context context, int configId) {
        context.getApplicationContext()
                .getSharedPreferences(LEGACY_CONFIGS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PREFIX + configId)
                .apply();
    }

    @NonNull
    private static SharedPreferences secretsPrefs(@NonNull Context context) {
        synchronized (LOCK) {
            if (cachedPrefs != null) {
                return cachedPrefs;
            }
            Context app = context.getApplicationContext();
            try {
                cachedPrefs = createEncryptedPrefs(app);
                return cachedPrefs;
            } catch (GeneralSecurityException | IOException e) {
                // Last-resort private prefs (still app-sandbox + backup-excluded).
                Log.e(TAG, "EncryptedSharedPreferences unavailable; falling back to private prefs", e);
                cachedPrefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return cachedPrefs;
            }
        }
    }

    /**
     * Same keystore alias and AES schemes as {@code MasterKeys.AES256_GCM_SPEC}, so
     * passwords already in {@code ssh_secrets} still decrypt.
     */
    @NonNull
    private static SharedPreferences createEncryptedPrefs(@NonNull Context app)
            throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                app,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
