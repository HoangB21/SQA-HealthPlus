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
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the doctorTimeTable method in the Doctor class.
 * This class tests various scenarios for retrieving the doctor's timetable.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class DoctorTimeTableTest {
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
     * Test case: DTT_01
     * Mục tiêu: Kiểm tra khả năng lấy dữ liệu lịch bác sĩ khi truy vấn thành công
     * Input: Dữ liệu mock trả về từ showTableData với các cột "day", "time_slot", "time_slot_id"
     * Expected Output: Trả về danh sách lịch với dữ liệu hợp lệ, ví dụ "Monday", "09:00-10:00", "t0001"
     * Ghi chú: Phủ nhánh thành công khi truy vấn lịch bác sĩ
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDoctorTimeTableSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> timeTableData = new ArrayList<>();
        timeTableData.add(new ArrayList<>(Arrays.asList("day", "time_slot", "time_slot_id")));
        timeTableData.add(new ArrayList<>(Arrays.asList("Monday", "09:00-10:00", "t0001")));
        when(dbOperator.showTableData(anyString(), anyString(), anyString())).thenReturn(timeTableData);

        // Act: Gọi phương thức doctorTimeTable
        ArrayList<ArrayList<String>> result = doctorInstance.doctorTimeTable();

        // Assert: Kiểm tra kết quả
        assertEquals("day", result.get(0).get(0), "Header đầu tiên phải là 'day'");
        assertEquals("Monday", result.get(1).get(0), "Dữ liệu ngày đầu tiên phải là 'Monday'");
    }

    /**
     * Test case: DTT_02
     * Mục tiêu: Kiểm tra xử lý khi truy vấn lịch bác sĩ trả về null
     * Input: Dữ liệu mock trả về từ showTableData là null
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh khi truy vấn trả về null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDoctorTimeTableNullData() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.showTableData(anyString(), anyString(), anyString())).thenReturn(null);

        // Act: Gọi phương thức doctorTimeTable
        ArrayList<ArrayList<String>> result = doctorInstance.doctorTimeTable();

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức doctorTimeTable phải trả về null khi dữ liệu trả về là null");
    }

    /**
     * Test case: DTT_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn lịch bác sĩ
     * Input: Dữ liệu mock ném SQLException khi truy vấn
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDoctorTimeTableException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.showTableData(anyString(), anyString(), anyString()))
                .thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức doctorTimeTable
        ArrayList<ArrayList<String>> result = doctorInstance.doctorTimeTable();

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức doctorTimeTable phải trả về null khi có lỗi SQLException");
    }

    /**
     * Test case: DTT_04
     * Mục tiêu: Kiểm tra khả năng thêm slot mới khi time_slot_id không hợp lệ (độ dài nhỏ hơn 2)
     * Input: day = "Monday", timeSlot = "09:00-10:00", dữ liệu mock trả về time_slot_id = "t"
     * Expected Output: Trả về true, lệnh INSERT được gọi với time_slot_id mặc định "t0001"
     * Ghi chú: Phủ nhánh xử lý khi time_slot_id không hợp lệ, sử dụng giá trị mặc định
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDoctorTimeTableAddSlotInvalidTimeSlotIDLength() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String day = "Monday";
        String timeSlot = "09:00-10:00";

        // Mock: timeSlotID không hợp lệ (độ dài nhỏ hơn 2)
        ArrayList<ArrayList<String>> timeSlotIDResult = new ArrayList<>();
        timeSlotIDResult.add(new ArrayList<>(Arrays.asList("time_slot_id")));
        timeSlotIDResult.add(new ArrayList<>(Arrays.asList("t"))); // Độ dài 1
        when(dbOperator.customSelection("SELECT time_slot_id FROM doctor_availability WHERE time_slot_id = (SELECT MAX(time_slot_id) FROM doctor_availability);"))
                .thenReturn(timeSlotIDResult);

        // Mock lệnh insert thành công với time_slot_id mặc định "t0001"
        String expectedSql = "t0001," + doctorInstance.slmcRegNo + "," + day + "," + timeSlot + ",0,0";
        when(dbOperator.addTableRow("doctor_availability", expectedSql)).thenReturn(true);

        // Act: Gọi phương thức doctorTimeTableAddSlot
        boolean result = doctorInstance.doctorTimeTableAddSlot(day, timeSlot);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức doctorTimeTableAddSlot phải trả về true khi thêm slot mới thành công với time_slot_id không hợp lệ");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT time_slot_id FROM doctor_availability WHERE time_slot_id = (SELECT MAX(time_slot_id) FROM doctor_availability);");
        verify(dbOperator, times(1)).addTableRow("doctor_availability", expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
}