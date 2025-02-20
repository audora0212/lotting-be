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
import com.audora.lotting_be.service.CustomerService;
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

import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class ExcelService {

    @Autowired
    private CustomerService customerService;

    /**
     * 엑셀 파일의 첫번째 시트, 4번째 행(Row 인덱스 3)의 데이터를 읽어
     * 각 컬럼값을 System.out.println으로 출력하고,
     * 아래 매핑 스펙에 따라 Customer 및 관련 객체에 저장한 후 DB에 저장합니다.
     *
     * [매핑 스펙]
     * --- 첫 번째 섹션 (A ~ CS: 인덱스 0 ~ 96) ---
     * (세부 매핑은 제공해주신 코드와 동일)
     *
     * --- 최종 섹션 (엑셀에서는 CT~CZ 7열 건너뛰고 DA부터 FO까지) ---
     * 1-based: DA=105 ~ FO=172; 0-based: DA=104 ~ FO=170
     *
     * 특별 처리:
     * - DQ(0-based 120), DU(124), EC(132), EE(134), EN(143)는 날짜형이며,
     *   셀의 원본 날짜값을 우선 사용합니다.
     * - 또한, 각 차수의 예정일자(planneddateString)가 있을 경우,
     *   만약 해당 문자열이 "yy-M-d" 형식이면 이를 파싱하여 Phase의 planneddate에 저장하고,
     *   그렇지 않으면 2100-01-01을 저장합니다.
     *   공란인 경우에는 planneddate는 그대로 null로 둡니다.
     */
    public void processExcelFile(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yy-M-d");

            XSSFSheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(3);
            if (row == null) {
                throw new IOException("엑셀 파일에 4번째 행(Row 3)이 존재하지 않습니다.");
            }

            // Customer 및 하위 객체 초기화
            Customer customer = new Customer();
            if (customer.getCustomerData() == null) customer.setCustomerData(new CustomerData());
            if (customer.getLegalAddress() == null) customer.setLegalAddress(new LegalAddress());
            if (customer.getFinancial() == null) customer.setFinancial(new Financial());
            if (customer.getDeposits() == null) customer.setDeposits(new Deposit());
            if (customer.getAttachments() == null) customer.setAttachments(new Attachments());
            if (customer.getCancel() == null) customer.setCancel(new Cancel());
            if (customer.getLoan() == null) customer.setLoan(new Loan());
            if (customer.getResponsible() == null) customer.setResponsible(new Responsible());
            if (customer.getDahim() == null) customer.setDahim(new Dahim());
            if (customer.getMgm() == null) customer.setMgm(new MGM());
            if (customer.getFirstemp() == null) customer.setFirstemp(new Firstemp());
            if (customer.getSecondemp() == null) customer.setSecondemp(new Secondemp());
            if (customer.getMeetingattend() == null) customer.setMeetingattend(new Meetingattend());
            if (customer.getAgenda() == null) customer.setAgenda(new Agenda());
            if (customer.getPostreceive() == null) customer.setPostreceive(new Postreceive());
            if (customer.getStatus() == null) {
                Status st = new Status();
                st.setCustomer(customer);
                customer.setStatus(st);
            }

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
            String colAE = formatter.formatCellValue(row.getCell(30), evaluator);
            System.out.println("Column AE (1차 합): " + colAE);
            if (!colAE.isEmpty()) {
                phase1.setFeesum(Long.parseLong(colAE.replaceAll("[^0-9-]+", "")));
            }
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
            String colAL = formatter.formatCellValue(row.getCell(37), evaluator);
            System.out.println("Column AL (2차 합): " + colAL);
            if (!colAL.isEmpty()) {
                phase2.setFeesum(Long.parseLong(colAL.replaceAll("[^0-9-]+", "")));
            }
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
            String colAS = formatter.formatCellValue(row.getCell(44), evaluator);
            System.out.println("Column AS (3차 합): " + colAS);
            if (!colAS.isEmpty()) {
                phase3.setFeesum(Long.parseLong(colAS.replaceAll("[^0-9-]+", "")));
            }
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
            String colAZ = formatter.formatCellValue(row.getCell(51), evaluator);
            System.out.println("Column AZ (4차 합): " + colAZ);
            if (!colAZ.isEmpty()) {
                phase4.setFeesum(Long.parseLong(colAZ.replaceAll("[^0-9-]+", "")));
            }
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
            String colBH = formatter.formatCellValue(row.getCell(59), evaluator);
            System.out.println("Column BH (5차 합): " + colBH);
            if (!colBH.isEmpty()) {
                phase5.setFeesum(Long.parseLong(colBH.replaceAll("[^0-9-]+", "")));
            }
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
            String colBP = formatter.formatCellValue(row.getCell(67), evaluator);
            System.out.println("Column BP (6차 합): " + colBP);
            if (!colBP.isEmpty()) {
                phase6.setFeesum(Long.parseLong(colBP.replaceAll("[^0-9-]+", "")));
            }
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
            String colBX = formatter.formatCellValue(row.getCell(75), evaluator);
            System.out.println("Column BX (7차 합): " + colBX);
            if (!colBX.isEmpty()) {
                phase7.setFeesum(Long.parseLong(colBX.replaceAll("[^0-9-]+", "")));
            }
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
            String colCE = formatter.formatCellValue(row.getCell(82), evaluator);
            System.out.println("Column CE (8차 합): " + colCE);
            if (!colCE.isEmpty()) {
                phase8.setFeesum(Long.parseLong(colCE.replaceAll("[^0-9-]+", "")));
            }
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
            String colCL = formatter.formatCellValue(row.getCell(89), evaluator);
            System.out.println("Column CL (9차 합): " + colCL);
            if (!colCL.isEmpty()) {
                phase9.setFeesum(Long.parseLong(colCL.replaceAll("[^0-9-]+", "")));
            }
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
            String colCS = formatter.formatCellValue(row.getCell(96), evaluator);
            System.out.println("Column CS (10차 합): " + colCS);
            if (!colCS.isEmpty()) {
                phase10.setFeesum(Long.parseLong(colCS.replaceAll("[^0-9-]+", "")));
            }
            phase10.setCustomer(customer);
            customer.getPhases().add(phase10);

            // --- 최종 섹션: DA ~ FO (0-based 인덱스 104 ~ 170) ---
            String colDA_final = formatter.formatCellValue(row.getCell(104), evaluator);
            System.out.println("Column DA (총 면제금액): " + colDA_final);
            if (!colDA_final.isEmpty()) {
                customer.getStatus().setExemptionsum(Long.parseLong(colDA_final.replaceAll("[^0-9-]+", "")));
            }
            String colDB_final = formatter.formatCellValue(row.getCell(105), evaluator);
            System.out.println("Column DB (해약 해지일자): " + colDB_final);
            if (!colDB_final.isEmpty()) {
                customer.getCancel().setCanceldate(parseDate(colDB_final, dtf));
            }
            String colDC_final = formatter.formatCellValue(row.getCell(106), evaluator);
            System.out.println("Column DC (해약 환급일자): " + colDC_final);
            if (!colDC_final.isEmpty()) {
                customer.getCancel().setRefunddate(parseDate(colDC_final, dtf));
            }
            String colDD_final = formatter.formatCellValue(row.getCell(107), evaluator);
            System.out.println("Column DD (해약 환급금): " + colDD_final);
            if (!colDD_final.isEmpty()) {
                customer.getCancel().setRefundamount(Integer.parseInt(colDD_final.replaceAll("[^0-9-]+", "")));
            }
            String colDE_final = formatter.formatCellValue(row.getCell(108), evaluator);
            System.out.println("Column DE (납입총액): " + colDE_final);
            if (!colDE_final.isEmpty()) {
                customer.getStatus().setAmmountsum(Long.parseLong(colDE_final.replaceAll("[^0-9-]+", "")));
            }
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
            String colDW_final = formatter.formatCellValue(row.getCell(126), evaluator);
            System.out.println("Column DW (MGM 수수료): " + colDW_final);
            if (!colDW_final.isEmpty()) {
                customer.getMgm().setMgmfee(Long.parseLong(colDW_final.replaceAll("[^0-9-]+", "")));
            }
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
            System.out.println("Column FN (부속서류 사은품명): " + colFN_final);
            customer.getAttachments().setPrizename(colFN_final);
            // FO: 0-based 170
            String colFO_final = formatter.formatCellValue(row.getCell(170), evaluator);
            System.out.println("Column FO (부속서류 출자금): " + colFO_final);
            customer.getAttachments().setInvestmentfile("o".equalsIgnoreCase(colFO_final));

            // --- 매핑 끝 ---
            customerService.createCustomer(customer);
        }
    }

    // 날짜 파싱 헬퍼: "yy-M-d" 형식을 우선 사용, 실패하면 "yyyy" 형식으로 파싱
    private LocalDate parseDate(String s, DateTimeFormatter dtf) {
        String text = s.replace("\"", "").trim();
        try {
            return LocalDate.parse(text, dtf);
        } catch (DateTimeParseException e) {
            DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy");
            return LocalDate.parse(text, dtf2);
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
}
