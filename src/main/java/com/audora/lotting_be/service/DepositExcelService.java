package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.model.customer.minor.Loan;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepositExcelService {

    private static final Logger logger = LoggerFactory.getLogger(DepositExcelService.class);

    private final DepositHistoryRepository depositHistoryRepository;
    private final CustomerRepository customerRepository;
    private final DepositHistoryService depositHistoryService;

    public DepositExcelService(DepositHistoryRepository depositHistoryRepository,
                               CustomerRepository customerRepository,
                               DepositHistoryService depositHistoryService) {
        this.depositHistoryRepository = depositHistoryRepository;
        this.customerRepository = customerRepository;
        this.depositHistoryService = depositHistoryService;
    }

    public void processDepositExcelFileWithProgress(MultipartFile file, SseEmitter emitter) throws IOException {
        DataFormatter formatter = new DataFormatter(Locale.getDefault());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (InputStream is = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            XSSFSheet sheet = workbook.getSheetAt(0);
            int startRow = 1; // 첫 행은 헤더
            int lastRow = sheet.getLastRowNum();
            int totalRows = lastRow - startRow + 1;
            logger.info("총 {}건의 행을 처리합니다.", totalRows);

            for (int i = startRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    logger.warn("행 {}가 null입니다. 건너뜁니다.", i);
                    continue;
                }

                try {
                    DepositHistory dh = new DepositHistory();

                    // A: 거래 id (인덱스 0)
//                    String idStr = formatter.formatCellValue(row.getCell(0));
//                    if (!idStr.isEmpty()) {
//                        try {
//                            dh.setId(Long.parseLong(idStr.replaceAll("[^0-9]", "")));
//                        } catch (NumberFormatException e) {
//                            logger.warn("행 {}: 거래 id 파싱 실패 - {}", i, e.getMessage());
//                        }
//                    }

// 거래일시 처리 (셀 인덱스 1)
                    Cell cellB = row.getCell(1);
                    LocalDateTime transactionDateTime = null;
                    if (cellB != null) {
                        String dateStr = formatter.formatCellValue(cellB);
                        if (!dateStr.isEmpty()) {
                            String cleaned = dateStr.replaceAll("[\\r\\n]+", " ").trim();
                            String[] patterns = {
                                    "yyyy.MM.dd HH:mm:ss",
                                    "yyyy-MM-dd HH:mm:ss",
                                    "yyyy.MM.dd"  // 시간 정보 없음
                            };
                            for (String pattern : patterns) {
                                try {
                                    DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                                    if (pattern.equals("yyyy.MM.dd")) {
                                        LocalDate ld = LocalDate.parse(cleaned, dtf2);
                                        transactionDateTime = ld.atStartOfDay();
                                    } else {
                                        transactionDateTime = LocalDateTime.parse(cleaned, dtf2);
                                    }
                                    break; // 성공하면 루프 탈출
                                } catch (Exception ex) {
                                    logger.warn("행 {}: 거래일시 '{}' 파싱 실패 with pattern {}: {}", i, cleaned, pattern, ex.getMessage());
                                }
                            }
                            if (transactionDateTime == null) {
                                logger.warn("행 {}: 거래일시 '{}' 파싱 실패 (시도한 패턴: {})", i, cleaned, String.join(", ", patterns));
                            }
                        }
                    }
                    dh.setTransactionDateTime(transactionDateTime);

                    // C: 적요 (인덱스 2)
                    dh.setDescription(formatter.formatCellValue(row.getCell(2)));

                    // D: 기재내용 (인덱스 3)
                    dh.setDetails(formatter.formatCellValue(row.getCell(3)));

// E: 계약자 (인덱스 4) → 고객 식별자로 활용
                    String contractor = formatter.formatCellValue(row.getCell(4)).trim();
                    dh.setContractor(contractor);
                    if (!contractor.isEmpty()) {
                        // 기존의 Optional<Customer> 대신, 해당 이름과 일치하는 고객들을 리스트로 조회
                        List<Customer> matchingCustomers = customerRepository.findByCustomerDataNameContaining(contractor)
                                .stream()
                                // 이름이 정확히 일치하는 고객만 필터링 (공백 제거 등 필요한 전처리 후 비교)
                                .filter(c -> contractor.equals(c.getCustomerData().getName()))
                                .collect(Collectors.toList());

                        if (!matchingCustomers.isEmpty()) {
                            // 여러 고객이 검색될 경우, 고객번호(id)가 가장 높은 고객 선택
                            Customer selectedCustomer = matchingCustomers.stream()
                                    .max(Comparator.comparing(Customer::getId))
                                    .get();
                            dh.setCustomer(selectedCustomer);
                        } else {
                            // 고객 이름과 일치하는 결과가 없으면 기본 고객(id:1)에 할당
                            Optional<Customer> defaultCustomerOpt = customerRepository.findById(1);
                            if (defaultCustomerOpt.isPresent()) {
                                dh.setCustomer(defaultCustomerOpt.get());
                            } else {
                                logger.warn("행 {}: 기본 고객(id:1)을 찾을 수 없습니다.", i);
                            }
                        }
                    }


                    // F: 찾으신금액 (인덱스 5)
                    String withdrawnStr = formatter.formatCellValue(row.getCell(5));
                    if (!withdrawnStr.isEmpty()) {
                        try {
                            dh.setWithdrawnAmount(Long.parseLong(withdrawnStr.replaceAll("[^0-9]", "")));
                        } catch (Exception ex) {
                            logger.warn("행 {}: 찾으신금액 파싱 실패 - {}", i, ex.getMessage());
                        }
                    }

                    // 맡기신금액 처리 (셀 인덱스 6)
                    String depositAmtStr = formatter.formatCellValue(row.getCell(6));
                    long depositAmt = 0L;
                    if (!depositAmtStr.isEmpty()) {
                        try {
                            depositAmt = Long.parseLong(depositAmtStr.replaceAll("[^0-9]", ""));
                            dh.setDepositAmount(depositAmt);
                        } catch (Exception ex) {
                            logger.warn("행 {}: 맡기신금액 파싱 실패 - {}", i, ex.getMessage());
                        }
                    }

                    // H: 거래후잔액 (인덱스 7)
                    String balanceStr = formatter.formatCellValue(row.getCell(7));
                    if (!balanceStr.isEmpty()) {
                        try {
                            dh.setBalanceAfter(Long.parseLong(balanceStr.replaceAll("[^0-9]", "")));
                        } catch (Exception ex) {
                            logger.warn("행 {}: 거래후잔액 파싱 실패 - {}", i, ex.getMessage());
                        }
                    }

                    // I: 취급점 (인덱스 8)
                    dh.setBranch(formatter.formatCellValue(row.getCell(8)));

                    // J: 계좌 (인덱스 9)
                    dh.setAccount(formatter.formatCellValue(row.getCell(9)));


                    // V: selfRecord (인덱스 21)
                    String selfRecord = formatter.formatCellValue(row.getCell(21)).trim();
                    // W: loanRecord (인덱스 22)
                    String loanRecord = formatter.formatCellValue(row.getCell(22)).trim();


                    // selfRecord 또는 loanRecord 값이 있다면 대출/자납 기록으로 처리
                    if (!selfRecord.isEmpty() || !loanRecord.isEmpty()) {
                        dh.setLoanStatus("o");

                        // 만약 loanRecord 값이 있다면 우선 loanammount에 depositAmt 저장
                        if (!loanRecord.isEmpty()) {
                            dh.setLoanRecord(loanRecord);
                            // loan_details 객체가 없으면 새로 생성
                            if (dh.getLoanDetails() == null) {
                                dh.setLoanDetails(new Loan());
                            }
                            dh.getLoanDetails().setLoanammount(depositAmt);
                        } else if (!selfRecord.isEmpty()) {
                            // loanRecord가 없고 selfRecord만 있으면 selfammount에 depositAmt 저장
                            dh.setSelfRecord(selfRecord);
                            if (dh.getLoanDetails() == null) {
                                dh.setLoanDetails(new Loan());
                            }
                            dh.getLoanDetails().setSelfammount(depositAmt);
                        }
                        // loanStatus가 "o"인 경우 loanselfsum에는 depositAmt를 저장
                        if (dh.getLoanDetails() == null) {
                            dh.setLoanDetails(new Loan());
                        }
                        dh.getLoanDetails().setLoanselfsum(depositAmt);
                    } else {
                        dh.setLoanStatus("");
                        dh.setSelfRecord("");
                        dh.setLoanRecord("");
                    }

                    // loanStatus가 "o"라면, depositPhase1~10 중 값이 있는 항목의 Phase 번호를 targetPhases에 추가
                    if ("o".equals(dh.getLoanStatus())) {
                        ArrayList<Integer> targetPhases = new ArrayList<>();
                        if (formatter.formatCellValue(row.getCell(11)) != null && !formatter.formatCellValue(row.getCell(11)).trim().isEmpty()) {
                            targetPhases.add(1);
                        }
                        if (formatter.formatCellValue(row.getCell(12)) != null && !formatter.formatCellValue(row.getCell(12)).trim().isEmpty()) {
                            targetPhases.add(2);
                        }
                        if (formatter.formatCellValue(row.getCell(13)) != null && !formatter.formatCellValue(row.getCell(13)).trim().isEmpty()) {
                            targetPhases.add(3);
                        }
                        if (formatter.formatCellValue(row.getCell(14)) != null && !formatter.formatCellValue(row.getCell(14)).trim().isEmpty()) {
                            targetPhases.add(4);
                        }
                        if (formatter.formatCellValue(row.getCell(15)) != null && !formatter.formatCellValue(row.getCell(15)).trim().isEmpty()) {
                            targetPhases.add(5);
                        }
                        if (formatter.formatCellValue(row.getCell(16)) != null && !formatter.formatCellValue(row.getCell(16)).trim().isEmpty()) {
                            targetPhases.add(6);
                        }
                        if (formatter.formatCellValue(row.getCell(17)) != null && !formatter.formatCellValue(row.getCell(17)).trim().isEmpty()) {
                            targetPhases.add(7);
                        }
                        if (formatter.formatCellValue(row.getCell(18)) != null && !formatter.formatCellValue(row.getCell(18)).trim().isEmpty()) {
                            targetPhases.add(8);
                        }
                        if (formatter.formatCellValue(row.getCell(19)) != null && !formatter.formatCellValue(row.getCell(19)).trim().isEmpty()) {
                            targetPhases.add(9);
                        }
                        if (formatter.formatCellValue(row.getCell(20)) != null && !formatter.formatCellValue(row.getCell(20)).trim().isEmpty()) {
                            targetPhases.add(10);
                        }
                        dh.setTargetPhases(targetPhases);
                    }
// 기존에 다른 셀들을 읽은 후, depositPhase1 셀을 추가로 읽습니다.
                    Cell depositPhase1Cell = row.getCell(11);
                    if (depositPhase1Cell != null) {
                        String depositPhase1Value = formatter.formatCellValue(depositPhase1Cell).trim();
                        dh.setDepositPhase1(depositPhase1Value);
                        logger.info("Row {} depositPhase1 값: {}", i, depositPhase1Value);
                    } else {
                        logger.info("Row {} depositPhase1 셀이 비어 있습니다.", i);
                    }
                    // 저장 및 재계산 호출
                    depositHistoryService.createDepositHistory(dh);
                    logger.info("행 {} 처리 완료.", i);

                } catch (Exception e) {
                    logger.error("행 {} 처리 중 예외 발생: {}", i, e.getMessage());
                    // 문제 발생한 행은 건너뛰고 계속 진행
                }

                // 10건마다 또는 마지막 행에서 진행률 전송
                if ((i - startRow + 1) % 10 == 0 || i == lastRow) {
                    try {
                        String progressMsg = (i - startRow + 1) + "/" + totalRows;
                        emitter.send(SseEmitter.event().name("progress").data(progressMsg));
                        logger.info("진행 상황: {}", progressMsg);
                    } catch (Exception ex) {
                        logger.warn("행 {}에서 진행 상황 전송 중 오류: {}", i, ex.getMessage());
                    }
                }
            }

            emitter.send(SseEmitter.event().name("complete").data("Deposit excel processing complete."));
            emitter.complete();
        } catch (IOException e) {
            logger.error("엑셀 파일 처리 중 IOException 발생: {}", e.getMessage());
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
            } catch (Exception ex) {
                logger.error("에러 이벤트 전송 중 예외 발생: {}", ex.getMessage());
            }
            emitter.completeWithError(e);
        }
    }

    @Transactional
    public void fillDepFormat(File tempFile, List<DepositHistory> depositHistories) throws IOException {
        // 데이터 포매터 및 날짜 포맷터 준비
        DataFormatter formatter = new DataFormatter(Locale.getDefault());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try (FileInputStream fis = new FileInputStream(tempFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);
            int startRow = 1; // 0번 행은 헤더

            for (int i = 0; i < depositHistories.size(); i++) {
                DepositHistory dh = depositHistories.get(i);
                Row row = sheet.getRow(startRow + i);
                if (row == null) {
                    row = sheet.createRow(startRow + i);
                }
                int col = 0;
                Cell cell;

                // Column 0: DepositHistory ID
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getId() != null ? dh.getId() : 0);
                col++;

                // Column 1: 거래일시
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getTransactionDateTime() != null ? dh.getTransactionDateTime().format(dtf) : "");
                col++;

                // Column 2: 적요
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDescription() != null ? dh.getDescription() : "");
                col++;

                // Column 3: 기재내용
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDetails() != null ? dh.getDetails() : "");
                col++;

                // Column 4: 계약자
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getContractor() != null ? dh.getContractor() : "");
                col++;

                // Column 5: 찾으신금액
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getWithdrawnAmount() != null ? dh.getWithdrawnAmount() : 0);
                col++;

                // Column 6: 맡기신금액
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositAmount() != null ? dh.getDepositAmount() : 0);
                col++;

                // Column 7: 거래후 잔액
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getBalanceAfter() != null ? dh.getBalanceAfter() : 0);
                col++;

                // Column 8: 취급점
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getBranch() != null ? dh.getBranch() : "");
                col++;

                // Column 9: 계좌
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getAccount() != null ? dh.getAccount() : "");
                col++;

                col++;

                // Column 10: depositPhase1
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase1() != null ? dh.getDepositPhase1() : "");
                col++;


                // Column 11: depositPhase2
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase2() != null ? dh.getDepositPhase2() : "");
                col++;

                // Column 12: depositPhase3
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase3() != null ? dh.getDepositPhase3() : "");
                col++;

                // Column 13: depositPhase4
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase4() != null ? dh.getDepositPhase4() : "");
                col++;

                // Column 14: depositPhase5
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase5() != null ? dh.getDepositPhase5() : "");
                col++;

                // Column 15: depositPhase6
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase6() != null ? dh.getDepositPhase6() : "");
                col++;

                // Column 16: depositPhase7
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase7() != null ? dh.getDepositPhase7() : "");
                col++;

                // Column 17: depositPhase8
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase8() != null ? dh.getDepositPhase8() : "");
                col++;

                // Column 18: depositPhase9
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase9() != null ? dh.getDepositPhase9() : "");
                col++;

                // Column 19: depositPhase10
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getDepositPhase10() != null ? dh.getDepositPhase10() : "");
                col++;

                // Column 20: selfRec
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getSelfRecord() != null ? dh.getSelfRecord() : "");
                col++;

                // Column 21: loanRecord
                cell = row.getCell(col);
                if (cell == null) { cell = row.createCell(col); }
                cell.setCellValue(dh.getLoanRecord() != null ? dh.getLoanRecord() : "");
                col++;

            } // for end

            workbook.setForceFormulaRecalculation(true);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                workbook.write(fos);
            }
        }
    }



}
