package com.example.transcription.service;

public final class Errors {
    private Errors() {}
    public static class NotFound extends RuntimeException { public NotFound(String m) { super(m); } }
    public static class Forbidden extends RuntimeException { public Forbidden(String m) { super(m); } }
    public static class Unauthorized extends RuntimeException { public Unauthorized(String m) { super(m); } }
    public static class BadRequest extends RuntimeException { public BadRequest(String m) { super(m); } }
}
