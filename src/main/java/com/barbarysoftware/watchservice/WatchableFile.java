package com.barbarysoftware.watchservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;

public class WatchableFile implements Watchable {

    private final File file;

    public WatchableFile(File file) {
        if (file == null) {
            throw new NullPointerException("file must not be null");
        }
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public WatchKey register(WatchService watcher,
                             WatchEvent.Kind<?>[] events,
                             WatchEvent.Modifier... modifiers)
            throws IOException {
        if (watcher == null)
            throw new NullPointerException();
        if (!(watcher instanceof AbstractWatchService))
            throw new ProviderMismatchException();
        return ((AbstractWatchService) watcher).register(this, events, modifiers);
    }

    private static final WatchEvent.Modifier[] NO_MODIFIERS = new WatchEvent.Modifier[0];

    @Override
    public final WatchKey register(WatchService watcher,
                                   WatchEvent.Kind<?>... events)
            throws IOException {
        return register(watcher, events, NO_MODIFIERS);
    }

    @Override
    public String toString() {
        return "Path{" +
                "file=" + file +
                '}';
    }
}
