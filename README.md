# HealthPlus

## Overview
HealthPlus is a comprehensive Hospital Management System designed to streamline healthcare operations. The system is built using Java and follows a modular architecture to ensure maintainability and scalability.

## Features
- Patient Management
- Appointment Scheduling
- Doctor Management
- Pharmacy Management
- Billing and Payment Processing
- Inventory Management

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   ├── Admin/
│   │   ├── Cashier/
│   │   ├── Doctor/
│   │   ├── Laboratory/
│   │   ├── Pharmacist/
│   │   ├── Receptionist/
│   │   └── com/
│   │       └── hms/
│   │           └── hms_test_2/
│   └── resources/
│       ├── fxml/
│       ├── imgs/
│       └── styles/
└── test/
    └── java/
        ├── Doctor/
        ├── Pharmacist/
        └── Cashier/
```

## Testing
The project includes comprehensive unit tests for the following modules:
- Doctor Module
- Pharmacist Module
- Cashier Module

### Running Tests
To run the tests, use the following command:
```bash
mvn test
```

## Dependencies
- Java 8 or higher
- Maven
- JavaFX
- MySQL Database

## Installation
1. Clone the repository
2. Set up the MySQL database
3. Configure database connection in `src/main/resources/config.properties`
4. Build the project using Maven:
```bash
mvn clean install
```

## Usage
1. Run the application:
```bash
java -jar target/healthplus.jar
```
2. Login with appropriate credentials based on user role

## Contributing
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License
This project is licensed under the MIT License - see the LICENSE file for details.
