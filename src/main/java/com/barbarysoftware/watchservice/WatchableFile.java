/*
 * Forked from: https://github.com/gjoseph/BarbaryWatchService
 * Waiting to see what the license is - https://github.com/gjoseph/BarbaryWatchService/issues/6
 */
package com.barbarysoftware.watchservice;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class WatchableFile implements Path {

    private final Path file;

    public WatchableFile(Path file) {
        if (file == null) {
            throw new NullPointerException("file must not be null");
        }
        this.file = file;
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
    public Iterator<Path> iterator() {
        return file.iterator();
    }

    @Override
    public int compareTo(Path other) {
        return file.compareTo(other);
    }

    @Override
    public boolean equals(Object other) {
        return file.equals(other);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public void forEach(Consumer<? super Path> action) {
        file.forEach(action);
    }

    @Override
    public Spliterator<Path> spliterator() {
        return file.spliterator();
    }

    @Override
    public FileSystem getFileSystem() {
        return file.getFileSystem();
    }

    @Override
    public boolean isAbsolute() {
        return file.isAbsolute();
    }

    @Override
    public Path getRoot() {
        return file.getRoot();
    }

    @Override
    public Path getFileName() {
        return file.getFileName();
    }

    @Override
    public Path getParent() {
        return file.getParent();
    }

    @Override
    public int getNameCount() {
        return file.getNameCount();
    }

    @Override
    public Path getName(int index) {
        return file.getName(index);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return file.subpath(beginIndex, endIndex);
    }

    @Override
    public boolean startsWith(Path other) {
        return file.startsWith(other);
    }

    @Override
    public boolean startsWith(String other) {
        return file.startsWith(other);
    }

    @Override
    public boolean endsWith(Path other) {
        return file.endsWith(other);
    }

    @Override
    public boolean endsWith(String other) {
        return file.endsWith(other);
    }

    @Override
    public Path normalize() {
        return file.normalize();
    }

    @Override
    public Path resolve(Path other) {
        return file.resolve(other);
    }

    @Override
    public Path resolve(String other) {
        return file.resolve(other);
    }

    @Override
    public Path resolveSibling(Path other) {
        return file.resolveSibling(other);
    }

    @Override
    public Path resolveSibling(String other) {
        return file.resolveSibling(other);
    }

    @Override
    public Path relativize(Path other) {
        return file.relativize(other);
    }

    @Override
    public URI toUri() {
        return file.toUri();
    }

    @Override
    public Path toAbsolutePath() {
        return file.toAbsolutePath();
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return file.toRealPath(options);
    }

    @Override
    public File toFile() {
        return file.toFile();
    }
}
