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
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the searchByName method in the Doctor class.
 * This class tests various scenarios for searching patients by name in the database.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class SearchByNameTest {
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
     * Test case: SBN_01
     * Mục tiêu: Kiểm tra khả năng tìm kiếm bệnh nhân với tên chỉ có một phần (first_name)
     * Input: namePart = "John", dữ liệu mock trả về với các cột "patient_id", "first_name", "last_name", "date_of_birth"
     * Expected Output: Trả về danh sách bệnh nhân với dữ liệu hợp lệ, ví dụ "pat001", "John", "Doe", "1990-01-01"
     * Ghi chú: Phủ nhánh thành công khi tìm kiếm với một phần tên
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testSearchByNameSinglePart() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> nameData = new ArrayList<>();
        nameData.add(new ArrayList<>(Arrays.asList("patient_id", "first_name", "last_name", "date_of_birth")));
        nameData.add(new ArrayList<>(Arrays.asList("pat001", "John", "Doe", "1990-01-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(nameData);

        // Act: Gọi phương thức searchByName
        ArrayList<ArrayList<String>> result = doctorInstance.searchByName("John");

        // Assert: Kiểm tra kết quả
        assertEquals("pat001", result.get(1).get(0), "Dữ liệu bệnh nhân đầu tiên phải có patient_id là 'pat001'");
    }

    /**
     * Test case: SBN_02
     * Mục tiêu: Kiểm tra khả năng tìm kiếm bệnh nhân với tên có hai phần (first_name và last_name)
     * Input: namePart = "John Doe", dữ liệu mock trả về với các cột "patient_id", "first_name", "last_name", "date_of_birth"
     * Expected Output: Trả về danh sách bệnh nhân với dữ liệu hợp lệ, ví dụ "pat001", "John", "Doe", "1990-01-01"
     * Ghi chú: Phủ nhánh thành công khi tìm kiếm với hai phần tên
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testSearchByNameTwoParts() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> nameData = new ArrayList<>();
        nameData.add(new ArrayList<>(Arrays.asList("patient_id", "first_name", "last_name", "date_of_birth")));
        nameData.add(new ArrayList<>(Arrays.asList("pat001", "John", "Doe", "1990-01-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(nameData);

        // Act: Gọi phương thức searchByName
        ArrayList<ArrayList<String>> result = doctorInstance.searchByName("John Doe");

        // Assert: Kiểm tra kết quả
        assertEquals("pat001", result.get(1).get(0), "Dữ liệu bệnh nhân đầu tiên phải có patient_id là 'pat001'");
    }

    /**
     * Test case: SBN_03
     * Mục tiêu: Kiểm tra khả năng tìm kiếm bệnh nhân với tên có hơn hai phần
     * Input: namePart = "John Doe Smith", dữ liệu mock trả về với các cột "patient_id", "first_name", "last_name", "date_of_birth"
     * Expected Output: Trả về danh sách bệnh nhân với dữ liệu hợp lệ, ví dụ "pat001", "John", "Doe", "1990-01-01"
     * Ghi chú: Phủ nhánh thành công khi tìm kiếm với hơn hai phần tên
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testSearchByNameMoreThanTwoParts() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> nameData = new ArrayList<>();
        nameData.add(new ArrayList<>(Arrays.asList("patient_id", "first_name", "last_name", "date_of_birth")));
        nameData.add(new ArrayList<>(Arrays.asList("pat001", "John", "Doe", "1990-01-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(nameData);

        // Act: Gọi phương thức searchByName
        ArrayList<ArrayList<String>> result = doctorInstance.searchByName("John Doe Smith");

        // Assert: Kiểm tra kết quả
        assertEquals("pat001", result.get(1).get(0), "Dữ liệu bệnh nhân đầu tiên phải có patient_id là 'pat001'");
    }

    /**
     * Test case: SBN_04
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi tìm kiếm bệnh nhân
     * Input: namePart = "John", dữ liệu mock ném SQLException
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testSearchByNameException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức searchByName
        ArrayList<ArrayList<String>> result = doctorInstance.searchByName("John");

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức searchByName phải trả về null khi có lỗi SQLException");
    }

    /**
     * Test case: SBN_05
     * Mục tiêu: Kiểm tra xử lý khi đầu vào chỉ chứa khoảng trắng
     * Input: namePart = "  ", không gọi database
     * Expected Output: Trả về null, không thực hiện truy vấn
     * Ghi chú: Phủ nhánh khi đầu vào không hợp lệ (chỉ chứa khoảng trắng)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testSearchByNameOnlyWhitespace() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào
        String namePart = "  "; // Chỉ chứa khoảng trắng

        // Act: Gọi phương thức searchByName
        ArrayList<ArrayList<String>> result = doctorInstance.searchByName(namePart);

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức searchByName phải trả về null vì namePart chỉ chứa khoảng trắng");

        // Verify: Đảm bảo không có tương tác với database
        verifyNoInteractions(dbOperator);
    }

    /**
     * Test case: SBN_06
     * Mục tiêu: Kiểm tra khả năng tìm kiếm bệnh nhân với tên có hơn hai từ
     * Input: namePart = "John Doe Extra", dữ liệu mock trả về với các cột "patient_id", "first_name", "last_name", "date_of_birth"
     * Expected Output: Trả về danh sách bệnh nhân với dữ liệu hợp lệ, ví dụ "pat001", "John", "Doe", "1990-01-01"
     * Ghi chú: Phủ nhánh thành công khi tìm kiếm với hơn hai từ, kiểm tra SQL chính xác
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testSearchByNameMoreThanTwoWords() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String namePart = "John Doe Extra";
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>(Arrays.asList("patient_id", "first_name", "last_name", "date_of_birth")));
        mockResult.add(new ArrayList<>(Arrays.asList("pat001", "John", "Doe", "1990-01-01")));
        when(dbOperator.customSelection("SELECT patient.patient_id, person.first_name, person.last_name, person.date_of_birth " +
                "FROM person INNER JOIN patient ON person.person_id = patient.person_id " +
                "WHERE person.first_name LIKE 'John%' AND person.last_name LIKE 'Doe%' " +
                "LIMIT 10;"))
                .thenReturn(mockResult);

        // Act: Gọi phương thức searchByName
        ArrayList<ArrayList<String>> result = doctorInstance.searchByName(namePart);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(2, result.size(), "Kết quả phải chứa 2 hàng (header và dữ liệu)");
        assertEquals("pat001", result.get(1).get(0), "Patient_id phải là 'pat001'");
        assertEquals("John", result.get(1).get(1), "First_name phải là 'John'");
        assertEquals("Doe", result.get(1).get(2), "Last_name phải là 'Doe'");
        assertEquals("1990-01-01", result.get(1).get(3), "Date_of_birth phải là '1990-01-01'");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT patient.patient_id, person.first_name, person.last_name, person.date_of_birth " +
                        "FROM person INNER JOIN patient ON person.person_id = patient.person_id " +
                        "WHERE person.first_name LIKE 'John%' AND person.last_name LIKE 'Doe%' " +
                        "LIMIT 10;");
        verifyNoMoreInteractions(dbOperator);
    }
}