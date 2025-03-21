// src/main/java/com/audora/lotting_be/service/RefundService.java
package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.refund.CancelledCustomerRefund;
import com.audora.lotting_be.repository.CancelledCustomerRefundRepository;
import com.audora.lotting_be.repository.CustomerRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class RefundService {

    @Autowired
    private CancelledCustomerRefundRepository refundRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // 기존 환불 레코드 생성 로직
    public void createRefundRecord(Customer customer, Map<String, Object> cancelInfo) {
        if (refundRepository.existsByCustomerId(customer.getId())) {
            return; // 이미 기록이 있으면 추가하지 않음
        }
        CancelledCustomerRefund refund = new CancelledCustomerRefund();
        refund.setCustomerId(customer.getId());

        if (customer.getCustomerData() != null) {
            refund.setName(customer.getCustomerData().getName());
            if (customer.getCustomerData().getResnumfront() != null && customer.getCustomerData().getResnumback() != null) {
                refund.setResidentNumber(customer.getCustomerData().getResnumfront() + "-" +
                        customer.getCustomerData().getResnumback());
            }
        }
        refund.setSource(customer.getRegisterpath());

        if (customer.getDeposits() != null) {
            refund.setPaymentDate(customer.getDeposits().getDepositdate());
            refund.setPaymentAmount(customer.getDeposits().getDepositammount());
        }

        if (customer.getCancel() != null) {
            refund.setCancelDate(customer.getCancel().getCanceldate());
            refund.setRefundDate(customer.getCancel().getRefunddate());
            if (customer.getCancel().getRefundamount() != null) {
                refund.setRefundAmount(customer.getCancel().getRefundamount().longValue());
            }
        }

        if (customer.getFinancial() != null) {
            refund.setInstitution(customer.getFinancial().getBankname());
            refund.setAccountNumber(customer.getFinancial().getAccountnum());
        }

        if (customer.getCustomerData() != null) {
            refund.setDepositor(customer.getCustomerData().getName());
        }

        if (customer.getResponsible() != null) {
            refund.setManagerGeneral(customer.getResponsible().getGeneralmanagement());
            refund.setManagerDivision(customer.getResponsible().getDivision());
            refund.setManagerTeam(customer.getResponsible().getTeam());
            refund.setManagerName(customer.getResponsible().getManagername());
        }

        if (cancelInfo != null) {
            refund.setReason((String) cancelInfo.get("reason"));
            refund.setRemarks((String) cancelInfo.get("remarks"));
            refund.setSource((String) cancelInfo.get("source"));
        }

        refundRepository.save(refund);
    }

    public SseEmitter uploadRefundExcelFileWithProgress(MultipartFile file, SseEmitter emitter) {
        SseEmitter sseEmitter = new SseEmitter(3000000L); // 최대 3000초 타임아웃
        CompletableFuture.runAsync(() -> {
            try {
                processRefundExcelFile(file, sseEmitter);
                sseEmitter.send(SseEmitter.event().name("complete").data("Refund excel processing complete."));
                sseEmitter.complete();
            } catch (Exception e) {
                try {
                    sseEmitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                    // 추가 로깅 처리 가능
                }
                sseEmitter.completeWithError(e);
            }
        });
        return sseEmitter;
    }

    // 셀의 실제 날짜값을 가져오는 헬퍼 메서드
    private LocalDate getDateFromCell(Cell cell, DataFormatter formatter, DateTimeFormatter dtf) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } else {
            String raw = getRawCellValue(cell, formatter);
            if (!raw.isEmpty()) {
                return LocalDate.parse(raw, dtf);
            }
        }
        return null;
    }

    // 셀의 원시값을 가져오는 헬퍼 메서드 (XSSFCell인 경우)
    private String getRawCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        if (cell instanceof XSSFCell) {
            XSSFCell xcell = (XSSFCell) cell;
            if (xcell.getCTCell().isSetV()) {
                return xcell.getCTCell().getV();
            }
        }
        return formatter.formatCellValue(cell);
    }

    // 환불 엑셀 파일의 내용을 처리하는 메서드 (업로드 시 사용)
    private void processRefundExcelFile(MultipartFile file, SseEmitter emitter) throws Exception {
        DataFormatter formatter = new DataFormatter(Locale.getDefault());
        // 날짜 형식: "yyyy-MM-dd"
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int startRow = 2; // 3행부터 데이터 추출 (0-indexed 2)
            int lastRow = sheet.getLastRowNum();
            int totalRows = lastRow - startRow + 1;
            int processed = 0;

            for (int i = startRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                CancelledCustomerRefund refund = new CancelledCustomerRefund();

                // A: 성명
                String name = formatter.formatCellValue(row.getCell(0));
                refund.setName(name);

                // customer_id를 이름으로 검색
                if (name != null && !name.trim().isEmpty()) {
                    List<Customer> matchingCustomers = customerRepository.findByCustomerDataNameContaining(name);
                    if (matchingCustomers != null && !matchingCustomers.isEmpty()) {
                        Customer selected = matchingCustomers.stream()
                                .max((c1, c2) -> c1.getId().compareTo(c2.getId()))
                                .orElse(null);
                        if (selected != null) {
                            refund.setCustomerId(selected.getId());
                        }
                    } else {
                        refund.setCustomerId(null);
                    }
                } else {
                    refund.setCustomerId(null);
                }

                // B: 주민번호 – 실제 원시값 사용
                refund.setResidentNumber(getRawCellValue(row.getCell(1), formatter));

                // C: 출처
                refund.setSource(formatter.formatCellValue(row.getCell(2)));

                // D: 납입금일자 – 실제 날짜값 사용
                refund.setPaymentDate(getDateFromCell(row.getCell(3), formatter, dateFormatter));

                // E: 납입금액
                String paymentAmountStr = formatter.formatCellValue(row.getCell(4));
                if (!paymentAmountStr.isEmpty()) {
                    try {
                        refund.setPaymentAmount(Long.parseLong(paymentAmountStr.replaceAll("[^0-9]", "")));
                    } catch (NumberFormatException nfe) {
                        refund.setPaymentAmount(0L);
                    }
                }

                // F: F열에 "x" 기록
                Cell cellF = row.createCell(5);
                cellF.setCellValue("x");

                // G: 해약일자 – 실제 날짜값 사용
                refund.setCancelDate(getDateFromCell(row.getCell(6), formatter, dateFormatter));

                // H: 환급일자 – 실제 날짜값 사용
                refund.setRefundDate(getDateFromCell(row.getCell(7), formatter, dateFormatter));

                // I: 환급금
                String refundAmountStr = formatter.formatCellValue(row.getCell(8));
                if (!refundAmountStr.isEmpty()) {
                    try {
                        refund.setRefundAmount(Long.parseLong(refundAmountStr.replaceAll("[^0-9]", "")));
                    } catch (NumberFormatException nfe) {
                        refund.setRefundAmount(0L);
                    }
                }

                // J: 기관
                refund.setInstitution(formatter.formatCellValue(row.getCell(9)));

                // K: 계좌번호
                refund.setAccountNumber(formatter.formatCellValue(row.getCell(10)));

                // L: 입금자
                refund.setDepositor(formatter.formatCellValue(row.getCell(11)));

                // M: 담당 총괄
                refund.setManagerGeneral(formatter.formatCellValue(row.getCell(12)));

                // N: 담당 본부
                refund.setManagerDivision(formatter.formatCellValue(row.getCell(13)));

                // O: 담당 팀
                refund.setManagerTeam(formatter.formatCellValue(row.getCell(14)));

                // P: 담당 성명
                refund.setManagerName(formatter.formatCellValue(row.getCell(15)));

                // Q: 건너뛰기 (셀 16 건너뜀)

                // R: 사유
                refund.setReason(formatter.formatCellValue(row.getCell(17)));

                // S: 비고
                refund.setRemarks(formatter.formatCellValue(row.getCell(18)));

                refundRepository.save(refund);
                processed++;
                if (processed % 10 == 0 || i == lastRow) {
                    emitter.send(SseEmitter.event().name("progress").data(processed + "/" + totalRows));
                }
            }
        }
    }

    // 모든 환불 레코드를 반환하는 메서드
    public List<CancelledCustomerRefund> getAllRefundRecords() {
        return refundRepository.findAll();
    }

    // fillRefformat: refformat.xlsx 템플릿에 환불 데이터를 기록하는 메서드
    // src/main/java/com/audora/lotting_be/service/RefundService.java
    public void fillRefformat(File tempFile, List<CancelledCustomerRefund> refunds, SseEmitter emitter) throws Exception {
        try (FileInputStream fis = new FileInputStream(tempFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            // 템플릿에서 헤더는 1행(0번행)이고, 데이터는 3행(0-indexed 2)부터 기록
            int startRow = 2;
            int currentRow = startRow;
            int total = refunds.size();

            for (CancelledCustomerRefund refund : refunds) {
                Row row = sheet.getRow(currentRow);
                if (row == null) {
                    row = sheet.createRow(currentRow);
                }
                // 기존 템플릿 셀 스타일 유지를 위해 MissingCellPolicy를 사용하여 셀 가져오기
                Cell cellA = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellA.setCellValue(refund.getName() != null ? refund.getName() : "");

                Cell cellB = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellB.setCellValue(refund.getResidentNumber() != null ? refund.getResidentNumber() : "");

                Cell cellC = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellC.setCellValue(refund.getSource() != null ? refund.getSource() : "");

                Cell cellD = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellD.setCellValue(refund.getPaymentDate() != null ? refund.getPaymentDate().toString() : "");

                Cell cellE = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellE.setCellValue(refund.getPaymentAmount() != null ? refund.getPaymentAmount() : 0);

                // F열: 항상 "x" 기록 (셀 인덱스 5)
                Cell cellF = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellF.setCellValue("x");

                Cell cellG = row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellG.setCellValue(refund.getCancelDate() != null ? refund.getCancelDate().toString() : "");

                Cell cellH = row.getCell(7, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellH.setCellValue(refund.getRefundDate() != null ? refund.getRefundDate().toString() : "");

                Cell cellI = row.getCell(8, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellI.setCellValue(refund.getRefundAmount() != null ? refund.getRefundAmount() : 0);

                Cell cellJ = row.getCell(9, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellJ.setCellValue(refund.getInstitution() != null ? refund.getInstitution() : "");

                Cell cellK = row.getCell(10, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellK.setCellValue(refund.getAccountNumber() != null ? refund.getAccountNumber() : "");

                Cell cellL = row.getCell(11, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellL.setCellValue(refund.getDepositor() != null ? refund.getDepositor() : "");

                Cell cellM = row.getCell(12, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellM.setCellValue(refund.getManagerGeneral() != null ? refund.getManagerGeneral() : "");

                Cell cellN = row.getCell(13, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellN.setCellValue(refund.getManagerDivision() != null ? refund.getManagerDivision() : "");

                Cell cellO = row.getCell(14, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellO.setCellValue(refund.getManagerTeam() != null ? refund.getManagerTeam() : "");

                Cell cellP = row.getCell(15, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellP.setCellValue(refund.getManagerName() != null ? refund.getManagerName() : "");

                // Q열은 건너뜀 (셀 인덱스 16은 그대로 둠)

                Cell cellR = row.getCell(17, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellR.setCellValue(refund.getReason() != null ? refund.getReason() : "");

                Cell cellS = row.getCell(18, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellS.setCellValue(refund.getRemarks() != null ? refund.getRemarks() : "");

                currentRow++;
                emitter.send(SseEmitter.event().name("progress").data((currentRow - startRow) + "/" + total));
            }
            workbook.setForceFormulaRecalculation(true);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                workbook.write(fos);
            }
        }
    }

}
