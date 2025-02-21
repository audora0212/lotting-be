package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

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

    /**
     * 업로드된 엑셀 파일을 읽어 DepositHistory 엔티티를 생성하고 DB에 저장합니다.
     * 진행 상황은 SseEmitter를 통해 클라이언트로 전달됩니다.
     *
     * @param file    업로드된 엑셀 파일
     * @param emitter SSEEmitter (타임아웃 시간은 컨트롤러에서 설정)
     * @throws IOException 엑셀 파일 I/O 예외
     */
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
                    String idStr = formatter.formatCellValue(row.getCell(0));
                    if (!idStr.isEmpty()) {
                        try {
                            dh.setId(Long.parseLong(idStr.replaceAll("[^0-9]", "")));
                        } catch (NumberFormatException e) {
                            logger.warn("행 {}: 거래 id 파싱 실패 - {}", i, e.getMessage());
                        }
                    }

                    Cell cellB = row.getCell(1);
                    LocalDateTime transactionDateTime = null;
                    if (cellB != null) {
                        // 우선 DataFormatter로 문자열 추출
                        String dateStr = formatter.formatCellValue(cellB);
                        if (!dateStr.isEmpty()) {
                            // 1) 개행(\n, \r\n) 제거(공백 치환)
                            String cleaned = dateStr.replaceAll("[\r\n]+", " ").trim();
                            // 예: "2022.03.18\n15:17:42" → "2022.03.18 15:17:42"

                            // 2) 복수 패턴 시도
                            String[] patterns = {
                                    "yyyy.MM.dd HH:mm:ss",
                                    "yyyy-MM-dd HH:mm:ss",
                                    // 필요하다면 더 추가 가능
                            };

                            for (String pattern : patterns) {
                                try {
                                    DateTimeFormatter customDtf = DateTimeFormatter.ofPattern(pattern);
                                    transactionDateTime = LocalDateTime.parse(cleaned, customDtf);
                                    // 성공하면 루프 탈출
                                    break;
                                } catch (Exception ex) {
                                    // 실패하면 다음 패턴으로 넘어감
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
                        Optional<Customer> customerOpt = customerRepository.findByCustomerDataName(contractor);
                        if (customerOpt.isPresent()) {
                            dh.setCustomer(customerOpt.get());
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

                    // G: 맡기신금액 (인덱스 6)
                    String depositAmtStr = formatter.formatCellValue(row.getCell(6));
                    if (!depositAmtStr.isEmpty()) {
                        try {
                            dh.setDepositAmount(Long.parseLong(depositAmtStr.replaceAll("[^0-9]", "")));
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
                    dh.setSelfRecord(selfRecord);

                    // W: loanRecord (인덱스 22)
                    String loanRecord = formatter.formatCellValue(row.getCell(22)).trim();
                    dh.setLoanRecord(loanRecord);

                    // selfRecord와 loanRecord 중 하나라도 값이 있으면 loanStatus를 "o"로 설정
                    if (!selfRecord.isEmpty() || !loanRecord.isEmpty()) {
                        dh.setLoanStatus("o");
                    } else {
                        dh.setLoanStatus("");
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
}
