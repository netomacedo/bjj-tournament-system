package com.bjj.tournament.enums;

/**
 * Gender categories for competition divisions
 * Note: For kids under 10, gender is not required (mixed divisions)
 */
public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    NOT_APPLICABLE("N/A"); // For kids under 10 years old
    
    private final String displayName;
    
    Gender(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
