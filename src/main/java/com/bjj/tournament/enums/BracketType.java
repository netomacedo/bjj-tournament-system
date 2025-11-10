package com.bjj.tournament.enums;

/**
 * Types of bracket/tournament formats
 */
public enum BracketType {
    SINGLE_ELIMINATION("Single Elimination"),     // Traditional knockout format
    DOUBLE_ELIMINATION("Double Elimination"),     // Losers bracket format
    ROUND_ROBIN("Round Robin");                   // Everyone fights everyone
    
    private final String displayName;
    
    BracketType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
