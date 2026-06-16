/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (LoginFailLogUtil)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.util
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.util;

import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 로그인 실패 파일 로그 + logback LOGIN_FAIL 로거.
 *
 * <p>파일: {@code {logDir}/login_fail_{userId}.log} — Jenkins {@code login_fail_*.log} 패턴과 호환.
 */
@Slf4j
@Component
public class LoginFailLogUtil {

  private static final Logger LOGIN_FAIL = LoggerFactory.getLogger("LOGIN_FAIL");
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Value("${burty.login-fail.log-dir:${LOG_PATH:./logs}}")
  private String logDir;

  @Value("${burty.login-fail.file-prefix:login_fail_}")
  private String filePrefix;

  @Value("${burty.login-fail.file-suffix:.log}")
  private String fileSuffix;

  @PostConstruct
  void init() {
    File dir = new File(logDir);
    if (!dir.exists() && !dir.mkdirs()) {
      log.warn("login-fail log directory could not be created: {}", logDir);
    }
  }

  public void writeFailLog(String userId, String ip, String reason) {
    String safeUser = sanitizeUserId(userId);
    String logPath = logDir + File.separator + filePrefix + safeUser + fileSuffix;
    String now = LocalDateTime.now().format(FORMATTER);
    String line = String.format("%s userId=%s ip=%s reason=%s", now, safeUser, ip, reason);

    LOGIN_FAIL.warn(line);

    try (FileWriter fw = new FileWriter(logPath, true);
        BufferedWriter bw = new BufferedWriter(fw)) {
      bw.write(line);
      bw.newLine();
    } catch (IOException e) {
      log.error("login fail file write error userId={}: {}", safeUser, e.getMessage());
    }
  }

  public void logAdminFailure(String username, String reason) {
    writeFailLog(username, resolveClientIp(), reason);
  }

  public void logAuthFailure(HttpServletRequest request, String reason) {
    writeFailLog(extractActor(request), IpUtil.getClientIp(request), reason);
  }

  public void logTokenFailure(HttpServletRequest request, String reason) {
    writeFailLog("token", IpUtil.getClientIp(request), reason);
  }

  public int countFails(String userId) {
    File file = new File(userLogPath(userId));
    if (!file.exists()) {
      return 0;
    }
    int count = 0;
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      while (reader.readLine() != null) {
        count++;
      }
    } catch (IOException e) {
      log.error("login fail count read error userId={}: {}", userId, e.getMessage());
    }
    return count;
  }

  public void clearFailLog(String userId) {
    File file = new File(userLogPath(userId));
    if (file.exists() && file.delete()) {
      log.debug("login fail log cleared userId={}", userId);
    }
  }

  private String userLogPath(String userId) {
    return logDir + File.separator + filePrefix + sanitizeUserId(userId) + fileSuffix;
  }

  private static String sanitizeUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      return "unknown";
    }
    return userId.replaceAll("[^a-zA-Z0-9._@-]", "_");
  }

  private static String resolveClientIp() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      return "unknown";
    }
    return IpUtil.getClientIp(attrs.getRequest());
  }

  private static String extractActor(HttpServletRequest request) {
    String headerUser = request.getHeader("X-User-Id");
    if (headerUser != null && !headerUser.isBlank()) {
      return headerUser.trim();
    }
    return "unknown";
  }

  public void requireNotLocked(String userId, int maxAttempts) {
    if (countFails(userId) >= maxAttempts) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "로그인 실패 횟수 초과. 잠시 후 다시 시도해 주세요.");
    }
  }
}
