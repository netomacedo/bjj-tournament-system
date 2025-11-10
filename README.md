# BJJ Tournament Management System

A comprehensive Brazilian Jiu-Jitsu tournament management system based on IBJJF (International Brazilian Jiu-Jitsu Federation) rules. Built with Java Spring Boot backend and designed to integrate with React frontend.

## 🥋 Features

### Core Functionality
- **Athlete Registration**: Complete registration system with validation for belt rank, age, gender, and weight
- **Automatic Division Creation**: Groups athletes by belt, age category, gender, and weight class
- **Match Generation**: 
  - **Automatic**: Algorithm-based fair bracket generation
  - **Manual**: Coach-driven matchups based on experience and skill level
- **Real-time Scoring**: IBJJF-compliant scoring system (points, advantages, penalties)
- **Bracket Management**: Visual bracket tree with drag-and-drop athlete movement
- **Multiple Bracket Types**: Single elimination, double elimination, and round robin

### IBJJF Compliance
- ✅ Complete belt ranking system (White-Grey through Red belt)
- ✅ Age categories (Mighty Mite to Master 7)
- ✅ Gender separation (with special handling for kids under 10)
- ✅ Weight classes for adults and kids
- ✅ Match duration based on age and belt
- ✅ Scoring system (points, advantages, penalties)
- ✅ Submission tracking

## 🏗️ Architecture

```
bjj-tournament/
├── src/
│   └── main/
│       ├── java/com/bjj/tournament/
│       │   ├── entity/          # JPA entities (Athlete, Tournament, Match, etc.)
│       │   ├── dto/             # Data Transfer Objects for API requests/responses
│       │   ├── repository/      # Spring Data JPA repositories
│       │   ├── service/         # Business logic layer
│       │   ├── controller/      # REST API endpoints
│       │   ├── enums/           # Belt ranks, age categories, match status, etc.
│       │   └── TournamentApplication.java
│       └── resources/
│           └── application.properties
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (recommended)
- PostgreSQL 15+ (if not using Docker)

### Option 1: Run with Docker (Recommended)

1. **Clone the repository**:
```bash
git clone <repository-url>
cd bjj-tournament
```

2. **Start the application**:
```bash
docker-compose up -d
```

This will start:
- PostgreSQL database on port 5432
- Spring Boot application on port 8080

3. **Check application status**:
```bash
docker-compose ps
```

4. **View logs**:
```bash
docker-compose logs -f app
```

5. **Stop the application**:
```bash
docker-compose down
```

### Option 2: Run Locally

1. **Setup PostgreSQL database**:
```sql
CREATE DATABASE bjj_tournament;
CREATE USER bjj_user WITH PASSWORD 'bjj_password';
GRANT ALL PRIVILEGES ON DATABASE bjj_tournament TO bjj_user;
```

2. **Configure application**:
Update `src/main/resources/application.properties` with your database credentials.

3. **Build and run**:
```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📡 API Endpoints

### Athletes

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/athletes` | Register new athlete |
| GET | `/api/athletes` | Get all athletes |
| GET | `/api/athletes/{id}` | Get athlete by ID |
| GET | `/api/athletes/belt/{beltRank}` | Get athletes by belt rank |
| GET | `/api/athletes/age?min=18&max=29` | Get athletes by age range |
| GET | `/api/athletes/search?name=John` | Search athletes by name |
| PUT | `/api/athletes/{id}` | Update athlete |
| DELETE | `/api/athletes/{id}` | Delete athlete |

### Tournaments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tournaments` | Create tournament |
| GET | `/api/tournaments` | Get all tournaments |
| GET | `/api/tournaments/{id}` | Get tournament by ID |
| GET | `/api/tournaments/upcoming` | Get upcoming tournaments |
| POST | `/api/tournaments/{id}/start` | Start tournament |
| POST | `/api/tournaments/{id}/close-registration` | Close registration |
| POST | `/api/tournaments/{id}/complete` | Complete tournament |
| POST | `/api/tournaments/divisions/{divisionId}/generate-matches` | Auto-generate matches |
| POST | `/api/tournaments/divisions/{divisionId}/generate-matches-manual` | Manual match generation |

### Matches

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/matches/{id}` | Get match by ID |
| GET | `/api/matches/division/{divisionId}` | Get all matches in division |
| GET | `/api/matches/division/{divisionId}/round/{roundNumber}` | Get matches by round |
| GET | `/api/matches/athlete/{athleteId}` | Get athlete's matches |
| POST | `/api/matches/{id}/start` | Start match |
| PUT | `/api/matches/{id}` | Update match scores |
| POST | `/api/matches/{id}/submission` | Record submission |
| POST | `/api/matches/{id}/walkover` | Record walkover |
| POST | `/api/matches/{id}/assign-mat` | Assign to mat/ring |

## 💡 BJJ Tournament System Ideas

### 1. **Weight Class Management**
- **Dynamic weight classes**: Automatically adjust based on registered athletes
- **Absolute division**: Open weight category for champions
- **Weight-in scheduling**: Automated weight-in time slots

### 2. **Seeding System**
- **Ranking-based seeding**: Use previous tournament results
- **Team distribution**: Ensure teammates don't meet early
- **Regional seeding**: Balance competitors from different regions

### 3. **Match Scheduling**
- **Multi-mat coordination**: Parallel matches on multiple mats
- **Rest time calculation**: Ensure adequate rest between matches
- **Conflict detection**: Prevent athletes from double-booking

### 4. **Scoring Enhancements**
- **Video replay integration**: Link match videos to results
- **Statistical tracking**: Track submission types, average match duration
- **Performance metrics**: Points per match, submission rate, etc.

### 5. **Coach & Team Features**
- **Team standings**: Aggregate team scores
- **Coach dashboard**: View all team athletes and their matches
- **Team registration**: Bulk athlete registration

### 6. **Live Streaming & Spectator Features**
- **Live brackets**: Real-time bracket updates
- **Match notifications**: Alert athletes of upcoming matches
- **Spectator mode**: Public view of brackets and results

### 7. **Awards & Recognition**
- **Medal tracking**: Gold, silver, bronze medals
- **Tournament champions**: Overall winners by division
- **Special awards**: Best submission, most technical match, etc.

### 8. **Advanced Bracket Types**
- **Round Robin Pools**: Groups before elimination
- **Repechage**: Consolation bracket for bronze medal
- **King of the Mat**: Continuous challenge format

### 9. **Rules Engine**
- **Custom scoring rules**: Adapt for different organizations (IBJJF, ADCC, Grappling Industries)
- **Time rules**: Different durations for GI vs NO-GI
- **Point multipliers**: Special rules for kids divisions

### 10. **Registration Features**
- **Online payment**: Integrated payment processing
- **Early bird pricing**: Discount for early registration
- **Team discounts**: Bulk registration discounts
- **Waiver management**: Digital liability waivers

### 11. **Communication System**
- **SMS notifications**: Match schedule updates
- **Email confirmations**: Registration and bracket assignments
- **In-app messaging**: Coach-to-athlete communication

### 12. **Analytics & Reporting**
- **Tournament statistics**: Participation trends
- **Division analysis**: Most popular divisions
- **Financial reports**: Revenue and expense tracking
- **Athlete history**: Performance across multiple tournaments

## 🎯 Implementation Roadmap

### Phase 1: Core System (Current)
- ✅ Athlete registration
- ✅ Tournament creation
- ✅ Division management
- ✅ Match generation (auto & manual)
- ✅ IBJJF scoring system
- ✅ Bracket advancement

### Phase 2: Enhanced Features (Next)
- [ ] React frontend with drag-and-drop brackets
- [ ] Real-time match updates via WebSocket
- [ ] Multi-mat coordination
- [ ] Team management
- [ ] Automated weight class assignment

### Phase 3: Advanced Features (Future)
- [ ] Mobile app for athletes and coaches
- [ ] Live streaming integration
- [ ] Payment processing
- [ ] Advanced analytics dashboard
- [ ] Multi-organization support

## 🌐 Deployment Options

### Free Hosting Options

1. **Render.com** (Recommended)
   - Free PostgreSQL database
   - Free web service (spins down after inactivity)
   - Easy Docker deployment

2. **Railway.app**
   - $5/month credit (enough for small tournaments)
   - PostgreSQL included
   - Simple Git-based deployment

3. **Fly.io**
   - Free tier includes PostgreSQL
   - Excellent for Docker containers
   - Global edge deployment

4. **Heroku**
   - Free tier with limitations
   - Easy PostgreSQL addon
   - Simple deployment via Git

### Deployment Steps (Render.com Example)

1. **Create account** at render.com

2. **Create PostgreSQL database**:
   - Name: `bjj-tournament-db`
   - Note the connection string

3. **Create Web Service**:
   - Choose "Docker"
   - Connect your Git repository
   - Set environment variables:
     ```
     SPRING_DATASOURCE_URL=<your-postgres-url>
     SPRING_DATASOURCE_USERNAME=<username>
     SPRING_DATASOURCE_PASSWORD=<password>
     ```

4. **Deploy**: Render will automatically build and deploy

## 🧪 Testing

### Sample Athlete Registration
```bash
curl -X POST http://localhost:8080/api/athletes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Silva",
    "dateOfBirth": "1995-06-15",
    "gender": "MALE",
    "beltRank": "BLUE",
    "weight": 75.5,
    "team": "Gracie Barra",
    "coachName": "Master Carlos",
    "email": "john.silva@email.com",
    "phone": "+1234567890"
  }'
```

### Sample Tournament Creation
```bash
curl -X POST http://localhost:8080/api/tournaments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Summer BJJ Championship 2025",
    "description": "Annual summer tournament",
    "location": "Sports Arena, City Center",
    "tournamentDate": "2025-07-15",
    "registrationDeadline": "2025-07-01",
    "organizer": "Local BJJ Federation",
    "contactEmail": "info@bjjfed.com"
  }'
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests.

## 📝 License

This project is open source and available under the MIT License.

## 👨‍💻 Authors

BJJ Tournament Team

## 🙏 Acknowledgments

- IBJJF for the comprehensive ruleset
- Brazilian Jiu-Jitsu community
- Spring Boot and PostgreSQL communities

---

**Ready to revolutionize BJJ tournament management! 🥋🏆**

For questions or support, please open an issue on GitHub.
