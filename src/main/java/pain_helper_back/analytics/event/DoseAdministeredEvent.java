package pain_helper_back.analytics.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Событие введения дозы препарата.
 */

@Getter
public class DoseAdministeredEvent extends ApplicationEvent {
    private final String patientMrn;
    private final String drugName;
    private final Double dosage;
    private final String unit;
    private final String administeredBy;

    public DoseAdministeredEvent(String patientMrn, String drugName, Double dosage,
                                 String unit, String administeredBy) {
        super(patientMrn);
        this.patientMrn = patientMrn;
        this.drugName = drugName;
        this.dosage = dosage;
        this.unit = unit;
        this.administeredBy = administeredBy;
    }
}
