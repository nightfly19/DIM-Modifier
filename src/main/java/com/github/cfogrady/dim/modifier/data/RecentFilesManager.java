package com.github.cfogrady.dim.modifier.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class RecentFilesManager {
    private static final String RECENT_FILES_KEY = "recent_files";
    private static final int MAX_RECENT_FILES = 10;
    private final Preferences preferences;

    public RecentFilesManager() {
        this.preferences = Preferences.userNodeForPackage(RecentFilesManager.class);
    }

    public void addFile(File file) {
        List<File> files = getRecentFiles();
        files.remove(file); // Remove if already exists to move it to the top
        files.add(0, file);

        if (files.size() > MAX_RECENT_FILES) {
            files = files.subList(0, MAX_RECENT_FILES);
        }

        saveRecentFiles(files);
    }

    public List<File> getRecentFiles() {
        String recentFilesString = preferences.get(RECENT_FILES_KEY, "");
        if (recentFilesString.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(recentFilesString.split(File.pathSeparator))
                .map(File::new)
                .filter(File::exists)
                .collect(Collectors.toList());
    }

    private void saveRecentFiles(List<File> files) {
        String recentFilesString = files.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));
        preferences.put(RECENT_FILES_KEY, recentFilesString);
    }
}
