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

public class UpdateAccountInfoTest {
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
    public void testUpdateAccountInfoSuccess() throws SQLException {
        // Trường hợp thành công với đầu vào hợp lệ
        boolean result = doctorInstance.updateAccountInfo("username newDoc#password newPass");
        assertTrue(result, "Phương thức updateAccountInfo phải trả về true khi cập nhật thành công");

        // Kiểm tra dữ liệu trong database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT username, password FROM sys_user WHERE user_id = (SELECT user_id FROM doctor WHERE slmc_reg_no = '22387')");
        assertTrue(rs.next(), "Phải có bản ghi trong bảng sys_user với user_id tương ứng với slmc_reg_no = '22387'");
        assertEquals("newDoc", rs.getString("username"), "Tên người dùng phải được cập nhật thành newDoc");
        assertEquals("newPass", rs.getString("password"), "Mật khẩu phải được cập nhật thành newPass");
        assertFalse(rs.next(), "Chỉ nên có một bản ghi được cập nhật");
    }

    @Test
    public void testUpdateAccountInfoException() throws SQLException {
        // Trường hợp SQLException: Đổi tên bảng để gây lỗi
        Statement stmt = connection.createStatement();
        stmt.execute("RENAME TABLE sys_user TO sys_user_temp");

        boolean result = doctorInstance.updateAccountInfo("username newDoc#password newPass");
        assertFalse(result, "Phương thức updateAccountInfo phải trả về false khi có lỗi cơ sở dữ liệu");

        // Khôi phục bảng
        stmt.execute("RENAME TABLE sys_user_temp TO sys_user");

        // Kiểm tra dữ liệu không thay đổi
        ResultSet rs = stmt.executeQuery("SELECT username, password FROM sys_user WHERE user_id = (SELECT user_id FROM doctor WHERE slmc_reg_no = '22387')");
        assertTrue(rs.next(), "Phải có bản ghi trong bảng sys_user với user_id tương ứng với slmc_reg_no = '22387'");
        // Giả sử giá trị ban đầu của username và password
        assertEquals("oldDoc", rs.getString("username"), "Tên người dùng không được thay đổi");
        assertEquals("oldPass", rs.getString("password"), "Mật khẩu không được thay đổi");
        assertFalse(rs.next(), "Chỉ nên có một bản ghi");

        // Trường hợp gây StringIndexOutOfBoundsException (info rỗng)
        assertThrows(StringIndexOutOfBoundsException.class, () -> {
            doctorInstance.updateAccountInfo("");
        }, "Phương thức updateAccountInfo phải ném StringIndexOutOfBoundsException khi đầu vào rỗng");

        // Trường hợp gây ArrayIndexOutOfBoundsException (info sai định dạng)
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            doctorInstance.updateAccountInfo("username#password newPass");
        }, "Phương thức updateAccountInfo phải ném ArrayIndexOutOfBoundsException khi định dạng đầu vào không hợp lệ");
    }
    @Test
    public void testUpdateAccountInfoExceptionByMock() throws SQLException, ClassNotFoundException {
        when(dbOperator.customInsertion(anyString())).thenThrow(new SQLException("Database error"));

        boolean result = doctorInstance.updateAccountInfo("username newDoc#password newPass");
        assertFalse(result);
    }
}