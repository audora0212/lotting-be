// FeeService.java
package com.audora.lotting_be.service;

import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.model.Fee.FeePerPhase;
import com.audora.lotting_be.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FeeService {

    @Autowired
    private FeeRepository feeRepository;

    /**
     * 새로운 Fee와 연관된 FeePerPhase 생성
     *
     * @param fee 생성할 Fee 객체
     * @return 생성된 Fee 객체
     */
    public Fee createFee(Fee fee) {
        // 양방향 관계 설정
        if (fee.getFeePerPhases() != null) {
            for (FeePerPhase phase : fee.getFeePerPhases()) {
                phase.setFee(fee);
            }
        }
        return feeRepository.save(fee);
    }

    /**
     * ID로 Fee 조회
     *
     * @param id Fee의 ID
     * @return Fee 객체 (존재할 경우), 없을 경우 null
     */
    public Fee getFeeById(Long id) {
        return feeRepository.findById(id).orElse(null);
    }

    /**
     * 모든 Fee 조회
     *
     * @return 모든 Fee의 리스트
     */
    public List<Fee> getAllFees() {
        return feeRepository.findAll();
    }
}
