package com.nuri.application.service;

import com.nuri.application.port.in.NuriUseCase;
import com.nuri.application.port.out.MonthlyReportDeliveryPort;
import com.nuri.application.port.out.MonthlyReportHistoryPort;
import com.nuri.domain.repository.UserRepository;
import com.nuri.domain.model.MonthlyReport;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class MonthlyReportBatchService {
    private final NuriUseCase nuriUseCase;
    private final MonthlyReportDeliveryPort deliveryPort;
    private final MonthlyReportHistoryPort historyPort;
    private final UserRepository userRepository;

    public MonthlyReportBatchService(NuriUseCase nuriUseCase, MonthlyReportDeliveryPort deliveryPort,
                                     MonthlyReportHistoryPort historyPort, UserRepository userRepository) {
        this.nuriUseCase = nuriUseCase;
        this.deliveryPort = deliveryPort;
        this.historyPort = historyPort;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "${nuri.report.cron:0 0 9 1 * *}")
    public void generateAndDeliverMonthlyReports() {
        List<String> userIds = userRepository.findAll().stream()
                .map(u -> u.getUserId().toString())
                .toList();
        for (String userId : userIds) {
            try {
                MonthlyReport report = nuriUseCase.createMonthlyReport(userId);
                byte[] pdf = createPdf(report);
                deliveryPort.deliver(userId, pdf, "monthly-report-" + YearMonth.now() + ".pdf");
                historyPort.saveHistory(userId, report.getMonth(), "SUCCESS", "scheduled delivery");
            } catch (Exception e) {
                historyPort.saveHistory(userId, YearMonth.now().toString(), "FAILED", e.getMessage());
            }
        }
    }

    private byte[] createPdf(MonthlyReport report) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, out);
        // DRM 최소 정책: 사용자 패스워드 + 소유자 패스워드 + 인쇄 제한
        writer.setEncryption(
                report.getUserId().getBytes(),
                ("owner-" + report.getUserId()).getBytes(),
                com.lowagie.text.pdf.PdfWriter.ALLOW_SCREENREADERS,
                com.lowagie.text.pdf.PdfWriter.ENCRYPTION_AES_128
        );
        document.open();
        document.add(new Paragraph("NURI Monthly Report"));
        document.add(new Paragraph("Watermark: CONFIDENTIAL / " + report.getUserId()));
        document.add(new Paragraph("User: " + report.getUserId()));
        document.add(new Paragraph("Month: " + report.getMonth()));
        document.add(new Paragraph("Summary: " + report.getEasyReadSummary()));
        document.add(new Paragraph("Signal: " + report.getSignalColor()));
        document.close();
        return out.toByteArray();
    }
}
