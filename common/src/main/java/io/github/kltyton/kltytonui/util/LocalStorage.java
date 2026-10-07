package io.github.kltyton.kltytonui.util;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.spi.KuiServices;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Persists Web Storage values through the target's mapped NBT API. */
public class LocalStorage extends Storage {
    private static volatile File localStorageFilePath;

    public void save() {
        File storageFile = resolveStorageFile();
        if (storageFile == null) return;
        try {
            Path file = storageFile.toPath();
            Files.createDirectories(file.getParent());
            KuiServices.client().writeLocalStorage(file, data);
        } catch (IOException | RuntimeException failure) {
            KltytonUI.LOGGER.error("Failed to save LocalStorage data to {}", storageFile, failure);
        }
    }

    public void load() {
        File storageFile = resolveStorageFile();
        if (storageFile == null || !storageFile.isFile()) return;
        try {
            Map<String, String> loaded = KuiServices.client().readLocalStorage(storageFile.toPath());
            data.clear();
            data.putAll(loaded);
        } catch (IOException | RuntimeException failure) {
            KltytonUI.LOGGER.error("Failed to load LocalStorage data from {}", storageFile, failure);
        }
    }

    private static File resolveStorageFile() {
        File cached = localStorageFilePath;
        if (cached != null) return cached;
        synchronized (LocalStorage.class) {
            if (localStorageFilePath != null) return localStorageFilePath;
            Path configDir = KuiServices.client().getConfigDirectory();
            if (configDir == null) return null;
            localStorageFilePath = configDir.resolve(KltytonUI.MODID).resolve("localStorage.nbt").toFile();
            return localStorageFilePath;
        }
    }

    public static File getStorageFile() {
        return resolveStorageFile();
    }

    @Override
    public void setItem(String key, String value) {
        if (key == null || key.isBlank()) return;
        super.setItem(key, value);
        save();
    }

    @Override
    public void removeItem(String key) {
        if (key == null || key.isBlank()) return;
        super.removeItem(key);
        save();
    }

    @Override
    public void clear() {
        super.clear();
        save();
    }
}
