package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.service.DepositExcelService;
import com.audora.lotting_be.service.DepositHistoryService;
import com.audora.lotting_be.util.FileCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/deposithistory/excel")
public class DepositExcelController {

    @Autowired
    private DepositExcelService depositExcelService;
    @Autowired
    private DepositHistoryService depositHistoryService;
    /**
     * 엑셀 파일 업로드 엔드포인트
     * 파일을 받으면 DepositExcelService를 호출하여 파싱 및 DB 저장을 진행하고,
     * SSE를 통해 진행 상황을 클라이언트에 전달합니다.
     *
     * @param file 업로드된 엑셀 파일
     * @return 진행 상황을 전달하는 SseEmitter
     */
    @PostMapping(value = "/upload", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadDepositHistoryExcel(@RequestParam("file") MultipartFile file) {
        SseEmitter emitter = new SseEmitter(3000000L); // 최대 3000초(50분) timeout
        CompletableFuture.runAsync(() -> {
            try {
                depositExcelService.processDepositExcelFileWithProgress(file, emitter);
            } catch (IOException e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                    // 추가 로깅 처리 가능
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * [신규] 모든 DepositHistory 데이터를 depformat.xlsx 템플릿에 채워서 파일로 생성하고,
     * 진행 상황을 SSE로 전달합니다.
     *
     * 프론트에서 GET /api/deposithistory/excel/download/progress 로 호출하면,
     * 최종적으로 고유 fileId가 SSE 이벤트 complete로 전달됩니다.
     */
    @GetMapping(value = "/download/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter downloadDepositHistoryExcelProgress() {
        SseEmitter emitter = new SseEmitter(3000000L); // 최대 3000초 timeout
        CompletableFuture.runAsync(() -> {
            File tempFile = null;
            try {
                // 모든 DepositHistory 조회
                List<DepositHistory> depositHistories = depositHistoryService.getAllDepositHistories();
                if (depositHistories == null || depositHistories.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("No deposit histories found."));
                    emitter.complete();
                    return;
                }
                // 템플릿 파일 로드 (excel_templates/depformat.xlsx)
                ClassPathResource templateResource = new ClassPathResource("excel_templates/depformat.xlsx");
                if (!templateResource.exists()) {
                    emitter.send(SseEmitter.event().name("error").data("Template file not found."));
                    emitter.complete();
                    return;
                }
                // 임시 파일 복사
                tempFile = Files.createTempFile("depformat-", ".xlsx").toFile();
                try (InputStream is = templateResource.getInputStream()) {
                    Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                // 템플릿에 depositHistory 데이터를 기록
                depositExcelService.fillDepFormat(tempFile, depositHistories);
                // 파일 생성 완료 후 고유 식별자 생성 및 캐시에 저장
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

    /**
     * [신규] fileId를 이용하여 생성된 DepositHistory 엑셀 파일을 다운로드합니다.
     */
    @GetMapping("/download/file")
    public ResponseEntity<Resource> downloadDepositExcelFile(@RequestParam("fileId") String fileId) {
        try {
            File tempFile = FileCache.get(fileId);
            if (tempFile == null || !tempFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Resource resource = new UrlResource(tempFile.toURI());
            String encodedFilename = UriUtils.encode("deposit_histories.xlsx", StandardCharsets.UTF_8);
            MediaType mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            // 파일 다운로드 후 캐시에서 제거 (원하는 경우)
            FileCache.remove(fileId);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
