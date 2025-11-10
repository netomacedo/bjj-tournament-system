package com.bjj.tournament.enums;

/**
 * Age categories based on IBJJF competition divisions
 * Each category has specific age ranges and match durations
 */
public enum AgeCategory {
    // Kids categories
    MIGHTY_MITE("Mighty Mite", 4, 5, 3),      // 4-5 years, 3 min matches
    TINY_TOT("Tiny Tot", 6, 7, 3),            // 6-7 years, 3 min matches
    WEE_ONE("Wee One", 8, 9, 4),              // 8-9 years, 4 min matches
    LITTLE_ONE("Little One", 10, 12, 4),      // 10-12 years, 4 min matches
    PRE_TEEN("Pre-Teen", 13, 15, 5),          // 13-15 years, 5 min matches
    
    // Juvenile and Adult categories
    JUVENILE("Juvenile", 16, 17, 5),          // 16-17 years, 5 min matches
    ADULT("Adult", 18, 29, 5),                // 18-29 years, 5 min matches (6-10 for black belt)
    MASTER_1("Master 1", 30, 35, 5),          // 30-35 years
    MASTER_2("Master 2", 36, 40, 5),          // 36-40 years
    MASTER_3("Master 3", 41, 45, 5),          // 41-45 years
    MASTER_4("Master 4", 46, 50, 5),          // 46-50 years
    MASTER_5("Master 5", 51, 55, 5),          // 51-55 years
    MASTER_6("Master 6", 56, 60, 4),          // 56-60 years
    MASTER_7("Master 7", 61, 150, 4);         // 61+ years
    
    private final String displayName;
    private final int minAge;
    private final int maxAge;
    private final int matchDurationMinutes;
    
    AgeCategory(String displayName, int minAge, int maxAge, int matchDurationMinutes) {
        this.displayName = displayName;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.matchDurationMinutes = matchDurationMinutes;
    }
    
    /**
     * Determine if gender separation is required for this age category
     * Kids under 10 don't require gender separation
     */
    public boolean requiresGenderSeparation() {
        return minAge >= 10;
    }
    
    /**
     * Find the appropriate age category for a given age
     */
    public static AgeCategory fromAge(int age) {
        for (AgeCategory category : values()) {
            if (age >= category.minAge && age <= category.maxAge) {
                return category;
            }
        }
        throw new IllegalArgumentException("No age category found for age: " + age);
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getMinAge() {
        return minAge;
    }
    
    public int getMaxAge() {
        return maxAge;
    }
    
    public int getMatchDurationMinutes() {
        return matchDurationMinutes;
    }
}
