/*
 * Forked from: https://github.com/gjoseph/BarbaryWatchService
 * Waiting to see what the license is - https://github.com/gjoseph/BarbaryWatchService/issues/6
 */
package com.barbarysoftware.watchservice;

import java.nio.file.WatchService;

public final class MacOSXWatchServiceFactory {
    private MacOSXWatchServiceFactory() {}

    public static WatchService newWatchService() {
        return new MacOSXListeningWatchService();
    }
}
