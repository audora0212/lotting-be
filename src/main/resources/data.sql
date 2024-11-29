-- data.sql

-- Fee 테이블에 데이터 삽입
INSERT INTO fee (group, floor, batch, type, supplyarea, priceperp, price, paymentratio, paysum)
VALUES ('A', '10', '1', 'Type1', 100, 2000000, 200000000, 0.1, 20000000);

-- FeePerPhase 테이블에 데이터 삽입
INSERT INTO fee_per_phase (phase_number, phasefee, phasedate, fee_id)
VALUES (1, 5000000, '1달', 1),
       (2, 5000000, '2달', 1),
       (3, 10000000, '3달', 1);
