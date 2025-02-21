package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.payload.response.MessageResponse;
import com.audora.lotting_be.service.CustomerService;
import com.audora.lotting_be.service.ExcelService;
import com.audora.lotting_be.util.FileCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // CustomerService 주입
    @Autowired
    private CustomerService customerService;

    // 새로 추가된 ExcelService 주입
    @Autowired
    private ExcelService excelService;

    /**
     * 파일 업로드 예시 메서드
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
            Path path = Paths.get(uploadDir).resolve(fileName);
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok(fileName); // 파일명 반환
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Could not upload file: " + e.getMessage());
        }
    }

    /**
     * 파일 다운로드 예시 메서드
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("id") Long id,
                                                 @RequestParam("filename") String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                // 확장자별 MIME 타입 지정
                String extension = com.google.common.io.Files.getFileExtension(filePath.toString());
                switch (extension.toLowerCase()) {
                    case "pdf":
                        contentType = "application/pdf";
                        break;
                    case "jpg":
                    case "jpeg":
                        contentType = "image/jpeg";
                        break;
                    case "png":
                        contentType = "image/png";
                        break;
                    default:
                        contentType = "application/octet-stream";
                }
            }

            String encodedFileName = UriUtils.encode(resource.getFilename(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 파일 삭제 예시 메서드
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam("filename") String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return ResponseEntity.ok("파일이 성공적으로 삭제되었습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("파일을 찾을 수 없습니다.");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * ------------------------------------------------------------------------
     * [신규] /format1/{id} 로 GET 요청이 들어올 때,
     * - format1.xlsx 복사(TempFile)
     * - {id}로 고객 정보 조회 & 엑셀 특정 셀들에 채워넣기 (ExcelService로 위임)
     * - 완성본을 다운로드 응답 후, 복사본 파일은 삭제
     * ------------------------------------------------------------------------
     */
    @GetMapping("/format1/{id}")
    public ResponseEntity<Resource> generateFormat1AndDownload(@PathVariable("id") Integer id) {
        // 1) 고객 조회
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // 2) 템플릿 파일을 ClassPathResource로 불러오기
        ClassPathResource templateResource = new ClassPathResource("excel_templates/format1.xlsx");
        if (!templateResource.exists()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        // 3) 임시 파일 복사
        File tempFile;
        try {
            tempFile = Files.createTempFile("format1-", ".xlsx").toFile();
            try (InputStream is = templateResource.getInputStream()) {
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        // 4) ExcelService로 위임하여 엑셀에 데이터 기입
        try {
            excelService.fillFormat1(tempFile, customer);
        } catch (IOException e) {
            // 작업 실패 시 tempFile 삭제 후 에러 반환
            tempFile.delete();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 5) tempFile -> 메모리에 올려 Resource로 변환, tempFile 삭제 후 반환
        ByteArrayResource resource;
        try {
            byte[] fileBytes = Files.readAllBytes(tempFile.toPath());
            resource = new ByteArrayResource(fileBytes);
        } catch (IOException e) {
            tempFile.delete();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        tempFile.delete();

        String downloadFilename = "일반 신청서.xlsx";
        String encodedFilename = UriUtils.encode(downloadFilename, StandardCharsets.UTF_8);

        MediaType mediaType = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @GetMapping("/format2/{id}")
    public ResponseEntity<Resource> generateFormat2AndDownload(@PathVariable("id") Integer id) {
        // 1) 고객 조회
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // 2) 템플릿 파일을 ClassPathResource로 불러오기
        ClassPathResource templateResource = new ClassPathResource("excel_templates/format2.xlsx");
        if (!templateResource.exists()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        // 3) 임시 파일 복사
        File tempFile;
        try {
            tempFile = Files.createTempFile("format2-", ".xlsx").toFile();
            try (InputStream is = templateResource.getInputStream()) {
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        // 4) ExcelService로 위임하여 엑셀에 데이터 기입
        try {
            excelService.fillFormat2(tempFile, customer);
        } catch (IOException e) {
            tempFile.delete();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 5) 메모리에 올려서 다운로드 응답
        ByteArrayResource resource;
        try {
            byte[] fileBytes = Files.readAllBytes(tempFile.toPath());
            resource = new ByteArrayResource(fileBytes);
        } catch (IOException e) {
            tempFile.delete();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        // 임시 파일 삭제
        tempFile.delete();

        String downloadFilename = "일반 부속 서류.xlsx";
        String encodedFilename = UriUtils.encode(downloadFilename, StandardCharsets.UTF_8);

        MediaType mediaType = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @PostMapping(value = "/uploadExcelWithProgress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadExcelWithProgress(@RequestParam("file") MultipartFile file) {
        SseEmitter emitter = new SseEmitter(3000000L);
        CompletableFuture.runAsync(() -> {
            try {
                excelService.processExcelFileWithProgress(file, emitter);
                emitter.send(SseEmitter.event().name("complete").data("Parsing complete"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                    // 로그 처리 등
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    // (a) SSE 엔드포인트 : 파일 생성 및 진행 상황 전달
    @GetMapping(value = "/regfiledownload/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateRegFile() {
        SseEmitter emitter = new SseEmitter(3000000L);
        CompletableFuture.runAsync(() -> {
            File tempFile = null;
            try {
                // 고객 목록 조회 (phases 등 미리 초기화한 메서드 사용)
                List<Customer> customers = customerService.getAllCustomersWithPhases();
                if (customers == null || customers.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("No customers found."));
                    emitter.complete();
                    return;
                }

                // 템플릿 파일 로드
                ClassPathResource templateResource = new ClassPathResource("excel_templates/regformat.xlsx");
                if (!templateResource.exists()) {
                    emitter.send(SseEmitter.event().name("error").data("Template file not found."));
                    emitter.complete();
                    return;
                }

                // 템플릿 파일을 임시 파일로 복사
                tempFile = Files.createTempFile("regformat-", ".xlsx").toFile();
                try (InputStream is = templateResource.getInputStream()) {
                    Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                // 엑셀 템플릿에 고객 데이터 기록 (진행 상황 전달)
                excelService.fillRegFormat(tempFile, customers, emitter);

                // 파일 생성 완료 후 고유 식별자 생성 및 캐시에 저장
                String fileId = UUID.randomUUID().toString();
                FileCache.put(fileId, tempFile);

                emitter.send(SseEmitter.event().name("complete").data(fileId));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                    // 무시
                }
                emitter.completeWithError(e);
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        });
        return emitter;
    }

    // (b) 파일 다운로드 엔드포인트 : fileId를 이용하여 실제 파일 전달
    @GetMapping("/regfiledownload/file")
    public ResponseEntity<Resource> downloadGeneratedRegFile(@RequestParam("fileId") String fileId) {
        try {
            File tempFile = FileCache.get(fileId);
            if (tempFile == null || !tempFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Resource resource = new UrlResource(tempFile.toURI());
            String encodedFilename = UriUtils.encode("regformat_download.xlsx", StandardCharsets.UTF_8);
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
