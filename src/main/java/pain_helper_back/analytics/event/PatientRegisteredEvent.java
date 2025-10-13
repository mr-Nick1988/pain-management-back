package pain_helper_back.analytics.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 * Событие: Пациент зарегистрирован
 * Публикуется в NurseServiceImpl.registerPatient() ИЛИ DoctorServiceImpl.createPatient()
 */
@Getter
public class PatientRegisteredEvent extends ApplicationEvent {
    private final Long patientId;
    private final String patientMrn;
    private final String registeredBy; // nurseId or doctor
    private final String registeredByRole; // "NURSE" or "DOCTOR"
    private final LocalDateTime registeredAt;
    private final Integer age;
    private final String gender;

    public PatientRegisteredEvent(Object source, Long patientId, String patientMrn,
                                  String registeredBy,String registeredByRole, LocalDateTime registeredAt,
                                  Integer age, String gender) {
        super(source);
        this.patientId = patientId;
        this.patientMrn = patientMrn;
        this.registeredBy = registeredBy;
        this.registeredByRole = registeredByRole;
        this.registeredAt = registeredAt;
        this.age = age;
        this.gender = gender;
    }
}
