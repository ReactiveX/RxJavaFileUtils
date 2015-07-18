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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class FileSystemEventOnSubscribeTest {
    private static final String testDirPath = "/tmp/testDirPath-" + UUID.randomUUID().toString();

    private static File dir;

    @Before
    public  void setup() {
        dir = new File(testDirPath);
        dir.mkdirs();
    }

    @After
    public  void teardown() throws Exception {
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
    public void testWatchForFileCreateAndModify() throws Exception {
        Map<Path, FileSystemEventKind[]> paths = new HashMap<>();

        paths.put(dir.toPath(), new FileSystemEventKind[] {
            FileSystemEventKind.ENTRY_CREATE,
            FileSystemEventKind.ENTRY_MODIFY } );

        Observable<FileSystemEvent> fileSystemWatcher =
            FileSystemWatcher
                .newBuilder()
                .addPaths(paths)
                .withScheduler(Schedulers.io())
                .build();

        TestSubscriber subscriber = new TestSubscriber();

        CountDownLatch latch = new CountDownLatch(FileSystemWatcher.IS_MAC ? 3 : 2);
        Subscription subscribe = fileSystemWatcher
            .doOnNext(a -> {
                latch.countDown();
                FileSystemEventKind kind = a.getFileSystemEventKind();

                System.out.println("Got an event for " + kind.name());

            })
            .subscribe(subscriber);

        boolean closed = subscribe.isUnsubscribed();

        Assert.assertFalse(closed);

        File file = new File(dir, "testFile" + System.currentTimeMillis());
        file.createNewFile();

        Thread.sleep(3000);

        FileWriter writer = new FileWriter(file, true);
        writer.write(1);
        writer.flush();
        writer.close();

        latch.await();

        subscribe.unsubscribe();

        closed = subscribe.isUnsubscribed();

        Assert.assertTrue(closed);

        if (FileSystemWatcher.IS_MAC) {
            subscriber.assertValueCount(3);
        } else {
            subscriber.assertValueCount(2);
        }

    }
}