/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package rx.fileutils;

import com.barbarysoftware.watchservice.MacOSXWatchServiceFactory;
import com.barbarysoftware.watchservice.WatchableFile;
import com.sun.nio.file.SensitivityWatchEventModifier;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public final class FileSystemWatcher {

    static final boolean IS_MAC;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        IS_MAC = os.contains("mac");
    }

    private FileSystemWatcher() {}

    private static class FileSystemEventOnSubscribe implements Observable.OnSubscribe<FileSystemEvent> {
        private WatchService watcher;
        private Scheduler scheduler;
        private volatile boolean close = false;
        private final Set<Path> watchedPaths;

        public FileSystemEventOnSubscribe(
            Map<Path, FileSystemEventKind[]> paths,
            Scheduler scheduler,
            boolean scanCurrentFs
        ) {
            watchedPaths = new HashSet<Path>();
            if (scanCurrentFs) {
                paths.forEach((path, kind) -> {
                    if (Arrays.asList(kind).contains(FileSystemEventKind.ENTRY_CREATE)) {
                        watchedPaths.add(path);
                    }
                });
            }

            try {
                if (IS_MAC) {
                    this.watcher = MacOSXWatchServiceFactory.newWatchService();

                    for (Path path : paths.keySet()) {
                        final WatchableFile watchableFile = new WatchableFile(path);
                        FileSystemEventKind[] kinds = paths.get(path);
                        watchableFile.register(watcher, FileSystemEventKind.toWatchEventKinds(kinds));
                    }
                }
                else {
                    this.watcher = FileSystems.getDefault().newWatchService();

                    for (Path path : paths.keySet()) {
                        FileSystemEventKind[] kinds = paths.get(path);
                        path.register(watcher, FileSystemEventKind.toWatchEventKinds(kinds), SensitivityWatchEventModifier.HIGH);
                    }
                }
                this.scheduler = scheduler;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public FileSystemEventOnSubscribe(Map<Path, FileSystemEventKind[]> paths, Scheduler scheduler) {
            this(paths, scheduler, false);
        }

        @Override
        public void call(Subscriber<? super FileSystemEvent> subscriber) {
            // scan the watchedPaths and trigger fake ENTRY_CREATE events
            watchedPaths.forEach(path -> {
                getEventsForCurrentFiles(path).forEach(event -> {
                    FileSystemEvent fileSystemEvent = new FileSystemEvent(event);
                    subscriber.onNext(fileSystemEvent);
                });
            });

            Scheduler.Worker worker = scheduler.createWorker();
            subscriber.add(worker);

            worker.schedule(() -> {
                do {
                    try {
                        WatchKey key = watcher.take();
                        if (key == null) {
                            continue;
                        }

                        for (WatchEvent<?> event : key.pollEvents()) {
                            FileSystemEvent fileSystemEvent = new FileSystemEvent(event);
                            subscriber.onNext(fileSystemEvent);
                        }

                        if (!key.reset()) {
                            close();
                        }
                    } catch (Throwable t) {
                        subscriber.onError(t);
                    }
                } while (!close);

                subscriber.onCompleted();
            });
        }

        public void close() {
            this.close = true;

            try {
                watcher.close();
            } catch (Exception e) {}
        }

        /**
         * Return fake ENTRY_CREATE events for the current files.
         * This simplify the code by treating current files the same way as new files.
         */
        private List<WatchEvent<Path>> getEventsForCurrentFiles(Path directory) {
            final List<WatchEvent<Path>> events = new ArrayList<>();
            try {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                        events.add(pathToWatchEvent(path));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
                        events.add(pathToWatchEvent(path));
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            return events;
        }

        private WatchEvent<Path> pathToWatchEvent(Path path) {
            return new WatchEvent<Path>() {
                @Override
                public Kind<Path> kind() {
                    return ENTRY_CREATE;
                }

                @Override
                public int count() {
                    return 1;
                }

                @Override
                public Path context() {
                    return path;
                }
            };
        }
    }

    public static class Builder {
        private Map<Path, FileSystemEventKind[]> paths = new HashMap<>();

        private Scheduler scheduler = Schedulers.newThread();

        private boolean scanCurrentFS = false;

        Builder() {}

        public Builder addPath(Path path, FileSystemEventKind... kinds) {
            paths.put(path, kinds);

            return this;
        }

        public Builder addPaths(Map<Path, FileSystemEventKind[]> paths) {
            this.paths.putAll(paths);

            return this;
        }

        public Builder withScheduler(Scheduler scheduler) {
            this.scheduler = scheduler;

            return this;
        }

        public Builder withCurrentFsScanning(boolean enable) {
            this.scanCurrentFS = enable;

            return this;
        }

        public Observable<FileSystemEvent> build() {
            try {
                FileSystemEventOnSubscribe fileSystemEventOnSubscribe
                    = new FileSystemEventOnSubscribe(paths, scheduler, scanCurrentFS);

                Observable<FileSystemEvent> fileSystemEventObservable
                    = Observable.create(fileSystemEventOnSubscribe);

                fileSystemEventObservable
                    .doOnUnsubscribe(fileSystemEventOnSubscribe::close);

                return fileSystemEventObservable;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
