package com.burty.application.service;

import com.burty.application.port.in.BurtyUseCase;
import com.burty.application.port.out.MonthlyReportDeliveryPort;
import com.burty.application.port.out.MonthlyReportHistoryPort;
import com.burty.domain.repository.UserRepository;
import com.burty.domain.model.MonthlyReport;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.YearMonth;
import java.util.List;

import static com.lowagie.text.pdf.PdfWriter.ALLOW_SCREENREADERS;
import static com.lowagie.text.pdf.PdfWriter.ENCRYPTION_AES_128;

@Service
public class MonthlyReportBatchService {
    private static final Logger log = LoggerFactory.getLogger(MonthlyReportBatchService.class);
    private final BurtyUseCase burtyUseCase;
    private final MonthlyReportDeliveryPort deliveryPort;
    private final MonthlyReportHistoryPort historyPort;
    private final UserRepository userRepository;

    public MonthlyReportBatchService(BurtyUseCase burtyUseCase, MonthlyReportDeliveryPort deliveryPort,
                                     MonthlyReportHistoryPort historyPort, UserRepository userRepository) {
        this.burtyUseCase = burtyUseCase;
        this.deliveryPort = deliveryPort;
        this.historyPort = historyPort;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "${burty.report.cron:0 0 9 1 * *}")
    public void generateAndDeliverMonthlyReports() {
        List<String> userIds = userRepository.findAll().stream()
                .map(u -> u.getUserId().toString())
                .toList();
        for (String userId : userIds) {
            try {
                MonthlyReport report = burtyUseCase.createMonthlyReport(userId);
                byte[] pdf = createPdf(report);
                deliveryPort.deliver(userId, pdf, "monthly-report-" + YearMonth.now() + ".pdf");
                historyPort.saveHistory(userId, report.getMonth(), "SUCCESS", "scheduled delivery");
                log.info("KPI monthly_report userId={} status=SUCCESS signal={} action={}",
                        userId, report.getSignalColor(), report.getPrimaryAction());
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
        writer.setEncryption(report.getUserId().getBytes(), ("owner-" + report.getUserId()).getBytes(), ALLOW_SCREENREADERS, ENCRYPTION_AES_128);
        document.open();
        document.add(new Paragraph("BURTY Monthly Report"));
        document.add(new Paragraph("Watermark: CONFIDENTIAL / " + report.getUserId()));
        document.add(new Paragraph("User: " + report.getUserId()));
        document.add(new Paragraph("Month: " + report.getMonth()));
        document.add(new Paragraph("Summary: " + report.getEasyReadSummary()));
        document.add(new Paragraph("Signal: " + report.getSignalColor()));
        document.add(new Paragraph("Primary action: " + report.getPrimaryAction()));
        for (String point : report.getKeyPoints()) {
            document.add(new Paragraph("- " + point));
        }
        document.close();
        return out.toByteArray();
    }
}
