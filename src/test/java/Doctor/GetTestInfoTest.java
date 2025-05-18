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
 * Unit test class for testing the getTestInfo method in the Doctor class.
 * This class tests various scenarios for retrieving test information from the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetTestInfoTest {
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
        connection = DatabaseOperator.c;
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
     * Test case: GTI_01
     * Mục tiêu: Kiểm tra khả năng lấy thông tin xét nghiệm khi truy vấn thành công
     * Input: Dữ liệu mock trả về từ customSelection với các cột "test_id", "test_name", "test_description"
     * Expected Output: Trả về danh sách thông tin xét nghiệm với dữ liệu hợp lệ, ví dụ "tst001", "BloodTest", "Complete Blood Count"
     * Ghi chú: Phủ nhánh thành công khi truy vấn thông tin xét nghiệm
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestInfoSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> testData = new ArrayList<>();
        testData.add(new ArrayList<>(Arrays.asList("test_id", "test_name", "test_description")));
        testData.add(new ArrayList<>(Arrays.asList("tst001", "BloodTest", "Complete Blood Count")));
        when(dbOperator.customSelection(anyString())).thenReturn(testData);

        // Act: Gọi phương thức getTestInfo
        ArrayList<ArrayList<String>> result = doctorInstance.getTestInfo();

        // Assert: Kiểm tra kết quả
        assertEquals("BloodTest", result.get(1).get(1), "Dữ liệu xét nghiệm đầu tiên phải có test_name là 'BloodTest'");
    }

    /**
     * Test case: GTI_02
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn thông tin xét nghiệm
     * Input: Dữ liệu mock ném SQLException khi truy vấn
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestInfoException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getTestInfo
        ArrayList<ArrayList<String>> result = doctorInstance.getTestInfo();

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getTestInfo phải trả về null khi có lỗi SQLException");
    }
}