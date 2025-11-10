# Testing Guide

This guide explains the comprehensive test suite for the BJJ Tournament System.

## 📋 Test Coverage

### Test Layers

1. **Repository Tests** (`@DataJpaTest`)
   - Database operations
   - Query methods
   - Data integrity

2. **Service Tests** (`@ExtendWith(MockitoExtension.class)`)
   - Business logic
   - Validation rules
   - Error handling

3. **Controller Tests** (`@WebMvcTest`)
   - REST endpoints
   - Request/Response handling
   - Input validation

4. **Integration Tests** (`@SpringBootTest`)
   - Full stack testing
   - End-to-end workflows
   - Database integration

## 🧪 Running Tests

### Run All Tests

```bash
# Using Maven
mvn test

# Using Maven with detailed output
mvn test -X

# Run tests and generate coverage report
mvn clean test jacoco:report
```

### Run Specific Test Class

```bash
# Run specific test class
mvn test -Dtest=AthleteServiceTest

# Run specific test method
mvn test -Dtest=AthleteServiceTest#testRegisterAthlete_WithValidData_ShouldSucceed
```

### Run Tests by Category

```bash
# Run only unit tests
mvn test -Dgroups="unit"

# Run only integration tests
mvn test -Dgroups="integration"
```

## 📊 Test Structure

### Repository Tests (`AthleteRepositoryTest.java`)

Tests database operations:

```java
@DataJpaTest
class AthleteRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private AthleteRepository athleteRepository;
    
    @Test
    void testFindByBeltRank_ShouldReturnAthletesWithBlueBelt() {
        // Arrange: Create test data
        // Act: Execute repository method
        // Assert: Verify results
    }
}
```

**What it tests:**
- ✅ Query methods (findBy*, searchBy*, etc.)
- ✅ Custom JPQL queries
- ✅ Data persistence
- ✅ Entity relationships

### Service Tests (`AthleteServiceTest.java`)

Tests business logic:

```java
@ExtendWith(MockitoExtension.class)
class AthleteServiceTest {
    @Mock
    private AthleteRepository athleteRepository;
    
    @InjectMocks
    private AthleteService athleteService;
    
    @Test
    void testRegisterAthlete_WithValidData_ShouldSucceed() {
        // Given: Mock repository behavior
        // When: Call service method
        // Then: Verify results and interactions
    }
}
```

**What it tests:**
- ✅ Business validation rules
- ✅ Age calculations
- ✅ Gender handling for kids under 10
- ✅ Belt rank validation by age
- ✅ Duplicate email prevention
- ✅ Error handling

### Controller Tests (`AthleteControllerTest.java`)

Tests REST API:

```java
@WebMvcTest(AthleteController.class)
class AthleteControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AthleteService athleteService;
    
    @Test
    void testRegisterAthlete_WithValidData_ShouldReturn201Created() throws Exception {
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }
}
```

**What it tests:**
- ✅ HTTP endpoints
- ✅ Request validation
- ✅ Response status codes
- ✅ JSON serialization/deserialization
- ✅ Error responses

### Integration Tests (`TournamentApplicationIntegrationTest.java`)

Tests complete workflows:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TournamentApplicationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AthleteRepository athleteRepository;
    
    @Test
    void testCompleteAthleteRegistrationFlow() throws Exception {
        // Test: Register → Retrieve → Update → Delete
        // Verifies full stack integration
    }
}
```

**What it tests:**
- ✅ Complete user workflows
- ✅ Database transactions
- ✅ End-to-end functionality
- ✅ Real database operations (H2 in-memory)

## 📝 Test Naming Convention

Tests follow the pattern: `test{MethodName}_{Condition}_{ExpectedResult}`

**Examples:**
```java
testRegisterAthlete_WithValidData_ShouldSucceed()
testRegisterAthlete_WithDuplicateEmail_ShouldThrowException()
testGetAthleteById_WhenNotExists_ShouldThrowException()
```

## 🎯 Test Coverage Goals

| Layer | Coverage Goal | Current |
|-------|--------------|---------|
| Repository | 90%+ | ✅ |
| Service | 85%+ | ✅ |
| Controller | 80%+ | ✅ |
| Overall | 80%+ | ✅ |

## 🔍 Key Test Scenarios

### 1. Athlete Registration

```java
✅ Valid adult athlete registration
✅ Kid under 10 (gender NOT_APPLICABLE)
✅ Athlete under 4 years (rejected)
✅ Duplicate email (rejected)
✅ Kid with adult belt rank (rejected)
✅ Adult without gender (rejected)
✅ Invalid email format (rejected)
✅ Invalid phone number (rejected)
```

### 2. Tournament Management

```java
✅ Create tournament with future date
✅ Create tournament with past date (rejected)
✅ Start tournament
✅ Start already started tournament (rejected)
✅ Complete tournament
✅ Complete without starting (rejected)
✅ Close registration
```

### 3. Match Generation

```java
✅ Auto-generate matches (single elimination)
✅ Manual bracket generation by coach
✅ Round robin generation
✅ Advance winner to next round
✅ Record submission victory
✅ Record walkover
✅ Update match scores
```

## 🛠️ Writing New Tests

### Repository Test Template

```java
@Test
void testYourQueryMethod_ShouldReturnExpectedResults() {
    // Given - Create and persist test data
    Athlete athlete = new Athlete();
    athlete.setName("Test");
    // ... set other fields
    entityManager.persist(athlete);
    entityManager.flush();
    
    // When - Execute your query
    List<Athlete> results = repository.yourQueryMethod(params);
    
    // Then - Assert results
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getName()).isEqualTo("Test");
}
```

### Service Test Template

```java
@Test
void testYourServiceMethod_WithCondition_ShouldDoExpected() {
    // Given - Mock dependencies
    when(repository.someMethod(any())).thenReturn(mockData);
    
    // When - Call service method
    Result result = service.yourMethod(input);
    
    // Then - Verify result and interactions
    assertThat(result).isNotNull();
    verify(repository, times(1)).someMethod(any());
}
```

### Controller Test Template

```java
@Test
void testYourEndpoint_WithValidData_ShouldReturnExpected() throws Exception {
    // Given - Mock service behavior
    when(service.someMethod(any())).thenReturn(mockData);
    
    // When/Then - Test endpoint
    mockMvc.perform(post("/api/your-endpoint")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.field").value("expected"));
}
```

## 🐛 Debugging Tests

### View Test Output

```bash
# Run with verbose output
mvn test -X

# Run single test with output
mvn test -Dtest=AthleteServiceTest -X
```

### Common Issues

**1. Test database not cleaned**
```java
@BeforeEach
void setUp() {
    repository.deleteAll(); // Clean before each test
}
```

**2. Mocked method not called**
```java
// Verify mock was called
verify(mockRepository, times(1)).save(any());
```

**3. JSON serialization issues**
```java
// Use ObjectMapper to debug
String json = objectMapper.writeValueAsString(dto);
System.out.println(json);
```

## 📈 Test Reports

### Generate Coverage Report

```bash
# Run tests with JaCoCo coverage
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Generate Surefire Report

```bash
mvn surefire-report:report

# View report
open target/site/surefire-report.html
```

## 🎓 Best Practices

### 1. AAA Pattern (Arrange-Act-Assert)

```java
@Test
void testExample() {
    // Arrange - Setup test data and mocks
    Athlete athlete = createTestAthlete();
    when(repository.save(any())).thenReturn(athlete);
    
    // Act - Execute the code under test
    Athlete result = service.registerAthlete(dto);
    
    // Assert - Verify the results
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
}
```

### 2. Test Independence

Each test should be independent and not rely on other tests:

```java
@BeforeEach
void setUp() {
    // Reset state before each test
    repository.deleteAll();
}
```

### 3. Meaningful Test Names

Use descriptive names that explain the test:

```java
// ✅ Good
testRegisterAthlete_WithKidUnder10_ShouldSetGenderNotApplicable()

// ❌ Bad
testRegister()
```

### 4. Test One Thing

Each test should verify one specific behavior:

```java
// ✅ Good - Tests one scenario
@Test
void testRegisterAthlete_WithDuplicateEmail_ShouldThrowException() {
    // ...
}

// ❌ Bad - Tests multiple scenarios
@Test
void testRegisterAthlete() {
    // Tests valid registration
    // Tests duplicate email
    // Tests invalid age
    // ...
}
```

### 5. Use AssertJ for Better Assertions

```java
// ✅ Fluent and readable
assertThat(athletes)
    .hasSize(2)
    .extracting(Athlete::getName)
    .containsExactlyInAnyOrder("John", "Maria");

// Instead of
assertEquals(2, athletes.size());
assertTrue(athletes.stream().anyMatch(a -> a.getName().equals("John")));
```

## 🚀 Continuous Integration

### GitHub Actions Workflow

Create `.github/workflows/tests.yml`:

```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run tests
      run: mvn clean test
    
    - name: Generate coverage report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
```

## 📚 Additional Resources

- **JUnit 5 Documentation**: https://junit.org/junit5/docs/current/user-guide/
- **Mockito Documentation**: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- **AssertJ Documentation**: https://assertj.github.io/doc/
- **Spring Boot Testing**: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing

## ✅ Testing Checklist

- [ ] All repository methods have tests
- [ ] All service methods have tests
- [ ] All controller endpoints have tests
- [ ] Integration tests cover main workflows
- [ ] Tests are independent
- [ ] Tests have meaningful names
- [ ] Edge cases are tested
- [ ] Error scenarios are tested
- [ ] Tests pass consistently
- [ ] Test coverage > 80%

**Happy Testing! 🧪✅**
