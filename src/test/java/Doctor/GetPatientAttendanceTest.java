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
 * Unit test class for testing the getPatientAttendance method in the Doctor class.
 * This class tests various scenarios for retrieving patient attendance data from the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetPatientAttendanceTest {
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
     * Test case: GPA_01
     * Mục tiêu: Kiểm tra khả năng lấy dữ liệu tham dự của bệnh nhân cho tất cả bác sĩ khi truy vấn thành công
     * Input: doctorID = "All", dữ liệu mock trả về với cột "date", giá trị "2023-10-01"
     * Expected Output: Trả về danh sách dữ liệu tham dự với dữ liệu hợp lệ, ví dụ "2023-10-01"
     * Ghi chú: Phủ nhánh thành công khi truy vấn dữ liệu tham dự cho tất cả bác sĩ
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetPatientAttendanceAllDoctors() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> attendanceData = new ArrayList<>();
        attendanceData.add(new ArrayList<>(Arrays.asList("date")));
        attendanceData.add(new ArrayList<>(Arrays.asList("2023-10-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(attendanceData);

        // Act: Gọi phương thức getPatientAttendance
        ArrayList<ArrayList<String>> result = doctorInstance.getPatientAttendance("All");

        // Assert: Kiểm tra kết quả
        assertEquals("2023-10-01", result.get(1).get(0), "Dữ liệu tham dự đầu tiên phải có ngày là '2023-10-01'");
    }

    /**
     * Test case: GPA_02
     * Mục tiêu: Kiểm tra khả năng lấy dữ liệu tham dự của bệnh nhân cho một bác sĩ cụ thể khi truy vấn thành công
     * Input: doctorID = "slmc001", dữ liệu mock trả về với cột "date", giá trị "2023-10-01"
     * Expected Output: Trả về danh sách dữ liệu tham dự với dữ liệu hợp lệ, ví dụ "2023-10-01"
     * Ghi chú: Phủ nhánh thành công khi truy vấn dữ liệu tham dự cho một bác sĩ cụ thể
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetPatientAttendanceSpecificDoctor() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> attendanceData = new ArrayList<>();
        attendanceData.add(new ArrayList<>(Arrays.asList("date")));
        attendanceData.add(new ArrayList<>(Arrays.asList("2023-10-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(attendanceData);

        // Act: Gọi phương thức getPatientAttendance
        ArrayList<ArrayList<String>> result = doctorInstance.getPatientAttendance("slmc001");

        // Assert: Kiểm tra kết quả
        assertEquals("2023-10-01", result.get(1).get(0), "Dữ liệu tham dự đầu tiên phải có ngày là '2023-10-01'");
    }

    /**
     * Test case: GPA_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn dữ liệu tham dự của bệnh nhân
     * Input: doctorID = "All", dữ liệu mock ném SQLException
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetPatientAttendanceException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getPatientAttendance
        ArrayList<ArrayList<String>> result = doctorInstance.getPatientAttendance("All");

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getPatientAttendance phải trả về null khi có lỗi SQLException");
    }
}