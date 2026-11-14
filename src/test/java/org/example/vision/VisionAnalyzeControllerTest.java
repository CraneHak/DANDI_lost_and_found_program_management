package org.example.vision;

import org.example.report.Report;
import org.example.report.ReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 검수 대기 신고의 사진을 사진 파일 없이 reportId만으로 분석하는 경로 */
class VisionAnalyzeControllerTest {

    private static final String RAW = "https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/lost-items/a.jpg";

    private final VisionAnalyzeService service = mock(VisionAnalyzeService.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final VisionAnalyzeController controller = new VisionAnalyzeController(service, reports);

    private Report report(String image) {
        Report r = new Report();
        r.setItemName("학생증");
        r.setImage(image);
        when(reports.findById(31L)).thenReturn(Optional.of(r));
        return r;
    }

    @Test void uploadedFileIsAnalyzedAsBefore() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        controller.analyze(file, null, DocumentType.ID_CARD);
        verify(service).analyze(file, DocumentType.ID_CARD);
        verifyNoInteractions(reports);
    }

    @Test void withoutFileTheReportsStoredPhotoIsAnalyzed() throws Exception {
        report(RAW);
        controller.analyze(null, 31L, DocumentType.ID_CARD);
        verify(service).analyzeStoredImage(RAW, DocumentType.ID_CARD);
        verify(service, never()).analyze(any(), any());
    }

    @Test void emptyFileFallsBackToTheReportsPhoto() throws Exception {
        report(RAW);
        MockMultipartFile empty = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[0]);
        controller.analyze(empty, 31L, DocumentType.BANK_CARD);
        verify(service).analyzeStoredImage(RAW, DocumentType.BANK_CARD);
    }

    @Test void neitherFileNorReportIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> controller.analyze(null, null, DocumentType.NONE));
        verifyNoInteractions(service);
    }

    @Test void unknownReportIsRejected() {
        when(reports.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> controller.analyze(null, 99L, DocumentType.NONE));
        verifyNoInteractions(service);
    }

    @Test void reportWithoutPhotoIsRejected() {
        report("  ");
        assertThrows(IllegalArgumentException.class, () -> controller.analyze(null, 31L, DocumentType.ID_CARD));
        verifyNoInteractions(service);
    }

    @Test void alreadyMosaickedPhotoIsRejected() {
        report("/files/vision/mosaic/abc.jpeg");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.analyze(null, 31L, DocumentType.ID_CARD));
        assertTrue(ex.getMessage().contains("모자이크"));
        verifyNoInteractions(service);
    }
}
