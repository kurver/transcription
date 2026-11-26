package com.example.transcription.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalDiskFileStorage implements FileStorage {
    private final Path root;

    public LocalDiskFileStorage(@Value("${app.storage.root:./data/uploads}") String root) throws IOException {
        this.root = Path.of(root).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    @Override
    public String store(InputStream in) throws IOException {
        String key = UUID.randomUUID().toString(); // original filename is never used on disk
        Files.copy(in, root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public byte[] load(String key) throws IOException {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) throw new IOException("Invalid storage key");
        return Files.readAllBytes(p);
    }
}
