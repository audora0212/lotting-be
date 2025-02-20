package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.payload.response.MessageResponse;
import com.audora.lotting_be.service.CustomerService;
import com.audora.lotting_be.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

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

    @PostMapping("/uploadExcel")
    public ResponseEntity<?> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        try {
            // ExcelService의 새로운 메서드를 호출하여 파일 파싱 및 DB 저장
            System.out.println("excelfile detected");
            excelService.processRegExcelFile(file);
            return ResponseEntity.ok(new MessageResponse("엑셀 파일이 성공적으로 처리되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("엑셀 파일 처리 중 오류가 발생했습니다."));
        }
    }
}
