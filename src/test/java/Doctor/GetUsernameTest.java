package Doctor;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for testing the getUsername method in the Doctor class.
 * This class tests scenarios for retrieving the username of the Doctor instance.
 * Uses direct database access for validation without mocking.
 */
public class GetUsernameTest {
    private DatabaseOperator dbOperator; // Real DatabaseOperator for database interactions
    private Doctor doctorInstance; // Doctor instance under test
    private Connection connection; // Database connection for direct database access

    /**
     * Set up the test environment before each test case.
     * Initializes the database connection and DatabaseOperator.
     * @throws SQLException if a database access error occurs
     */
    @BeforeEach
    public void setUp() throws SQLException {
        // Establish a connection to the MySQL database
        connection = DatabaseOperator.c;
        connection.setAutoCommit(false); // Disable auto-commit to control transactions

        // Initialize real DatabaseOperator
        dbOperator = new DatabaseOperator();
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
     * Test case: GUN_01
     * Mục tiêu: Kiểm tra khả năng lấy username khi username không null
     * Input: Doctor được khởi tạo với username = "user001"
     * Expected Output: Trả về "user001"
     * Ghi chú: Phủ nhánh thành công khi username không null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetUsernameSuccessWithNonNullUsername() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu cho constructor của Doctor
        ArrayList<ArrayList<String>> slmcData = new ArrayList<>();
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("22387")));
        // Giả lập dữ liệu trả về từ database khi Doctor khởi tạo
        // Lưu ý: Không sử dụng mock ở đây, chỉ mô tả logic khởi tạo
        doctorInstance = new Doctor("user001");
        doctorInstance.dbOperator = dbOperator;
        doctorInstance.slmcRegNo = "22387";

        // Act: Gọi phương thức getUsername
        String result = doctorInstance.getUsername();

        // Assert: Kiểm tra kết quả
        assertEquals("user001", result, "Phương thức getUsername phải trả về username đúng khi username không null");
    }

    /**
     * Test case: GUN_02
     * Mục tiêu: Kiểm tra khả năng lấy username khi username là null
     * Input: Doctor được khởi tạo với username = null
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh khi username là null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetUsernameWithNullUsername() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu cho constructor của Doctor
        ArrayList<ArrayList<String>> slmcData = new ArrayList<>();
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("22387")));
        // Giả lập dữ liệu trả về từ database khi Doctor khởi tạo
        // Lưu ý: Không sử dụng mock ở đây, chỉ mô tả logic khởi tạo
        doctorInstance = new Doctor(null); // Truyền null vào constructor
        doctorInstance.dbOperator = dbOperator;
        doctorInstance.slmcRegNo = "22387";

        // Act: Gọi phương thức getUsername
        String result = doctorInstance.getUsername();

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getUsername phải trả về null khi username là null");
    }
}