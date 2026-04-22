package com.nuri.application.port.out;

public interface EasyReadPort {
    String toEasyRead(String rawText);
    String toSignalColor(double volatilityPercent);
}
