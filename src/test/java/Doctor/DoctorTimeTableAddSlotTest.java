package Doctor;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the doctorTimeTableAddSlot method in the Doctor class.
 * This class tests various scenarios for adding a time slot to the doctor's availability.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class DoctorTimeTableAddSlotTest {
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
        doctorInstance.slmcRegNo = "22387"; // Set slmcRegNo for the Doctor instance
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
     * Test case: DTAS_01
     * Mục tiêu: Kiểm tra khả năng thêm slot mới khi bảng có dữ liệu hợp lệ và time_slot_id cần đệm số 0
     * Input: day = "Monday", timeSlot = "09:00-10:00", dữ liệu mock trả về time_slot_id = "t0001"
     * Expected Output: Trả về true, lệnh INSERT được gọi với time_slot_id = "t0002"
     * Ghi chú: Phủ nhánh thành công với trường hợp time_slot_id cần đệm số 0
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDoctorTimeTableAddSlotSuccessWithZeroPadding() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> maxTimeSlotData = new ArrayList<>();
        maxTimeSlotData.add(new ArrayList<>(Arrays.asList("time_slot_id")));
        maxTimeSlotData.add(new ArrayList<>(Arrays.asList("t0001")));
        when(dbOperator.customSelection(anyString())).thenReturn(maxTimeSlotData);
        when(dbOperator.addTableRow(anyString(), anyString())).thenReturn(true);

        // Act: Gọi phương thức doctorTimeTableAddSlot
        boolean result = doctorInstance.doctorTimeTableAddSlot("Monday", "09:00-10:00");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức doctorTimeTableAddSlot phải trả về true khi thêm slot thành công");

        // Verify: Đảm bảo lệnh INSERT được gọi với time_slot_id mới
        verify(dbOperator, times(1)).addTableRow(eq("doctor_availability"), contains("t0002"));
    }

    /**
     * Test case: DTAS_02
     * Mục tiêu: Kiểm tra khả năng thêm slot mới khi bảng có dữ liệu hợp lệ và time_slot_id không cần đệm số 0
     * Input: day = "Monday", timeSlot = "09:00-10:00", dữ liệu mock trả về time_slot_id = "t0010"
     * Expected Output: Trả về true, lệnh INSERT được gọi với time_slot_id = "t0011"
     * Ghi chú: Phủ nhánh thành công với trường hợp time_slot_id không cần đệm số 0
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDoctorTimeTableAddSlotSuccessWithoutZeroPadding() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> maxTimeSlotData = new ArrayList<>();
        maxTimeSlotData.add(new ArrayList<>(Arrays.asList("time_slot_id")));
        maxTimeSlotData.add(new ArrayList<>(Arrays.asList("t0010")));
        when(dbOperator.customSelection(anyString())).thenReturn(maxTimeSlotData);
        when(dbOperator.addTableRow(anyString(), anyString())).thenReturn(true);

        // Act: Gọi phương thức doctorTimeTableAddSlot
        boolean result = doctorInstance.doctorTimeTableAddSlot("Monday", "09:00-10:00");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức doctorTimeTableAddSlot phải trả về true khi thêm slot thành công");

        // Verify: Đảm bảo lệnh INSERT được gọi với time_slot_id mới
        verify(dbOperator, times(1)).addTableRow(eq("doctor_availability"), contains("t0011"));
    }

    /**
     * Test case: DTAS_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn time_slot_id
     * Input: day = "Monday", timeSlot = "09:00-10:00", dữ liệu mock ném SQLException
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDoctorTimeTableAddSlotException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức doctorTimeTableAddSlot
        boolean result = doctorInstance.doctorTimeTableAddSlot("Monday", "09:00-10:00");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức doctorTimeTableAddSlot phải trả về false khi có lỗi SQLException");
    }
}