package com.barbarysoftware.watchservice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.UUID;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class Demo {

    public static void main(String[] args) throws IOException, InterruptedException {

        final WatchService watcher = MacOSXWatchServiceFactory.newWatchService();

        final String home = System.getProperty("user.home");
        final String testDirPath = "/tmp/testDirPath-" + UUID.randomUUID().toString();

        File dir;
        dir = new File(testDirPath);
        dir.mkdirs();


        final WatchableFile file1 = new WatchableFile(dir.toPath());
        final WatchableFile file2 = new WatchableFile(new File(home + "/Documents").toPath());

        file1.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        file2.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        final Thread consumer = new Thread(createRunnable(watcher));
        consumer.start();

        String fileName=   "testFile" + System.currentTimeMillis();
        Thread t = new Thread(() -> {

            try {
                System.out.println("Creating a file");
                File f = new File(dir,fileName);
                f.createNewFile();

                Thread.sleep(1500);

                System.out.println("Writing to a file");
                FileWriter writer = new FileWriter(f.getPath());
                writer.write("hello");
                writer.flush();
                writer.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
        t.setDaemon(true);
        t.start();

        t.join();
        Thread.sleep(1500);
        System.exit(0);

    }

    private static Runnable createRunnable(final WatchService watcher) {
        return new Runnable() {
            public void run() {
                for (; ;) {

                    // wait for key to be signaled
                    WatchKey key;
                    try {
                        key = watcher.take();
                    } catch (InterruptedException x) {
                        return;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == OVERFLOW) {
                            continue;
                        }
                        // The filename is the context of the event.
                        @SuppressWarnings({"unchecked"}) WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        System.out.printf("Detected file system event: %s at %s%n", kind, ev.context());

                    }

                    // Reset the key -- this step is critical to receive further watch events.

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }

                }
            }
        };
    }
}
