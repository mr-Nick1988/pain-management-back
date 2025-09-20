package pain_helper_back.treatment_protocol.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pain_helper_back.nurse.entity.Emr;
import pain_helper_back.nurse.entity.Recommendation;
import pain_helper_back.nurse.entity.Vas;
import pain_helper_back.treatment_protocol.repository.TreatmentProtocolRepository;

@Service
@RequiredArgsConstructor
public class TreatmentProtocolService {
    private final TreatmentProtocolRepository treatmentProtocolRepository;




    public Recommendation generateRecommendation(Emr emr, Vas vas) {
        return null;
    }
}
