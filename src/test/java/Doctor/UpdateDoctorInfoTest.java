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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class UpdateDoctorInfoTest {
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
    public void testUpdateDoctorInfoSuccess() throws SQLException {
        // Trường hợp thành công với đầu vào hợp lệ
        boolean result = doctorInstance.updateDoctorInfo("specialization Cardiology#experience 10 years");
        assertTrue(result, "Phương thức updateDoctorInfo phải trả về true khi cập nhật thành công");

        // Kiểm tra dữ liệu trong database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT specialization, experience FROM doctor WHERE slmc_reg_no = '22387'");
        assertTrue(rs.next(), "Phải có bản ghi trong bảng doctor với slmc_reg_no = '22387'");
        assertEquals("Cardiology", rs.getString("specialization"), "Chuyên môn phải được cập nhật thành Cardiology");
        assertEquals("10 years", rs.getString("experience"), "Kinh nghiệm phải được cập nhật thành 10 years");
        assertFalse(rs.next(), "Chỉ nên có một bản ghi được cập nhật");
    }

    @Test
    public void testUpdateDoctorInfoException() throws SQLException {
        // Trường hợp SQLException: Đổi tên bảng để gây lỗi
        Statement stmt = connection.createStatement();
        stmt.execute("RENAME TABLE doctor TO doctor_temp");

        boolean result = doctorInstance.updateDoctorInfo("specialization Cardiology#experience 10 years");
        assertFalse(result, "Phương thức updateDoctorInfo phải trả về false khi có lỗi cơ sở dữ liệu");

        // Khôi phục bảng
        stmt.execute("RENAME TABLE doctor_temp TO doctor");

        // Kiểm tra dữ liệu không thay đổi
        ResultSet rs = stmt.executeQuery("SELECT specialization, experience FROM doctor WHERE slmc_reg_no = '22387'");
        assertTrue(rs.next(), "Phải có bản ghi trong bảng doctor với slmc_reg_no = '22387'");
        // Giả sử giá trị ban đầu của specialization và experience
        assertEquals("Neurology", rs.getString("specialization"), "Chuyên môn không được thay đổi");
        assertEquals("5 years", rs.getString("experience"), "Kinh nghiệm không được thay đổi");
        assertFalse(rs.next(), "Chỉ nên có một bản ghi");

        // Trường hợp gây StringIndexOutOfBoundsException (info rỗng)
        assertThrows(StringIndexOutOfBoundsException.class, () -> {
            doctorInstance.updateDoctorInfo("");
        }, "Phương thức updateDoctorInfo phải ném StringIndexOutOfBoundsException khi đầu vào rỗng");

        // Trường hợp gây ArrayIndexOutOfBoundsException (info sai định dạng)
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            doctorInstance.updateDoctorInfo("specialization#experience 10 years");
        }, "Phương thức updateDoctorInfo phải ném ArrayIndexOutOfBoundsException khi định dạng đầu vào không hợp lệ");
    }
    @Test
    public void testUpdateDoctorInfoExceptionByMock() throws SQLException, ClassNotFoundException {
        when(dbOperator.customInsertion(anyString())).thenThrow(new SQLException("Database error"));

        boolean result = doctorInstance.updateDoctorInfo("specialization Cardiology#experience 10 years");
        assertFalse(result);
    }

}