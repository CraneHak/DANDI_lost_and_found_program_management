package org.example.service;

import org.example.common.ImageUrlService;
import org.example.controller.CreateLostItemRequest;
import org.example.controller.UpdateLostItemRequest;
import org.example.entity.LostItem;
import org.example.keyword.KeywordAlertService;
import org.example.pickup.CollectionLogRepository;
import org.example.pickup.PickupPassRepository;
import org.example.report.Report;
import org.example.report.ReportRepository;
import org.example.repository.LostItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 신분증·카드는 모자이크 사진이 아니면 어느 공개 경로로도 등록·수정할 수 없다. 거부될 때는 S3 업로드도 일어나지 않는다. */
class LostItemSensitivePhotoTest {

    private static final String MOSAIC = "/files/vision/mosaic/643f6f09.jpeg";

    private final LostItemRepository repository = mock(LostItemRepository.class);
    private final S3Service s3 = mock(S3Service.class);
    private final ImageUrlService imageUrls = mock(ImageUrlService.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final LostItemService service = new LostItemService(
            repository, s3, imageUrls, mock(CollectionLogRepository.class),
            mock(PickupPassRepository.class), reports, mock(KeywordAlertService.class));

    private void assertRejected(org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(s3);
        verify(imageUrls, never()).normalizeForStorage(any());
        verify(repository, never()).save(any());
    }

    private CreateLostItemRequest json(String category, String image) {
        return new CreateLostItemRequest(null, "학생증", null, category, null, null, null, null,
                null, null, image, null, null, null, null);
    }

    // ---- 관리자 직접 등록 (JSON)
    @Test void jsonRegisterRejectsRawIdCardPhoto() {
        assertRejected(() -> service.createFromRequest(json("신분증", "data:image/png;base64,AAAA")));
    }

    @Test void jsonRegisterAllowsMosaicIdCardPhoto() {
        when(imageUrls.normalizeForStorage(MOSAIC)).thenReturn(MOSAIC);
        when(repository.save(any(LostItem.class))).thenAnswer(c -> c.getArgument(0));
        assertDoesNotThrow(() -> service.createFromRequest(json("신분증", MOSAIC)));
    }

    @Test void jsonRegisterAllowsRawPhotoForOrdinaryItem() {
        when(imageUrls.normalizeForStorage("data:image/png;base64,AAAA")).thenReturn("https://s3/x.png");
        when(repository.save(any(LostItem.class))).thenAnswer(c -> c.getArgument(0));
        assertDoesNotThrow(() -> service.createFromRequest(json("화장품", "data:image/png;base64,AAAA")));
    }

    // ---- 관리자 직접 등록 (multipart)
    @Test void multipartRegisterRejectsUploadedFileForCard() {
        MockMultipartFile file = new MockMultipartFile("image", "card.jpg", "image/jpeg", new byte[]{1, 2, 3});
        assertRejected(() -> service.createFromMultipart(
                "체크카드", null, "카드/티켓", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, file, null));
    }

    @Test void multipartRegisterRejectsRawUrlForIdCard() {
        assertRejected(() -> service.createFromMultipart(
                "학생증", null, "신분증", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, "https://s3/raw.jpg", null, null, null, null, null));
    }

    @Test void multipartRegisterAllowsMosaicUrlForIdCard() {
        when(imageUrls.normalizeForStorage(MOSAIC)).thenReturn(MOSAIC);
        when(repository.save(any(LostItem.class))).thenAnswer(c -> c.getArgument(0));
        assertDoesNotThrow(() -> service.createFromMultipart(
                "학생증", null, "신분증", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, MOSAIC, null, null));
    }

    // ---- 신고 승인 안전장치
    @Test void approvalSafetyNetRejectsRawPhotoForIdCardReport() {
        Report report = new Report();
        report.setItemName("학생증");
        report.setCategory("신분증");
        report.setImage("https://s3/raw.jpg");
        assertRejected(() -> service.upsertFromReport(report));
    }

    @Test void approvalAllowsMosaicPhotoForIdCardReport() {
        Report report = new Report();
        report.setItemName("학생증");
        report.setCategory("신분증");
        report.setImage(MOSAIC);
        when(repository.findByReportId(any())).thenReturn(Optional.empty());
        when(imageUrls.normalizeForStorage(MOSAIC)).thenReturn(MOSAIC);
        when(repository.save(any(LostItem.class))).thenAnswer(c -> c.getArgument(0));
        assertDoesNotThrow(() -> service.upsertFromReport(report));
    }

    // ---- 사진 교체(수정)
    private LostItem existing(String type, String image) {
        LostItem item = new LostItem();
        item.setItemName("카드");
        item.setItemType(type);
        item.setImageUrl(image);
        when(repository.findById(5)).thenReturn(Optional.of(item));
        when(repository.save(any(LostItem.class))).thenAnswer(c -> c.getArgument(0));
        return item;
    }

    private UpdateLostItemRequest patch(String image, String itemType) {
        return new UpdateLostItemRequest(null, null, null, null, null, null, "보관함", null, image, itemType);
    }

    @Test void updateRejectsReplacingWithRawPhotoOnCard() {
        existing("카드/티켓", MOSAIC);
        assertRejected(() -> service.update(5, patch("data:image/png;base64,AAAA", null)));
    }

    @Test void updateAllowsReplacingWithMosaicPhotoOnCard() {
        existing("카드/티켓", "https://s3/old.jpg");
        when(imageUrls.normalizeForStorage(MOSAIC)).thenReturn(MOSAIC);
        assertDoesNotThrow(() -> service.update(5, patch(MOSAIC, null)));
    }

    @Test void updateWithSamePhotoDoesNotBlockOtherEdits() {
        // 이미 공개된 옛 카드 사진(모자이크 아님)이 있어도, 같은 사진을 다시 보낸 위치·이름 수정은 막지 않는다
        existing("카드/티켓", "https://s3/old.jpg");
        when(imageUrls.normalizeForStorage("https://s3/old.jpg")).thenReturn("https://s3/old.jpg");
        assertDoesNotThrow(() -> service.update(5, patch("https://s3/old.jpg", "카드/티켓")));
    }
}
