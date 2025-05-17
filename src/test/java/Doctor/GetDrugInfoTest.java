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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the getDrugInfo method in the Doctor class.
 * This class tests various scenarios for retrieving drug information from the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetDrugInfoTest {
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
     * Test case: GDI_01
     * Mục tiêu: Kiểm tra khả năng lấy thông tin thuốc khi truy vấn thành công
     * Input: Dữ liệu mock trả về từ customSelection với các cột "drug_id", "name"
     * Expected Output: Trả về danh sách thông tin thuốc với dữ liệu hợp lệ, ví dụ "d001", "Paracetamol"
     * Ghi chú: Phủ nhánh thành công khi truy vấn thông tin thuốc
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetDrugInfoSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> drugData = new ArrayList<>();
        drugData.add(new ArrayList<>(Arrays.asList("drug_id", "name")));
        drugData.add(new ArrayList<>(Arrays.asList("d001", "Paracetamol")));
        when(dbOperator.customSelection(anyString())).thenReturn(drugData);

        // Act: Gọi phương thức getDrugInfo
        ArrayList<ArrayList<String>> result = doctorInstance.getDrugInfo();

        // Assert: Kiểm tra kết quả
        assertEquals("Paracetamol", result.get(1).get(1), "Dữ liệu thuốc đầu tiên phải có tên là 'Paracetamol'");
    }

    /**
     * Test case: GDI_02
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn thông tin thuốc
     * Input: Dữ liệu mock ném SQLException khi truy vấn
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetDrugInfoException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getDrugInfo
        ArrayList<ArrayList<String>> result = doctorInstance.getDrugInfo();

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getDrugInfo phải trả về null khi có lỗi SQLException");
    }
}