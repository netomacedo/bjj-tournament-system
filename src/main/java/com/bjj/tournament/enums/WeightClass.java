package com.bjj.tournament.enums;

/**
 * Weight classes based on IBJJF weight divisions (in kg)
 * Different weight classes for Adult Male, Adult Female, and Kids
 */
public enum WeightClass {
    // Adult Male weight classes (with gi)
    ADULT_MALE_ROOSTER("Rooster", 57.5, Gender.MALE, true),
    ADULT_MALE_LIGHT_FEATHER("Light Feather", 64.0, Gender.MALE, true),
    ADULT_MALE_FEATHER("Feather", 70.0, Gender.MALE, true),
    ADULT_MALE_LIGHT("Light", 76.0, Gender.MALE, true),
    ADULT_MALE_MIDDLE("Middle", 82.3, Gender.MALE, true),
    ADULT_MALE_MEDIUM_HEAVY("Medium Heavy", 88.3, Gender.MALE, true),
    ADULT_MALE_HEAVY("Heavy", 94.3, Gender.MALE, true),
    ADULT_MALE_SUPER_HEAVY("Super Heavy", 100.5, Gender.MALE, true),
    ADULT_MALE_ULTRA_HEAVY("Ultra Heavy", 999.9, Gender.MALE, true), // No upper limit
    
    // Adult Female weight classes (with gi)
    ADULT_FEMALE_ROOSTER("Rooster", 48.5, Gender.FEMALE, true),
    ADULT_FEMALE_LIGHT_FEATHER("Light Feather", 53.5, Gender.FEMALE, true),
    ADULT_FEMALE_FEATHER("Feather", 58.5, Gender.FEMALE, true),
    ADULT_FEMALE_LIGHT("Light", 64.0, Gender.FEMALE, true),
    ADULT_FEMALE_MIDDLE("Middle", 69.0, Gender.FEMALE, true),
    ADULT_FEMALE_MEDIUM_HEAVY("Medium Heavy", 74.0, Gender.FEMALE, true),
    ADULT_FEMALE_HEAVY("Heavy", 79.3, Gender.FEMALE, true),
    ADULT_FEMALE_SUPER_HEAVY("Super Heavy", 999.9, Gender.FEMALE, true), // No upper limit
    
    // Kids weight classes (simplified)
    KIDS_LIGHT("Light", 30.0, Gender.NOT_APPLICABLE, false),
    KIDS_MIDDLE("Middle", 37.0, Gender.NOT_APPLICABLE, false),
    KIDS_MEDIUM_HEAVY("Medium Heavy", 44.0, Gender.NOT_APPLICABLE, false),
    KIDS_HEAVY("Heavy", 52.0, Gender.NOT_APPLICABLE, false),
    KIDS_SUPER_HEAVY("Super Heavy", 999.9, Gender.NOT_APPLICABLE, false);
    
    private final String displayName;
    private final double maxWeightKg;
    private final Gender applicableGender;
    private final boolean isAdult;
    
    WeightClass(String displayName, double maxWeightKg, Gender applicableGender, boolean isAdult) {
        this.displayName = displayName;
        this.maxWeightKg = maxWeightKg;
        this.applicableGender = applicableGender;
        this.isAdult = isAdult;
    }
    
    /**
     * Find appropriate weight class for an athlete based on weight, gender, and age
     */
    public static WeightClass findWeightClass(double weight, Gender gender, boolean isAdult) {
        for (WeightClass wc : values()) {
            if (wc.isAdult == isAdult && 
                (wc.applicableGender == gender || wc.applicableGender == Gender.NOT_APPLICABLE) &&
                weight <= wc.maxWeightKg) {
                return wc;
            }
        }
        // Return heaviest class if no match found
        return isAdult ? 
               (gender == Gender.MALE ? ADULT_MALE_ULTRA_HEAVY : ADULT_FEMALE_SUPER_HEAVY) :
               KIDS_SUPER_HEAVY;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getMaxWeightKg() {
        return maxWeightKg;
    }
    
    public Gender getApplicableGender() {
        return applicableGender;
    }
    
    public boolean isAdult() {
        return isAdult;
    }
}
