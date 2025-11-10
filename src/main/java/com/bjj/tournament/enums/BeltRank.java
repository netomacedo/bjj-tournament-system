package com.bjj.tournament.enums;

/**
 * Belt ranking system based on IBJJF (International Brazilian Jiu-Jitsu Federation)
 * This enum represents all belt ranks from beginner to master level
 */
public enum BeltRank {
    // Kids belts (under 16)
    WHITE_GREY("White-Grey", 0, true),
    GREY("Grey", 1, true),
    GREY_BLACK("Grey-Black", 2, true),
    YELLOW_WHITE("Yellow-White", 3, true),
    YELLOW("Yellow", 4, true),
    YELLOW_BLACK("Yellow-Black", 5, true),
    ORANGE_WHITE("Orange-White", 6, true),
    ORANGE("Orange", 7, true),
    ORANGE_BLACK("Orange-Black", 8, true),
    GREEN_WHITE("Green-White", 9, true),
    GREEN("Green", 10, true),
    GREEN_BLACK("Green-Black", 11, true),
    
    // Adult belts (16+)
    WHITE("White", 12, false),
    BLUE("Blue", 13, false),
    PURPLE("Purple", 14, false),
    BROWN("Brown", 15, false),
    BLACK("Black", 16, false),
    
    // Master belts (Black belt with coral bar)
    CORAL("Coral", 17, false), // Red and Black belt (7th/8th degree)
    RED("Red", 18, false);      // Red belt (9th/10th degree)
    
    private final String displayName;
    private final int rank;
    private final boolean isKidsBelt;
    
    BeltRank(String displayName, int rank, boolean isKidsBelt) {
        this.displayName = displayName;
        this.rank = rank;
        this.isKidsBelt = isKidsBelt;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getRank() {
        return rank;
    }
    
    public boolean isKidsBelt() {
        return isKidsBelt;
    }
}
