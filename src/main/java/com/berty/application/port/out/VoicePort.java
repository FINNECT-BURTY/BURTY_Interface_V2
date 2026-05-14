package com.berty.application.port.out;

public interface VoicePort {
    String speechToText(String audioBase64);
    String textToSpeech(String text);
}
