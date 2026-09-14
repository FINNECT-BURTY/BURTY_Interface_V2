package com.burty.application.dto.consult;

/**
 * 음성 기능을 지금 쓸 수 있는지.
 *
 * <p>화면이 마이크 버튼을 내놓기 전에 묻는다. 쓸 수 없는데 버튼을 두면 사용자는 눌러 보고 나서야 안 된다는 것을 알게 되고, 스텁이 켜진 환경에서는 가짜 인식 결과를
 * 진짜로 받아들인다.
 *
 * @param available 실제 제공자로 음성 인식·합성을 할 수 있으면 true
 * @param reason 쓸 수 없을 때 화면에 보여줄 문구. 쓸 수 있으면 null
 */
public record VoiceCapabilityResponse(boolean available, String reason) {}
