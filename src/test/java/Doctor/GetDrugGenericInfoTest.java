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
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit test class for testing the getDrugGenericInfo method in the Doctor class.
 * This class tests various scenarios for retrieving generic drug information from the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetDrugGenericInfoTest {
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
     * Test case: GDGI_01
     * Mục tiêu: Kiểm tra khả năng lấy danh sách tên generic của thuốc khi truy vấn thành công
     * Input: Dữ liệu mock trả về từ customSelection với các cột "generic_name", chứa "Paracetamol" và "Ibuprofen"
     * Expected Output: Trả về danh sách tên generic với dữ liệu hợp lệ, ví dụ "Paracetamol", "Ibuprofen"
     * Ghi chú: Phủ nhánh thành công khi truy vấn danh sách tên generic của thuốc
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetDrugGenericInfoSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> genericData = new ArrayList<>();
        genericData.add(new ArrayList<>(Arrays.asList("generic_name")));
        genericData.add(new ArrayList<>(Arrays.asList("Paracetamol")));
        genericData.add(new ArrayList<>(Arrays.asList("Paracetamol")));
        genericData.add(new ArrayList<>(Arrays.asList("Ibuprofen")));
        when(dbOperator.customSelection(anyString())).thenReturn(genericData);

        // Act: Gọi phương thức getDrugGenericInfo
        ArrayList<String> result = doctorInstance.getDrugGenericInfo();

        // Assert: Kiểm tra kết quả
        assertEquals(2, result.size(), "Danh sách tên generic phải chứa 2 phần tử duy nhất");
        assertTrue(result.contains("Paracetamol"), "Danh sách phải chứa tên generic 'Paracetamol'");
        assertTrue(result.contains("Ibuprofen"), "Danh sách phải chứa tên generic 'Ibuprofen'");
    }

    /**
     * Test case: GDGI_02
     * Mục tiêu: Kiểm tra xử lý khi truy vấn tên generic của thuốc trả về null
     * Input: Dữ liệu mock trả về từ customSelection là null
     * Expected Output: Trả về danh sách rỗng
     * Ghi chú: Phủ nhánh khi truy vấn trả về null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetDrugGenericInfoNullData() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        // Act: Gọi phương thức getDrugGenericInfo
        ArrayList<String> result = doctorInstance.getDrugGenericInfo();

        // Assert: Kiểm tra kết quả
        assertTrue(result.isEmpty(), "Phương thức getDrugGenericInfo phải trả về danh sách rỗng khi dữ liệu trả về là null");
    }

    /**
     * Test case: GDGI_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn tên generic của thuốc
     * Input: Dữ liệu mock ném SQLException khi truy vấn
     * Expected Output: Trả về danh sách rỗng
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetDrugGenericInfoException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getDrugGenericInfo
        ArrayList<String> result = doctorInstance.getDrugGenericInfo();

        // Assert: Kiểm tra kết quả
        assertTrue(result.isEmpty(), "Phương thức getDrugGenericInfo phải trả về danh sách rỗng khi có lỗi SQLException");
    }
}