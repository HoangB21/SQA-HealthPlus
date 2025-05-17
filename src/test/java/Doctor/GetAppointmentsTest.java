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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit test class for testing the getAppointments method in the Doctor class.
 * This class tests various scenarios for retrieving the doctor's appointments from the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetAppointmentsTest {
    @Mock
    private DatabaseOperator dbOperator; // Mocked DatabaseOperator for simulating database interactions

    private Doctor doctorInstance; // Doctor instance under test
    private Connection connection; // Database connection for direct database access
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
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true",
                "root",
                "Huycode12003."
        );
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
        connection.rollback();
        connection.setAutoCommit(true); // Restore auto-commit mode
        connection.close();

        // Close Mockito resources
        closeable.close();
    }

    /**
     * Test case: GAP_01
     * Mục tiêu: Kiểm tra khả năng lấy danh sách lịch hẹn khi truy vấn thành công
     * Input: Dữ liệu mock trả về từ showTableData với các cột "patient_id", "date"
     * Expected Output: Trả về danh sách lịch hẹn với dữ liệu hợp lệ, ví dụ "pat001", "2023-10-01"
     * Ghi chú: Phủ nhánh thành công khi truy vấn lịch hẹn
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetAppointmentsSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> appointmentData = new ArrayList<>();
        appointmentData.add(new ArrayList<>(Arrays.asList("patient_id", "date")));
        appointmentData.add(new ArrayList<>(Arrays.asList("pat001", "2023-10-01")));
        when(dbOperator.showTableData(anyString(), anyString(), anyString())).thenReturn(appointmentData);

        // Act: Gọi phương thức getAppointments
        ArrayList<ArrayList<String>> result = doctorInstance.getAppointments();

        // Assert: Kiểm tra kết quả
        assertEquals("patient", result.get(0).get(0), "Header đầu tiên phải là 'patient'");
        assertEquals("pat001", result.get(1).get(0), "Dữ liệu bệnh nhân đầu tiên phải có patient_id là 'pat001'");
    }

    /**
     * Test case: GAP_02
     * Mục tiêu: Kiểm tra xử lý khi truy vấn lịch hẹn trả về null
     * Input: Dữ liệu mock trả về từ showTableData là null
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh khi truy vấn trả về null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetAppointmentsNullData() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.showTableData(anyString(), anyString(), anyString())).thenReturn(null);

        // Act: Gọi phương thức getAppointments
        ArrayList<ArrayList<String>> result = doctorInstance.getAppointments();

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getAppointments phải trả về null khi dữ liệu trả về là null");
    }

    /**
     * Test case: GAP_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn lịch hẹn
     * Input: Dữ liệu mock ném SQLException khi truy vấn
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetAppointmentsException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.showTableData(anyString(), anyString(), anyString()))
                .thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getAppointments
        ArrayList<ArrayList<String>> result = doctorInstance.getAppointments();

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getAppointments phải trả về null khi có lỗi SQLException");
    }
}