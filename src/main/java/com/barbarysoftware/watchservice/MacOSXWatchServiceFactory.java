package com.barbarysoftware.watchservice;

import java.nio.file.WatchService;

public final class MacOSXWatchServiceFactory {
    private MacOSXWatchServiceFactory() {}

    public static WatchService newWatchService() {
        return new MacOSXListeningWatchService();
    }
}
