package com.audora.lotting_be.controller;

import com.audora.lotting_be.service.DepositExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/deposithistory/excel")
public class DepositExcelController {

    @Autowired
    private DepositExcelService depositExcelService;

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
}
