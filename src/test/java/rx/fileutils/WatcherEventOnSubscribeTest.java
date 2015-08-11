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
import rx.observers.TestSubscriber;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class WatcherEventOnSubscribeTest {
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
        HashMap<Path, WatchEvent.Kind[]> paths = new HashMap<>();

        paths.put(dir.toPath(), new WatchEvent.Kind[] {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY } );

        WatcherEventObservable watcherEventObservable = WatcherEventObservable.create(paths);

        TestSubscriber subscriber = new TestSubscriber();

        CountDownLatch latch = new CountDownLatch(WatcherEventObservable.IS_MAC ? 3 : 2);
        watcherEventObservable
            .doOnNext(a -> {
                latch.countDown();
                WatchEvent.Kind<?> kind = a.kind();

                System.out.println("Got an event for " + kind.name());

            })
            .subscribe(subscriber);

        boolean closed = watcherEventObservable.isClosed();

        Assert.assertFalse(closed);

        File file = new File(dir, "testFile" + System.currentTimeMillis());
        file.createNewFile();

        Thread.sleep(3000);

        FileWriter writer = new FileWriter(file, true);
        writer.write(1);
        writer.flush();
        writer.close();

        latch.await();

        watcherEventObservable.close();

        closed = watcherEventObservable.isClosed();

        Assert.assertTrue(closed);

        if (WatcherEventObservable.IS_MAC) {
            subscriber.assertValueCount(3);
        } else {
            subscriber.assertValueCount(2);
        }

    }
}