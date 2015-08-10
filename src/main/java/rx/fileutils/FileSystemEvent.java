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

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

public class FileSystemEvent {
    private FileSystemEventKind fileSystemEventKind;

    private Path path;

    FileSystemEvent(WatchEvent event) {
        WatchEvent.Kind kind = event.kind();
        if (StandardWatchEventKinds.OVERFLOW != kind) {
            this.path = (Path) event.context();
        }

        this.fileSystemEventKind = FileSystemEventKind.toFileSystemEventKind(kind);
    }

    public FileSystemEventKind getFileSystemEventKind() {
        return fileSystemEventKind;
    }

    public Path getPath() {
        return path;
    }
}
