// DataInitializer.java
package com.audora.lotting_be;

import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.model.Fee.FeePerPhase;
import com.audora.lotting_be.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private FeeRepository feeRepository;

    @Override
    public void run(String... args) throws Exception {
        Fee existingFee = feeRepository.findByTypeAndGroupnameAndBatch("Type1", "A", "1");

        if (existingFee == null) {
            Fee fee = new Fee();
            fee.setGroupname("A");
            fee.setFloor("10");
            fee.setBatch("1");
            fee.setType("Type1");
            fee.setSupplyarea(100L);
            fee.setPriceperp(2000000L);
            fee.setPrice(200000000L);
            fee.setPaymentratio(0.1);
            fee.setPaysum(20000000L);

            List<FeePerPhase> feePerPhases = new ArrayList<>();

            for (int i = 1; i <= 10; i++) {
                FeePerPhase phase = new FeePerPhase();
                phase.setPhaseNumber(i);
                phase.setPhasefee(5000000L * i);
                phase.setPhasedate(i + "달");
                phase.setFee(fee);
                feePerPhases.add(phase);
            }

            fee.setFeePerPhases(feePerPhases);

            feeRepository.save(fee);
        }
    }
}
