package Doctor;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit test class for testing the getLabPatientInfo method in the Doctor class.
 * This class tests various scenarios for retrieving lab patient information based on an appointment ID.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetLabPatientInfoTest {
    @Mock
    private DatabaseOperator dbOperator; // Mocked DatabaseOperator for simulating database interactions

    private Doctor doctorInstance; // Doctor instance under test
    private Connection connection; // Database connection for direct database access

    /**
     * Set up the test environment before each test case.
     * Initializes the database connection, Doctor instance, and mocks the DatabaseOperator.
     * @throws SQLException if a database access error occurs
     */
    @BeforeEach
    public void setUp() throws SQLException {
        // Initialize Mockito mocks
        MockitoAnnotations.openMocks(this);

        // Establish a connection to the MySQL database
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true",
                "root",
                "Huycode12003."
        );
        connection.setAutoCommit(false); // Disable auto-commit to control transactions

        // Initialize Doctor instance
        doctorInstance = new Doctor("user001");
        doctorInstance.dbOperator = dbOperator;
        doctorInstance.userID = "user001"; // Ensure userID is set for the Doctor instance
    }

    /**
     * Clean up the test environment after each test case.
     * Rolls back the database transaction and closes the connection.
     * @throws SQLException if a database access error occurs
     */
    @AfterEach
    public void tearDown() throws SQLException {
        // Rollback any changes made to the database during the test
        if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true); // Restore auto-commit mode
            connection.close();
        }
    }

    /**
     * Test case: GLPI_01
     * Mục tiêu: Kiểm tra khả năng lấy thông tin bệnh nhân từ phòng lab khi truy vấn thành công
     * Input: appID = "lab001", dữ liệu mock trả về với các cột "first_name", "last_name", "gender", "date_of_birth"
     * Expected Output: Trả về danh sách thông tin bệnh nhân với dữ liệu hợp lệ, ví dụ "John", "Doe", "Male", "1990-01-01"
     * Ghi chú: Phủ nhánh thành công khi truy vấn thông tin bệnh nhân từ phòng lab
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetLabPatientInfoSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> patientData = new ArrayList<>();
        patientData.add(new ArrayList<>(Arrays.asList("first_name", "last_name", "gender", "date_of_birth")));
        patientData.add(new ArrayList<>(Arrays.asList("John", "Doe", "Male", "1990-01-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(patientData);

        // Act: Gọi phương thức getLabPatientInfo
        ArrayList<ArrayList<String>> result = doctorInstance.getLabPatientInfo("lab001");

        // Assert: Kiểm tra kết quả
        assertEquals("John", result.get(1).get(0), "Dữ liệu bệnh nhân đầu tiên phải có first_name là 'John'");
    }

    /**
     * Test case: GLPI_02
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn thông tin bệnh nhân từ phòng lab
     * Input: appID = "lab001", dữ liệu mock ném SQLException
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetLabPatientInfoException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getLabPatientInfo
        ArrayList<ArrayList<String>> result = doctorInstance.getLabPatientInfo("lab001");

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getLabPatientInfo phải trả về null khi có lỗi SQLException");
    }
}