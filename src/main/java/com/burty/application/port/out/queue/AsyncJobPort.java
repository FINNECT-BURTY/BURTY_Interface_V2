package com.burty.application.port.out.queue;

import java.util.Map;

public interface AsyncJobPort {

  void publish(AsyncJobType type, Map<String, String> payload);

  boolean isEnabled();
}
