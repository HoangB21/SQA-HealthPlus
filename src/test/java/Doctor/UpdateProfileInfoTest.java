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

/**
 * Unit test class for testing the updateProfileInfo method in the Doctor class.
 * This class tests various scenarios for updating the doctor's profile information in the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class UpdateProfileInfoTest {
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
     * Test case: UPI_01
     * Mục tiêu: Kiểm tra khả năng cập nhật thông tin hồ sơ bác sĩ thành công với đầu vào hợp lệ
     * Input: info = "first_name John#last_name Doe", bảng person có bản ghi với person_id = 'hms00081'
     * Expected Output: Trả về true, thông tin trong database được cập nhật thành "John", "Doe"
     * Ghi chú: Phủ nhánh thành công khi cập nhật thông tin hồ sơ bác sĩ
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void testUpdateProfileInfoSuccess() throws SQLException {
        // Arrange: Không cần mock vì sử dụng database trực tiếp
        // Act: Gọi phương thức updateProfileInfo
        boolean result = doctorInstance.updateProfileInfo("first_name John#last_name Doe");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức updateProfileInfo phải trả về true khi cập nhật thành công");

        // Check database: Kiểm tra dữ liệu đã được cập nhật
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT first_name, last_name FROM person WHERE person_id = 'hms00081'");
        assertTrue(rs.next(), "Phải có bản ghi trong bảng person với person_id = 'hms00081'");
        assertEquals("John", rs.getString("first_name"), "Tên phải được cập nhật thành John");
        assertEquals("Doe", rs.getString("last_name"), "Họ phải được cập nhật thành Doe");
        assertFalse(rs.next(), "Chỉ nên có một bản ghi được cập nhật");
    }

    /**
     * Test case: UPI_02
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException và các ngoại lệ đầu vào không hợp lệ
     * Input: info = "first_name John#last_name Doe" (SQLException), "" (StringIndexOutOfBoundsException), "first_name#last_name Doe" (ArrayIndexOutOfBoundsException)
     * Expected Output: Trả về false (SQLException), ném StringIndexOutOfBoundsException (input rỗng), ném ArrayIndexOutOfBoundsException (input sai định dạng)
     * Ghi chú: Phủ nhánh lỗi SQLException và các ngoại lệ đầu vào không hợp lệ
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void testUpdateProfileInfoException() throws SQLException {
        // Arrange: Gây lỗi SQLException bằng cách đổi tên bảng
        Statement stmt = connection.createStatement();
        stmt.execute("RENAME TABLE person TO person_temp");

        // Act & Assert: Kiểm tra trường hợp SQLException
        boolean result = doctorInstance.updateProfileInfo("first_name John#last_name Doe");
        assertFalse(result, "Phương thức updateProfileInfo phải trả về false khi có lỗi cơ sở dữ liệu");

        // Khôi phục bảng
        stmt.execute("RENAME TABLE person_temp TO person");

        // Check database: Đảm bảo dữ liệu không thay đổi
        ResultSet rs = stmt.executeQuery("SELECT first_name, last_name FROM person WHERE person_id = 'hms00081'");
        assertTrue(rs.next(), "Phải có bản ghi trong bảng person với person_id = 'hms00081'");
        assertEquals("Keerthi", rs.getString("first_name"), "Tên không được thay đổi");
        assertEquals("Perera", rs.getString("last_name"), "Họ không được thay đổi");
        assertFalse(rs.next(), "Chỉ nên có một bản ghi");

        // Act & Assert: Kiểm tra trường hợp StringIndexOutOfBoundsException (input rỗng)
        assertThrows(StringIndexOutOfBoundsException.class, () -> {
            doctorInstance.updateProfileInfo("");
        }, "Phương thức updateProfileInfo phải ném StringIndexOutOfBoundsException khi đầu vào rỗng");

        // Act & Assert: Kiểm tra trường hợp ArrayIndexOutOfBoundsException (input sai định dạng)
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            doctorInstance.updateProfileInfo("first_name#last_name Doe");
        }, "Phương thức updateProfileInfo phải ném ArrayIndexOutOfBoundsException khi định dạng đầu vào không hợp lệ");
    }

    /**
     * Test case: UPI_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException bằng mock
     * Input: info = "first_name John#last_name Doe", mock ném SQLException
     * Expected Output: Trả về false
     * Ghi chú: Phủ nhánh lỗi SQLException khi sử dụng mock
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testUpdateProfileInfoExceptionByMock() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customInsertion(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức updateProfileInfo
        boolean result = doctorInstance.updateProfileInfo("first_name John#last_name Doe");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức updateProfileInfo phải trả về false khi customInsertion ném SQLException");
    }
}