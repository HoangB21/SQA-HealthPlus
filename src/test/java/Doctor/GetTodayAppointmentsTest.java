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
 * Unit test class for testing the getTodayAppointments method in the Doctor class.
 * This class tests various scenarios for retrieving the doctor's appointments for the current day.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetTodayAppointmentsTest {
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
     * Test case: GTA_01
     * Mục tiêu: Kiểm tra khả năng lấy số lượng lịch hẹn hôm nay khi truy vấn thành công
     * Input: Dữ liệu mock trả về từ customSelection với cột "Appointments", giá trị "5"
     * Expected Output: Trả về chuỗi rỗng "" (do mã nguồn hiện tại không gán giá trị cho apps)
     * Ghi chú: Phủ nhánh thành công khi truy vấn lịch hẹn hôm nay, nhưng mã nguồn có vấn đề
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTodayAppointmentsSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> appointmentData = new ArrayList<>();
        appointmentData.add(new ArrayList<>(Arrays.asList("Appointments")));
        appointmentData.add(new ArrayList<>(Arrays.asList("5")));
        when(dbOperator.customSelection(anyString())).thenReturn(appointmentData);

        // Act: Gọi phương thức getTodayAppointments
        String result = doctorInstance.getTodayAppointments();

        // Assert: Kiểm tra kết quả
        assertEquals("", result, "Phương thức getTodayAppointments hiện tại trả về chuỗi rỗng do mã nguồn không gán giá trị cho apps");
    }

    /**
     * Test case: GTA_02
     * Mục tiêu: Kiểm tra xử lý khi truy vấn lịch hẹn hôm nay trả về null
     * Input: Dữ liệu mock trả về từ customSelection là null
     * Expected Output: Trả về chuỗi rỗng ""
     * Ghi chú: Phủ nhánh khi truy vấn trả về null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTodayAppointmentsNullData() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        // Act: Gọi phương thức getTodayAppointments
        String result = doctorInstance.getTodayAppointments();

        // Assert: Kiểm tra kết quả
        assertEquals("", result, "Phương thức getTodayAppointments phải trả về chuỗi rỗng khi dữ liệu trả về là null");
    }

    /**
     * Test case: GTA_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn lịch hẹn hôm nay
     * Input: Dữ liệu mock ném SQLException
     * Expected Output: Trả về chuỗi rỗng ""
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTodayAppointmentsException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getTodayAppointments
        String result = doctorInstance.getTodayAppointments();

        // Assert: Kiểm tra kết quả
        assertEquals("", result, "Phương thức getTodayAppointments phải trả về chuỗi rỗng khi có lỗi SQLException");
    }
}