package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.Status;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFTextBox;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExcelService {

    /**
     * format1.xlsx 에 특정 셀들에 데이터 채워넣기
     *
     * @param tempFile  미리 복사해둔 임시 엑셀 파일
     * @param customer  대상 고객
     * @throws IOException 파일 I/O 에러
     */
    public void fillFormat1(File tempFile, Customer customer) throws IOException {
        try (FileInputStream fis = new FileInputStream(tempFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            // 예) AX29 -> row=28, col=49 : customer.id
            getCell(sheet, 28, 49).setCellValue(customer.getId());

            // BC34 -> row=33, col=54 : customer.type
            getCell(sheet, 33, 54).setCellValue(
                    customer.getType() != null ? customer.getType() : ""
            );

            // BI34 -> row=33, col=60 : groupname
            getCell(sheet, 33, 60).setCellValue(
                    customer.getGroupname() != null ? customer.getGroupname() : ""
            );

            // BC37 -> row=36, col=54 : turn
            getCell(sheet, 36, 54).setCellValue(
                    customer.getTurn() != null ? customer.getTurn() : ""
            );

            // H64 -> row=63, col=7 : CustomerData.name
            if (customer.getCustomerData() != null) {
                getCell(sheet, 63, 7).setCellValue(
                        customer.getCustomerData().getName() != null
                                ? customer.getCustomerData().getName()
                                : ""
                );
            }

            // W64 -> row=63, col=22 : resnumfront-resnumback
            if (customer.getCustomerData() != null) {
                String rrn = "";
                if (customer.getCustomerData().getResnumfront() != null
                        && customer.getCustomerData().getResnumback() != null) {
                    rrn = customer.getCustomerData().getResnumfront()
                            + "-"
                            + customer.getCustomerData().getResnumback();
                }
                getCell(sheet, 63, 22).setCellValue(rrn);
            }

            // H66 -> row=65, col=7 : (법정) 주소+상세주소
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

            // H68 -> row=67, col=7 : email
            if (customer.getCustomerData() != null) {
                getCell(sheet, 67, 7).setCellValue(
                        customer.getCustomerData().getEmail() != null
                                ? customer.getCustomerData().getEmail()
                                : ""
                );
            }

            // X70 -> row=69, col=23 : phone
            if (customer.getCustomerData() != null) {
                getCell(sheet, 69, 23).setCellValue(
                        customer.getCustomerData().getPhone() != null
                                ? customer.getCustomerData().getPhone()
                                : ""
                );
            }

            // X74 -> row=73, col=23 : registerprice
            if (customer.getRegisterprice() != null) {
                getCell(sheet, 73, 23).setCellValue(customer.getRegisterprice());
            }

            // C82 -> row=81, col=2 : Financial.bankname
            // L82 -> row=81, col=11 : Financial.accountnum
            // W82 -> row=81, col=22 : Financial.trustcompany
            if (customer.getFinancial() != null) {
                getCell(sheet, 81, 2).setCellValue(
                        customer.getFinancial().getBankname() != null
                                ? customer.getFinancial().getBankname()
                                : ""
                );
                getCell(sheet, 81, 11).setCellValue(
                        customer.getFinancial().getAccountnum() != null
                                ? customer.getFinancial().getAccountnum()
                                : ""
                );
                getCell(sheet, 81, 22).setCellValue(
                        customer.getFinancial().getTrustcompany() != null
                                ? customer.getFinancial().getTrustcompany()
                                : ""
                );
            }

            // N97 -> row=96, col=13 : registerdate
            if (customer.getRegisterdate() != null) {
                getCell(sheet, 96, 13).setCellValue(customer.getRegisterdate().toString());
            }

            // Phase 관련
            // BA66(1차), BA68(2차), BA70(3차), BA72(4차),
            // BA74(5차), BA76(6차), BA78(7차), BA80(8차)
            // -> row=65,67,69,71,73,75,77,79 col=52
            // BH65(1차), BH67(2차), BH69(3차), BH71(4차)
            // -> row=64,66,68,70 col=59
            List<Phase> phases = customer.getPhases();

            // charge 1 ~ 8
            if (phases != null && phases.size() > 0) {
                getCell(sheet, 65, 52).setCellValue(phases.get(0).getCharge() != null
                        ? phases.get(0).getCharge() : 0);
            }
            if (phases != null && phases.size() > 1) {
                getCell(sheet, 67, 52).setCellValue(phases.get(1).getCharge() != null
                        ? phases.get(1).getCharge() : 0);
            }
            if (phases != null && phases.size() > 2) {
                getCell(sheet, 69, 52).setCellValue(phases.get(2).getCharge() != null
                        ? phases.get(2).getCharge() : 0);
            }
            if (phases != null && phases.size() > 3) {
                getCell(sheet, 71, 52).setCellValue(phases.get(3).getCharge() != null
                        ? phases.get(3).getCharge() : 0);
            }
            if (phases != null && phases.size() > 4) {
                getCell(sheet, 73, 52).setCellValue(phases.get(4).getCharge() != null
                        ? phases.get(4).getCharge() : 0);
            }
            if (phases != null && phases.size() > 5) {
                getCell(sheet, 75, 52).setCellValue(phases.get(5).getCharge() != null
                        ? phases.get(5).getCharge() : 0);
            }
            if (phases != null && phases.size() > 6) {
                getCell(sheet, 77, 52).setCellValue(phases.get(6).getCharge() != null
                        ? phases.get(6).getCharge() : 0);
            }
            if (phases != null && phases.size() > 7) {
                getCell(sheet, 79, 52).setCellValue(phases.get(7).getCharge() != null
                        ? phases.get(7).getCharge() : 0);
            }

            // planneddate 1 ~ 4
            if (phases != null && phases.size() > 0) {
                LocalDate d1 = phases.get(0).getPlanneddate();
                getCell(sheet, 64, 59).setCellValue(d1 != null ? d1.toString() : "");
            }
            if (phases != null && phases.size() > 1) {
                LocalDate d2 = phases.get(1).getPlanneddate();
                getCell(sheet, 66, 59).setCellValue(d2 != null ? d2.toString() : "");
            }
            if (phases != null && phases.size() > 2) {
                LocalDate d3 = phases.get(2).getPlanneddate();
                getCell(sheet, 68, 59).setCellValue(d3 != null ? d3.toString() : "");
            }
            if (phases != null && phases.size() > 3) {
                LocalDate d4 = phases.get(3).getPlanneddate();
                getCell(sheet, 70, 59).setCellValue(d4 != null ? d4.toString() : "");
            }

            // Status.ammountsum -> BA82 (row=81, col=52)
            Status status = customer.getStatus();
            if (status != null && status.getAmmountsum() != null) {
                getCell(sheet, 81, 52).setCellValue(status.getAmmountsum());
            }

            // 엑셀 수정 내용을 tempFile에 저장
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                // 수식 재계산
                workbook.setForceFormulaRecalculation(true);
                workbook.write(fos);
            }
        }
    }

    /**
     * format2.xlsx 에 특정 셀들, 텍스트박스에 데이터 채워넣기
     */
    public void fillFormat2(File tempFile, Customer customer) throws IOException {
        try (FileInputStream fis = new FileInputStream(tempFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            // (1) 셀 수정
            // G9 -> row=8,col=6 : name
            if (customer.getCustomerData() != null && customer.getCustomerData().getName() != null) {
                getCell(sheet, 8, 6).setCellValue(customer.getCustomerData().getName());
            }
            // L49 -> row=48,col=11 : registerdate
            LocalDate rd = customer.getRegisterdate();
            if (rd != null) {
                getCell(sheet, 48, 11).setCellValue(rd.toString());
            }
            // L64 -> row=63,col=11 : name
            if (customer.getCustomerData() != null && customer.getCustomerData().getName() != null) {
                getCell(sheet, 63, 11).setCellValue(customer.getCustomerData().getName());
            }
            // L66 -> row=65,col=11 : resnumfront
            if (customer.getCustomerData() != null && customer.getCustomerData().getResnumfront() != null) {
                getCell(sheet, 65, 11).setCellValue(customer.getCustomerData().getResnumfront());
            }
            // L68 -> row=67,col=11 : phone
            if (customer.getCustomerData() != null && customer.getCustomerData().getPhone() != null) {
                getCell(sheet, 67, 11).setCellValue(customer.getCustomerData().getPhone());
            }
            // L70 -> row=69,col=11 : 우편번호 + 상세주소
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

            // M151 -> row=150,col=12 : resnumfront-resnumback
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
        }
    }

    /**
     * 엑셀 셀 객체를 안전하게 가져오는 헬퍼 메서드
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
