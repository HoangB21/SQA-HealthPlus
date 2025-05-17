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

public class RemoveDoctorTimeTest {
    @Mock
    private DatabaseOperator dbOperator;
    private Doctor doctorInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true",
                "root",
                "Huycode12003."
        );
        connection.setAutoCommit(false);

        doctorInstance = new Doctor("user001");
        doctorInstance.dbOperator = dbOperator;
        doctorInstance.slmcRegNo = "22387";
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // Rollback giao dịch và đóng kết nối
        if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    @Test
    public void testRemoveDoctorTimeByDayAndSlotSuccessWithExistingRecord() throws SQLException {
        // Chuẩn bị dữ liệu trong bảng doctor_availability
        Statement stmt = connection.createStatement();
        stmt.execute("INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, time_slot_id) " +
                "VALUES ('22387', 'Monday', '09:00-10:00', 't0001')");

        // Trường hợp thành công: Xóa dữ liệu hợp lệ với removeDoctorTime(String day, String slot)
        boolean result = doctorInstance.removeDoctorTime("Monday", "09:00-10:00");
        assertTrue(result, "Phương thức removeDoctorTime(String day, String slot) phải trả về true khi xóa thành công");

        // Kiểm tra dữ liệu đã bị xóa
        ResultSet rs = stmt.executeQuery("SELECT * FROM doctor_availability WHERE slmc_reg_no = '22387' AND day = 'Monday' AND time_slot = '09:00-10:00'");
        assertFalse(rs.next(), "Bản ghi phải bị xóa khỏi bảng doctor_availability (removeDoctorTime by day and slot)");
    }

    @Test
    public void testRemoveDoctorTimeByDayAndSlotSuccessWithNoMatchingRecord() throws SQLException {
        // Trường hợp thành công: Xóa dữ liệu khi không có bản ghi nào khớp
        boolean result = doctorInstance.removeDoctorTime("Monday", "09:00-10:00");
        assertTrue(result, "Phương thức removeDoctorTime(String day, String slot) phải trả về true ngay cả khi không có bản ghi nào khớp");

        // Kiểm tra không có bản ghi nào
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM doctor_availability WHERE slmc_reg_no = '22387' AND day = 'Monday' AND time_slot = '09:00-10:00'");
        assertFalse(rs.next(), "Không có bản ghi nào khớp để xóa (removeDoctorTime by day and slot)");
    }

    @Test
    public void testRemoveDoctorTimeByDayAndSlotSQLException() throws SQLException {
        // Trường hợp SQLException: Đổi tên bảng để gây lỗi
        Statement stmt = connection.createStatement();
        stmt.execute("RENAME TABLE doctor_availability TO doctor_availability_temp");

        boolean result = doctorInstance.removeDoctorTime("Monday", "09:00-10:00");
        assertFalse(result, "Phương thức removeDoctorTime(String day, String slot) phải trả về false khi có lỗi cơ sở dữ liệu");

        // Khôi phục bảng
        stmt.execute("RENAME TABLE doctor_availability_temp TO doctor_availability");
    }

    @Test
    public void testRemoveDoctorTimeByIdSuccessWithExistingRecord() throws SQLException {
        // Chuẩn bị dữ liệu trong bảng doctor_availability
        Statement stmt = connection.createStatement();
        stmt.execute("INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, time_slot_id) " +
                "VALUES ('22387', 'Tuesday', '10:00-11:00', 't0002')");

        // Trường hợp thành công: Xóa dữ liệu hợp lệ với removeDoctorTime(String id)
        boolean result = doctorInstance.removeDoctorTime("t0002");
        assertTrue(result, "Phương thức removeDoctorTime(String id) phải trả về true khi xóa thành công");

        // Kiểm tra dữ liệu đã bị xóa
        ResultSet rs = stmt.executeQuery("SELECT * FROM doctor_availability WHERE time_slot_id = 't0002'");
        assertFalse(rs.next(), "Bản ghi phải bị xóa khỏi bảng doctor_availability (removeDoctorTime by timeSlotID)");
    }

    @Test
    public void testRemoveDoctorTimeByIdSuccessWithNoMatchingRecord() throws SQLException {
        // Trường hợp thành công: Xóa dữ liệu khi không có bản ghi nào khớp
        boolean result = doctorInstance.removeDoctorTime("t0001");
        assertTrue(result, "Phương thức removeDoctorTime(String id) phải trả về true ngay cả khi không có bản ghi nào khớp");

        // Kiểm tra không có bản ghi nào
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM doctor_availability WHERE time_slot_id = 't0001'");
        assertFalse(rs.next(), "Không có bản ghi nào khớp để xóa (removeDoctorTime by timeSlotID)");
    }

    @Test
    public void testRemoveDoctorTimeByIdSQLException() throws SQLException {
        // Trường hợp SQLException: Đổi tên bảng để gây lỗi
        Statement stmt = connection.createStatement();
        stmt.execute("RENAME TABLE doctor_availability TO doctor_availability_temp");

        boolean result = doctorInstance.removeDoctorTime("t0001");
        assertFalse(result, "Phương thức removeDoctorTime(String id) phải trả về false khi có lỗi cơ sở dữ liệu");

        // Khôi phục bảng
        stmt.execute("RENAME TABLE doctor_availability_temp TO doctor_availability");
    }
    @Test
    public void testRemoveDoctorTimeByIdClassNotFoundExceptionByMock() throws SQLException, ClassNotFoundException {
        doThrow(new ClassNotFoundException("Driver not found")).when(dbOperator).customDeletion(anyString());

        boolean result = doctorInstance.removeDoctorTime("t0001");
        assertFalse(result);
        verify(dbOperator, times(1)).customDeletion(anyString());
    }
    @Test
    public void testRemoveDoctorTimeByDayAndSlotClassNotFoundExceptionByMock() throws SQLException, ClassNotFoundException {
        doThrow(new ClassNotFoundException("Driver not found")).when(dbOperator).customDeletion(anyString());

        boolean result = doctorInstance.removeDoctorTime("Monday", "09:00-10:00");
        assertFalse(result);
        verify(dbOperator, times(1)).customDeletion(anyString());
    }
}