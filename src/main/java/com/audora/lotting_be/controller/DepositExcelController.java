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
    @PostMapping(value = "/upload", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadDepositHistoryExcel(@RequestParam("file") MultipartFile file) {
        SseEmitter emitter = new SseEmitter(3000000L); // 3000초(50분)
        CompletableFuture.runAsync(() -> {
            try {
                depositExcelService.processDepositExcelFileWithProgress(file, emitter);
            } catch (IOException e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }


    @GetMapping(value = "/download/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter downloadDepositHistoryExcelProgress() {
        SseEmitter emitter = new SseEmitter(3000000L);
        CompletableFuture.runAsync(() -> {
            File tempFile = null;
            try {
                List<DepositHistory> depositHistories = depositHistoryService.getAllDepositHistories();
                if (depositHistories == null || depositHistories.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("No deposit histories found."));
                    emitter.complete();
                    return;
                }
                ClassPathResource templateResource = new ClassPathResource("excel_templates/depformat.xlsx");
                if (!templateResource.exists()) {
                    emitter.send(SseEmitter.event().name("error").data("Template file not found."));
                    emitter.complete();
                    return;
                }
                tempFile = Files.createTempFile("depformat-", ".xlsx").toFile();
                try (InputStream is = templateResource.getInputStream()) {
                    Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                depositExcelService.fillDepFormat(tempFile, depositHistories);
                String fileId = UUID.randomUUID().toString();
                FileCache.put(fileId, tempFile);
                emitter.send(SseEmitter.event().name("complete").data(fileId));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                }
                emitter.completeWithError(e);
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        });
        return emitter;
    }


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
