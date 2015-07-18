package rx.fileutils;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by rroeser on 7/17/15.
 */
public enum FileSystemEventKind {
    OVERFLOW,
    ENTRY_CREATE,
    ENTRY_DELETE,
    ENTRY_MODIFY;

    static WatchEvent.Kind<?> toWatchEventKind(FileSystemEventKind fileSystemEventKind) {
        WatchEvent.Kind kind = null;

        switch (fileSystemEventKind) {
            case OVERFLOW:
                kind = StandardWatchEventKinds.OVERFLOW;
                break;
            case ENTRY_CREATE:
                kind = StandardWatchEventKinds.ENTRY_CREATE;
                break;
            case ENTRY_DELETE:
                kind = StandardWatchEventKinds.ENTRY_DELETE;
                break;
            case ENTRY_MODIFY:
                kind = StandardWatchEventKinds.ENTRY_MODIFY;
                break;
        }

        return kind;
    }

    static WatchEvent.Kind<?>[] toWatchEventKinds(FileSystemEventKind... fileSystemEventKinds) {
        return Arrays
            .asList(fileSystemEventKinds)
            .stream()
            .map(FileSystemEventKind::toWatchEventKind)
            .collect(Collectors.toList())
            .toArray(new WatchEvent.Kind<?>[fileSystemEventKinds.length]);
    }

    static FileSystemEventKind toFileSystemEventKind(WatchEvent.Kind<?> watchEventKind) {
        FileSystemEventKind kind = null;

        if (watchEventKind == StandardWatchEventKinds.OVERFLOW) {
            kind = OVERFLOW;
        } else if (watchEventKind == StandardWatchEventKinds.ENTRY_CREATE) {
            kind = ENTRY_CREATE;
        } else if (watchEventKind == StandardWatchEventKinds.ENTRY_DELETE) {
            kind = ENTRY_DELETE;
        } else if (watchEventKind == StandardWatchEventKinds.ENTRY_MODIFY) {
            kind = ENTRY_MODIFY;
        }

        return kind;
    }

}
