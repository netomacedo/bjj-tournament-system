package com.bjj.tournament.enums;

/**
 * Status of a match in the tournament
 */
public enum MatchStatus {
    PENDING("Pending"),           // Match is scheduled but not started
    IN_PROGRESS("In Progress"),   // Match is currently being fought
    COMPLETED("Completed"),       // Match is finished
    WALKOVER("Walkover"),         // One competitor didn't show up
    CANCELLED("Cancelled");       // Match was cancelled
    
    private final String displayName;
    
    MatchStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
