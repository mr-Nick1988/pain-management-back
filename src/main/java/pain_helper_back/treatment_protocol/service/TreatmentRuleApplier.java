package pain_helper_back.treatment_protocol.service;

import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;

import java.util.List;

public interface TreatmentRuleApplier {

    void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient, List<String> rejectionReasons);
}