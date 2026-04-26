package org.example.report;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<Report> findAll() {
        return reportRepository.findAll();
    }

    @Transactional
    public Report create(Report report) {
        return reportRepository.save(report);
    }

    @Transactional
    public Report updateStatus(Long id, ReportStatus newStatus) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
        report.setStatus(newStatus);
        return reportRepository.save(report);
    }

    @Transactional
    public void delete(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new ReportNotFoundException(id);
        }
        reportRepository.deleteById(id);
    }
}
