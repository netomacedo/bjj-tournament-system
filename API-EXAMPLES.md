# API Testing Guide

This guide provides comprehensive examples for testing all API endpoints.

## Setup

Base URL: `http://localhost:8080`

For testing, you can use:
- cURL (command line)
- Postman
- Any REST client

## 1. Register Athletes

### Register Adult Male Athlete
```bash
curl -X POST http://localhost:8080/api/athletes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Carlos Gracie Jr",
    "dateOfBirth": "1990-05-20",
    "gender": "MALE",
    "beltRank": "BLUE",
    "weight": 82.0,
    "team": "Gracie Barra",
    "coachName": "Master Pedro",
    "email": "carlos@email.com",
    "phone": "+15551234567",
    "experienceNotes": "3 years training, competed in 5 tournaments"
  }'
```

### Register Female Athlete
```bash
curl -X POST http://localhost:8080/api/athletes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ana Silva",
    "dateOfBirth": "1995-08-15",
    "gender": "FEMALE",
    "beltRank": "PURPLE",
    "weight": 64.0,
    "team": "Alliance",
    "coachName": "Coach Maria",
    "email": "ana@email.com",
    "phone": "+15559876543"
  }'
```

### Register Kid Under 10 (No Gender Required)
```bash
curl -X POST http://localhost:8080/api/athletes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Little Renzo",
    "dateOfBirth": "2017-03-10",
    "beltRank": "GREY",
    "weight": 30.0,
    "team": "Kids BJJ Academy",
    "coachName": "Coach Junior",
    "email": "parent@email.com",
    "phone": "+15551112222",
    "experienceNotes": "Training for 1 year, very enthusiastic"
  }'
```

## 2. Create Tournament

```bash
curl -X POST http://localhost:8080/api/tournaments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Spring BJJ Open 2025",
    "description": "Annual spring championship featuring all belt levels",
    "location": "Downtown Sports Center, Main Arena",
    "tournamentDate": "2025-06-15",
    "registrationDeadline": "2025-06-01",
    "organizer": "State BJJ Federation",
    "contactEmail": "contact@bjjopen.com",
    "rules": "IBJJF rules apply. GI required. 5-minute matches for adults."
  }'
```

## 3. Query Athletes

### Get All Athletes
```bash
curl http://localhost:8080/api/athletes
```

### Get Athletes by Belt Rank
```bash
curl http://localhost:8080/api/athletes/belt/BLUE
```

### Get Athletes by Age Range (18-29 years)
```bash
curl "http://localhost:8080/api/athletes/age?min=18&max=29"
```

### Search Athletes by Name
```bash
curl "http://localhost:8080/api/athletes/search?name=Carlos"
```

### Get Athletes by Team
```bash
curl http://localhost:8080/api/athletes/team/Gracie%20Barra
```

## 4. Tournament Management

### Get All Tournaments
```bash
curl http://localhost:8080/api/tournaments
```

### Get Upcoming Tournaments
```bash
curl http://localhost:8080/api/tournaments/upcoming
```

### Get Specific Tournament
```bash
curl http://localhost:8080/api/tournaments/1
```

### Start Tournament
```bash
curl -X POST http://localhost:8080/api/tournaments/1/start
```

### Close Registration
```bash
curl -X POST http://localhost:8080/api/tournaments/1/close-registration
```

## 5. Generate Matches

### Automatic Match Generation
```bash
curl -X POST http://localhost:8080/api/tournaments/divisions/1/generate-matches
```

### Manual Match Generation by Coach
```bash
curl -X POST http://localhost:8080/api/tournaments/divisions/1/generate-matches-manual \
  -H "Content-Type: application/json" \
  -d '[
    [1, 2],
    [3, 4],
    [5, 6]
  ]'
```
*Note: Replace numbers with actual athlete IDs who should compete against each other*

## 6. Match Management

### Get All Matches in a Division
```bash
curl http://localhost:8080/api/matches/division/1
```

### Get Matches by Round
```bash
curl http://localhost:8080/api/matches/division/1/round/1
```

### Get Pending Matches
```bash
curl http://localhost:8080/api/matches/division/1/pending
```

### Get Athlete's Matches
```bash
curl http://localhost:8080/api/matches/athlete/1
```

### Start a Match
```bash
curl -X POST http://localhost:8080/api/matches/1/start
```

### Update Match Scores (During Match)
```bash
curl -X PUT http://localhost:8080/api/matches/1 \
  -H "Content-Type: application/json" \
  -d '{
    "athlete1Points": 4,
    "athlete2Points": 2,
    "athlete1Advantages": 1,
    "athlete2Advantages": 0,
    "athlete1Penalties": 0,
    "athlete2Penalties": 1,
    "status": "IN_PROGRESS"
  }'
```

### Complete Match with Final Scores
```bash
curl -X PUT http://localhost:8080/api/matches/1 \
  -H "Content-Type: application/json" \
  -d '{
    "athlete1Points": 7,
    "athlete2Points": 2,
    "athlete1Advantages": 2,
    "athlete2Advantages": 0,
    "athlete1Penalties": 0,
    "athlete2Penalties": 0,
    "status": "COMPLETED",
    "winnerId": 1,
    "notes": "Dominant performance by athlete 1"
  }'
```

### Record Submission Victory
```bash
curl -X POST "http://localhost:8080/api/matches/1/submission?winnerId=1&submissionType=Triangle%20Choke"
```

### Record Walkover
```bash
curl -X POST "http://localhost:8080/api/matches/1/walkover?winnerId=1"
```

### Assign Match to Mat/Ring
```bash
curl -X POST "http://localhost:8080/api/matches/1/assign-mat?matNumber=3"
```

## 7. Update Athlete Information

```bash
curl -X PUT http://localhost:8080/api/athletes/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Carlos Gracie Jr",
    "weight": 83.5,
    "phone": "+15551234999",
    "experienceNotes": "Updated: 4 years training, competed in 8 tournaments"
  }'
```

## 8. Complete Workflow Example

Here's a complete workflow from registration to match completion:

```bash
# 1. Create a tournament
TOURNAMENT_ID=$(curl -X POST http://localhost:8080/api/tournaments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Tournament",
    "location": "Test Arena",
    "tournamentDate": "2025-12-01",
    "organizer": "Test Org"
  }' | jq -r '.id')

echo "Created tournament ID: $TOURNAMENT_ID"

# 2. Register athletes
ATHLETE1_ID=$(curl -X POST http://localhost:8080/api/athletes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Athlete One",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "beltRank": "BLUE",
    "weight": 75.0,
    "email": "athlete1@test.com"
  }' | jq -r '.id')

ATHLETE2_ID=$(curl -X POST http://localhost:8080/api/athletes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Athlete Two",
    "dateOfBirth": "1992-01-01",
    "gender": "MALE",
    "beltRank": "BLUE",
    "weight": 74.0,
    "email": "athlete2@test.com"
  }' | jq -r '.id')

echo "Registered athletes: $ATHLETE1_ID, $ATHLETE2_ID"

# 3. Generate matches (assuming division 1 exists with these athletes)
# Note: In reality, you'd need to assign athletes to divisions first

# 4. Start a match
curl -X POST http://localhost:8080/api/matches/1/start

# 5. Update match scores during competition
curl -X PUT http://localhost:8080/api/matches/1 \
  -H "Content-Type: application/json" \
  -d '{
    "athlete1Points": 5,
    "athlete2Points": 2,
    "status": "IN_PROGRESS"
  }'

# 6. Complete the match
curl -X PUT http://localhost:8080/api/matches/1 \
  -H "Content-Type: application/json" \
  -d '{
    "athlete1Points": 7,
    "athlete2Points": 2,
    "status": "COMPLETED",
    "winnerId": 1
  }'
```

## Testing Tips

1. **Use Postman Collections**: Import these curl commands into Postman for easier testing
2. **Save IDs**: Store created IDs (tournaments, athletes, matches) for subsequent tests
3. **Check Logs**: Monitor application logs for detailed error messages
4. **Database Access**: Use a PostgreSQL client to inspect data directly
5. **Valid Data**: Ensure dates are in the future for tournaments, ages are valid, etc.

## Common Scenarios

### Scenario 1: Kids Tournament (Under 10)
- Register kids without gender (it's set to NOT_APPLICABLE automatically)
- All kids in same belt rank can compete together
- Match duration is shorter (3-4 minutes)

### Scenario 2: Adult Tournament with Weight Classes
- Register adults with accurate weight
- System automatically assigns weight classes
- Separate divisions for males and females

### Scenario 3: Coach Manual Bracket
- Coach reviews registered athletes
- Creates custom matchups based on experience
- Uses manual match generation endpoint

### Scenario 4: Multi-Mat Tournament
- Assign matches to different mats (1, 2, 3, etc.)
- Track multiple matches simultaneously
- Coordinate athlete rest times

## Error Handling

The API returns standard HTTP status codes:
- `200 OK`: Successful GET/PUT requests
- `201 Created`: Successful POST requests
- `204 No Content`: Successful DELETE requests
- `400 Bad Request`: Invalid input data
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

Error response format:
```json
{
  "timestamp": "2025-06-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Athlete with email already exists",
  "path": "/api/athletes"
}
```

## Next Steps

After testing the backend API:
1. Build the React frontend to consume these endpoints
2. Implement WebSocket for real-time updates
3. Add authentication and authorization
4. Deploy to production environment
