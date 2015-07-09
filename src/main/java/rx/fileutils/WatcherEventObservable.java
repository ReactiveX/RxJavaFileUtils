package rx.fileutils;

import com.sun.nio.file.SensitivityWatchEventModifier;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.lang.Exception;import java.lang.Override;import java.lang.RuntimeException;import java.lang.System;import java.lang.Throwable;import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Created by rroeser on 7/8/15.
 */
public class WatcherEventObservable extends Observable<WatchEvent<?>> {

    protected WatcherEventOnSubscribe watcherEventOnSubscribe;

    private static class WatcherEventOnSubscribe implements OnSubscribe<WatchEvent<?>> {
        private WatchService watcher;

        private Scheduler scheduler;

        private volatile boolean close = false;

        public WatcherEventOnSubscribe(Map<Path, WatchEvent.Kind[]> paths, Scheduler scheduler) {
            try {
                this.watcher = FileSystems.getDefault().newWatchService();

                for (Path path : paths.keySet()) {
                    path.register(watcher, paths.get(path), SensitivityWatchEventModifier.HIGH);
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

                            System.out.println("Get event => " + kind.name());

                            if (kind == OVERFLOW) {
                                continue;
                            } else {
                                subscriber.onNext(event);
                            }
                        }

                        if (!key.reset()) {
                            System.out.println("Didn't reset to closing loop");
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

            watcherEventObservable.doOnUnsubscribe(watcherEventOnSubscribe::close);

            return watcherEventObservable;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static WatcherEventObservable create(Map<Path, WatchEvent.Kind[]> paths) {
        return create(paths, Schedulers.newThread());
    }

    public void close() {
        watcherEventOnSubscribe.close();
    }

    public boolean isClosed() {
        return watcherEventOnSubscribe.isClosed();
    }
}
