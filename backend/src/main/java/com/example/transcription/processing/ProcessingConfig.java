package com.example.transcription.processing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.processing.enabled", havingValue = "true", matchIfMissing = true)
public class ProcessingConfig {}
