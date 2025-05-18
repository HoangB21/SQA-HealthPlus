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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit test class for testing the getDrugBrandInfo method in the Doctor class.
 * This class tests various scenarios for retrieving drug brand information based on a generic name.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetDrugBrandInfoTest {
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
     * Test case: GDBI_01
     * Mục tiêu: Kiểm tra khả năng lấy danh sách thương hiệu thuốc khi truy vấn thành công
     * Input: genericName = "Paracetamol", dữ liệu mock trả về các thương hiệu "Panadol", "Calpol"
     * Expected Output: Trả về danh sách thương hiệu thuốc, chứa "Panadol" và "Calpol"
     * Ghi chú: Phủ nhánh thành công khi truy vấn danh sách thương hiệu thuốc
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetDrugBrandInfoSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> brandData = new ArrayList<>();
        brandData.add(new ArrayList<>(Arrays.asList("brand_name")));
        brandData.add(new ArrayList<>(Arrays.asList("Panadol")));
        brandData.add(new ArrayList<>(Arrays.asList("Calpol")));
        when(dbOperator.customSelection(anyString())).thenReturn(brandData);

        // Act: Gọi phương thức getDrugBrandInfo
        ArrayList<String> result = doctorInstance.getDrugBrandInfo("Paracetamol");

        // Assert: Kiểm tra kết quả
        assertEquals(2, result.size(), "Danh sách thương hiệu phải chứa 2 phần tử");
        assertTrue(result.contains("Panadol"), "Danh sách phải chứa thương hiệu 'Panadol'");
        assertTrue(result.contains("Calpol"), "Danh sách phải chứa thương hiệu 'Calpol'");
    }

    /**
     * Test case: GDBI_02
     * Mục tiêu: Kiểm tra xử lý khi truy vấn thương hiệu thuốc trả về null
     * Input: genericName = "Paracetamol", dữ liệu mock trả về null
     * Expected Output: Trả về danh sách rỗng
     * Ghi chú: Phủ nhánh khi truy vấn trả về null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetDrugBrandInfoNullData() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        // Act: Gọi phương thức getDrugBrandInfo
        ArrayList<String> result = doctorInstance.getDrugBrandInfo("Paracetamol");

        // Assert: Kiểm tra kết quả
        assertTrue(result.isEmpty(), "Phương thức getDrugBrandInfo phải trả về danh sách rỗng khi dữ liệu trả về là null");
    }

    /**
     * Test case: GDBI_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn thương hiệu thuốc
     * Input: genericName = "Paracetamol", dữ liệu mock ném SQLException
     * Expected Output: Trả về danh sách rỗng
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetDrugBrandInfoException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getDrugBrandInfo
        ArrayList<String> result = doctorInstance.getDrugBrandInfo("Paracetamol");

        // Assert: Kiểm tra kết quả
        assertTrue(result.isEmpty(), "Phương thức getDrugBrandInfo phải trả về danh sách rỗng khi có lỗi SQLException");
    }
}