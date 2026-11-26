package com.example.transcription.storage;

import java.io.IOException;
import java.io.InputStream;

/** Abstraction over file storage; swap for Azure Blob in production. */
public interface FileStorage {
    /** Stores the content and returns an opaque key. */
    String store(InputStream in) throws IOException;

    byte[] load(String key) throws IOException;
}
