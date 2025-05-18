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
 * Unit test class for testing the getLabFee method in the Doctor class.
 * This class tests various scenarios for retrieving lab fees for a specific test from the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetLabFeeTest {
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
     * Test case: GLF_01
     * Mục tiêu: Kiểm tra khả năng lấy phí phòng lab khi truy vấn thành công
     * Input: test = "BloodTest", dữ liệu mock trả về với cột "test_fee", giá trị "500"
     * Expected Output: Trả về phí phòng lab là "500"
     * Ghi chú: Phủ nhánh thành công khi truy vấn phí phòng lab
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetLabFeeSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> feeData = new ArrayList<>();
        feeData.add(new ArrayList<>(Arrays.asList("test_fee")));
        feeData.add(new ArrayList<>(Arrays.asList("500")));
        when(dbOperator.customSelection(anyString())).thenReturn(feeData);

        // Act: Gọi phương thức getLabFee
        String result = doctorInstance.getLabFee("BloodTest");

        // Assert: Kiểm tra kết quả
        assertEquals("500", result, "Phương thức getLabFee phải trả về phí phòng lab là '500'");
    }

    /**
     * Test case: GLF_02
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn phí phòng lab
     * Input: test = "BloodTest", dữ liệu mock ném SQLException
     * Expected Output: Trả về chuỗi rỗng ""
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetLabFeeException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getLabFee
        String result = doctorInstance.getLabFee("BloodTest");

        // Assert: Kiểm tra kết quả
        assertEquals("", result, "Phương thức getLabFee phải trả về chuỗi rỗng khi có lỗi SQLException");
    }
}