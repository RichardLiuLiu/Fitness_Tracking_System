package demo.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class MedicalInfo {
    private long medicalInfoId;
    private String bandMake;
    private String medCode;
    private String medicalInfoClassification;
    private String description;
    private String aidInstructions;
    private String fmi;
    private String bfr;

}
