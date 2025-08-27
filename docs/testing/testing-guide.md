# CampusWorks Testing Guide

## Overview

This document provides comprehensive testing instructions for the CampusWorks platform, including unit tests, integration tests, API testing, and end-to-end testing strategies.

## Testing Strategy

### 1. Unit Testing
- **Framework**: JUnit 5, Mockito
- **Coverage Target**: 80%+ code coverage
- **Scope**: Individual service methods and components

### 2. Integration Testing
- **Framework**: Spring Boot Test, TestContainers
- **Scope**: Service-to-service communication, database interactions

### 3. API Testing
- **Tools**: Postman, REST Assured
- **Scope**: All REST endpoints, authentication flows

### 4. End-to-End Testing
- **Framework**: Selenium, Cucumber
- **Scope**: Complete user workflows

## Running Tests

### Prerequisites
```bash
# Ensure Docker is running for integration tests
docker --version

# Start test databases
docker-compose -f docker-compose.test.yml up -d
```

### Unit Tests
```bash
# Run all unit tests
mvn test

# Run tests for specific service
mvn test -pl auth-service

# Run with coverage report
mvn test jacoco:report
```

### Integration Tests
```bash
# Run integration tests
mvn verify

# Run specific integration test
mvn test -Dtest=AuthServiceIntegrationTest
```

### API Tests with Maven
```bash
# Run API tests using REST Assured
mvn test -Dtest=*ApiTest
```

## Test Data Setup

### Database Test Data
```sql
-- Test users
INSERT INTO users (email, password, first_name, last_name, role, email_verified, created_at) VALUES
('student1@test.com', '$2a$10$encrypted_password', 'John', 'Doe', 'STUDENT', true, NOW()),
('student2@test.com', '$2a$10$encrypted_password', 'Jane', 'Smith', 'STUDENT', true, NOW()),
('admin@test.com', '$2a$10$encrypted_password', 'Admin', 'User', 'ADMIN', true, NOW());

-- Test tasks
INSERT INTO tasks (title, description, owner_id, status, min_budget, max_budget, created_at) VALUES
('Test Task 1', 'Sample task for testing', 1, 'OPEN', 50.00, 100.00, NOW()),
('Test Task 2', 'Another test task', 2, 'DRAFT', 75.00, 150.00, NOW());
```

### Test Environment Configuration
```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
jwt.secret=test-secret-key-for-testing-only
logging.level.com.campusworks=DEBUG
```

## Sample Test Cases

### 1. Auth Service Tests

#### User Registration Test
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @MockBean
    private EmailService emailService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
            .email("test@university.edu")
            .password("Password123!")
            .firstName("Test")
            .lastName("User")
            .universityName("Test University")
            .build();

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getEmail()).isEqualTo("test@university.edu");
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void shouldThrowExceptionForDuplicateEmail() {
        // Given
        RegisterRequest request = createRegisterRequest("existing@test.com");
        authService.register(request); // First registration

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.register(request); // Duplicate registration
        });
    }
}
```

#### JWT Token Test
```java
@Test
void shouldGenerateValidJwtToken() {
    // Given
    User user = createTestUser();
    
    // When
    String token = jwtService.generateToken(user);
    
    // Then
    assertThat(token).isNotNull();
    assertThat(jwtService.isTokenValid(token, user)).isTrue();
    assertThat(jwtService.extractUsername(token)).isEqualTo(user.getEmail());
}
```

### 2. Task Service Tests

#### Task Creation Test
```java
@SpringBootTest
@Transactional
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void shouldCreateTaskSuccessfully() {
        // Given
        CreateTaskRequest request = CreateTaskRequest.builder()
            .title("Test Task")
            .description("Test description")
            .minBudget(new BigDecimal("50.00"))
            .maxBudget(new BigDecimal("100.00"))
            .category("RESEARCH")
            .subject("Computer Science")
            .build();

        Long ownerId = 1L;

        // When
        TaskResponse response = taskService.createTask(request, ownerId);

        // Then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TaskStatus.DRAFT);
        
        Task savedTask = taskRepository.findById(response.getId()).orElse(null);
        assertThat(savedTask).isNotNull();
        assertThat(savedTask.getOwnerId()).isEqualTo(ownerId);
    }
}
```

### 3. Bidding Service Tests

#### Bid Placement Test
```java
@SpringBootTest
@Transactional
class BiddingServiceTest {

    @Autowired
    private BiddingService biddingService;

    @MockBean
    private TaskServiceClient taskServiceClient;

    @Test
    void shouldPlaceBidSuccessfully() {
        // Given
        Long taskId = 1L;
        Long bidderId = 2L;
        PlaceBidRequest request = PlaceBidRequest.builder()
            .taskId(taskId)
            .amount(new BigDecimal("75.00"))
            .proposal("I can complete this task efficiently")
            .build();

        when(taskServiceClient.getTask(taskId)).thenReturn(createOpenTask());

        // When
        BidResponse response = biddingService.placeBid(request, bidderId);

        // Then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("75.00"));
        assertThat(response.getStatus()).isEqualTo(BidStatus.ACTIVE);
    }
}
```

### 4. Integration Tests

#### Complete Workflow Integration Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.profiles.active=test")
class WorkflowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthService authService;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Register and authenticate test user
        authToken = authenticateTestUser();
    }

    @Test
    void shouldCompleteFullTaskWorkflow() {
        // 1. Create task
        TaskResponse task = createTestTask();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DRAFT);

        // 2. Publish task
        publishTask(task.getId());

        // 3. Place bid
        BidResponse bid = placeBidOnTask(task.getId());
        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);

        // 4. Simulate bidding end and winner selection
        simulateBiddingEnd(task.getId());

        // 5. Submit work
        submitTaskWork(task.getId());

        // 6. Review and approve work
        approveTaskWork(task.getId());

        // 7. Verify payment release
        verifyPaymentReleased(task.getId());
    }
}
```

## API Testing with Postman

### Environment Setup
```json
{
  "name": "CampusWorks Test Environment",
  "values": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080",
      "enabled": true
    },
    {
      "key": "authToken",
      "value": "",
      "enabled": true
    }
  ]
}
```

### Pre-request Scripts
```javascript
// Authentication script
const loginRequest = {
    url: pm.environment.get("baseUrl") + "/auth/login",
    method: 'POST',
    header: {
        'Content-Type': 'application/json'
    },
    body: {
        mode: 'raw',
        raw: JSON.stringify({
            email: "test@university.edu",
            password: "Password123!"
        })
    }
};

pm.sendRequest(loginRequest, function (err, response) {
    if (response.json().success) {
        pm.environment.set("authToken", response.json().data.token);
    }
});
```

### Test Scripts
```javascript
// Response validation script
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has success field", function () {
    const responseJson = pm.response.json();
    pm.expect(responseJson).to.have.property('success');
    pm.expect(responseJson.success).to.be.true;
});

pm.test("Response contains required data", function () {
    const responseJson = pm.response.json();
    pm.expect(responseJson).to.have.property('data');
    pm.expect(responseJson.data).to.have.property('id');
});
```

## Performance Testing

### Load Testing with JMeter
```xml
<!-- Sample JMeter test plan -->
<TestPlan>
    <ThreadGroup>
        <elementProp name="ThreadGroup.main_controller" 
                    elementType="LoopController">
            <boolProp name="LoopController.continue_forever">false</boolProp>
            <intProp name="LoopController.loops">100</intProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">10</stringProp>
        <stringProp name="ThreadGroup.ramp_time">30</stringProp>
    </ThreadGroup>
</TestPlan>
```

### Performance Benchmarks
- **Authentication**: < 200ms response time
- **Task Creation**: < 500ms response time
- **Bid Placement**: < 300ms response time
- **Payment Processing**: < 2000ms response time
- **Search Operations**: < 1000ms response time

## Test Coverage Reports

### Generating Coverage Reports
```bash
# Generate JaCoCo coverage report
mvn clean test jacoco:report

# Generate aggregate coverage report for all modules
mvn clean test jacoco:report-aggregate

# View coverage report
open target/site/jacoco/index.html
```

### Coverage Targets
- **Minimum Coverage**: 80%
- **Critical Services**: 90%+ (Auth, Payment, Task)
- **Utility Classes**: 95%+

## Continuous Integration Testing

### GitHub Actions Workflow
```yaml
name: CI Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test
        ports:
          - 3306:3306
          
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'adopt'
        
    - name: Cache Maven packages
      uses: actions/cache@v2
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        
    - name: Run tests
      run: mvn clean verify
      
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: '**/target/surefire-reports/*.xml'
        reporter: java-junit
```

## Test Data Management

### Test Data Factories
```java
public class TestDataFactory {
    
    public static User createTestUser(String email) {
        return User.builder()
            .email(email)
            .firstName("Test")
            .lastName("User")
            .password("$2a$10$encrypted_password")
            .role(UserRole.STUDENT)
            .emailVerified(true)
            .build();
    }
    
    public static Task createTestTask(Long ownerId) {
        return Task.builder()
            .title("Test Task")
            .description("Test description")
            .ownerId(ownerId)
            .status(TaskStatus.OPEN)
            .minBudget(new BigDecimal("50.00"))
            .maxBudget(new BigDecimal("100.00"))
            .category("RESEARCH")
            .subject("Computer Science")
            .build();
    }
}
```

## Debugging Tests

### Enabling Debug Logging
```properties
# application-test.properties
logging.level.com.campusworks=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Test Debugging Tips
1. Use `@Sql` annotation to set up test data
2. Use `@TestPropertySource` for test-specific configuration
3. Use `@MockBean` for external service dependencies
4. Use `@Transactional` for test data isolation
5. Use TestContainers for database integration tests

## Common Testing Issues

### Issue 1: Authentication in Tests
```java
// Solution: Use @WithMockUser or custom security context
@Test
@WithMockUser(username = "test@test.com", roles = "STUDENT")
void testAuthenticatedEndpoint() {
    // Test implementation
}
```

### Issue 2: Database State Management
```java
// Solution: Use @Transactional or @Sql annotations
@Test
@Transactional
@Sql("/test-data/tasks.sql")
void testWithPreloadedData() {
    // Test implementation
}
```

### Issue 3: Asynchronous Operations
```java
// Solution: Use @Async test utilities
@Test
void testAsyncOperation() throws InterruptedException {
    // Trigger async operation
    asyncService.processAsync();
    
    // Wait for completion
    await().atMost(5, TimeUnit.SECONDS)
           .until(() -> asyncService.isCompleted());
    
    // Verify results
    assertThat(asyncService.getResult()).isNotNull();
}
```

This comprehensive testing guide ensures robust quality assurance for the CampusWorks platform across all layers and components.