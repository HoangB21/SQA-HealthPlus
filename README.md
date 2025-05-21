# HealthPlus Testing Project

This project contains test cases for the [HealthPlus Healthcare Management System](https://github.com/heshanera/HealthPlus), focusing on comprehensive testing of the Receptionist module functionalities.

## Overview

This testing project aims to ensure the reliability and correctness of core functionalities in the HealthPlus system, particularly focusing on:

- Patient registration and information management
- Appointment scheduling and management
- Doctor availability tracking
- Refund processing
- Lab appointment handling

## Test Coverage

### Receptionist Package Tests

1. **Patient Management Tests** (`SetPatientInfoTest.java`)
   - Patient registration with valid data
   - Input validation and error handling
   - Database operation error scenarios
   - ID generation and format validation

2. **Appointment Management Tests** (`MakeAppointmentTest.java`)
   - Appointment creation for current/next week
   - Doctor availability updates
   - Temporary bill generation
   - Error handling and validation

3. **Doctor Availability Tests** (`DoctorAppointmentAvailableTimeTest.java`)
   - Time slot availability calculation
   - Next available appointment time computation
   - Date and time format handling
   - Error scenarios handling

4. **Refund Processing Tests** (`RefundTest.java`)
   - Refund record creation
   - Payment validation
   - Transaction handling
   - Error scenarios

## Test Implementation Details

- Uses JUnit 5 for test framework
- Mockito for database operation mocking
- Transaction management for database tests
- Comprehensive test documentation following standardized format

## Project Setup

### Prerequisites
- Java OpenJDK 17
- Maven 3.9.9
- MariaDB 11.6.2
- Original HealthPlus system dependencies

### Database Setup
1. Create test database:
```sql
CREATE DATABASE test_HMS2;
```

2. Import test data:
```sql
USE test_HMS2;
SOURCE path/to/hms_db.sql;
```

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SetPatientInfoTest

# Run with coverage report
mvn test jacoco:report
```

## Test Documentation Structure

Each test class follows a standardized documentation format:

```java
/**
 * Test class for [method] in [class].
 * 
 * Method Purpose:
 * - Main functionality
 * - Key operations
 * 
 * Method Parameters:
 * - Parameter descriptions
 * 
 * Method Return Format:
 * - Success/Failure conditions
 * 
 * Business Rules:
 * 1. Rule descriptions
 * 2. Validation criteria
 */
```

## Acknowledgments

This testing project is based on the [HealthPlus system](https://github.com/heshanera/HealthPlus) by [@heshanera](https://github.com/heshanera). The original system provides the foundation for developing these comprehensive test cases.

## License

This project is licensed under the Apache 2.0 License - see the original [LICENSE](https://github.com/heshanera/HealthPlus/blob/master/LICENSE) file for details.
