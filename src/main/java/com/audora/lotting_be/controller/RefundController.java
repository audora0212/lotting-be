package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.refund.CancelledCustomerRefund;
import com.audora.lotting_be.service.RefundService;
import com.audora.lotting_be.util.FileCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    @Autowired
    private RefundService refundService;

    // 기존 환불 엑셀 업로드 엔드포인트 (SSE로 진행률 전송)
    @PostMapping(value = "/excel/upload", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadRefundExcel(@RequestParam("file") MultipartFile file) {
        // 기존 로직 그대로 사용
        return refundService.uploadRefundExcelFileWithProgress(file, new SseEmitter(3000000L));
    }

    // 환불 엑셀 다운로드 진행 SSE 엔드포인트
    @GetMapping(value = "/excel/download/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter downloadRefundExcelProgress() {
        SseEmitter emitter = new SseEmitter(3000000L);
        CompletableFuture.runAsync(() -> {
            File tempFile = null;
            try {
                // 모든 환불 기록 조회
                List<CancelledCustomerRefund> refundList = refundService.getAllRefundRecords();
                if (refundList == null || refundList.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("No refund records found."));
                    emitter.complete();
                    return;
                }
                // 템플릿 파일 로드 (classpath: excel_templates/refformat.xlsx)
                org.springframework.core.io.ClassPathResource templateResource = new org.springframework.core.io.ClassPathResource("excel_templates/refformat.xlsx");
                if (!templateResource.exists()) {
                    emitter.send(SseEmitter.event().name("error").data("Template file not found."));
                    emitter.complete();
                    return;
                }
                // 임시 파일 복사
                tempFile = Files.createTempFile("refformat-", ".xlsx").toFile();
                try (InputStream is = templateResource.getInputStream()) {
                    Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                // RefundService 내부의 fillRefformat 메서드를 호출하여 템플릿에 환불 데이터를 기록
                refundService.fillRefformat(tempFile, refundList, emitter);
                // 파일 생성 완료 후 고유 fileId 생성 및 캐시에 저장
                String fileId = UUID.randomUUID().toString();
                FileCache.put(fileId, tempFile);
                emitter.send(SseEmitter.event().name("complete").data(fileId));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                    // 추가 로깅 가능
                }
                emitter.completeWithError(e);
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        });
        return emitter;
    }

    // 환불 엑셀 파일 다운로드 엔드포인트
    @GetMapping("/excel/download/file")
    public ResponseEntity<Resource> downloadRefundExcelFile(@RequestParam("fileId") String fileId) {
        try {
            File tempFile = FileCache.get(fileId);
            if (tempFile == null || !tempFile.exists()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }
            Resource resource = new UrlResource(tempFile.toURI());
            String encodedFilename = UriUtils.encode("refund_file.xlsx", StandardCharsets.UTF_8);
            MediaType mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            // 파일 다운로드 후 캐시에서 제거
            FileCache.remove(fileId);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).build();
        }
    }
}
