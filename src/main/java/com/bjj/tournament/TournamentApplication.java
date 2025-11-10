package com.bjj.tournament;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application for BJJ Tournament System
 * 
 * This application provides a comprehensive tournament management system
 * based on IBJJF (International Brazilian Jiu-Jitsu Federation) rules.
 * 
 * Features:
 * - Athlete registration with belt rank, age, gender, and weight
 * - Automatic division creation based on IBJJF categories
 * - Match generation (automatic or manual by coach)
 * - Real-time match scoring and bracket management
 * - Support for different bracket types (single/double elimination, round robin)
 * 
 * @author BJJ Tournament Team
 * @version 1.0.0
 */
@SpringBootApplication
public class TournamentApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TournamentApplication.class, args);
    }
}
