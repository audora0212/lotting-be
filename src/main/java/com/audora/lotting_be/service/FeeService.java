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

    public Fee createFee(Fee fee) {
        if (fee.getFeePerPhases() != null) {
            for (FeePerPhase phase : fee.getFeePerPhases()) {
                phase.setFee(fee);
            }
        }
        return feeRepository.save(fee);
    }

    public Fee getFeeById(Long id) {
        return feeRepository.findById(id).orElse(null);
    }


    public List<Fee> getAllFees() {
        return feeRepository.findAll();
    }
}
