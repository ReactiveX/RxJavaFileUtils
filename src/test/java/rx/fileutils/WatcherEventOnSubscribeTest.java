package rx.fileutils;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

public class WatcherEventOnSubscribeTest {
    private static final String testDirPath = "/tmp/testDirPath-" + UUID.randomUUID().toString();

    private static File dir;

    @BeforeClass
    public static void setup() {
        dir = new File(testDirPath);
        dir.mkdirs();
    }

    @AfterClass
    public static void teardown() throws Exception {
        deleteRecursive(dir);
    }

    public static void deleteRecursive(File path){
        File[] c = path.listFiles();
        System.out.println("Cleaning out folder:" + path.toString());
        for (File file : c){
            if (file.isDirectory()){
                System.out.println("Deleting file:" + file.toString());
                deleteRecursive(file);
                file.delete();
            } else {
                file.delete();
            }
        }

        path.delete();
    }

    @Test(timeout = 15_000)
    public void testWatchForFileCreate() throws Exception {
        HashMap<Path, WatchEvent.Kind[]> paths = new HashMap<>();

        paths.put(dir.toPath(), new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE } );

        WatcherEventObservable watcherEventObservable = WatcherEventObservable.create(paths);

        TestSubscriber subscriber = new TestSubscriber();

        CountDownLatch latch = new CountDownLatch(1);

        watcherEventObservable
            .doOnNext(a -> {
                try {
                    latch.countDown();
                } catch (Throwable t) {}

                WatchEvent.Kind<?> kind = a.kind();

                System.out.println("Got an event for " + kind.name());

            })
            .count()
            .subscribe(subscriber);

        boolean closed = watcherEventObservable.isClosed();

        Assert.assertFalse(closed);

        File file = new File(dir, "testFile" + System.currentTimeMillis());
        file.createNewFile();

        latch.await();

        watcherEventObservable.close();

        closed = watcherEventObservable.isClosed();

        Assert.assertTrue(closed);

        subscriber.assertValue(1);
    }

    @Test(timeout = 15_000)
    public void testWatchForFiledModify() throws Exception {
        File file = new File(dir, "testFile" + System.currentTimeMillis());
        file.createNewFile();


        HashMap<Path, WatchEvent.Kind[]> paths = new HashMap<>();

        paths.put(dir.toPath(), new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_MODIFY } );

        WatcherEventObservable watcherEventObservable = WatcherEventObservable.create(paths);

        TestSubscriber subscriber = new TestSubscriber();

        CountDownLatch latch = new CountDownLatch(1);

        watcherEventObservable
            .doOnNext(a -> {
                try {
                    latch.countDown();
                } catch (Throwable t) {
                }

                WatchEvent.Kind<?> kind = a.kind();

                System.out.println("Got an event for " + kind.name());

            })
            .count()
            .subscribe(subscriber);

        boolean closed = watcherEventObservable.isClosed();

        Assert.assertFalse(closed);

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("hello");
        fileWriter.flush();
        fileWriter.close();

        latch.await();

        watcherEventObservable.close();

        closed = watcherEventObservable.isClosed();

        Assert.assertTrue(closed);

        subscriber.assertValue(1);
    }

    @Test(timeout = 5_000)
    public void testWatchForFileCreateAndModify() throws Exception {
        HashMap<Path, WatchEvent.Kind[]> paths = new HashMap<>();

        paths.put(dir.toPath(), new WatchEvent.Kind[] {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY } );

        WatcherEventObservable watcherEventObservable = WatcherEventObservable.create(paths);

        TestSubscriber subscriber = new TestSubscriber();

        LongAdder count = new LongAdder();

        watcherEventObservable
            .doOnNext(a -> {
                count.increment();
                WatchEvent.Kind<?> kind = a.kind();

                System.out.println("Got an event for " + kind.name());

            })
            .count()
            .subscribe(subscriber);

        boolean closed = watcherEventObservable.isClosed();

        Assert.assertFalse(closed);

        File file = new File(dir, "testFile" + System.currentTimeMillis());
        file.createNewFile();

        FileWriter writer = new FileWriter(file, true);

        for (int i = 0; i < 100_000; i++) {
            writer.write(i);

            writer.flush();
        }

        writer.close();


        while (count.sum() < 2) {}

        watcherEventObservable.close();

        closed = watcherEventObservable.isClosed();

        Assert.assertTrue(closed);

        subscriber.assertValue(2);
    }
}