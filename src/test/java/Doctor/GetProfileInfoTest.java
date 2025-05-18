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
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit test class for testing the getProfileInfo method in the Doctor class.
 * This class tests various scenarios for retrieving the doctor's profile information from the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetProfileInfoTest {
    @Mock
    private DatabaseOperator dbOperator; // Mocked DatabaseOperator for simulating database interactions

    private Connection connection; // Database connection for direct database access
    private Doctor doctorInstance; // Doctor instance under test
    private AutoCloseable closeable; // AutoCloseable for closing Mockito resources

    /**
     * Set up the test environment before each test case.
     * Initializes the database connection, Doctor instance, and mocks the DatabaseOperator.
     * @throws SQLException if a database access error occurs
     */
    @BeforeEach
    public void setUp() throws SQLException {
        // Initialize Mockito mocks
        closeable = MockitoAnnotations.openMocks(this);

        // Establish a connection to the MySQL database
        connection = DatabaseOperator.c;
        connection.setAutoCommit(false); // Disable auto-commit to control transactions

        // Initialize Doctor instance
        doctorInstance = new Doctor("user001");
        doctorInstance.dbOperator = dbOperator;
        doctorInstance.slmcRegNo = "22387"; // Set slmcRegNo for the Doctor instance
    }

    /**
     * Clean up the test environment after each test case.
     * Rolls back the database transaction, closes the connection, and closes Mockito resources.
     * @throws Exception if an error occurs during cleanup
     */
    @AfterEach
    public void tearDown() throws Exception {
        // Rollback any changes made to the database during the test
        if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true); // Restore auto-commit mode
            connection.close();
        }

        // Close Mockito resources
        closeable.close();
    }

    /**
     * Test case: GPI_01
     * Mục tiêu: Kiểm tra khả năng lấy thông tin hồ sơ bác sĩ khi truy vấn thành công
     * Input: Dữ liệu mock trả về từ customSelection với các cột "first_name", "last_name"
     * Expected Output: Trả về HashMap chứa thông tin hồ sơ, ví dụ "first_name" = "John", "last_name" = "Doe"
     * Ghi chú: Phủ nhánh thành công khi truy vấn thông tin hồ sơ bác sĩ
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetProfileInfoSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> profileData = new ArrayList<>();
        profileData.add(new ArrayList<>(Arrays.asList("first_name", "last_name")));
        profileData.add(new ArrayList<>(Arrays.asList("John", "Doe")));
        when(dbOperator.customSelection(anyString())).thenReturn(profileData);

        // Act: Gọi phương thức getProfileInfo
        HashMap<String, String> result = doctorInstance.getProfileInfo();

        // Assert: Kiểm tra kết quả
        assertEquals("John", result.get("first_name"), "First_name trong hồ sơ phải là 'John'");
        assertEquals("Doe", result.get("last_name"), "Last_name trong hồ sơ phải là 'Doe'");
    }

    /**
     * Test case: GPI_02
     * Mục tiêu: Kiểm tra xử lý khi truy vấn thông tin hồ sơ trả về null
     * Input: Dữ liệu mock trả về từ customSelection là null
     * Expected Output: Trả về HashMap rỗng
     * Ghi chú: Phủ nhánh khi truy vấn trả về null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetProfileInfoNullData() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        // Act: Gọi phương thức getProfileInfo
        HashMap<String, String> result = doctorInstance.getProfileInfo();

        // Assert: Kiểm tra kết quả
        assertTrue(result.isEmpty(), "Phương thức getProfileInfo phải trả về HashMap rỗng khi dữ liệu trả về là null");
    }

    /**
     * Test case: GPI_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn thông tin hồ sơ
     * Input: Dữ liệu mock ném SQLException
     * Expected Output: Trả về HashMap rỗng
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetProfileInfoException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getProfileInfo
        HashMap<String, String> result = doctorInstance.getProfileInfo();

        // Assert: Kiểm tra kết quả
        assertTrue(result.isEmpty(), "Phương thức getProfileInfo phải trả về HashMap rỗng khi có lỗi SQLException");
    }
}