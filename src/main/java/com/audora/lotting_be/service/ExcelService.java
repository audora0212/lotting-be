package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Status;
import com.audora.lotting_be.model.customer.minor.Agenda;
import com.audora.lotting_be.model.customer.Attachments;
import com.audora.lotting_be.model.customer.minor.Cancel;
import com.audora.lotting_be.model.customer.minor.CustomerData;
import com.audora.lotting_be.model.customer.minor.Dahim;
import com.audora.lotting_be.model.customer.minor.Deposit;
import com.audora.lotting_be.model.customer.minor.Financial;
import com.audora.lotting_be.model.customer.minor.Firstemp;
import com.audora.lotting_be.model.customer.minor.LegalAddress;
import com.audora.lotting_be.model.customer.minor.Loan;
import com.audora.lotting_be.model.customer.minor.Meetingattend;
import com.audora.lotting_be.model.customer.minor.MGM;
import com.audora.lotting_be.model.customer.minor.Postreceive;
import com.audora.lotting_be.model.customer.minor.Responsible;
import com.audora.lotting_be.model.customer.minor.Secondemp;
import com.audora.lotting_be.model.customer.Phase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Service
public class ExcelService {

    @Autowired
    private CustomerService customerService;

    public void fillRegFormat(File tempFile, List<Customer> customers, SseEmitter emitter) throws IOException {
        try (FileInputStream fis = new FileInputStream(tempFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);
            int rowIndex = 2;
            int total = customers.size();

            for (int i = 0; i < total; i++) {
                Customer customer = customers.get(i);

                // 고객 id가 1이면 건너뛰기
                if (customer.getId() == 1) {
                    continue;
                }
                System.out.println(customer.getId());

                // 해당 행 가져오기(없으면 생성)
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    row = sheet.createRow(rowIndex);
                }

                //고객정보 기입시작 code spread start
                // ── 기본 정보 ──
                // Column A (0): 관리번호
                Cell cellA = row.getCell(0);
                if (cellA == null) cellA = row.createCell(0);
                cellA.setCellValue(customer.getId());

                // Column B (1): 분류(회원)
                Cell cellB = row.getCell(1);
                if (cellB == null) cellB = row.createCell(1);
                cellB.setCellValue(customer.getCustomertype() != null ? customer.getCustomertype() : "");

                // Column C (2): 타입
                Cell cellC = row.getCell(2);
                if (cellC == null) cellC = row.createCell(2);
                cellC.setCellValue(customer.getType() != null ? customer.getType() : "");

                // Column D (3): 군
                Cell cellD = row.getCell(3);
                if (cellD == null) cellD = row.createCell(3);
                cellD.setCellValue(customer.getGroupname() != null ? customer.getGroupname() : "");

                // Column E (4): 순번
                Cell cellE = row.getCell(4);
                if (cellE == null) cellE = row.createCell(4);
                cellE.setCellValue(customer.getTurn() != null ? customer.getTurn() : "");

                // Column F (5): 7차면제
                Cell cellF = row.getCell(5);
                if (cellF == null) cellF = row.createCell(5);
                cellF.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getExemption7())) ? "o" : "");

                // Column G (6): 임시동호
                Cell cellG = row.getCell(6);
                if (cellG == null) cellG = row.createCell(6);
                cellG.setCellValue(customer.getTemptype() != null ? customer.getTemptype() : "");

                // Column H (7): 가입차순
                Cell cellH = row.getCell(7);
                if (cellH == null) cellH = row.createCell(7);
                cellH.setCellValue(customer.getBatch() != null ? customer.getBatch() : "");

                // Column I (8): 신탁사제출
                Cell cellI = row.getCell(8);
                if (cellI == null) cellI = row.createCell(8);
                cellI.setCellValue((customer.getFinancial() != null && customer.getFinancial().getTrustcompanydate() != null)
                        ? customer.getFinancial().getTrustcompanydate().toString() : "");

                // Column J (9): 가입일자
                Cell cellJ = row.getCell(9);
                if (cellJ == null) cellJ = row.createCell(9);
                cellJ.setCellValue(customer.getRegisterdate() != null ? customer.getRegisterdate().toString() : "");

                // Column K (10): 가입가
                Cell cellK = row.getCell(10);
                if (cellK == null) cellK = row.createCell(10);
                cellK.setCellValue(customer.getRegisterprice() != null ? customer.getRegisterprice() : 0);

                // Column L (11): 지산A동계약서
                Cell cellL = row.getCell(11);
                if (cellL == null) cellL = row.createCell(11);
                cellL.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getContract())) ? "1" : "");

                // Column M (12): 동의서
                Cell cellM = row.getCell(12);
                if (cellM == null) cellM = row.createCell(12);
                cellM.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getAgreement())) ? "1" : "");

                // Column N (13): 성명
                Cell cellN = row.getCell(13);
                if (cellN == null) cellN = row.createCell(13);
                cellN.setCellValue((customer.getCustomerData() != null && customer.getCustomerData().getName() != null)
                        ? customer.getCustomerData().getName() : "");

                // Column O (14): 주민번호 (resnumfront-resnumback)
                Cell cellO = row.getCell(14);
                if (cellO == null) cellO = row.createCell(14);
                if (customer.getCustomerData() != null
                        && customer.getCustomerData().getResnumfront() != null
                        && customer.getCustomerData().getResnumback() != null) {
                    cellO.setCellValue(customer.getCustomerData().getResnumfront() + "-" + customer.getCustomerData().getResnumback());
                } else {
                    cellO.setCellValue("");
                }

                // Column P (15): 휴대전화
                Cell cellP = row.getCell(15);
                if (cellP == null) cellP = row.createCell(15);
                cellP.setCellValue((customer.getCustomerData() != null && customer.getCustomerData().getPhone() != null)
                        ? customer.getCustomerData().getPhone() : "");

                // Column Q (16): 법정주소 우편번호
                Cell cellQ2 = row.getCell(16);
                if (cellQ2 == null) cellQ2 = row.createCell(16);
                cellQ2.setCellValue((customer.getLegalAddress() != null && customer.getLegalAddress().getPostnumber() != null)
                        ? customer.getLegalAddress().getPostnumber() : "");

                // ── 법정주소 도/군 분리 ──
                String legalPostFull = (customer.getLegalAddress() != null && customer.getLegalAddress().getPost() != null)
                        ? customer.getLegalAddress().getPost() : "";
                String[] legalParts = legalPostFull.split("\\s+");
                String legalDo = legalParts.length >= 1 ? legalParts[0] : "";
                String legalGun = legalParts.length >= 2 ? legalParts[1] : "";
                // Column R (17): 법정주소 - 도
                Cell cellR = row.getCell(17);
                if (cellR == null) cellR = row.createCell(17);
                cellR.setCellValue(legalDo);
                // Column S (18): 법정주소 - 군
                Cell cellS = row.getCell(18);
                if (cellS == null) cellS = row.createCell(18);
                cellS.setCellValue(legalGun);

                // Column T (19): 법정주소 상세주소
                Cell cellT = row.getCell(19);
                if (cellT == null) cellT = row.createCell(19);
                cellT.setCellValue((customer.getLegalAddress() != null && customer.getLegalAddress().getDetailaddress() != null)
                        ? customer.getLegalAddress().getDetailaddress() : "");

                // Column U (20): 금융기관 은행명
                Cell cellU = row.getCell(20);
                if (cellU == null) cellU = row.createCell(20);
                cellU.setCellValue((customer.getFinancial() != null && customer.getFinancial().getBankname() != null)
                        ? customer.getFinancial().getBankname() : "");

                // Column V (21): 금융기관 계좌번호
                Cell cellV = row.getCell(21);
                if (cellV == null) cellV = row.createCell(21);
                cellV.setCellValue((customer.getFinancial() != null && customer.getFinancial().getAccountnum() != null)
                        ? customer.getFinancial().getAccountnum() : "");

                // Column W (22): 금융기관 예금주
                Cell cellW = row.getCell(22);
                if (cellW == null) cellW = row.createCell(22);
                cellW.setCellValue((customer.getFinancial() != null && customer.getFinancial().getAccountholder() != null)
                        ? customer.getFinancial().getAccountholder() : "");

                // Column X (23): 금융기관 신탁사
                Cell cellX = row.getCell(23);
                if (cellX == null) cellX = row.createCell(23);
                cellX.setCellValue((customer.getFinancial() != null && customer.getFinancial().getTrustcompany() != null)
                        ? customer.getFinancial().getTrustcompany() : "");

                // Column Y (24): 예약금 납입일자
                Cell cellY = row.getCell(24);
                if (cellY == null) cellY = row.createCell(24);
                cellY.setCellValue((customer.getDeposits() != null && customer.getDeposits().getDepositdate() != null)
                        ? customer.getDeposits().getDepositdate().toString() : "");

                // Column Z (25): 예약금 금액
                Cell cellZ = row.getCell(25);
                if (cellZ == null) cellZ = row.createCell(25);
                cellZ.setCellValue((customer.getDeposits() != null && customer.getDeposits().getDepositammount() != null)
                        ? customer.getDeposits().getDepositammount() : 0);

                // ── Phase 데이터 ──
                // Phase 1 (인덱스 0)
                // Column AA (26): 1차 완납일자
                Cell cellAA = row.getCell(26);
                if (cellAA == null) cellAA = row.createCell(26);
                cellAA.setCellValue((customer.getPhases() != null && customer.getPhases().size() > 0
                        && customer.getPhases().get(0).getFullpaiddate() != null)
                        ? customer.getPhases().get(0).getFullpaiddate().toString() : "");
                // Column AB (27): 1차 부담금
                Cell cellAB = row.getCell(27);
                if (cellAB == null) cellAB = row.createCell(27);
                cellAB.setCellValue((customer.getPhases() != null && customer.getPhases().size() > 0
                        && customer.getPhases().get(0).getCharge() != null)
                        ? customer.getPhases().get(0).getCharge() : 0);
                // Column AC (28): 1차 업무대행비
                Cell cellAC = row.getCell(28);
                if (cellAC == null) cellAC = row.createCell(28);
                cellAC.setCellValue((customer.getPhases() != null && customer.getPhases().size() > 0
                        && customer.getPhases().get(0).getService() != null)
                        ? customer.getPhases().get(0).getService() : 0);
                // Column AD (29): 1차 이동
                Cell cellAD = row.getCell(29);
                if (cellAD == null) cellAD = row.createCell(29);
                cellAD.setCellValue((customer.getPhases() != null && customer.getPhases().size() > 0
                        && customer.getPhases().get(0).getMove() != null)
                        ? customer.getPhases().get(0).getMove() : "");
                // Column AE (30): 1차 합
                Cell cellAE = row.getCell(30);
                if (cellAE == null) cellAE = row.createCell(30);
                cellAE.setCellValue((customer.getPhases() != null && customer.getPhases().size() > 0
                        && customer.getPhases().get(0).getFeesum() != null)
                        ? customer.getPhases().get(0).getFeesum() : 0);

                // Phase 2 (인덱스 1) – 예정일자 조건 처리
                // Column AF (31): 2차 예정일자
                Cell cellAF = row.getCell(31);
                if (cellAF == null) cellAF = row.createCell(31);
                if (customer.getPhases() != null && customer.getPhases().size() > 1) {
                    Phase phase2 = customer.getPhases().get(1);
                    if (phase2.getPlanneddate() != null && !phase2.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellAF.setCellValue(phase2.getPlanneddate().toString());
                    } else {
                        cellAF.setCellValue(phase2.getPlanneddateString() != null ? phase2.getPlanneddateString() : "");
                    }
                } else {
                    cellAF.setCellValue("");
                }
                // Column AG (32): 2차 완납일자
                Cell cellAG = row.getCell(32);
                if (cellAG == null) cellAG = row.createCell(32);
                if (customer.getPhases() != null && customer.getPhases().size() > 1) {
                    Phase phase2 = customer.getPhases().get(1);
                    cellAG.setCellValue(phase2.getFullpaiddate() != null ? phase2.getFullpaiddate().toString() : "");
                } else {
                    cellAG.setCellValue("");
                }
                // Column AH (33): 2차 부담금
                Cell cellAH = row.getCell(33);
                if (cellAH == null) cellAH = row.createCell(33);
                if (customer.getPhases() != null && customer.getPhases().size() > 1) {
                    Phase phase2 = customer.getPhases().get(1);
                    cellAH.setCellValue(phase2.getCharge() != null ? phase2.getCharge() : 0);
                } else {
                    cellAH.setCellValue(0);
                }
                // Column AI (34): 2차 할인액
                Cell cellAI = row.getCell(34);
                if (cellAI == null) cellAI = row.createCell(34);
                if (customer.getPhases() != null && customer.getPhases().size() > 1) {
                    Phase phase2 = customer.getPhases().get(1);
                    cellAI.setCellValue(phase2.getDiscount() != null ? Math.abs(phase2.getDiscount()) : 0);
                } else {
                    cellAI.setCellValue(0);
                }
                // Column AJ (35): 2차 업무대행비
                Cell cellAJ = row.getCell(35);
                if (cellAJ == null) cellAJ = row.createCell(35);
                if (customer.getPhases() != null && customer.getPhases().size() > 1) {
                    Phase phase2 = customer.getPhases().get(1);
                    cellAJ.setCellValue(phase2.getService() != null ? phase2.getService() : 0);
                } else {
                    cellAJ.setCellValue(0);
                }
                // Column AK (36): 2차 이동
                Cell cellAK = row.getCell(36);
                if (cellAK == null) cellAK = row.createCell(36);
                if (customer.getPhases() != null && customer.getPhases().size() > 1) {
                    Phase phase2 = customer.getPhases().get(1);
                    cellAK.setCellValue(phase2.getMove() != null ? phase2.getMove() : "");
                } else {
                    cellAK.setCellValue("");
                }
                // Column AL (37): 2차 합
                Cell cellAL = row.getCell(37);
                if (cellAL == null) cellAL = row.createCell(37);
                if (customer.getPhases() != null && customer.getPhases().size() > 1) {
                    Phase phase2 = customer.getPhases().get(1);
                    cellAL.setCellValue(phase2.getFeesum() != null ? phase2.getFeesum() : 0);
                } else {
                    cellAL.setCellValue(0);
                }

                // Phase 3 (인덱스 2) – 예정일자 조건 처리
                // Column AM (38): 3차 예정일자
                Cell cellAM = row.getCell(38);
                if (cellAM == null) cellAM = row.createCell(38);
                if (customer.getPhases() != null && customer.getPhases().size() > 2) {
                    Phase phase3 = customer.getPhases().get(2);
                    if (phase3.getPlanneddate() != null && !phase3.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellAM.setCellValue(phase3.getPlanneddate().toString());
                    } else {
                        cellAM.setCellValue(phase3.getPlanneddateString() != null ? phase3.getPlanneddateString() : "");
                    }
                } else {
                    cellAM.setCellValue("");
                }
                // Column AN (39): 3차 완납일자
                Cell cellAN = row.getCell(39);
                if (cellAN == null) cellAN = row.createCell(39);
                if (customer.getPhases() != null && customer.getPhases().size() > 2) {
                    Phase phase3 = customer.getPhases().get(2);
                    cellAN.setCellValue(phase3.getFullpaiddate() != null ? phase3.getFullpaiddate().toString() : "");
                } else {
                    cellAN.setCellValue("");
                }
                // Column AO (40): 3차 부담금
                Cell cellAO = row.getCell(40);
                if (cellAO == null) cellAO = row.createCell(40);
                if (customer.getPhases() != null && customer.getPhases().size() > 2) {
                    Phase phase3 = customer.getPhases().get(2);
                    cellAO.setCellValue(phase3.getCharge() != null ? phase3.getCharge() : 0);
                } else {
                    cellAO.setCellValue(0);
                }
                // Column AP (41): 3차 할인액
                Cell cellAP = row.getCell(41);
                if (cellAP == null) cellAP = row.createCell(41);
                if (customer.getPhases() != null && customer.getPhases().size() > 2) {
                    Phase phase3 = customer.getPhases().get(2);
                    cellAP.setCellValue(phase3.getDiscount() != null ? Math.abs(phase3.getDiscount()) : 0);
                } else {
                    cellAP.setCellValue(0);
                }
                // Column AQ (42): 3차 업무대행비
                Cell cellAQ = row.getCell(42);
                if (cellAQ == null) cellAQ = row.createCell(42);
                if (customer.getPhases() != null && customer.getPhases().size() > 2) {
                    Phase phase3 = customer.getPhases().get(2);
                    cellAQ.setCellValue(phase3.getService() != null ? phase3.getService() : 0);
                } else {
                    cellAQ.setCellValue(0);
                }
                // Column AR (43): 3차 이동
                Cell cellAR = row.getCell(43);
                if (cellAR == null) cellAR = row.createCell(43);
                if (customer.getPhases() != null && customer.getPhases().size() > 2) {
                    Phase phase3 = customer.getPhases().get(2);
                    cellAR.setCellValue(phase3.getMove() != null ? phase3.getMove() : "");
                } else {
                    cellAR.setCellValue("");
                }
                // Column AS (44): 3차 합
                Cell cellAS = row.getCell(44);
                if (cellAS == null) cellAS = row.createCell(44);
                if (customer.getPhases() != null && customer.getPhases().size() > 2) {
                    Phase phase3 = customer.getPhases().get(2);
                    cellAS.setCellValue(phase3.getFeesum() != null ? phase3.getFeesum() : 0);
                } else {
                    cellAS.setCellValue(0);
                }

                // Phase 4 (인덱스 3) – 예정일자 조건 처리
                // Column AT (45): 4차 예정일자
                Cell cellAT = row.getCell(45);
                if (cellAT == null) cellAT = row.createCell(45);
                if (customer.getPhases() != null && customer.getPhases().size() > 3) {
                    Phase phase4 = customer.getPhases().get(3);
                    if (phase4.getPlanneddate() != null && !phase4.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellAT.setCellValue(phase4.getPlanneddate().toString());
                    } else {
                        cellAT.setCellValue(phase4.getPlanneddateString() != null ? phase4.getPlanneddateString() : "");
                    }
                } else {
                    cellAT.setCellValue("");
                }
                // Column AU (46): 4차 완납일자
                Cell cellAU = row.getCell(46);
                if (cellAU == null) cellAU = row.createCell(46);
                if (customer.getPhases() != null && customer.getPhases().size() > 3) {
                    Phase phase4 = customer.getPhases().get(3);
                    cellAU.setCellValue(phase4.getFullpaiddate() != null ? phase4.getFullpaiddate().toString() : "");
                } else {
                    cellAU.setCellValue("");
                }
                // Column AV (47): 4차 부담금
                Cell cellAV = row.getCell(47);
                if (cellAV == null) cellAV = row.createCell(47);
                if (customer.getPhases() != null && customer.getPhases().size() > 3) {
                    Phase phase4 = customer.getPhases().get(3);
                    cellAV.setCellValue(phase4.getCharge() != null ? phase4.getCharge() : 0);
                } else {
                    cellAV.setCellValue(0);
                }
                // Column AW (48): 4차 할인액
                Cell cellAW = row.getCell(48);
                if (cellAW == null) cellAW = row.createCell(48);
                if (customer.getPhases() != null && customer.getPhases().size() > 3) {
                    Phase phase4 = customer.getPhases().get(3);
                    cellAW.setCellValue(phase4.getDiscount() != null ? Math.abs(phase4.getDiscount()) : 0);
                } else {
                    cellAW.setCellValue(0);
                }
                // Column AX (49): 4차 업무대행비
                Cell cellAX = row.getCell(49);
                if (cellAX == null) cellAX = row.createCell(49);
                if (customer.getPhases() != null && customer.getPhases().size() > 3) {
                    Phase phase4 = customer.getPhases().get(3);
                    cellAX.setCellValue(phase4.getService() != null ? phase4.getService() : 0);
                } else {
                    cellAX.setCellValue(0);
                }
                // Column AY (50): 4차 이동
                Cell cellAY = row.getCell(50);
                if (cellAY == null) cellAY = row.createCell(50);
                if (customer.getPhases() != null && customer.getPhases().size() > 3) {
                    Phase phase4 = customer.getPhases().get(3);
                    cellAY.setCellValue(phase4.getMove() != null ? phase4.getMove() : "");
                } else {
                    cellAY.setCellValue("");
                }
                // Column AZ (51): 4차 합
                Cell cellAZ = row.getCell(51);
                if (cellAZ == null) cellAZ = row.createCell(51);
                if (customer.getPhases() != null && customer.getPhases().size() > 3) {
                    Phase phase4 = customer.getPhases().get(3);
                    cellAZ.setCellValue(phase4.getFeesum() != null ? phase4.getFeesum() : 0);
                } else {
                    cellAZ.setCellValue(0);
                }

                // Phase 5 (인덱스 4) – 예정일자 조건 처리
                // Column BA (52): 5차 예정일자
                Cell cellBA = row.getCell(52);
                if (cellBA == null) cellBA = row.createCell(52);
                if (customer.getPhases() != null && customer.getPhases().size() > 4) {
                    Phase phase5 = customer.getPhases().get(4);
                    if (phase5.getPlanneddate() != null && !phase5.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellBA.setCellValue(phase5.getPlanneddate().toString());
                    } else {
                        cellBA.setCellValue(phase5.getPlanneddateString() != null ? phase5.getPlanneddateString() : "");
                    }
                } else {
                    cellBA.setCellValue("");
                }
                // Column BB (53): 5차 완납일자
                Cell cellBB = row.getCell(53);
                if (cellBB == null) cellBB = row.createCell(53);
                if (customer.getPhases() != null && customer.getPhases().size() > 4) {
                    Phase phase5 = customer.getPhases().get(4);
                    cellBB.setCellValue(phase5.getFullpaiddate() != null ? phase5.getFullpaiddate().toString() : "");
                } else {
                    cellBB.setCellValue("");
                }
                // Column BC (54): 5차 부담금
                Cell cellBC = row.getCell(54);
                if (cellBC == null) cellBC = row.createCell(54);
                if (customer.getPhases() != null && customer.getPhases().size() > 4) {
                    Phase phase5 = customer.getPhases().get(4);
                    cellBC.setCellValue(phase5.getCharge() != null ? phase5.getCharge() : 0);
                } else {
                    cellBC.setCellValue(0);
                }
                // Column BD (55): 5차 할인액
                Cell cellBD = row.getCell(55);
                if (cellBD == null) cellBD = row.createCell(55);
                if (customer.getPhases() != null && customer.getPhases().size() > 4) {
                    Phase phase5 = customer.getPhases().get(4);
                    cellBD.setCellValue(phase5.getDiscount() != null ? Math.abs(phase5.getDiscount()) : 0);
                } else {
                    cellBD.setCellValue(0);
                }
                // Column BE (56): 5차 면제금액
                Cell cellBE = row.getCell(56);
                if (cellBE == null) cellBE = row.createCell(56);
                if (customer.getPhases() != null && customer.getPhases().size() > 4) {
                    Phase phase5 = customer.getPhases().get(4);
                    cellBE.setCellValue(phase5.getExemption() != null ? Math.abs(phase5.getExemption()) : 0);
                } else {
                    cellBE.setCellValue(0);
                }
                // Column BF (57): 5차 업무대행비
                Cell cellBF = row.getCell(57);
                if (cellBF == null) cellBF = row.createCell(57);
                if (customer.getPhases() != null && customer.getPhases().size() > 4) {
                    Phase phase5 = customer.getPhases().get(4);
                    cellBF.setCellValue(phase5.getService() != null ? phase5.getService() : 0);
                } else {
                    cellBF.setCellValue(0);
                }
                // Column BG (58): 5차 이동
                Cell cellBG = row.getCell(58);
                if (cellBG == null) cellBG = row.createCell(58);
                if (customer.getPhases() != null && customer.getPhases().size() > 4) {
                    Phase phase5 = customer.getPhases().get(4);
                    cellBG.setCellValue(phase5.getMove() != null ? phase5.getMove() : "");
                } else {
                    cellBG.setCellValue("");
                }
                // Column BH (59): 5차 합
                Cell cellBH = row.getCell(59);
                if (cellBH == null) cellBH = row.createCell(59);
                if (customer.getPhases() != null && customer.getPhases().size() > 4) {
                    Phase phase5 = customer.getPhases().get(4);
                    cellBH.setCellValue(phase5.getFeesum() != null ? phase5.getFeesum() : 0);
                } else {
                    cellBH.setCellValue(0);
                }

                // Phase 6 (인덱스 5) – 예정일자 조건 처리
                // Column BI (60): 6차 예정일자
                Cell cellBI = row.getCell(60);
                if (cellBI == null) cellBI = row.createCell(60);
                if (customer.getPhases() != null && customer.getPhases().size() > 5) {
                    Phase phase6 = customer.getPhases().get(5);
                    if (phase6.getPlanneddate() != null && !phase6.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellBI.setCellValue(phase6.getPlanneddate().toString());
                    } else {
                        cellBI.setCellValue(phase6.getPlanneddateString() != null ? phase6.getPlanneddateString() : "");
                    }
                } else {
                    cellBI.setCellValue("");
                }
                // Column BJ (61): 6차 완납일자
                Cell cellBJ = row.getCell(61);
                if (cellBJ == null) cellBJ = row.createCell(61);
                if (customer.getPhases() != null && customer.getPhases().size() > 5) {
                    Phase phase6 = customer.getPhases().get(5);
                    cellBJ.setCellValue(phase6.getFullpaiddate() != null ? phase6.getFullpaiddate().toString() : "");
                } else {
                    cellBJ.setCellValue("");
                }
                // Column BK (62): 6차 부담금
                Cell cellBK = row.getCell(62);
                if (cellBK == null) cellBK = row.createCell(62);
                if (customer.getPhases() != null && customer.getPhases().size() > 5) {
                    Phase phase6 = customer.getPhases().get(5);
                    cellBK.setCellValue(phase6.getCharge() != null ? phase6.getCharge() : 0);
                } else {
                    cellBK.setCellValue(0);
                }
                // Column BL (63): 6차 할인액
                Cell cellBL = row.getCell(63);
                if (cellBL == null) cellBL = row.createCell(63);
                if (customer.getPhases() != null && customer.getPhases().size() > 5) {
                    Phase phase6 = customer.getPhases().get(5);
                    cellBL.setCellValue(phase6.getDiscount() != null ? Math.abs(phase6.getDiscount()) : 0);
                } else {
                    cellBL.setCellValue(0);
                }
                // Column BM (64): 6차 면제금액
                Cell cellBM = row.getCell(64);
                if (cellBM == null) cellBM = row.createCell(64);
                if (customer.getPhases() != null && customer.getPhases().size() > 5) {
                    Phase phase6 = customer.getPhases().get(5);
                    cellBM.setCellValue(phase6.getExemption() != null ? Math.abs(phase6.getExemption()) : 0);
                } else {
                    cellBM.setCellValue(0);
                }
                // Column BN (65): 6차 업무대행비
                Cell cellBN = row.getCell(65);
                if (cellBN == null) cellBN = row.createCell(65);
                if (customer.getPhases() != null && customer.getPhases().size() > 5) {
                    Phase phase6 = customer.getPhases().get(5);
                    cellBN.setCellValue(phase6.getService() != null ? phase6.getService() : 0);
                } else {
                    cellBN.setCellValue(0);
                }
                // Column BO (66): 6차 이동
                Cell cellBO = row.getCell(66);
                if (cellBO == null) cellBO = row.createCell(66);
                if (customer.getPhases() != null && customer.getPhases().size() > 5) {
                    Phase phase6 = customer.getPhases().get(5);
                    cellBO.setCellValue(phase6.getMove() != null ? phase6.getMove() : "");
                } else {
                    cellBO.setCellValue("");
                }
                // Column BP (67): 6차 합
                Cell cellBP = row.getCell(67);
                if (cellBP == null) cellBP = row.createCell(67);
                if (customer.getPhases() != null && customer.getPhases().size() > 5) {
                    Phase phase6 = customer.getPhases().get(5);
                    cellBP.setCellValue(phase6.getFeesum() != null ? phase6.getFeesum() : 0);
                } else {
                    cellBP.setCellValue(0);
                }

                // Phase 7 (인덱스 6) – 예정일자 조건 처리
                // Column BQ (68): 7차 예정일자
                Cell cellBQ = row.getCell(68);
                if (cellBQ == null) cellBQ = row.createCell(68);
                if (customer.getPhases() != null && customer.getPhases().size() > 6) {
                    Phase phase7 = customer.getPhases().get(6);
                    if (phase7.getPlanneddate() != null && !phase7.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellBQ.setCellValue(phase7.getPlanneddate().toString());
                    } else {
                        cellBQ.setCellValue(phase7.getPlanneddateString() != null ? phase7.getPlanneddateString() : "");
                    }
                } else {
                    cellBQ.setCellValue("");
                }
                // Column BR (69): 7차 완납일자
                Cell cellBR = row.getCell(69);
                if (cellBR == null) cellBR = row.createCell(69);
                if (customer.getPhases() != null && customer.getPhases().size() > 6) {
                    Phase phase7 = customer.getPhases().get(6);
                    cellBR.setCellValue(phase7.getFullpaiddate() != null ? phase7.getFullpaiddate().toString() : "");
                } else {
                    cellBR.setCellValue("");
                }
                // Column BS (70): 7차 부담금
                Cell cellBS = row.getCell(70);
                if (cellBS == null) cellBS = row.createCell(70);
                if (customer.getPhases() != null && customer.getPhases().size() > 6) {
                    Phase phase7 = customer.getPhases().get(6);
                    cellBS.setCellValue(phase7.getCharge() != null ? phase7.getCharge() : 0);
                } else {
                    cellBS.setCellValue(0);
                }
                // Column BT (71): 7차 할인액
                Cell cellBT = row.getCell(71);
                if (cellBT == null) cellBT = row.createCell(71);
                if (customer.getPhases() != null && customer.getPhases().size() > 6) {
                    Phase phase7 = customer.getPhases().get(6);
                    cellBT.setCellValue(phase7.getDiscount() != null ? Math.abs(phase7.getDiscount()) : 0);
                } else {
                    cellBT.setCellValue(0);
                }
                // Column BU (72): 7차 면제금액
                Cell cellBU = row.getCell(72);
                if (cellBU == null) cellBU = row.createCell(72);
                if (customer.getPhases() != null && customer.getPhases().size() > 6) {
                    Phase phase7 = customer.getPhases().get(6);
                    cellBU.setCellValue(phase7.getExemption() != null ? Math.abs(phase7.getExemption()) : 0);
                } else {
                    cellBU.setCellValue(0);
                }
                // Column BV (73): 7차 업무대행비
                Cell cellBV = row.getCell(73);
                if (cellBV == null) cellBV = row.createCell(73);
                if (customer.getPhases() != null && customer.getPhases().size() > 6) {
                    Phase phase7 = customer.getPhases().get(6);
                    cellBV.setCellValue(phase7.getService() != null ? phase7.getService() : 0);
                } else {
                    cellBV.setCellValue(0);
                }
                // Column BW (74): 7차 이동
                Cell cellBW = row.getCell(74);
                if (cellBW == null) cellBW = row.createCell(74);
                if (customer.getPhases() != null && customer.getPhases().size() > 6) {
                    Phase phase7 = customer.getPhases().get(6);
                    cellBW.setCellValue(phase7.getMove() != null ? phase7.getMove() : "");
                } else {
                    cellBW.setCellValue("");
                }
                // Column BX (75): 7차 합
                Cell cellBX = row.getCell(75);
                if (cellBX == null) cellBX = row.createCell(75);
                if (customer.getPhases() != null && customer.getPhases().size() > 6) {
                    Phase phase7 = customer.getPhases().get(6);
                    cellBX.setCellValue(phase7.getFeesum() != null ? phase7.getFeesum() : 0);
                } else {
                    cellBX.setCellValue(0);
                }

                // Phase 8 (인덱스 7) – 예정일자 조건 처리
                // Column BY (76): 8차 예정일자
                Cell cellBY = row.getCell(76);
                if (cellBY == null) cellBY = row.createCell(76);
                if (customer.getPhases() != null && customer.getPhases().size() > 7) {
                    Phase phase8 = customer.getPhases().get(7);
                    if (phase8.getPlanneddate() != null && !phase8.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellBY.setCellValue(phase8.getPlanneddate().toString());
                    } else {
                        cellBY.setCellValue(phase8.getPlanneddateString() != null ? phase8.getPlanneddateString() : "");
                    }
                } else {
                    cellBY.setCellValue("");
                }
                // Column BZ (77): 8차 완납일자
                Cell cellBZ = row.getCell(77);
                if (cellBZ == null) cellBZ = row.createCell(77);
                if (customer.getPhases() != null && customer.getPhases().size() > 7) {
                    Phase phase8 = customer.getPhases().get(7);
                    cellBZ.setCellValue(phase8.getFullpaiddate() != null ? phase8.getFullpaiddate().toString() : "");
                } else {
                    cellBZ.setCellValue("");
                }
                // Column CA (78): 8차 부담금
                Cell cellCA = row.getCell(78);
                if (cellCA == null) cellCA = row.createCell(78);
                if (customer.getPhases() != null && customer.getPhases().size() > 7) {
                    Phase phase8 = customer.getPhases().get(7);
                    cellCA.setCellValue(phase8.getCharge() != null ? phase8.getCharge() : 0);
                } else {
                    cellCA.setCellValue(0);
                }
                // Column CB (79): 8차 할인액
                Cell cellCB = row.getCell(79);
                if (cellCB == null) cellCB = row.createCell(79);
                if (customer.getPhases() != null && customer.getPhases().size() > 7) {
                    Phase phase8 = customer.getPhases().get(7);
                    cellCB.setCellValue(phase8.getDiscount() != null ? Math.abs(phase8.getDiscount()) : 0);
                } else {
                    cellCB.setCellValue(0);
                }
                // Column CC (80): 8차 업무대행비
                Cell cellCC = row.getCell(80);
                if (cellCC == null) cellCC = row.createCell(80);
                if (customer.getPhases() != null && customer.getPhases().size() > 7) {
                    Phase phase8 = customer.getPhases().get(7);
                    cellCC.setCellValue(phase8.getService() != null ? phase8.getService() : 0);
                } else {
                    cellCC.setCellValue(0);
                }
                // Column CD (81): 8차 이동
                Cell cellCD = row.getCell(81);
                if (cellCD == null) cellCD = row.createCell(81);
                if (customer.getPhases() != null && customer.getPhases().size() > 7) {
                    Phase phase8 = customer.getPhases().get(7);
                    cellCD.setCellValue(phase8.getMove() != null ? phase8.getMove() : "");
                } else {
                    cellCD.setCellValue("");
                }
                // Column CE (82): 8차 합
                Cell cellCE = row.getCell(82);
                if (cellCE == null) cellCE = row.createCell(82);
                if (customer.getPhases() != null && customer.getPhases().size() > 7) {
                    Phase phase8 = customer.getPhases().get(7);
                    cellCE.setCellValue(phase8.getFeesum() != null ? phase8.getFeesum() : 0);
                } else {
                    cellCE.setCellValue(0);
                }

                // Phase 9 (인덱스 8) – 예정일자 조건 처리
                // Column CF (83): 9차 예정일자
                Cell cellCF = row.getCell(83);
                if (cellCF == null) cellCF = row.createCell(83);
                if (customer.getPhases() != null && customer.getPhases().size() > 8) {
                    Phase phase9 = customer.getPhases().get(8);
                    if (phase9.getPlanneddate() != null && !phase9.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellCF.setCellValue(phase9.getPlanneddate().toString());
                    } else {
                        cellCF.setCellValue(phase9.getPlanneddateString() != null ? phase9.getPlanneddateString() : "");
                    }
                } else {
                    cellCF.setCellValue("");
                }
                // Column CG (84): 9차 완납일자
                Cell cellCG = row.getCell(84);
                if (cellCG == null) cellCG = row.createCell(84);
                if (customer.getPhases() != null && customer.getPhases().size() > 8) {
                    Phase phase9 = customer.getPhases().get(8);
                    cellCG.setCellValue(phase9.getFullpaiddate() != null ? phase9.getFullpaiddate().toString() : "");
                } else {
                    cellCG.setCellValue("");
                }
                // Column CH (85): 9차 부담금
                Cell cellCH = row.getCell(85);
                if (cellCH == null) cellCH = row.createCell(85);
                if (customer.getPhases() != null && customer.getPhases().size() > 8) {
                    Phase phase9 = customer.getPhases().get(8);
                    cellCH.setCellValue(phase9.getCharge() != null ? phase9.getCharge() : 0);
                } else {
                    cellCH.setCellValue(0);
                }
                // Column CI (86): 9차 할인액
                Cell cellCI = row.getCell(86);
                if (cellCI == null) cellCI = row.createCell(86);
                if (customer.getPhases() != null && customer.getPhases().size() > 8) {
                    Phase phase9 = customer.getPhases().get(8);
                    cellCI.setCellValue(phase9.getDiscount() != null ? Math.abs(phase9.getDiscount()) : 0);
                } else {
                    cellCI.setCellValue(0);
                }
                // Column CJ (87): 9차 업무대행비
                Cell cellCJ = row.getCell(87);
                if (cellCJ == null) cellCJ = row.createCell(87);
                if (customer.getPhases() != null && customer.getPhases().size() > 8) {
                    Phase phase9 = customer.getPhases().get(8);
                    cellCJ.setCellValue(phase9.getService() != null ? phase9.getService() : 0);
                } else {
                    cellCJ.setCellValue(0);
                }
                // Column CK (88): 9차 이동
                Cell cellCK = row.getCell(88);
                if (cellCK == null) cellCK = row.createCell(88);
                if (customer.getPhases() != null && customer.getPhases().size() > 8) {
                    Phase phase9 = customer.getPhases().get(8);
                    cellCK.setCellValue(phase9.getMove() != null ? phase9.getMove() : "");
                } else {
                    cellCK.setCellValue("");
                }
                // Column CL (89): 9차 합
                Cell cellCL = row.getCell(89);
                if (cellCL == null) cellCL = row.createCell(89);
                if (customer.getPhases() != null && customer.getPhases().size() > 8) {
                    Phase phase9 = customer.getPhases().get(8);
                    cellCL.setCellValue(phase9.getFeesum() != null ? phase9.getFeesum() : 0);
                } else {
                    cellCL.setCellValue(0);
                }

                // Phase 10 (인덱스 9) – 예정일자 조건 처리
                // Column CM (90): 10차 예정일자
                Cell cellCM = row.getCell(90);
                if (cellCM == null) cellCM = row.createCell(90);
                if (customer.getPhases() != null && customer.getPhases().size() > 9) {
                    Phase phase10 = customer.getPhases().get(9);
                    if (phase10.getPlanneddate() != null && !phase10.getPlanneddate().equals(LocalDate.of(2100, 1, 1))) {
                        cellCM.setCellValue(phase10.getPlanneddate().toString());
                    } else {
                        cellCM.setCellValue(phase10.getPlanneddateString() != null ? phase10.getPlanneddateString() : "");
                    }
                } else {
                    cellCM.setCellValue("");
                }
                // Column CN (91): 10차 완납일자
                Cell cellCN = row.getCell(91);
                if (cellCN == null) cellCN = row.createCell(91);
                if (customer.getPhases() != null && customer.getPhases().size() > 9) {
                    Phase phase10 = customer.getPhases().get(9);
                    cellCN.setCellValue(phase10.getFullpaiddate() != null ? phase10.getFullpaiddate().toString() : "");
                } else {
                    cellCN.setCellValue("");
                }
                // Column CO (92): 10차 부담금
                Cell cellCO = row.getCell(92);
                if (cellCO == null) cellCO = row.createCell(92);
                if (customer.getPhases() != null && customer.getPhases().size() > 9) {
                    Phase phase10 = customer.getPhases().get(9);
                    cellCO.setCellValue(phase10.getCharge() != null ? phase10.getCharge() : 0);
                } else {
                    cellCO.setCellValue(0);
                }
                // Column CP (93): 10차 할인액
                Cell cellCP = row.getCell(93);
                if (cellCP == null) cellCP = row.createCell(93);
                if (customer.getPhases() != null && customer.getPhases().size() > 9) {
                    Phase phase10 = customer.getPhases().get(9);
                    cellCP.setCellValue(phase10.getDiscount() != null ? Math.abs(phase10.getDiscount()) : 0);
                } else {
                    cellCP.setCellValue(0);
                }
                // Column CQ (94): 10차 업무대행비
                Cell cellCQ = row.getCell(94);
                if (cellCQ == null) cellCQ = row.createCell(94);
                if (customer.getPhases() != null && customer.getPhases().size() > 9) {
                    Phase phase10 = customer.getPhases().get(9);
                    cellCQ.setCellValue(phase10.getService() != null ? phase10.getService() : 0);
                } else {
                    cellCQ.setCellValue(0);
                }
                // Column CR (95): 10차 이동
                Cell cellCR = row.getCell(95);
                if (cellCR == null) cellCR = row.createCell(95);
                if (customer.getPhases() != null && customer.getPhases().size() > 9) {
                    Phase phase10 = customer.getPhases().get(9);
                    cellCR.setCellValue(phase10.getMove() != null ? phase10.getMove() : "");
                } else {
                    cellCR.setCellValue("");
                }
                // Column CS (96): 10차 합
                Cell cellCS = row.getCell(96);
                if (cellCS == null) cellCS = row.createCell(96);
                if (customer.getPhases() != null && customer.getPhases().size() > 9) {
                    Phase phase10 = customer.getPhases().get(9);
                    cellCS.setCellValue(phase10.getFeesum() != null ? phase10.getFeesum() : 0);
                } else {
                    cellCS.setCellValue(0);
                }

                // ── 최종 섹션 ──
                // Column DA (104): 총 면제금액
                Cell cellDA = row.getCell(104);
                if (cellDA == null) cellDA = row.createCell(104);
                cellDA.setCellValue((customer.getStatus() != null && customer.getStatus().getExemptionsum() != null)
                        ? customer.getStatus().getExemptionsum() : 0);

                // Column DB (105): 해약 해지일자
                Cell cellDB = row.getCell(105);
                if (cellDB == null) cellDB = row.createCell(105);
                cellDB.setCellValue((customer.getCancel() != null && customer.getCancel().getCanceldate() != null)
                        ? customer.getCancel().getCanceldate().toString() : "");

                // Column DC (106): 해약 환급일자
                Cell cellDC = row.getCell(106);
                if (cellDC == null) cellDC = row.createCell(106);
                cellDC.setCellValue((customer.getCancel() != null && customer.getCancel().getRefunddate() != null)
                        ? customer.getCancel().getRefunddate().toString() : "");

                // Column DD (107): 해약 환급금
                Cell cellDD = row.getCell(107);
                if (cellDD == null) cellDD = row.createCell(107);
                cellDD.setCellValue((customer.getCancel() != null && customer.getCancel().getRefundamount() != null)
                        ? customer.getCancel().getRefundamount() : 0);

                // Column DE (108): 납입총액
                Cell cellDE = row.getCell(108);
                if (cellDE == null) cellDE = row.createCell(108);
                cellDE.setCellValue((customer.getStatus() != null && customer.getStatus().getAmmountsum() != null)
                        ? customer.getStatus().getAmmountsum() : 0);

                // Column DF (109): 건너뛰기
                Cell cellDF = row.getCell(109);
                if (cellDF == null) cellDF = row.createCell(109);
                cellDF.setCellValue("");

                // Column DG (110): 담당 총괄
                Cell cellDG = row.getCell(110);
                if (cellDG == null) cellDG = row.createCell(110);
                cellDG.setCellValue((customer.getResponsible() != null && customer.getResponsible().getGeneralmanagement() != null)
                        ? customer.getResponsible().getGeneralmanagement() : "");

                // Column DH (111): 담당 본부
                Cell cellDH = row.getCell(111);
                if (cellDH == null) cellDH = row.createCell(111);
                cellDH.setCellValue((customer.getResponsible() != null && customer.getResponsible().getDivision() != null)
                        ? customer.getResponsible().getDivision() : "");

                // Column DI (112): 담당 팀
                Cell cellDI = row.getCell(112);
                if (cellDI == null) cellDI = row.createCell(112);
                cellDI.setCellValue((customer.getResponsible() != null && customer.getResponsible().getTeam() != null)
                        ? customer.getResponsible().getTeam() : "");

                // Column DJ (113): 담당 성명
                Cell cellDJ = row.getCell(113);
                if (cellDJ == null) cellDJ = row.createCell(113);
                cellDJ.setCellValue((customer.getResponsible() != null && customer.getResponsible().getManagername() != null)
                        ? customer.getResponsible().getManagername() : "");

                // Column DK (114): 담당 수수료지급
                Cell cellDK = row.getCell(114);
                if (cellDK == null) cellDK = row.createCell(114);
                cellDK.setCellValue((customer.getResponsible() != null && customer.getResponsible().getFeepaid() != null)
                        ? customer.getResponsible().getFeepaid() : "");

                // Column DL (115): 다힘 시상
                Cell cellDL = row.getCell(115);
                if (cellDL == null) cellDL = row.createCell(115);
                cellDL.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimsisang() != null)
                        ? customer.getDahim().getDahimsisang() : "");

                // Column DM (116): 다힘 일자
                Cell cellDM = row.getCell(116);
                if (cellDM == null) cellDM = row.createCell(116);
                cellDM.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimdate() != null)
                        ? customer.getDahim().getDahimdate().toString() : "");

                // Column DN (117): 다힘 6/30선지급
                Cell cellDN = row.getCell(117);
                if (cellDN == null) cellDN = row.createCell(117);
                cellDN.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimprepaid() != null)
                        ? customer.getDahim().getDahimprepaid() : "");

                // Column DO (118): 다힘 1회차청구
                Cell cellDO = row.getCell(118);
                if (cellDO == null) cellDO = row.createCell(118);
                cellDO.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimfirst() != null)
                        ? customer.getDahim().getDahimfirst() : "");

                // Column DP (119): 다힘 (1회차)금액
                Cell cellDP = row.getCell(119);
                if (cellDP == null) cellDP = row.createCell(119);
                cellDP.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimfirstpay() != null)
                        ? customer.getDahim().getDahimfirstpay() : "");

                // Column DQ (120): 다힘 일자2 – 날짜형 우선, 없으면 문자열
                Cell cellDQ = row.getCell(120);
                if (cellDQ == null) cellDQ = row.createCell(120);
                LocalDate dateDQ = getUnderlyingDate(cellDQ);
                if (dateDQ != null) {
                    cellDQ.setCellValue(dateDQ.toString());
                } else {
                    cellDQ.setCellValue(cellDQ.getStringCellValue());
                }

                // Column DR (121): 다힘 출처
                Cell cellDR = row.getCell(121);
                if (cellDR == null) cellDR = row.createCell(121);
                cellDR.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimsource() != null)
                        ? customer.getDahim().getDahimsource() : "");

                // Column DS (122): 다힘 2회차청구
                Cell cellDS = row.getCell(122);
                if (cellDS == null) cellDS = row.createCell(122);
                cellDS.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimsecond() != null)
                        ? customer.getDahim().getDahimsecond() : "");

                // Column DT (123): 다힘 (2회차)금액
                Cell cellDT = row.getCell(123);
                if (cellDT == null) cellDT = row.createCell(123);
                cellDT.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimsecondpay() != null)
                        ? customer.getDahim().getDahimsecondpay() : "");

                // Column DU (124): 다힘 일자3 – 날짜형 우선, 없으면 문자열
                Cell cellDU = row.getCell(124);
                if (cellDU == null) cellDU = row.createCell(124);
                LocalDate dateDU = getUnderlyingDate(cellDU);
                if (dateDU != null) {
                    cellDU.setCellValue(dateDU.toString());
                } else {
                    cellDU.setCellValue(cellDU.getStringCellValue());
                }

                // Column DV (125): 다힘 합계
                Cell cellDV = row.getCell(125);
                if (cellDV == null) cellDV = row.createCell(125);
                cellDV.setCellValue((customer.getDahim() != null && customer.getDahim().getDahimsum() != null)
                        ? customer.getDahim().getDahimsum() : "");

                // Column DW (126): MGM 수수료 (문자 그대로)
                Cell cellDW = row.getCell(126);
                if (cellDW == null) {
                    cellDW = row.createCell(126);
                }
                String mgmfeeStr = (customer.getMgm() != null && customer.getMgm().getMgmfee() != null)
                        ? customer.getMgm().getMgmfee() : "";
                cellDW.setCellValue(mgmfeeStr);

                // Column DX (127): MGM 업체명
                Cell cellDX = row.getCell(127);
                if (cellDX == null) cellDX = row.createCell(127);
                cellDX.setCellValue((customer.getMgm() != null && customer.getMgm().getMgmcompanyname() != null)
                        ? customer.getMgm().getMgmcompanyname() : "");

                // Column DY (128): MGM 이름
                Cell cellDY = row.getCell(128);
                if (cellDY == null) cellDY = row.createCell(128);
                cellDY.setCellValue((customer.getMgm() != null && customer.getMgm().getMgmname() != null)
                        ? customer.getMgm().getMgmname() : "");

                // Column DZ (129): MGM 기관
                Cell cellDZ = row.getCell(129);
                if (cellDZ == null) cellDZ = row.createCell(129);
                cellDZ.setCellValue((customer.getMgm() != null && customer.getMgm().getMgminstitution() != null)
                        ? customer.getMgm().getMgminstitution() : "");

                // Column EA (130): MGM 계좌
                Cell cellEA = row.getCell(130);
                if (cellEA == null) cellEA = row.createCell(130);
                cellEA.setCellValue((customer.getMgm() != null && customer.getMgm().getMgmaccount() != null)
                        ? customer.getMgm().getMgmaccount() : "");

                // Column EB (131): 1차(직원) 차순
                Cell cellEB = row.getCell(131);
                if (cellEB == null) cellEB = row.createCell(131);
                cellEB.setCellValue((customer.getFirstemp() != null && customer.getFirstemp().getFirstemptimes() != null)
                        ? customer.getFirstemp().getFirstemptimes() : "");

                // Column EC (132): 1차 지급일자
                Cell cellEC = row.getCell(132);
                if (cellEC == null) cellEC = row.createCell(132);
                cellEC.setCellValue((customer.getFirstemp() != null && customer.getFirstemp().getFirstempdate() != null)
                        ? customer.getFirstemp().getFirstempdate().toString() : "");

                // Column ED (133): 2차(직원) 차순
                Cell cellED = row.getCell(133);
                if (cellED == null) cellED = row.createCell(133);
                cellED.setCellValue((customer.getSecondemp() != null && customer.getSecondemp().getSecondemptimes() != null)
                        ? customer.getSecondemp().getSecondemptimes() : "");

                // Column EE (134): 2차 지급일자
                Cell cellEE = row.getCell(134);
                if (cellEE == null) cellEE = row.createCell(134);
                cellEE.setCellValue((customer.getSecondemp() != null && customer.getSecondemp().getSecondempdate() != null)
                        ? customer.getSecondemp().getSecondempdate().toString() : "");

                // Column EF (135): 부속서류 인감증명서
                Cell cellEF = row.getCell(135);
                if (cellEF == null) cellEF = row.createCell(135);
                cellEF.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getSealcertificateprovided())) ? "o" : "");

                // Column EG (136): 부속서류 본인서명확인서
                Cell cellEG = row.getCell(136);
                if (cellEG == null) cellEG = row.createCell(136);
                cellEG.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getSelfsignatureconfirmationprovided())) ? "o" : "");

                // Column EH (137): 부속서류 신분증
                Cell cellEH = row.getCell(137);
                if (cellEH == null) cellEH = row.createCell(137);
                cellEH.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getIdcopyprovided())) ? "o" : "");

                // Column EI (138): 부속서류 확약서
                Cell cellEI = row.getCell(138);
                if (cellEI == null) cellEI = row.createCell(138);
                cellEI.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getCommitmentletterprovided())) ? "o" : "");

                // Column EJ (139): 부속서류 창준위용
                Cell cellEJ = row.getCell(139);
                if (cellEJ == null) cellEJ = row.createCell(139);
                cellEJ.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getForfounding())) ? "o" : "");

                // Column EK (140): 부속서류 무상옵션
                Cell cellEK = row.getCell(140);
                if (cellEK == null) cellEK = row.createCell(140);
                cellEK.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getFreeoption())) ? "o" : "");

                // Column EL (141): 부속서류 선호도조사
                Cell cellEL = row.getCell(141);
                if (cellEL == null) cellEL = row.createCell(141);
                cellEL.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getPreferenceattachment())) ? "o" : "");

                // Column EM (142): 부속서류 총회동의서
                Cell cellEM = row.getCell(142);
                if (cellEM == null) cellEM = row.createCell(142);
                cellEM.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getPrizeattachment())) ? "o" : "");

                // Column EN (143): 부속서류 사은품 지급일자
                Cell cellEN = row.getCell(143);
                if (cellEN == null) cellEN = row.createCell(143);
                cellEN.setCellValue((customer.getAttachments() != null
                        && customer.getAttachments().getPrizedate() != null)
                        ? customer.getAttachments().getPrizedate().toString() : "");

                // Column EO (144): 이메일
                Cell cellEO = row.getCell(144);
                if (cellEO == null) cellEO = row.createCell(144);
                cellEO.setCellValue((customer.getCustomerData() != null
                        && customer.getCustomerData().getEmail() != null)
                        ? customer.getCustomerData().getEmail() : "");

                // Column EP (145): 우편물수령주소 우편번호 (이미 처리됨)

                // Column EQ (146) & ER (147) – 우편물수령주소 도/군는 이미 분리하여 처리함
                // Column ES (148): 우편물수령주소 상세주소
                Cell cellES = row.getCell(148);
                if (cellES == null) cellES = row.createCell(148);
                cellES.setCellValue((customer.getPostreceive() != null
                        && customer.getPostreceive().getDetailaddressreceive() != null)
                        ? customer.getPostreceive().getDetailaddressreceive() : "");

                // Column ET (149): 비고
                Cell cellET = row.getCell(149);
                if (cellET == null) cellET = row.createCell(149);
                cellET.setCellValue(customer.getAdditional() != null ? customer.getAdditional() : "");

                // Column EU (150): 가입경로
                Cell cellEU = row.getCell(150);
                if (cellEU == null) cellEU = row.createCell(150);
                cellEU.setCellValue(customer.getRegisterpath() != null ? customer.getRegisterpath() : "");

                // Column EV (151): 총회참석 서면
                Cell cellEV = row.getCell(151);
                if (cellEV == null) cellEV = row.createCell(151);
                cellEV.setCellValue((customer.getMeetingattend() != null
                        && customer.getMeetingattend().getFtofattend() != null)
                        ? customer.getMeetingattend().getFtofattend() : "");

                // Column EW (152): 총회참석 직접
                Cell cellEW = row.getCell(152);
                if (cellEW == null) cellEW = row.createCell(152);
                cellEW.setCellValue((customer.getMeetingattend() != null
                        && customer.getMeetingattend().getSelfattend() != null)
                        ? customer.getMeetingattend().getSelfattend() : "");

                // Column EX (153): 총회참석 대리
                Cell cellEX = row.getCell(153);
                if (cellEX == null) cellEX = row.createCell(153);
                cellEX.setCellValue((customer.getMeetingattend() != null
                        && customer.getMeetingattend().getBehalfattend() != null)
                        ? customer.getMeetingattend().getBehalfattend() : "");

                // Column EY (154): 특이사항
                Cell cellEY = row.getCell(154);
                if (cellEY == null) cellEY = row.createCell(154);
                cellEY.setCellValue(customer.getSpecialnote() != null ? customer.getSpecialnote() : "");

                // Column EZ (155): 투표기기
                Cell cellEZ = row.getCell(155);
                if (cellEZ == null) cellEZ = row.createCell(155);
                cellEZ.setCellValue(customer.getVotemachine() != null ? customer.getVotemachine() : "");

                // Column FA (156): 안건 제1호
                Cell cellFA = row.getCell(156);
                if (cellFA == null) cellFA = row.createCell(156);
                cellFA.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda1() != null)
                        ? customer.getAgenda().getAgenda1() : "");

                // Column FB (157): 안건 제2-1호
                Cell cellFB = row.getCell(157);
                if (cellFB == null) cellFB = row.createCell(157);
                cellFB.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda2_1() != null)
                        ? customer.getAgenda().getAgenda2_1() : "");

                // Column FC (158): 안건 제2-2호
                Cell cellFC = row.getCell(158);
                if (cellFC == null) cellFC = row.createCell(158);
                cellFC.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda2_2() != null)
                        ? customer.getAgenda().getAgenda2_2() : "");

                // Column FD (159): 안건 제2-3호
                Cell cellFD = row.getCell(159);
                if (cellFD == null) cellFD = row.createCell(159);
                cellFD.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda2_3() != null)
                        ? customer.getAgenda().getAgenda2_3() : "");

                // Column FE (160): 안건 제2-4호
                Cell cellFE = row.getCell(160);
                if (cellFE == null) cellFE = row.createCell(160);
                cellFE.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda2_4() != null)
                        ? customer.getAgenda().getAgenda2_4() : "");

                // Column FF (161): 안건 제3호
                Cell cellFF = row.getCell(161);
                if (cellFF == null) cellFF = row.createCell(161);
                cellFF.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda3() != null)
                        ? customer.getAgenda().getAgenda3() : "");

                // Column FG (162): 안건 제4호
                Cell cellFG = row.getCell(162);
                if (cellFG == null) cellFG = row.createCell(162);
                cellFG.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda4() != null)
                        ? customer.getAgenda().getAgenda4() : "");

                // Column FH (163): 안건 제5호
                Cell cellFH = row.getCell(163);
                if (cellFH == null) cellFH = row.createCell(163);
                cellFH.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda5() != null)
                        ? customer.getAgenda().getAgenda5() : "");

                // Column FI (164): 안건 제6호
                Cell cellFI = row.getCell(164);
                if (cellFI == null) cellFI = row.createCell(164);
                cellFI.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda6() != null)
                        ? customer.getAgenda().getAgenda6() : "");

                // Column FJ (165): 안건 제7호
                Cell cellFJ = row.getCell(165);
                if (cellFJ == null) cellFJ = row.createCell(165);
                cellFJ.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda7() != null)
                        ? customer.getAgenda().getAgenda7() : "");

                // Column FK (166): 안건 제8호
                Cell cellFK = row.getCell(166);
                if (cellFK == null) cellFK = row.createCell(166);
                cellFK.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda8() != null)
                        ? customer.getAgenda().getAgenda8() : "");

                // Column FL (167): 안건 제9호
                Cell cellFL = row.getCell(167);
                if (cellFL == null) cellFL = row.createCell(167);
                cellFL.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda9() != null)
                        ? customer.getAgenda().getAgenda9() : "");

                // Column FM (168): 안건 제10호
                Cell cellFM = row.getCell(168);
                if (cellFM == null) cellFM = row.createCell(168);
                cellFM.setCellValue((customer.getAgenda() != null && customer.getAgenda().getAgenda10() != null)
                        ? customer.getAgenda().getAgenda10() : "");

                // Column FN (169): 부속서류 사은품명
                Cell cellFN = row.getCell(169);
                if (cellFN == null) cellFN = row.createCell(169);
                cellFN.setCellValue(customer.getPrizewinning() != null ? customer.getPrizewinning() : "");

                // Column FO (170): 부속서류 출자금 (flag "o")
                Cell cellFO = row.getCell(170);
                if (cellFO == null) cellFO = row.createCell(170);
                cellFO.setCellValue((customer.getAttachments() != null
                        && Boolean.TRUE.equals(customer.getAttachments().getInvestmentfile())) ? "o" : "");
                //엑셀에 고객정보 기입끝 : code spread complete

                rowIndex++;

                // 진행 상황 업데이트 (예: "현재 처리된 고객번호 / 전체 고객 수")
                try {
                    emitter.send(SseEmitter.event().name("progress").data((i + 1) + "/" + total));
                } catch (Exception ex) {
                    // 진행 상황 전송 실패시 무시
                }
            }

            workbook.setForceFormulaRecalculation(true);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                workbook.write(fos);
            }
        }
    }

    public void processExcelFileWithProgress(MultipartFile file, SseEmitter emitter) throws IOException {
        DataFormatter formatter = new DataFormatter(Locale.getDefault());
        // 날짜 파싱 포맷 (예: "yy-M-d")
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yy-M-d");

        try (InputStream is = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            XSSFSheet sheet = workbook.getSheetAt(0);
            // 예제에서는 4번째 행(인덱스 3)부터 고객 데이터가 시작된다고 가정합니다.
            int startRow = 2;
            int lastRow = sheet.getLastRowNum();
            // 3) A열이 비어있는 행을 만나면 중단, 그 직전까지를 "유효한 마지막 행"으로 설정
            int realLastRow = startRow;
            for (int i = startRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    // row 자체가 비어 있으면 중단
                    break;
                }

                // A열(0번 칼럼)
                Cell cellA = row.getCell(0);
                String valA = (cellA != null) ? formatter.formatCellValue(cellA).trim() : "";

                if (valA.isEmpty()) {
                    // A열이 공백이라면 여기서 데이터가 끝났다고 판단
                    break;
                }
                // 그렇지 않으면 유효한 데이터 행이므로 업데이트
                realLastRow = i;
            }
            // realLastRow가 최종 유효 행
            // 따라서 totalCustomers = (realLastRow - startRow + 1)
            int totalCustomers = realLastRow >= startRow ? (realLastRow - startRow + 1) : 0;

            // 각 행(고객)에 대해 처리
            for (int i = startRow; i <= realLastRow; i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) {
                    // 비어있는 행은 건너뜁니다.
                    continue;
                }
                // 각 행을 Customer 객체로 파싱 (필요한 모든 컬럼 매핑 구현)
                Customer customer = parseCustomerFromRow(row, dtf, formatter, evaluator);
                // DB에 저장 (여기서는 createCustomer 내부에서 Phase 등 추가 로직이 수행될 수 있음)
                customerService.createCustomer(customer, false);

                // 진행 상황 전송: 처리한 고객 수/전체 고객 수
                int current = i - startRow + 1;
                emitter.send(SseEmitter.event().name("progress").data(current + "/" + totalCustomers));
            }
        }
    }

    /**
     * 엑셀의 한 행(Row)을 읽어 Customer 객체로 매핑하는 예시 메서드
     * 실제 구현에서는 모든 필요한 컬럼에 대해 값을 매핑하세요.
     *
     * @param row       엑셀의 한 행
     * @param dtf       날짜 파싱 포맷
     * @param formatter DataFormatter
     * @return 매핑된 Customer 객체
     */
    private Customer parseCustomerFromRow(org.apache.poi.ss.usermodel.Row row, DateTimeFormatter dtf, DataFormatter formatter,FormulaEvaluator evaluator) {
        Customer customer = new Customer();
        // 하위 임베디드 객체 초기화
        customer.setCustomerData(new CustomerData());
        customer.setLegalAddress(new LegalAddress());
        customer.setFinancial(new Financial());
        customer.setDeposits(new Deposit());
        customer.setAttachments(new Attachments());
        customer.setCancel(new Cancel());
        customer.setLoan(new Loan());
        customer.setResponsible(new Responsible());
        customer.setDahim(new Dahim());
        customer.setMgm(new MGM());
        customer.setFirstemp(new Firstemp());
        customer.setSecondemp(new Secondemp());
        customer.setMeetingattend(new Meetingattend());
        customer.setAgenda(new Agenda());
        customer.setPostreceive(new Postreceive());
        Status status = new Status();
        status.setCustomer(customer);
        customer.setStatus(status);

        // 예시 매핑
        // Column A (인덱스 0): 관리번호
        String colAcode = formatter.formatCellValue(row.getCell(0));
        if (!colAcode.isEmpty()) {
            try {
                customer.setId(Integer.parseInt(colAcode.replaceAll("[^0-9]+", "")));
            } catch (NumberFormatException e) {
                // 필요시 로그 처리
            }
        }

        //엑셀파일 해부시작 excel spread start
        // --- 첫 번째 섹션: A ~ CS (인덱스 0 ~ 96) ---
        String colA = formatter.formatCellValue(row.getCell(0), evaluator);
        System.out.println("Column A (관리번호): " + colA);
        if (!colA.isEmpty()) {
            customer.setId(Integer.parseInt(colA.replaceAll("[^0-9-]+", "")));
        }
        String colB = formatter.formatCellValue(row.getCell(1), evaluator);
        System.out.println("Column B (분류(회원)): " + colB);
        customer.setCustomertype(colB);
        String colC = formatter.formatCellValue(row.getCell(2), evaluator);
        System.out.println("Column C (타입): " + colC);
        customer.setType(colC);
        String colD = formatter.formatCellValue(row.getCell(3), evaluator);
        System.out.println("Column D (군): " + colD);
        customer.setGroupname(colD);
        String colE = formatter.formatCellValue(row.getCell(4), evaluator);
        System.out.println("Column E (순번): " + colE);
        customer.setTurn(colE);
        String colF = formatter.formatCellValue(row.getCell(5), evaluator);
        System.out.println("Column F (7차면제): " + colF);
        customer.getAttachments().setExemption7("o".equalsIgnoreCase(colF));
        String colG = formatter.formatCellValue(row.getCell(6), evaluator);
        System.out.println("Column G (임시동호): " + colG);
        customer.setTemptype(colG);
        String colH = formatter.formatCellValue(row.getCell(7), evaluator);
        System.out.println("Column H (가입차순): " + colH);
        customer.setBatch(colH);

        String colI = formatter.formatCellValue(row.getCell(8), evaluator);
        System.out.println("Column I (신탁사제출): " + colI);
        if (!colI.isEmpty()) {
            customer.getFinancial().setTrustcompanydate(parseDate(colI, dtf));
        }


        String colJ = formatter.formatCellValue(row.getCell(9), evaluator);
        System.out.println("Column J (가입일자): " + colJ);
        if (!colJ.isEmpty()) {
            customer.setRegisterdate(parseDate(colJ, dtf));
        }
        String colK = formatter.formatCellValue(row.getCell(10), evaluator);
        System.out.println("Column K (가입가): " + colK);
        if (!colK.isEmpty()) {
            customer.setRegisterprice(Long.parseLong(colK.replaceAll("[^0-9-]+", "")));
        }
        String colL = formatter.formatCellValue(row.getCell(11), evaluator);
        System.out.println("Column L (지산A동계약서): " + colL);
        customer.getAttachments().setContract("1".equals(colL));
        String colM = formatter.formatCellValue(row.getCell(12), evaluator);
        System.out.println("Column M (동의서): " + colM);
        customer.getAttachments().setAgreement("1".equals(colM));
        String colN = formatter.formatCellValue(row.getCell(13), evaluator);
        System.out.println("Column N (성명): " + colN);
        customer.getCustomerData().setName(colN);
        String colO = formatter.formatCellValue(row.getCell(14), evaluator);
        System.out.println("Column O (주민번호): " + colO);
        if (colO.contains("-")) {
            String[] parts = colO.split("-");
            if (parts.length == 2) {
                customer.getCustomerData().setResnumfront(Integer.parseInt(parts[0].replaceAll("[^0-9]+", "")));
                customer.getCustomerData().setResnumback(Integer.parseInt(parts[1].replaceAll("[^0-9]+", "")));
            }
        }
        String colP = formatter.formatCellValue(row.getCell(15), evaluator).replaceAll("[^0-9]", "");
        System.out.println("Column P (휴대전화): " + colP);
        customer.getCustomerData().setPhone(colP);
        String colQ = formatter.formatCellValue(row.getCell(16), evaluator);
        System.out.println("Column Q (법정주소 우편번호): " + colQ);
        customer.getLegalAddress().setPostnumber(colQ);
        String colR = formatter.formatCellValue(row.getCell(17), evaluator);
        String colS = formatter.formatCellValue(row.getCell(18), evaluator);
        System.out.println("Column R (법정주소 도): " + colR);
        System.out.println("Column S (법정주소 군): " + colS);
        customer.getLegalAddress().setPost(colR + " " + colS);
        String colT = formatter.formatCellValue(row.getCell(19), evaluator);
        System.out.println("Column T (법정주소 상세주소): " + colT);
        customer.getLegalAddress().setDetailaddress(colT);
        String colU = formatter.formatCellValue(row.getCell(20), evaluator);
        System.out.println("Column U (금융기관 은행명): " + colU);
        customer.getFinancial().setBankname(colU);
        String colV = formatter.formatCellValue(row.getCell(21), evaluator);
        System.out.println("Column V (금융기관 계좌번호): " + colV);
        customer.getFinancial().setAccountnum(colV);
        String colW = formatter.formatCellValue(row.getCell(22), evaluator);
        System.out.println("Column W (금융기관 예금주): " + colW);
        customer.getFinancial().setAccountholder(colW);
        String colX = formatter.formatCellValue(row.getCell(23), evaluator);
        System.out.println("Column X (금융기관 신탁사): " + colX);
        customer.getFinancial().setTrustcompany(colX);
        String colY = formatter.formatCellValue(row.getCell(24), evaluator);
        System.out.println("Column Y (예약금 납입일자): " + colY);
        if (!colY.isEmpty()) {
            customer.getDeposits().setDepositdate(parseDate(colY, dtf));
        }
        String colZ = formatter.formatCellValue(row.getCell(25), evaluator);
        System.out.println("Column Z (예약금 금액): " + colZ);
        if (!colZ.isEmpty()) {
            customer.getDeposits().setDepositammount(Long.parseLong(colZ.replaceAll("[^0-9-]+", "")));
        }
        String colAA = formatter.formatCellValue(row.getCell(26), evaluator);
        System.out.println("Column AA (1차 완납일자): " + colAA);
        Phase phase1 = new Phase();
        if (!colAA.isEmpty()) {
            phase1.setFullpaiddate(parseDate(colAA, dtf));
        }
        String colAB = formatter.formatCellValue(row.getCell(27), evaluator);
        System.out.println("Column AB (1차 부담금): " + colAB);
        if (!colAB.isEmpty()) {
            phase1.setCharge(Long.parseLong(colAB.replaceAll("[^0-9-]+", "")));
        }
        String colAC = formatter.formatCellValue(row.getCell(28), evaluator);
        System.out.println("Column AC (1차 업무대행비): " + colAC);
        if (!colAC.isEmpty()) {
            phase1.setService(Long.parseLong(colAC.replaceAll("[^0-9-]+", "")));
        }
        String colAD = formatter.formatCellValue(row.getCell(29), evaluator);
        System.out.println("Column AD (1차 이동): " + colAD);
        phase1.setMove(colAD);

        String colAE_final = formatter.formatCellValue(row.getCell(30), evaluator).trim();
        System.out.println("Column AE (1차 합): " + colAE_final);
        long aeValue = 0L;
        if (!colAE_final.isEmpty()) {
            try {
                aeValue = Long.parseLong(colAE_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                aeValue = 0L;
            }
        } else {
            aeValue = 0L;
        }
        phase1.setFeesum(aeValue);
        phase1.setPhaseNumber(1);
        phase1.setCustomer(customer);
        customer.setPhases(new java.util.ArrayList<>());
        customer.getPhases().add(phase1);

        // --- 각 차수의 예정일자(planneddateString) 처리 ---
        // Phase 2 (2차 예정일자: 인덱스 31)
        String colAF = formatter.formatCellValue(row.getCell(31), evaluator);
        System.out.println("Column AF (2차 예정일자): " + colAF);
        Phase phase2 = new Phase();
        phase2.setPhaseNumber(2);
        phase2.setPlanneddateString(colAF);
        if (colAF != null && !colAF.trim().isEmpty()) {
            phase2.setPlanneddate(parsePlannedDate(colAF));
        }
        String colAG = formatter.formatCellValue(row.getCell(32), evaluator);
        System.out.println("Column AG (2차 완납일자): " + colAG);
        if (!colAG.isEmpty()) {
            phase2.setFullpaiddate(parseDate(colAG, dtf));
        }
        String colAH = formatter.formatCellValue(row.getCell(33), evaluator);
        System.out.println("Column AH (2차 부담금): " + colAH);
        if (!colAH.isEmpty()) {
            phase2.setCharge(Long.parseLong(colAH.replaceAll("[^0-9-]+", "")));
        }
        String colAI = formatter.formatCellValue(row.getCell(34), evaluator);
        System.out.println("Column AI (2차 할인액): " + colAI);
        if (!colAI.isEmpty()) {
            phase2.setDiscount(Math.abs(Long.parseLong(colAI.replaceAll("[^0-9-]+", ""))));
        }
        String colAJ = formatter.formatCellValue(row.getCell(35), evaluator);
        System.out.println("Column AJ (2차 업무대행비): " + colAJ);
        if (!colAJ.isEmpty()) {
            phase2.setService(Long.parseLong(colAJ.replaceAll("[^0-9-]+", "")));
        }
        String colAK = formatter.formatCellValue(row.getCell(36), evaluator);
        System.out.println("Column AK (2차 이동): " + colAK);
        phase2.setMove(colAK);
// AL: 2차 합 (예: index 37)
        String colAL_final = formatter.formatCellValue(row.getCell(37), evaluator).trim();
        System.out.println("Column AL (2차 합): " + colAL_final);
        long alValue = 0L;
        if (!colAL_final.isEmpty()) {
            try {
                alValue = Long.parseLong(colAL_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                alValue = 0L;
            }
        } else {
            alValue = 0L;
        }
        phase2.setFeesum(alValue);
        phase2.setCustomer(customer);
        customer.getPhases().add(phase2);

        // Phase 3 (3차 예정일자: 인덱스 38)
        String colAM = formatter.formatCellValue(row.getCell(38), evaluator);
        System.out.println("Column AM (3차 예정일자): " + colAM);
        Phase phase3 = new Phase();
        phase3.setPhaseNumber(3);
        phase3.setPlanneddateString(colAM);
        if (colAM != null && !colAM.trim().isEmpty()) {
            phase3.setPlanneddate(parsePlannedDate(colAM));
        }
        String colAN = formatter.formatCellValue(row.getCell(39), evaluator);
        System.out.println("Column AN (3차 완납일자): " + colAN);
        if (!colAN.isEmpty()) {
            phase3.setFullpaiddate(parseDate(colAN, dtf));
        }
        String colAO = formatter.formatCellValue(row.getCell(40), evaluator);
        System.out.println("Column AO (3차 부담금): " + colAO);
        if (!colAO.isEmpty()) {
            phase3.setCharge(Long.parseLong(colAO.replaceAll("[^0-9-]+", "")));
        }
        String colAP = formatter.formatCellValue(row.getCell(41), evaluator);
        System.out.println("Column AP (3차 할인액): " + colAP);
        if (!colAP.isEmpty()) {
            phase3.setDiscount(Math.abs(Long.parseLong(colAP.replaceAll("[^0-9-]+", ""))));
        }
        String colAQ = formatter.formatCellValue(row.getCell(42), evaluator);
        System.out.println("Column AQ (3차 업무대행비): " + colAQ);
        if (!colAQ.isEmpty()) {
            phase3.setService(Long.parseLong(colAQ.replaceAll("[^0-9-]+", "")));
        }
        String colAR = formatter.formatCellValue(row.getCell(43), evaluator);
        System.out.println("Column AR (3차 이동): " + colAR);
        phase3.setMove(colAR);
// AS: 3차 합 (예: index 44)
        String colAS_final = formatter.formatCellValue(row.getCell(44), evaluator).trim();
        System.out.println("Column AS (3차 합): " + colAS_final);
        long asValue = 0L;
        if (!colAS_final.isEmpty()) {
            try {
                asValue = Long.parseLong(colAS_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                asValue = 0L;
            }
        } else {
            asValue = 0L;
        }
        phase3.setFeesum(asValue);
        phase3.setCustomer(customer);
        customer.getPhases().add(phase3);

        // Phase 4 (4차 예정일자: 인덱스 45)
        String colAT = formatter.formatCellValue(row.getCell(45), evaluator);
        System.out.println("Column AT (4차 예정일자): " + colAT);
        Phase phase4 = new Phase();
        phase4.setPhaseNumber(4);
        phase4.setPlanneddateString(colAT);
        if (colAT != null && !colAT.trim().isEmpty()) {
            phase4.setPlanneddate(parsePlannedDate(colAT));
        }
        String colAU = formatter.formatCellValue(row.getCell(46), evaluator);
        System.out.println("Column AU (4차 완납일자): " + colAU);
        if (!colAU.isEmpty()) {
            phase4.setFullpaiddate(parseDate(colAU, dtf));
        }
        String colAV = formatter.formatCellValue(row.getCell(47), evaluator);
        System.out.println("Column AV (4차 부담금): " + colAV);
        if (!colAV.isEmpty()) {
            phase4.setCharge(Long.parseLong(colAV.replaceAll("[^0-9-]+", "")));
        }
        String colAW = formatter.formatCellValue(row.getCell(48), evaluator);
        System.out.println("Column AW (4차 할인액): " + colAW);
        if (!colAW.isEmpty()) {
            phase4.setDiscount(Math.abs(Long.parseLong(colAW.replaceAll("[^0-9-]+", ""))));
        }
        String colAX = formatter.formatCellValue(row.getCell(49), evaluator);
        System.out.println("Column AX (4차 업무대행비): " + colAX);
        if (!colAX.isEmpty()) {
            phase4.setService(Long.parseLong(colAX.replaceAll("[^0-9-]+", "")));
        }
        String colAY = formatter.formatCellValue(row.getCell(50), evaluator);
        System.out.println("Column AY (4차 이동): " + colAY);
        phase4.setMove(colAY);
// AZ: 4차 합 (예: index 51)
        String colAZ_final = formatter.formatCellValue(row.getCell(51), evaluator).trim();
        System.out.println("Column AZ (4차 합): " + colAZ_final);
        long azValue = 0L;
        if (!colAZ_final.isEmpty()) {
            try {
                azValue = Long.parseLong(colAZ_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                azValue = 0L;
            }
        } else {
            azValue = 0L;
        }
        phase4.setFeesum(azValue);
        phase4.setCustomer(customer);
        customer.getPhases().add(phase4);

        // Phase 5 (5차 예정일자: 인덱스 52)
        String colBA = formatter.formatCellValue(row.getCell(52), evaluator);
        System.out.println("Column BA (5차 예정일자): " + colBA);
        Phase phase5 = new Phase();
        phase5.setPhaseNumber(5);
        phase5.setPlanneddateString(colBA);
        if (colBA != null && !colBA.trim().isEmpty()) {
            phase5.setPlanneddate(parsePlannedDate(colBA));
        }
        String colBB = formatter.formatCellValue(row.getCell(53), evaluator);
        System.out.println("Column BB (5차 완납일자): " + colBB);
        if (!colBB.isEmpty()) {
            phase5.setFullpaiddate(parseDate(colBB, dtf));
        }
        String colBC = formatter.formatCellValue(row.getCell(54), evaluator);
        System.out.println("Column BC (5차 부담금): " + colBC);
        if (!colBC.isEmpty()) {
            phase5.setCharge(Long.parseLong(colBC.replaceAll("[^0-9-]+", "")));
        }
        String colBD = formatter.formatCellValue(row.getCell(55), evaluator);
        System.out.println("Column BD (5차 할인액): " + colBD);
        if (!colBD.isEmpty()) {
            phase5.setDiscount(Math.abs(Long.parseLong(colBD.replaceAll("[^0-9-]+", ""))));
        }
        String colBE = formatter.formatCellValue(row.getCell(56), evaluator);
        System.out.println("Column BE (5차 면제금액): " + colBE);
        if (!colBE.isEmpty()) {
            phase5.setExemption(Math.abs(Long.parseLong(colBE.replaceAll("[^0-9-]+", ""))));
        }
        String colBF = formatter.formatCellValue(row.getCell(57), evaluator);
        System.out.println("Column BF (5차 업무대행비): " + colBF);
        if (!colBF.isEmpty()) {
            phase5.setService(Long.parseLong(colBF.replaceAll("[^0-9-]+", "")));
        }
        String colBG = formatter.formatCellValue(row.getCell(58), evaluator);
        System.out.println("Column BG (5차 이동): " + colBG);
        phase5.setMove(colBG);
// BH: 5차 합 (예: index 59)
        String colBH_final = formatter.formatCellValue(row.getCell(59), evaluator).trim();
        System.out.println("Column BH (5차 합): " + colBH_final);
        long bhValue = 0L;
        if (!colBH_final.isEmpty()) {
            try {
                bhValue = Long.parseLong(colBH_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                bhValue = 0L;
            }
        } else {
            bhValue = 0L;
        }
        phase5.setFeesum(bhValue);
        phase5.setCustomer(customer);
        customer.getPhases().add(phase5);

        // Phase 6 (6차 예정일자: 인덱스 60)
        String colBI = formatter.formatCellValue(row.getCell(60), evaluator);
        System.out.println("Column BI (6차 예정일자): " + colBI);
        Phase phase6 = new Phase();
        phase6.setPhaseNumber(6);
        phase6.setPlanneddateString(colBI);
        if (colBI != null && !colBI.trim().isEmpty()) {
            phase6.setPlanneddate(parsePlannedDate(colBI));
        }
        String colBJ = formatter.formatCellValue(row.getCell(61), evaluator);
        System.out.println("Column BJ (6차 완납일자): " + colBJ);
        if (!colBJ.isEmpty()) {
            phase6.setFullpaiddate(parseDate(colBJ, dtf));
        }
        String colBK = formatter.formatCellValue(row.getCell(62), evaluator);
        System.out.println("Column BK (6차 부담금): " + colBK);
        if (!colBK.isEmpty()) {
            phase6.setCharge(Long.parseLong(colBK.replaceAll("[^0-9-]+", "")));
        }
        String colBL = formatter.formatCellValue(row.getCell(63), evaluator);
        System.out.println("Column BL (6차 할인액): " + colBL);
        if (!colBL.isEmpty()) {
            phase6.setDiscount(Math.abs(Long.parseLong(colBL.replaceAll("[^0-9-]+", ""))));
        }
        String colBM = formatter.formatCellValue(row.getCell(64), evaluator);
        System.out.println("Column BM (6차 면제금액): " + colBM);
        if (!colBM.isEmpty()) {
            phase6.setExemption(Math.abs(Long.parseLong(colBM.replaceAll("[^0-9-]+", ""))));
        }
        String colBN = formatter.formatCellValue(row.getCell(65), evaluator);
        System.out.println("Column BN (6차 업무대행비): " + colBN);
        if (!colBN.isEmpty()) {
            phase6.setService(Long.parseLong(colBN.replaceAll("[^0-9-]+", "")));
        }
        String colBO = formatter.formatCellValue(row.getCell(66), evaluator);
        System.out.println("Column BO (6차 이동): " + colBO);
        phase6.setMove(colBO);
// BP: 6차 합 (예: index 67)
        String colBP_final = formatter.formatCellValue(row.getCell(67), evaluator).trim();
        System.out.println("Column BP (6차 합): " + colBP_final);
        long bpValue = 0L;
        if (!colBP_final.isEmpty()) {
            try {
                bpValue = Long.parseLong(colBP_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                bpValue = 0L;
            }
        } else {
            bpValue = 0L;
        }
        phase6.setFeesum(bpValue);
        phase6.setCustomer(customer);
        customer.getPhases().add(phase6);

        // Phase 7 (7차 예정일자: 인덱스 68)
        String colBQ = formatter.formatCellValue(row.getCell(68), evaluator);
        System.out.println("Column BQ (7차 예정일자): " + colBQ);
        Phase phase7 = new Phase();
        phase7.setPhaseNumber(7);
        phase7.setPlanneddateString(colBQ);
        if (colBQ != null && !colBQ.trim().isEmpty()) {
            phase7.setPlanneddate(parsePlannedDate(colBQ));
        }
        String colBR = formatter.formatCellValue(row.getCell(69), evaluator);
        System.out.println("Column BR (7차 완납일자): " + colBR);
        if (!colBR.isEmpty()) {
            phase7.setFullpaiddate(parseDate(colBR, dtf));
        }
        String colBS = formatter.formatCellValue(row.getCell(70), evaluator);
        System.out.println("Column BS (7차 부담금): " + colBS);
        if (!colBS.isEmpty()) {
            phase7.setCharge(Long.parseLong(colBS.replaceAll("[^0-9-]+", "")));
        }
        String colBT = formatter.formatCellValue(row.getCell(71), evaluator);
        System.out.println("Column BT (7차 할인액): " + colBT);
        if (!colBT.isEmpty()) {
            phase7.setDiscount(Math.abs(Long.parseLong(colBT.replaceAll("[^0-9-]+", ""))));
        }
        String colBU = formatter.formatCellValue(row.getCell(72), evaluator);
        System.out.println("Column BU (7차 면제금액): " + colBU);
        if (!colBU.isEmpty()) {
            phase7.setExemption(Math.abs(Long.parseLong(colBU.replaceAll("[^0-9-]+", ""))));
        }
        String colBV = formatter.formatCellValue(row.getCell(73), evaluator);
        System.out.println("Column BV (7차 업무대행비): " + colBV);
        if (!colBV.isEmpty()) {
            phase7.setService(Long.parseLong(colBV.replaceAll("[^0-9-]+", "")));
        }
        String colBW = formatter.formatCellValue(row.getCell(74), evaluator);
        System.out.println("Column BW (7차 이동): " + colBW);
        phase7.setMove(colBW);
// BX: 7차 합 (예: index 75)
        String colBX_final = formatter.formatCellValue(row.getCell(75), evaluator).trim();
        System.out.println("Column BX (7차 합): " + colBX_final);
        long bxValue = 0L;
        if (!colBX_final.isEmpty()) {
            try {
                bxValue = Long.parseLong(colBX_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                bxValue = 0L;
            }
        } else {
            bxValue = 0L;
        }
        phase7.setFeesum(bxValue);
        phase7.setCustomer(customer);
        customer.getPhases().add(phase7);

        // Phase 8 (8차 예정일자: 인덱스 76)
        String colBY = formatter.formatCellValue(row.getCell(76), evaluator);
        System.out.println("Column BY (8차 예정일자): " + colBY);
        Phase phase8 = new Phase();
        phase8.setPhaseNumber(8);
        phase8.setPlanneddateString(colBY);
        if (colBY != null && !colBY.trim().isEmpty()) {
            phase8.setPlanneddate(parsePlannedDate(colBY));
        }
        String colBZ = formatter.formatCellValue(row.getCell(77), evaluator);
        System.out.println("Column BZ (8차 완납일자): " + colBZ);
        if (!colBZ.isEmpty()) {
            phase8.setFullpaiddate(parseDate(colBZ, dtf));
        }
        String colCA = formatter.formatCellValue(row.getCell(78), evaluator);
        System.out.println("Column CA (8차 부담금): " + colCA);
        if (!colCA.isEmpty()) {
            phase8.setCharge(Long.parseLong(colCA.replaceAll("[^0-9-]+", "")));
        }
        String colCB = formatter.formatCellValue(row.getCell(79), evaluator);
        System.out.println("Column CB (8차 할인액): " + colCB);
        if (!colCB.isEmpty()) {
            phase8.setDiscount(Math.abs(Long.parseLong(colCB.replaceAll("[^0-9-]+", ""))));
        }
        String colCC = formatter.formatCellValue(row.getCell(80), evaluator);
        System.out.println("Column CC (8차 업무대행비): " + colCC);
        if (!colCC.isEmpty()) {
            phase8.setService(Long.parseLong(colCC.replaceAll("[^0-9-]+", "")));
        }
        String colCD = formatter.formatCellValue(row.getCell(81), evaluator);
        System.out.println("Column CD (8차 이동): " + colCD);
        phase8.setMove(colCD);
// CE: 8차 합 (예: index 82)
        String colCE_final = formatter.formatCellValue(row.getCell(82), evaluator).trim();
        System.out.println("Column CE (8차 합): " + colCE_final);
        long ceValue = 0L;
        if (!colCE_final.isEmpty()) {
            try {
                ceValue = Long.parseLong(colCE_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                ceValue = 0L;
            }
        } else {
            ceValue = 0L;
        }
        phase8.setFeesum(ceValue);
        phase8.setCustomer(customer);
        customer.getPhases().add(phase8);

        // Phase 9 (9차 예정일자: 인덱스 83)
        String colCF = formatter.formatCellValue(row.getCell(83), evaluator);
        System.out.println("Column CF (9차 예정일자): " + colCF);
        Phase phase9 = new Phase();
        phase9.setPhaseNumber(9);
        phase9.setPlanneddateString(colCF);
        if (colCF != null && !colCF.trim().isEmpty()) {
            phase9.setPlanneddate(parsePlannedDate(colCF));
        }
        String colCG = formatter.formatCellValue(row.getCell(84), evaluator);
        System.out.println("Column CG (9차 완납일자): " + colCG);
        if (!colCG.isEmpty()) {
            phase9.setFullpaiddate(parseDate(colCG, dtf));
        }
        String colCH = formatter.formatCellValue(row.getCell(85), evaluator);
        System.out.println("Column CH (9차 부담금): " + colCH);
        if (!colCH.isEmpty()) {
            phase9.setCharge(Long.parseLong(colCH.replaceAll("[^0-9-]+", "")));
        }
        String colCI = formatter.formatCellValue(row.getCell(86), evaluator);
        System.out.println("Column CI (9차 할인액): " + colCI);
        if (!colCI.isEmpty()) {
            phase9.setDiscount(Math.abs(Long.parseLong(colCI.replaceAll("[^0-9-]+", ""))));
        }
        String colCJ = formatter.formatCellValue(row.getCell(87), evaluator);
        System.out.println("Column CJ (9차 업무대행비): " + colCJ);
        if (!colCJ.isEmpty()) {
            phase9.setService(Long.parseLong(colCJ.replaceAll("[^0-9-]+", "")));
        }
        String colCK = formatter.formatCellValue(row.getCell(88), evaluator);
        System.out.println("Column CK (9차 이동): " + colCK);
        phase9.setMove(colCK);
// CL: 9차 합 (예: index 89)
        String colCL_final = formatter.formatCellValue(row.getCell(89), evaluator).trim();
        System.out.println("Column CL (9차 합): " + colCL_final);
        long clValue = 0L;
        if (!colCL_final.isEmpty()) {
            try {
                clValue = Long.parseLong(colCL_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                clValue = 0L;
            }
        } else {
            clValue = 0L;
        }
        phase9.setFeesum(clValue);
        phase9.setCustomer(customer);
        customer.getPhases().add(phase9);

        // Phase 10 (10차 예정일자: 인덱스 90)
        String colCM = formatter.formatCellValue(row.getCell(90), evaluator);
        System.out.println("Column CM (10차 예정일자): " + colCM);
        Phase phase10 = new Phase();
        phase10.setPhaseNumber(10);
        phase10.setPlanneddateString(colCM);
        if (colCM != null && !colCM.trim().isEmpty()) {
            phase10.setPlanneddate(parsePlannedDate(colCM));
        }
        String colCN = formatter.formatCellValue(row.getCell(91), evaluator);
        System.out.println("Column CN (10차 완납일자): " + colCN);
        if (!colCN.isEmpty()) {
            phase10.setFullpaiddate(parseDate(colCN, dtf));
        }
        String colCO = formatter.formatCellValue(row.getCell(92), evaluator);
        System.out.println("Column CO (10차 부담금): " + colCO);
        if (!colCO.isEmpty()) {
            phase10.setCharge(Long.parseLong(colCO.replaceAll("[^0-9-]+", "")));
        }
        String colCP = formatter.formatCellValue(row.getCell(93), evaluator);
        System.out.println("Column CP (10차 할인액): " + colCP);
        if (!colCP.isEmpty()) {
            phase10.setDiscount(Math.abs(Long.parseLong(colCP.replaceAll("[^0-9-]+", ""))));
        }
        String colCQ = formatter.formatCellValue(row.getCell(94), evaluator);
        System.out.println("Column CQ (10차 업무대행비): " + colCQ);
        if (!colCQ.isEmpty()) {
            phase10.setService(Long.parseLong(colCQ.replaceAll("[^0-9-]+", "")));
        }
        String colCR = formatter.formatCellValue(row.getCell(95), evaluator);
        System.out.println("Column CR (10차 이동): " + colCR);
        phase10.setMove(colCR);
// CS: 10차 합 (예: index 96)
        String colCS_final = formatter.formatCellValue(row.getCell(96), evaluator).trim();
        System.out.println("Column CS (10차 합): " + colCS_final);
        long csValue = 0L;
        if (!colCS_final.isEmpty()) {
            try {
                csValue = Long.parseLong(colCS_final.replaceAll("[^0-9-]+", ""));
            } catch (NumberFormatException e) {
                csValue = 0L;
            }
        } else {
            csValue = 0L;
        }
        phase10.setFeesum(csValue);
        phase10.setCustomer(customer);
        customer.getPhases().add(phase10);


        // -------------------------------------------------------------
        // (추가) 인덱스 98, 99번 열의 합을 Loan.loanammount 로 저장
        // -------------------------------------------------------------
        String col98Str = formatter.formatCellValue(row.getCell(98), evaluator).trim();
        String col99Str = formatter.formatCellValue(row.getCell(99), evaluator).trim();

        long col98Val = parseLongOrZero(col98Str);
        long col99Val = parseLongOrZero(col99Str);

        long loanAmmountSum = col98Val + col99Val;
        customer.getLoan().setLoanammount(loanAmmountSum);

        // -------------------------------------------------------------
        // (추가) 인덱스 101번 열을 Loan.selfammount 로 저장
        // -------------------------------------------------------------
        String col101Str = formatter.formatCellValue(row.getCell(101), evaluator).trim();
        long selfAmmount = parseLongOrZero(col101Str);
        customer.getLoan().setSelfammount(selfAmmount);

        // -------------------------------------------------------------
        // (추가) 인덱스 102번 열을 Loan.loanselfsum, Loan.loanselfcurrent,
        //        Status.loanExceedAmount 에 모두 저장
        // -------------------------------------------------------------
        String col102Str = formatter.formatCellValue(row.getCell(102), evaluator).trim();
        long col102Val = parseLongOrZero(col102Str);

        customer.getLoan().setLoanselfsum(col102Val);
        customer.getLoan().setLoanselfcurrent(col102Val);
        customer.getStatus().setLoanExceedAmount(col102Val);


        // --- 최종 섹션: DA ~ FO (0-based 인덱스 104 ~ 170) ---
        String colDA_final = formatter.formatCellValue(row.getCell(104), evaluator);
        System.out.println("Column DA (총 면제금액): " + colDA_final);
        if (!colDA_final.isEmpty()) {
            customer.getStatus().setExemptionsum(Long.parseLong(colDA_final.replaceAll("[^0-9-]+", "")));
        }
// DB: 해약 해지일자 (0-based index 105)
        String colDB_final = formatter.formatCellValue(row.getCell(105), evaluator).trim();
        System.out.println("Column DB (해약 해지일자): " + colDB_final);
        LocalDate cancelDate = null;
        if (!colDB_final.isEmpty() && !colDB_final.equalsIgnoreCase("x")) {
            try {
                cancelDate = LocalDate.parse(colDB_final, dtf);
            } catch (Exception e) {
                // 날짜 파싱 실패 시 null 처리
                cancelDate = null;
            }
        }
        customer.getCancel().setCanceldate(cancelDate);

// DC: 해약 환급일자 (0-based index 106)
        String colDC_final = formatter.formatCellValue(row.getCell(106), evaluator).trim();
        System.out.println("Column DC (해약 환급일자): " + colDC_final);
        LocalDate refundDate = null;
        if (!colDC_final.isEmpty() && !colDC_final.equalsIgnoreCase("x")) {
            try {
                refundDate = LocalDate.parse(colDC_final, dtf);
            } catch (Exception e) {
                refundDate = null;
            }
        }
        customer.getCancel().setRefunddate(refundDate);

// DD: 해약 환급금 (0-based index 107)
// '-' 또는 숫자형이 아닌 값이면 0으로 처리
        String colDD_final = formatter.formatCellValue(row.getCell(107), evaluator).trim();
        System.out.println("Column DD (해약 환급금): " + colDD_final);
        int refundAmount = 0;
        if (!colDD_final.isEmpty() && !colDD_final.equals("-")) {
            try {
                refundAmount = Integer.parseInt(colDD_final.replaceAll("[^0-9]+", ""));
            } catch (Exception e) {
                refundAmount = 0;
            }
        }
        customer.getCancel().setRefundamount(refundAmount);

        String colDE_final = formatter.formatCellValue(row.getCell(108), evaluator).trim();
        System.out.println("Column DE (납입총액): " + colDE_final);
        long amountSum = 0L;
        if (!colDE_final.isEmpty()) {
            String numeric = colDE_final.replaceAll("[^0-9-]+", "");
            try {
                amountSum = Long.parseLong(numeric);
            } catch (NumberFormatException e) {
                amountSum = 0L;
            }
        }
        customer.getStatus().setAmmountsum(amountSum);
        System.out.println("Column DF (건너뛰기)");
        String colDG_final = formatter.formatCellValue(row.getCell(110), evaluator);
        System.out.println("Column DG (담당 총괄): " + colDG_final);
        customer.getResponsible().setGeneralmanagement(colDG_final);
        String colDH_final = formatter.formatCellValue(row.getCell(111), evaluator);
        System.out.println("Column DH (담당 본부): " + colDH_final);
        customer.getResponsible().setDivision(colDH_final);
        String colDI_final = formatter.formatCellValue(row.getCell(112), evaluator);
        System.out.println("Column DI (담당 팀): " + colDI_final);
        customer.getResponsible().setTeam(colDI_final);
        String colDJ_final = formatter.formatCellValue(row.getCell(113), evaluator);
        System.out.println("Column DJ (담당 성명): " + colDJ_final);
        customer.getResponsible().setManagername(colDJ_final);
        String colDK_final = formatter.formatCellValue(row.getCell(114), evaluator);
        System.out.println("Column DK (담당 수수료지급): " + colDK_final);
        customer.getResponsible().setFeepaid(colDK_final);
        String colDL_final = formatter.formatCellValue(row.getCell(115), evaluator);
        System.out.println("Column DL (다힘 시상): " + colDL_final);
        customer.getDahim().setDahimsisang(colDL_final);
        String colDM_final = formatter.formatCellValue(row.getCell(116), evaluator);
        System.out.println("Column DM (다힘 일자): " + colDM_final);
        if (!colDM_final.isEmpty()) {
            customer.getDahim().setDahimdate(parseDate(colDM_final, dtf));
        }
        String colDN_final = formatter.formatCellValue(row.getCell(117), evaluator);
        System.out.println("Column DN (다힘 6/30선지급): " + colDN_final);
        customer.getDahim().setDahimprepaid(colDN_final);
        String colDO_final = formatter.formatCellValue(row.getCell(118), evaluator);
        System.out.println("Column DO (다힘 1회차청구): " + colDO_final);
        customer.getDahim().setDahimfirst(colDO_final);
        String colDP_final = formatter.formatCellValue(row.getCell(119), evaluator);
        System.out.println("Column DP (다힘 (1회차)금액): " + colDP_final);
        if (!colDP_final.isEmpty()) {
            customer.getDahim().setDahimfirstpay(String.valueOf(Long.parseLong(colDP_final.replaceAll("[^0-9-]+", ""))));
        }
        Cell cellDQ = row.getCell(120);
        LocalDate dateDQ = getUnderlyingDate(cellDQ);
        if (dateDQ != null) {
            System.out.println("Column DQ (다힘 일자2, raw): " + dateDQ);
            customer.getDahim().setDahimdate2(dateDQ);
        } else {
            String colDQ_final = formatter.formatCellValue(cellDQ, evaluator);
            System.out.println("Column DQ (다힘 일자2, fallback): " + colDQ_final);
            if (!colDQ_final.isEmpty()) {
                customer.getDahim().setDahimdate2(parseDate(colDQ_final, dtf));
            }
        }
        String colDR_final = formatter.formatCellValue(row.getCell(121), evaluator);
        System.out.println("Column DR (다힘 출처): " + colDR_final);
        customer.getDahim().setDahimsource(colDR_final);
        String colDS_final = formatter.formatCellValue(row.getCell(122), evaluator);
        System.out.println("Column DS (다힘 2회차청구): " + colDS_final);
        customer.getDahim().setDahimsecond(colDS_final);
        String colDT_final = formatter.formatCellValue(row.getCell(123), evaluator);
        System.out.println("Column DT (다힘 (2회차)금액): " + colDT_final);
        if (!colDT_final.isEmpty()) {
            customer.getDahim().setDahimsecondpay(String.valueOf(Long.parseLong(colDT_final.replaceAll("[^0-9-]+", ""))));
        }
        Cell cellDU = row.getCell(124);
        LocalDate dateDU = getUnderlyingDate(cellDU);
        if (dateDU != null) {
            System.out.println("Column DU (다힘 일자3, raw): " + dateDU);
            customer.getDahim().setDahimdate3(dateDU);
        } else {
            String colDU_final = formatter.formatCellValue(cellDU, evaluator);
            System.out.println("Column DU (다힘 일자3, fallback): " + colDU_final);
            if (!colDU_final.isEmpty()) {
                customer.getDahim().setDahimdate3(parseDate(colDU_final, dtf));
            }
        }
        String colDV_final = formatter.formatCellValue(row.getCell(125), evaluator);
        System.out.println("Column DV (다힘 합계): " + colDV_final);
        customer.getDahim().setDahimsum(colDV_final);

        String colDW_final = formatter.formatCellValue(row.getCell(126), evaluator).trim();
        System.out.println("Column DW (MGM 수수료): " + colDW_final);
// 빈 문자열이면 ""로, 아니면 그대로 저장 (필요에 따라 추가 전처리 가능)
        customer.getMgm().setMgmfee(colDW_final.isEmpty() ? "" : colDW_final);
        String colDX_final = formatter.formatCellValue(row.getCell(127), evaluator);
        System.out.println("Column DX (MGM 업체명): " + colDX_final);
        customer.getMgm().setMgmcompanyname(colDX_final);
        String colDY_final = formatter.formatCellValue(row.getCell(128), evaluator);
        System.out.println("Column DY (MGM 이름): " + colDY_final);
        customer.getMgm().setMgmname(colDY_final);
        String colDZ_final = formatter.formatCellValue(row.getCell(129), evaluator);
        System.out.println("Column DZ (MGM 기관): " + colDZ_final);
        customer.getMgm().setMgminstitution(colDZ_final);
        String colEA_final = formatter.formatCellValue(row.getCell(130), evaluator);
        System.out.println("Column EA (MGM 계좌): " + colEA_final);
        customer.getMgm().setMgmaccount(colEA_final);
        // EB: 0-based 131
        String colEB_final = formatter.formatCellValue(row.getCell(131), evaluator);
        System.out.println("Column EB (1차(직원) 차순): " + colEB_final);
        customer.getFirstemp().setFirstemptimes(colEB_final);
        // EC: 0-based 132, 특별 처리 (1차 지급일자)
        Cell cellEC = row.getCell(132);
        LocalDate dateEC = getUnderlyingDate(cellEC);
        if (dateEC != null) {
            System.out.println("Column EC (1차 지급일자, raw): " + dateEC);
            customer.getFirstemp().setFirstempdate(dateEC);
        } else {
            String colEC_final = formatter.formatCellValue(cellEC, evaluator);
            System.out.println("Column EC (1차 지급일자, fallback): " + colEC_final);
            if (!colEC_final.isEmpty()) {
                customer.getFirstemp().setFirstempdate(parseDate(colEC_final, dtf));
            }
        }
        // ED: 0-based 133 (2차(직원) 차순)
        String colED_final = formatter.formatCellValue(row.getCell(133), evaluator);
        System.out.println("Column ED (2차(직원) 차순): " + colED_final);
        customer.getSecondemp().setSecondemptimes(colED_final);
        // EE: 0-based 134, 특별 처리 (2차 지급일자)
        Cell cellEE = row.getCell(134);
        LocalDate dateEE = getUnderlyingDate(cellEE);
        if (dateEE != null) {
            System.out.println("Column EE (2차 지급일자, raw): " + dateEE);
            customer.getSecondemp().setSecondempdate(dateEE);
        } else {
            String colEE_final = formatter.formatCellValue(cellEE, evaluator);
            System.out.println("Column EE (2차 지급일자, fallback): " + colEE_final);
            if (!colEE_final.isEmpty()) {
                customer.getSecondemp().setSecondempdate(parseDate(colEE_final, dtf));
            }
        }
        // EF: 0-based 135
        String colEF_final = formatter.formatCellValue(row.getCell(135), evaluator);
        System.out.println("Column EF (부속서류 인감증명서): " + colEF_final);
        customer.getAttachments().setSealcertificateprovided("o".equalsIgnoreCase(colEF_final));
        // EG: 0-based 136
        String colEG_final = formatter.formatCellValue(row.getCell(136), evaluator);
        System.out.println("Column EG (부속서류 본인서명확인서): " + colEG_final);
        customer.getAttachments().setSelfsignatureconfirmationprovided("o".equalsIgnoreCase(colEG_final));
        // EH: 0-based 137
        String colEH_final = formatter.formatCellValue(row.getCell(137), evaluator);
        System.out.println("Column EH (부속서류 신분증): " + colEH_final);
        customer.getAttachments().setIdcopyprovided("o".equalsIgnoreCase(colEH_final));
        // EI: 0-based 138
        String colEI_final = formatter.formatCellValue(row.getCell(138), evaluator);
        System.out.println("Column EI (부속서류 확약서): " + colEI_final);
        customer.getAttachments().setCommitmentletterprovided("o".equalsIgnoreCase(colEI_final));
        // EJ: 0-based 139
        String colEJ_final = formatter.formatCellValue(row.getCell(139), evaluator);
        System.out.println("Column EJ (부속서류 창준위용): " + colEJ_final);
        customer.getAttachments().setForfounding("o".equalsIgnoreCase(colEJ_final));
        // EK: 0-based 140
        String colEK_final = formatter.formatCellValue(row.getCell(140), evaluator);
        System.out.println("Column EK (부속서류 무상옵션): " + colEK_final);
        customer.getAttachments().setFreeoption("o".equalsIgnoreCase(colEK_final));
        // EL: 0-based 141
        String colEL_final = formatter.formatCellValue(row.getCell(141), evaluator);
        System.out.println("Column EL (부속서류 선호도조사): " + colEL_final);
        customer.getAttachments().setPreferenceattachment("o".equalsIgnoreCase(colEL_final));
        // EM: 0-based 142
        String colEM_final = formatter.formatCellValue(row.getCell(142), evaluator);
        System.out.println("Column EM (부속서류 총회동의서): " + colEM_final);
        customer.getAttachments().setPrizeattachment("o".equalsIgnoreCase(colEM_final));
        // EN: 0-based 143, 특별 처리 (부속서류 사은품 지급일자)
        Cell cellEN = row.getCell(143);
        LocalDate dateEN = getUnderlyingDate(cellEN);
        if (dateEN != null) {
            System.out.println("Column EN (부속서류 사은품 지급일자, raw): " + dateEN);
            customer.getAttachments().setPrizedate(dateEN);
            customer.getAttachments().setPrizeattachment(true);
        } else {
            String colEN_final = formatter.formatCellValue(cellEN, evaluator);
            System.out.println("Column EN (부속서류 사은품 지급일자, fallback): " + colEN_final);
            if (!colEN_final.isEmpty()) {
                customer.getAttachments().setPrizedate(parseDate(colEN_final, dtf));
                customer.getAttachments().setPrizeattachment(true);
            } else {
                customer.getAttachments().setPrizeattachment(false);
            }
        }
        // EO: 0-based 144
        String colEO_final = formatter.formatCellValue(row.getCell(144), evaluator);
        System.out.println("Column EO (이메일): " + colEO_final);
        customer.getCustomerData().setEmail(colEO_final);
        // EP: 0-based 145
        String colEP_final = formatter.formatCellValue(row.getCell(145), evaluator);
        System.out.println("Column EP (우편물수령주소 우편번호): " + colEP_final);
        customer.getPostreceive().setPostnumberreceive(colEP_final);
        // EQ: 0-based 146, ER: 0-based 147
        String colEQ_final = formatter.formatCellValue(row.getCell(146), evaluator);
        String colER_final = formatter.formatCellValue(row.getCell(147), evaluator);
        System.out.println("Column EQ (우편물수령주소 도): " + colEQ_final);
        System.out.println("Column ER (우편물수령주소 군): " + colER_final);
        customer.getPostreceive().setPostreceive(colEQ_final + " " + colER_final);
        // ES: 0-based 148
        String colES_final = formatter.formatCellValue(row.getCell(148), evaluator);
        System.out.println("Column ES (우편물수령주소 상세주소): " + colES_final);
        customer.getPostreceive().setDetailaddressreceive(colES_final);
        // ET: 0-based 149
        String colET_final = formatter.formatCellValue(row.getCell(149), evaluator);
        System.out.println("Column ET (비고): " + colET_final);
        customer.setAdditional(colET_final);
        // EU: 0-based 150
        String colEU_final = formatter.formatCellValue(row.getCell(150), evaluator);
        System.out.println("Column EU (가입경로): " + colEU_final);
        customer.setRegisterpath(colEU_final);
        // EV: 0-based 151
        String colEV_final = formatter.formatCellValue(row.getCell(151), evaluator);
        System.out.println("Column EV (총회참석 서면): " + colEV_final);
        customer.getMeetingattend().setFtofattend(colEV_final);
        // EW: 0-based 152
        String colEW_final = formatter.formatCellValue(row.getCell(152), evaluator);
        System.out.println("Column EW (총회참석 직접): " + colEW_final);
        customer.getMeetingattend().setSelfattend(colEW_final);
        // EX: 0-based 153
        String colEX_final = formatter.formatCellValue(row.getCell(153), evaluator);
        System.out.println("Column EX (총회참석 대리): " + colEX_final);
        customer.getMeetingattend().setBehalfattend(colEX_final);
        // EY: 0-based 154
        String colEY_final = formatter.formatCellValue(row.getCell(154), evaluator);
        System.out.println("Column EY (특이사항): " + colEY_final);
        customer.setSpecialnote(colEY_final);
        // EZ: 0-based 155
        String colEZ_final = formatter.formatCellValue(row.getCell(155), evaluator);
        System.out.println("Column EZ (투표기기): " + colEZ_final);
        customer.setVotemachine(colEZ_final);
        // FA: 0-based 156
        String colFA_final = formatter.formatCellValue(row.getCell(156), evaluator);
        System.out.println("Column FA (안건 제1호): " + colFA_final);
        customer.getAgenda().setAgenda1(colFA_final);
        // FB: 0-based 157
        String colFB_final = formatter.formatCellValue(row.getCell(157), evaluator);
        System.out.println("Column FB (안건 제2-1호): " + colFB_final);
        customer.getAgenda().setAgenda2_1(colFB_final);
        // FC: 0-based 158
        String colFC_final = formatter.formatCellValue(row.getCell(158), evaluator);
        System.out.println("Column FC (안건 제2-2호): " + colFC_final);
        customer.getAgenda().setAgenda2_2(colFC_final);
        // FD: 0-based 159
        String colFD_final = formatter.formatCellValue(row.getCell(159), evaluator);
        System.out.println("Column FD (안건 제2-3호): " + colFD_final);
        customer.getAgenda().setAgenda2_3(colFD_final);
        // FE: 0-based 160
        String colFE_final = formatter.formatCellValue(row.getCell(160), evaluator);
        System.out.println("Column FE (안건 제2-4호): " + colFE_final);
        customer.getAgenda().setAgenda2_4(colFE_final);
        // FF: 0-based 161
        String colFF_final = formatter.formatCellValue(row.getCell(161), evaluator);
        System.out.println("Column FF (안건 제3호): " + colFF_final);
        customer.getAgenda().setAgenda3(colFF_final);
        // FG: 0-based 162
        String colFG_final = formatter.formatCellValue(row.getCell(162), evaluator);
        System.out.println("Column FG (안건 제4호): " + colFG_final);
        customer.getAgenda().setAgenda4(colFG_final);
        // FH: 0-based 163
        String colFH_final = formatter.formatCellValue(row.getCell(163), evaluator);
        System.out.println("Column FH (안건 제5호): " + colFH_final);
        customer.getAgenda().setAgenda5(colFH_final);
        // FI: 0-based 164
        String colFI_final = formatter.formatCellValue(row.getCell(164), evaluator);
        System.out.println("Column FI (안건 제6호): " + colFI_final);
        customer.getAgenda().setAgenda6(colFI_final);
        // FJ: 0-based 165
        String colFJ_final = formatter.formatCellValue(row.getCell(165), evaluator);
        System.out.println("Column FJ (안건 제7호): " + colFJ_final);
        customer.getAgenda().setAgenda7(colFJ_final);
        // FK: 0-based 166
        String colFK_final = formatter.formatCellValue(row.getCell(166), evaluator);
        System.out.println("Column FK (안건 제8호): " + colFK_final);
        customer.getAgenda().setAgenda8(colFK_final);
        // FL: 0-based 167
        String colFL_final = formatter.formatCellValue(row.getCell(167), evaluator);
        System.out.println("Column FL (안건 제9호): " + colFL_final);
        customer.getAgenda().setAgenda9(colFL_final);
        // FM: 0-based 168
        String colFM_final = formatter.formatCellValue(row.getCell(168), evaluator);
        System.out.println("Column FM (안건 제10호): " + colFM_final);
        customer.getAgenda().setAgenda10(colFM_final);
        // FN: 0-based 169
        String colFN_final = formatter.formatCellValue(row.getCell(169), evaluator);
        System.out.println("Column FN (경품당첨 prizewinning): " + colFN_final);
        customer.setPrizewinning(colFN_final);
        // FO: 0-based 170
        String colFO_final = formatter.formatCellValue(row.getCell(170), evaluator);
        System.out.println("Column FO (부속서류 출자금): " + colFO_final);
        customer.getAttachments().setInvestmentfile("o".equalsIgnoreCase(colFO_final));
        //excel spread complete
        // --- 매핑 끝 ---
        return customer;
    }


    // 날짜 파싱 헬퍼: "yy-M-d" 형식을 우선 사용, 실패하면 "yyyy" 형식으로 파싱
    private LocalDate parseDate(String s, DateTimeFormatter dtf) {
        String text = s.replace("\"", "").trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text, dtf);
        } catch (DateTimeParseException e) {
            try {
                DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy");
                return LocalDate.parse(text, dtf2);
            } catch (DateTimeParseException ex) {
                // 날짜 형식이 아니면 null 반환하여 아무 값도 넣지 않음.
                return null;
            }
        }
    }

    // planneddateString 처리 헬퍼:
    // 만약 s가 공란이면 null, 아니면 "yy-M-d" 형식으로 파싱 시도; 실패하면 2100-01-01 리턴.
    private LocalDate parsePlannedDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        String cleaned = s.replaceAll("\"", "").trim();
        try {
            DateTimeFormatter plannedDtf = DateTimeFormatter.ofPattern("yy-M-d");
            return LocalDate.parse(cleaned, plannedDtf);
        } catch (Exception e) {
            return LocalDate.of(2100, 1, 1);
        }
    }

    // 셀에서 원본 날짜값(YYYY-MM-DD)을 추출하는 헬퍼
    private LocalDate getUnderlyingDate(Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    // --- fillFormat1 메서드 (원본 로직 유지) ---
    public void fillFormat1(File tempFile, Customer customer) throws IOException {
        try (FileInputStream fis = new FileInputStream(tempFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            getCell(sheet, 28, 49).setCellValue(customer.getId());
            getCell(sheet, 33, 54).setCellValue(customer.getType() != null ? customer.getType() : "");
            getCell(sheet, 33, 60).setCellValue(customer.getGroupname() != null ? customer.getGroupname() : "");
            getCell(sheet, 36, 54).setCellValue(customer.getTurn() != null ? customer.getTurn() : "");
            if (customer.getCustomerData() != null) {
                getCell(sheet, 63, 7).setCellValue(customer.getCustomerData().getName() != null ? customer.getCustomerData().getName() : "");
            }
            if (customer.getLegalAddress() != null) {
                String address = "";
                if (customer.getLegalAddress().getPost() != null) {
                    address += customer.getLegalAddress().getPost();
                }
                if (customer.getLegalAddress().getDetailaddress() != null) {
                    address += customer.getLegalAddress().getDetailaddress();
                }
                getCell(sheet, 65, 7).setCellValue(address);
            }
            if (customer.getCustomerData() != null) {
                getCell(sheet, 67, 7).setCellValue(customer.getCustomerData().getEmail() != null ? customer.getCustomerData().getEmail() : "");
            }
            getCell(sheet, 69, 23).setCellValue(customer.getCustomerData() != null ? customer.getCustomerData().getPhone() : "");
            if (customer.getRegisterprice() != null) {
                getCell(sheet, 73, 23).setCellValue(customer.getRegisterprice());
            }
            if (customer.getFinancial() != null) {
                getCell(sheet, 81, 2).setCellValue(customer.getFinancial().getBankname() != null ? customer.getFinancial().getBankname() : "");
                getCell(sheet, 81, 11).setCellValue(customer.getFinancial().getAccountnum() != null ? customer.getFinancial().getAccountnum() : "");
                getCell(sheet, 81, 22).setCellValue(customer.getFinancial().getTrustcompany() != null ? customer.getFinancial().getTrustcompany() : "");
            }
            if (customer.getRegisterdate() != null) {
                getCell(sheet, 96, 13).setCellValue(customer.getRegisterdate().toString());
            }
            if (customer.getPhases() != null && !customer.getPhases().isEmpty()) {
                getCell(sheet, 65, 52).setCellValue(customer.getPhases().get(0).getCharge() != null ? customer.getPhases().get(0).getCharge() : 0);
            }
            if (customer.getStatus() != null && customer.getStatus().getAmmountsum() != null) {
                getCell(sheet, 81, 52).setCellValue(customer.getStatus().getAmmountsum());
            }
            workbook.setForceFormulaRecalculation(true);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                workbook.write(fos);
            }
        }
    }

    // --- fillFormat2 메서드 (원본 로직 유지) ---
    public void fillFormat2(File tempFile, Customer customer) throws IOException {
        try (FileInputStream fis = new FileInputStream(tempFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            if (customer.getCustomerData() != null && customer.getCustomerData().getName() != null) {
                getCell(sheet, 8, 6).setCellValue(customer.getCustomerData().getName());
            }
            LocalDate rd = customer.getRegisterdate();
            if (rd != null) {
                getCell(sheet, 48, 11).setCellValue(rd.toString());
            }
            if (customer.getCustomerData() != null && customer.getCustomerData().getName() != null) {
                getCell(sheet, 63, 11).setCellValue(customer.getCustomerData().getName());
            }
            if (customer.getCustomerData() != null && customer.getCustomerData().getResnumfront() != null) {
                getCell(sheet, 65, 11).setCellValue(customer.getCustomerData().getResnumfront());
            }
            if (customer.getCustomerData() != null && customer.getCustomerData().getPhone() != null) {
                getCell(sheet, 67, 11).setCellValue(customer.getCustomerData().getPhone());
            }
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
            if (customer.getCustomerData() != null && customer.getCustomerData().getResnumfront() != null && customer.getCustomerData().getResnumback() != null) {
                String rrn = customer.getCustomerData().getResnumfront() + "-" + customer.getCustomerData().getResnumback();
                getCell(sheet, 150, 12).setCellValue(rrn);
            }
            workbook.setForceFormulaRecalculation(true);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                workbook.write(fos);
            }
        }
    }

    // getCell 헬퍼 (fillFormat1, fillFormat2에서 사용)
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

    private long parseLongOrZero(String numericStr) {
        if (numericStr == null || numericStr.isEmpty()) {
            return 0L;
        }
        try {
            // 숫자 이외 문자 제거
            String cleaned = numericStr.replaceAll("[^0-9\\-]", "");
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
