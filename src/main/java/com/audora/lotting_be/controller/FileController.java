package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.Status;
import com.audora.lotting_be.service.CustomerService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;
import org.apache.poi.xssf.usermodel.XSSFDrawing;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // CustomerService 주입
    @Autowired
    private CustomerService customerService;

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
            Resource resource = new UrlResource(filePath.toUri());
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
     * /format1/{id} 로직은 동일하다고 가정
     * (생략 or 유지)
     */

    /**
     * ------------------------------------------------------------------------
     * [신규] /format2/{id} 로 GET 요청이 들어올 때,
     * - format2.xlsx 복사(TempFile)
     * - {id}로 고객 조회 후, 질문에서 주신 셀(G9, L49, L64, L66, L68, L70, M151)과
     *   TextBox (34, 35, 36, 37, 1033)에 데이터 기입
     * - 추가로, A288, B288, C288 에도 기입
     * - 완성본 다운로드 후 임시 파일 삭제
     * ------------------------------------------------------------------------
     */
    @GetMapping("/format2/{id}")
    public ResponseEntity<Resource> generateFormat2AndDownload(@PathVariable("id") Integer id) {
        // 1) 고객 조회
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // 2) 템플릿 파일 존재 확인
        File templateFile = new File("src/main/resources/excel_templates/format2.xlsx");
        if (!templateFile.exists()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        // 3) 임시 파일 복사
        File tempFile;
        try {
            tempFile = Files.createTempFile("format2-", ".xlsx").toFile();
            Files.copy(templateFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        // 4) POI로 엑셀 열고, 각 셀 & 텍스트 박스 수정
        try (FileInputStream fis = new FileInputStream(tempFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            // (1) 셀 수정
            // G9 -> row=8,col=6
            if (customer.getCustomerData() != null && customer.getCustomerData().getName() != null) {
                getCell(sheet, 8, 6).setCellValue(customer.getCustomerData().getName());
            }
            // L49 -> row=48,col=11
            LocalDate rd = customer.getRegisterdate();
            if (rd != null) {
                getCell(sheet, 48, 11).setCellValue(rd.toString());
            }
            // L64 -> row=63,col=11 (name)
            if (customer.getCustomerData() != null && customer.getCustomerData().getName() != null) {
                getCell(sheet, 63, 11).setCellValue(customer.getCustomerData().getName());
            }
            // L66 -> row=65,col=11 (resnumfront)
            if (customer.getCustomerData() != null && customer.getCustomerData().getResnumfront() != null) {
                getCell(sheet, 65, 11).setCellValue(customer.getCustomerData().getResnumfront());
            }
            // L68 -> row=67,col=11 (phone)
            if (customer.getCustomerData() != null && customer.getCustomerData().getPhone() != null) {
                getCell(sheet, 67, 11).setCellValue(customer.getCustomerData().getPhone());
            }
            // L70 -> row=69,col=11 (우편번호 + 상세주소)
            String address = "";
            if (customer.getLegalAddress() != null) {
                if (customer.getLegalAddress().getPost() != null) {
                    address += customer.getLegalAddress().getPost();
                }
                if (customer.getLegalAddress().getDetailaddress() != null) {
                    address += customer.getLegalAddress().getDetailaddress();
                }
            }
            getCell(sheet, 69, 11).setCellValue(address);

            // M151 -> row=150,col=12 (resnumfront-resnumback)
            if (customer.getCustomerData() != null
                    && customer.getCustomerData().getResnumfront() != null
                    && customer.getCustomerData().getResnumback() != null) {
                String rrn = customer.getCustomerData().getResnumfront()
                        + "-" + customer.getCustomerData().getResnumback();
                getCell(sheet, 150, 12).setCellValue(rrn);
            }

            // === 추가사항: A288, B288, C288 ===
            // (row=287, col=0), (row=287, col=1), (row=287, col=2)
            // A288 -> id
            getCell(sheet, 287, 0).setCellValue(customer.getId());
            // B288 -> type
            getCell(sheet, 287, 1).setCellValue(
                    customer.getType() != null ? customer.getType() : ""
            );
            // C288 -> groupname
            getCell(sheet, 287, 2).setCellValue(
                    customer.getGroupname() != null ? customer.getGroupname() : ""
            );

            // (2) 텍스트 박스 수정
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing != null) {
                List<XSSFShape> shapes = drawing.getShapes();
                for (XSSFShape shape : shapes) {
                    if (shape instanceof XSSFTextBox) {
                        XSSFTextBox textBox = (XSSFTextBox) shape;
                        String shapeName = textBox.getShapeName(); // 예: "TextBox 34"

                        switch (shapeName) {
                            case "TextBox 34":
                                // CustomerData.name
                                if (customer.getCustomerData() != null
                                        && customer.getCustomerData().getName() != null) {
                                    textBox.setText(customer.getCustomerData().getName());
                                }
                                break;
                            case "TextBox 35":
                                // id
                                textBox.setText(customer.getId().toString());
                                break;
                            case "TextBox 36":
                                // type
                                if (customer.getType() != null) {
                                    textBox.setText(customer.getType());
                                }
                                break;
                            case "TextBox 37":
                                // groupname
                                if (customer.getGroupname() != null) {
                                    textBox.setText(customer.getGroupname());
                                }
                                break;
                            case "TextBox 1033":
                                // registerdate
                                if (rd != null) {
                                    textBox.setText(rd.toString());
                                }
                                break;
                            default:
                                // 다른 텍스트 박스는 수정 안 함
                                break;
                        }
                    }
                }
            }

            // (3) 수식 재계산(엑셀 열 때 자동)
            workbook.setForceFormulaRecalculation(true);

            // (4) 엑셀 수정 내용을 tempFile에 저장
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                workbook.write(fos);
            }

        } catch (IOException e) {
            tempFile.delete();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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

        String downloadFilename = "format2_" + id + "_" + System.currentTimeMillis() + ".xlsx";
        String encodedFilename = UriUtils.encode(downloadFilename, StandardCharsets.UTF_8);

        MediaType mediaType = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    /**
     * ------------------------------------------------------------------------
     * 헬퍼 메서드: 엑셀 특정 셀 객체를 가져오고 없으면 생성
     * ------------------------------------------------------------------------
     */
    private Cell getCell(XSSFSheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        return cell;
    }

}
