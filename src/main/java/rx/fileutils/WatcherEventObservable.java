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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Created by rroeser on 7/8/15.
 */
public class WatcherEventObservable extends Observable<WatchEvent<?>> {

    static final boolean IS_MAC;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        IS_MAC = os.contains("mac");
    }

    protected WatcherEventOnSubscribe watcherEventOnSubscribe;

    private static class WatcherEventOnSubscribe implements OnSubscribe<WatchEvent<?>> {
        private WatchService watcher;

        private Scheduler scheduler;

        private volatile boolean close = false;

        public void addPath(Path path, WatchEvent.Kind... kinds) throws Exception {
            if (IS_MAC) {
                final WatchableFile watchableFile = new WatchableFile(path);
                watchableFile.register(watcher, kinds);
            } else {
                path.register(watcher, kinds, SensitivityWatchEventModifier.HIGH);
            }
        }

        public WatcherEventOnSubscribe(Map<Path, WatchEvent.Kind[]> paths, Scheduler scheduler) {
            try {
                if (IS_MAC) {
                    this.watcher = MacOSXWatchServiceFactory.newWatchService();

                    for (Path path : paths.keySet()) {
                        final WatchableFile watchableFile = new WatchableFile(path);
                        watchableFile.register(watcher, paths.get(path));
                    }
                }
                else {
                    this.watcher = FileSystems.getDefault().newWatchService();

                    for (Path path : paths.keySet()) {
                        path.register(watcher, paths.get(path), SensitivityWatchEventModifier.HIGH);
                    }
                }
                this.scheduler = scheduler;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void call(Subscriber<? super WatchEvent<?>> subscriber) {

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
                            WatchEvent.Kind<?> kind = event.kind();

                            if (kind == OVERFLOW) {
                                continue;
                            } else {
                                subscriber.onNext(event);
                            }
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

        public boolean isClosed() {
            return close;
        }
    }

    protected WatcherEventObservable(WatcherEventOnSubscribe subscribe) {
        super(subscribe);

        this.watcherEventOnSubscribe = subscribe;
    }

    public static WatcherEventObservable create(Map<Path, WatchEvent.Kind[]> paths, Scheduler scheduler) {
        try {
            WatcherEventOnSubscribe watcherEventOnSubscribe
                = new WatcherEventOnSubscribe(paths, scheduler);

            WatcherEventObservable watcherEventObservable
                = new WatcherEventObservable(watcherEventOnSubscribe);

            watcherEventObservable
                .doOnSubscribe(() -> {
                })
                .doOnUnsubscribe(watcherEventOnSubscribe::close);

            return watcherEventObservable;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static WatcherEventObservable create(Map<Path, WatchEvent.Kind[]> paths) {
        return create(paths, Schedulers.newThread());
    }

    public static WatcherEventObservable create(Path path, WatchEvent.Kind... kinds) {
        Map<Path, WatchEvent.Kind[]> paths = new HashMap<>();

        paths.put(path, kinds);

        return create(paths);
    }

    public WatcherEventObservable addPath(Path path, WatchEvent.Kind... kinds) {
        try {
            watcherEventOnSubscribe.addPath(path, kinds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return this;
    }


    public void close() {
        watcherEventOnSubscribe.close();
    }

    public boolean isClosed() {
        return watcherEventOnSubscribe.isClosed();
    }
}
