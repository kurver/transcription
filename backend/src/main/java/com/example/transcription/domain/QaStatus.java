package com.example.transcription.domain;

/** NONE is only used in views (no review exists); stored reviews are OPEN → CLAIMED → PASSED/FAILED. */
public enum QaStatus { NONE, OPEN, CLAIMED, PASSED, FAILED }
