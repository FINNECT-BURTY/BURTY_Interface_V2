/**
 *
 *
 * <pre>
 * <b>Description  : 배치 애플리케이션 서비스 (MonthlyReportBatchService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.batch
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
package com.burty.application.service.batch;

import static com.lowagie.text.pdf.PdfWriter.ALLOW_SCREENREADERS;
import static com.lowagie.text.pdf.PdfWriter.ENCRYPTION_AES_128;

import com.burty.application.port.out.report.MonthlyReportDeliveryPort;
import com.burty.application.port.out.report.MonthlyReportHistoryPort;
import com.burty.application.service.consult.ConsultService;
import com.burty.core.constant.LogMessages;
import com.burty.domain.consult.model.MonthlyReport;
import com.burty.domain.user.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.YearMonth;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MonthlyReportBatchService {
  private static final Logger log = LoggerFactory.getLogger(MonthlyReportBatchService.class);
  private final ConsultService consultService;
  private final MonthlyReportDeliveryPort deliveryPort;
  private final MonthlyReportHistoryPort historyPort;
  private final UserRepository userRepository;

  public MonthlyReportBatchService(
      ConsultService consultService,
      MonthlyReportDeliveryPort deliveryPort,
      MonthlyReportHistoryPort historyPort,
      UserRepository userRepository) {
    this.consultService = consultService;
    this.deliveryPort = deliveryPort;
    this.historyPort = historyPort;
    this.userRepository = userRepository;
  }

  @Scheduled(cron = "${burty.report.cron:0 0 9 1 * *}")
  @SchedulerLock(name = "MonthlyReportBatch", lockAtLeastFor = "PT10M", lockAtMostFor = "PT3H")
  public void generateAndDeliverMonthlyReports() {
    List<String> userIds =
        userRepository.findAll().stream().map(u -> u.getUserId().toString()).toList();
    for (String userId : userIds) {
      try {
        MonthlyReport report = consultService.createMonthlyReport(userId);
        byte[] pdf = createPdf(report);
        deliveryPort.deliver(userId, pdf, "monthly-report-" + YearMonth.now() + ".pdf");
        historyPort.saveHistory(userId, report.month(), "SUCCESS", "scheduled delivery");
        log.info(
            LogMessages.Batch.MONTHLY_REPORT_KPI,
            userId,
            report.signalColor(),
            report.primaryAction());
      } catch (Exception e) {
        historyPort.saveHistory(userId, YearMonth.now().toString(), "FAILED", e.getMessage());
        log.warn("KPI monthly_report userId={} status=FAILED reason={}", userId, e.getMessage());
      }
    }
  }

  private byte[] createPdf(MonthlyReport report) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter writer = PdfWriter.getInstance(document, out);
    // DRM 최소 정책: 사용자 패스워드 + 소유자 패스워드 + 인쇄 제한
    writer.setEncryption(
        report.userId().getBytes(),
        ("owner-" + report.userId()).getBytes(),
        ALLOW_SCREENREADERS,
        ENCRYPTION_AES_128);
    document.open();
    document.add(new Paragraph("BURTY Monthly Report"));
    document.add(new Paragraph("Watermark: CONFIDENTIAL / " + report.userId()));
    document.add(new Paragraph("User: " + report.userId()));
    document.add(new Paragraph("Month: " + report.month()));
    document.add(new Paragraph("Summary: " + report.easyReadSummary()));
    document.add(new Paragraph("Signal: " + report.signalColor()));
    document.add(new Paragraph("Primary action: " + report.primaryAction()));
    for (String point : report.keyPoints()) {
      document.add(new Paragraph("- " + point));
    }
    document.close();
    return out.toByteArray();
  }
}
