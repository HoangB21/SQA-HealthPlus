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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the removeDoctorTime method in the Doctor class.
 * This class tests various scenarios for removing time slots from the doctor's availability schedule.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class RemoveDoctorTimeTest {
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
     * Test case: RDT_01
     * Mục tiêu: Kiểm tra khả năng xóa thời gian bác sĩ theo ngày và slot khi bản ghi tồn tại
     * Input: day = "Monday", slot = "09:00-10:00", bảng doctor_availability có bản ghi với slmc_reg_no = '22387'
     * Expected Output: Trả về true, bản ghi bị xóa khỏi bảng doctor_availability
     * Ghi chú: Phủ nhánh thành công khi xóa thời gian với bản ghi tồn tại
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void testRemoveDoctorTimeByDayAndSlotSuccessWithExistingRecord() throws SQLException {
        // Arrange: Chuẩn bị dữ liệu trong bảng doctor_availability
        Statement stmt = connection.createStatement();
        stmt.execute("INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, time_slot_id) " +
                "VALUES ('22387', 'Monday', '09:00-10:00', 't0001')");

        // Act: Gọi phương thức removeDoctorTime
        boolean result = doctorInstance.removeDoctorTime("Monday", "09:00-10:00");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức removeDoctorTime(String day, String slot) phải trả về true khi xóa thành công");

        // Check database: Kiểm tra bản ghi đã bị xóa
        ResultSet rs = stmt.executeQuery("SELECT * FROM doctor_availability WHERE slmc_reg_no = '22387' AND day = 'Monday' AND time_slot = '09:00-10:00'");
        assertFalse(rs.next(), "Bản ghi phải bị xóa khỏi bảng doctor_availability (removeDoctorTime by day and slot)");
    }

    /**
     * Test case: RDT_02
     * Mục tiêu: Kiểm tra khả năng xóa thời gian bác sĩ theo ngày và slot khi không có bản ghi khớp
     * Input: day = "Monday", slot = "09:00-10:00", bảng doctor_availability không có bản ghi khớp
     * Expected Output: Trả về true
     * Ghi chú: Phủ nhánh thành công khi không có bản ghi nào khớp để xóa
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void testRemoveDoctorTimeByDayAndSlotSuccessWithNoMatchingRecord() throws SQLException {
        // Act: Gọi phương thức removeDoctorTime
        boolean result = doctorInstance.removeDoctorTime("Monday", "09:00-10:00");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức removeDoctorTime(String day, String slot) phải trả về true ngay cả khi không có bản ghi nào khớp");

        // Check database: Kiểm tra không có bản ghi nào
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM doctor_availability WHERE slmc_reg_no = '22387' AND day = 'Monday' AND time_slot = '09:00-10:00'");
        assertFalse(rs.next(), "Không có bản ghi nào khớp để xóa (removeDoctorTime by day and slot)");
    }

    /**
     * Test case: RDT_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi xóa thời gian bác sĩ theo ngày và slot
     * Input: day = "Monday", slot = "09:00-10:00"
     * Expected Output: Trả về false
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void testRemoveDoctorTimeByDayAndSlotSQLException() throws SQLException, ClassNotFoundException {
        // Arrange: Gây lỗi SQLException bằng cách sử dụng mock
        doThrow(new SQLException("Simulated database error")).when(dbOperator).customDeletion(anyString());

        // Act: Gọi phương thức removeDoctorTime
        boolean result = doctorInstance.removeDoctorTime("Monday", "09:00-10:00");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức removeDoctorTime(String day, String slot) phải trả về false khi có lỗi cơ sở dữ liệu");

        // Verify: Đảm bảo customDeletion được gọi đúng
        verify(dbOperator, times(1)).customDeletion(anyString());
    }

    /**
     * Test case: RDT_04
     * Mục tiêu: Kiểm tra khả năng xóa thời gian bác sĩ theo time_slot_id khi bản ghi tồn tại
     * Input: id = "t0002", bảng doctor_availability có bản ghi với time_slot_id = 't0002'
     * Expected Output: Trả về true, bản ghi bị xóa khỏi bảng doctor_availability
     * Ghi chú: Phủ nhánh thành công khi xóa thời gian với bản ghi tồn tại
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void testRemoveDoctorTimeByIdSuccessWithExistingRecord() throws SQLException {
        // Arrange: Chuẩn bị dữ liệu trong bảng doctor_availability
        Statement stmt = connection.createStatement();
        stmt.execute("INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, time_slot_id) " +
                "VALUES ('22387', 'Tuesday', '10:00-11:00', 't0002')");

        // Act: Gọi phương thức removeDoctorTime
        boolean result = doctorInstance.removeDoctorTime("t0002");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức removeDoctorTime(String id) phải trả về true khi xóa thành công");

        // Check database: Kiểm tra bản ghi đã bị xóa
        ResultSet rs = stmt.executeQuery("SELECT * FROM doctor_availability WHERE time_slot_id = 't0002'");
        assertFalse(rs.next(), "Bản ghi phải bị xóa khỏi bảng doctor_availability (removeDoctorTime by timeSlotID)");
    }

    /**
     * Test case: RDT_05
     * Mục tiêu: Kiểm tra khả năng xóa thời gian bác sĩ theo time_slot_id khi không có bản ghi khớp
     * Input: id = "t0001", bảng doctor_availability không có bản ghi khớp
     * Expected Output: Trả về true
     * Ghi chú: Phủ nhánh thành công khi không có bản ghi nào khớp để xóa
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void testRemoveDoctorTimeByIdSuccessWithNoMatchingRecord() throws SQLException {
        // Act: Gọi phương thức removeDoctorTime
        boolean result = doctorInstance.removeDoctorTime("t0001");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức removeDoctorTime(String id) phải trả về true ngay cả khi không có bản ghi nào khớp");

        // Check database: Kiểm tra không có bản ghi nào
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM doctor_availability WHERE time_slot_id = 't0001'");
        assertFalse(rs.next(), "Không có bản ghi nào khớp để xóa (removeDoctorTime by timeSlotID)");
    }

    /**
     * Test case: RDT_06
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi xóa thời gian bác sĩ theo time_slot_id
     * Input: id = "t0001"
     * Expected Output: Trả về false
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void testRemoveDoctorTimeByIdSQLException() throws SQLException, ClassNotFoundException {
        // Arrange: Gây lỗi SQLException bằng cách sử dụng mock
        doThrow(new SQLException("Simulated database error")).when(dbOperator).customDeletion(anyString());

        // Act: Gọi phương thức removeDoctorTime
        boolean result = doctorInstance.removeDoctorTime("t0001");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức removeDoctorTime(String id) phải trả về false khi có lỗi cơ sở dữ liệu");

        // Verify: Đảm bảo customDeletion được gọi đúng
        verify(dbOperator, times(1)).customDeletion(anyString());
    }

    /**
     * Test case: RDT_07
     * Mục tiêu: Kiểm tra xử lý lỗi ClassNotFoundException khi xóa thời gian bác sĩ theo time_slot_id bằng mock
     * Input: id = "t0001", mock ném ClassNotFoundException
     * Expected Output: Trả về false
     * Ghi chú: Phủ nhánh lỗi ClassNotFoundException khi sử dụng mock
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testRemoveDoctorTimeByIdClassNotFoundExceptionByMock() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        doThrow(new ClassNotFoundException("Driver not found")).when(dbOperator).customDeletion(anyString());

        // Act: Gọi phương thức removeDoctorTime
        boolean result = doctorInstance.removeDoctorTime("t0001");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức removeDoctorTime(String id) phải trả về false khi có lỗi ClassNotFoundException");

        // Verify: Đảm bảo customDeletion được gọi đúng
        verify(dbOperator, times(1)).customDeletion(anyString());
    }

    /**
     * Test case: RDT_08
     * Mục tiêu: Kiểm tra xử lý lỗi ClassNotFoundException khi xóa thời gian bác sĩ theo ngày và slot bằng mock
     * Input: day = "Monday", slot = "09:00-10:00", mock ném ClassNotFoundException
     * Expected Output: Trả về false
     * Ghi chú: Phủ nhánh lỗi ClassNotFoundException khi sử dụng mock
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testRemoveDoctorTimeByDayAndSlotClassNotFoundExceptionByMock() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        doThrow(new ClassNotFoundException("Driver not found")).when(dbOperator).customDeletion(anyString());

        // Act: Gọi phương thức removeDoctorTime
        boolean result = doctorInstance.removeDoctorTime("Monday", "09:00-10:00");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức removeDoctorTime(String day, String slot) phải trả về false khi có lỗi ClassNotFoundException");

        // Verify: Đảm bảo customDeletion được gọi đúng
        verify(dbOperator, times(1)).customDeletion(anyString());
    }
}