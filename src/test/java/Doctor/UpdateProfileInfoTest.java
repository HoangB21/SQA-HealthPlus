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

public class UpdateProfileInfoTest {
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
    public void testUpdateProfileInfoSuccess() throws SQLException {
        boolean result = doctorInstance.updateProfileInfo("first_name John#last_name Doe");
        assertTrue(result, "Phương thức updateProfileInfo phải trả về true khi cập nhật thành công");
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT first_name, last_name FROM person WHERE person_id = 'hms00081'");
        assertTrue(rs.next(), "Phải có bản ghi trong bảng person với person_id = 'hms00081'");
        assertEquals("John", rs.getString("first_name"), "Tên phải được cập nhật thành John");
        assertEquals("Doe", rs.getString("last_name"), "Họ phải được cập nhật thành Doe");
        assertFalse(rs.next(), "Chỉ nên có một bản ghi được cập nhật");
    }
    @Test
    public void testUpdateProfileInfoException() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.execute("RENAME TABLE person TO person_temp");

        boolean result = doctorInstance.updateProfileInfo("first_name John#last_name Doe");
        assertFalse(result, "Phương thức updateProfileInfo phải trả về false khi có lỗi cơ sở dữ liệu");

        stmt.execute("RENAME TABLE person_temp TO person");

        ResultSet rs = stmt.executeQuery("SELECT first_name, last_name FROM person WHERE person_id = 'hms00081'");
        assertTrue(rs.next(), "Phải có bản ghi trong bảng person với person_id = 'hms00081'");
        assertEquals("Keerthi", rs.getString("first_name"), "Tên không được thay đổi");
        assertEquals("Perera", rs.getString("last_name"), "Họ không được thay đổi");
        assertFalse(rs.next(), "Chỉ nên có một bản ghi");

        assertThrows(StringIndexOutOfBoundsException.class, () -> {
            doctorInstance.updateProfileInfo("");
        }, "Phương thức updateProfileInfo phải ném StringIndexOutOfBoundsException khi đầu vào rỗng");

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            doctorInstance.updateProfileInfo("first_name#last_name Doe");
        }, "Phương thức updateProfileInfo phải ném ArrayIndexOutOfBoundsException khi định dạng đầu vào không hợp lệ");
    }
    @Test
    public void testUpdateProfileInfoExceptionByMock() throws SQLException, ClassNotFoundException {
        when(dbOperator.customInsertion(anyString())).thenThrow(new SQLException("Database error"));

        boolean result = doctorInstance.updateProfileInfo("first_name John#last_name Doe");
        assertFalse(result);
    }
}