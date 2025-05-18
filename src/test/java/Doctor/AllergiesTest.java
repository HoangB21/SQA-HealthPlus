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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the allergies method in the Doctor class.
 * This class tests various scenarios for adding allergies to a patient's record.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class AllergiesTest {
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
     * Test case: ALG_01
     * Mục tiêu: Kiểm tra khả năng thêm dị ứng mới cho bệnh nhân khi dữ liệu hợp lệ
     * Input: allergies = "Penicillin", patientID = "P001", dữ liệu hiện tại trong DB là "Dust"
     * Expected Output: Trả về true, lệnh UPDATE được gọi để cập nhật dị ứng thành "Dust,Penicillin"
     * Ghi chú: Phủ nhánh thành công khi thêm dị ứng mới vào danh sách hiện có
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testAllergies_Success() throws Exception {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String patientID = "P001";
        String allergies = "Penicillin";

        // Giả lập dữ liệu trả về từ database: dị ứng hiện tại là "Dust"
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>()); // Header row
        ArrayList<String> row = new ArrayList<>();
        row.add("Dust"); // Giá trị hiện tại
        mockResult.add(row);

        // Mock hành vi của customSelection và customInsertion
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).customInsertion(anyString());

        // Act: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức allergies phải trả về true khi thêm dị ứng thành công");

        // Verify: Đảm bảo lệnh UPDATE được gọi với giá trị dị ứng mới
        verify(dbOperator).customInsertion(contains("Dust,Penicillin"));
    }

    /**
     * Test case: ALG_02
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn dị ứng hiện tại
     * Input: allergies = "Peanuts", patientID = "P002"
     * Expected Output: Trả về false, không thực hiện cập nhật
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testAllergies_DBException() throws Exception {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("DB error"));

        // Act: Gọi phương thức allergies
        boolean result = doctorInstance.allergies("Peanuts", "P002");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức allergies phải trả về false khi có lỗi SQLException");
    }

    /**
     * Test case: ALG_03
     * Mục tiêu: Kiểm tra xử lý lỗi ClassNotFoundException khi truy vấn dị ứng hiện tại
     * Input: allergies = "Peanuts", patientID = "P003"
     * Expected Output: Trả về false, không thực hiện cập nhật
     * Ghi chú: Phủ nhánh lỗi ClassNotFoundException khi truy vấn dữ liệu từ database
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testAllergies_ClassNotFoundException() throws Exception {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        when(dbOperator.customSelection(anyString())).thenThrow(new ClassNotFoundException("Driver not found"));

        // Act: Gọi phương thức allergies
        boolean result = doctorInstance.allergies("Peanuts", "P003");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức allergies phải trả về false khi có lỗi ClassNotFoundException");

        // Verify: Đảm bảo customSelection được gọi đúng một lần
        verify(dbOperator, times(1)).customSelection(anyString());
    }

    /**
     * Test case: ALG_04
     * Mục tiêu: Kiểm tra xử lý khi đầu vào allergies là null
     * Input: allergies = null, patientID = "pat001"
     * Expected Output: Trả về false, không gọi database
     * Ghi chú: Phủ nhánh xử lý đầu vào không hợp lệ (allergies là null)
     */
    @Test
    public void testAllergiesInvalidInput() {
        // Arrange: Chuẩn bị dữ liệu đầu vào
        String allergies = null;
        String patientID = "pat001";

        // Act: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức allergies phải trả về false vì allergies là null");

        // Verify: Đảm bảo không có tương tác với database
        verifyNoInteractions(dbOperator);
    }

    /**
     * Test case: ALG_05
     * Mục tiêu: Kiểm tra khả năng thêm dị ứng mới với dữ liệu hợp lệ
     * Input: allergies = "Peanuts", patientID = "pat001", dữ liệu hiện tại trong DB là "Penicillin"
     * Expected Output: Trả về true, lệnh UPDATE được gọi để cập nhật dị ứng thành "Penicillin,Peanuts"
     * Ghi chú: Phủ nhánh thành công khi thêm dị ứng mới vào danh sách hiện có, sử dụng BDDMockito
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenValidDataWhenAddingAllergiesThenUpdateSuccessfully() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String allergies = "Peanuts";
        String patientID = "pat001";
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList("drug_allergies_and_reactions")));
        data.add(new ArrayList<>(Arrays.asList("Penicillin")));
        given(dbOperator.customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';"))
                .willReturn(data);
        willDoNothing().given(dbOperator)
                .customInsertion("UPDATE patient SET drug_allergies_and_reactions = 'Penicillin,Peanuts' WHERE patient_id = '" + patientID + "';");

        // Act: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức allergies phải trả về true khi thêm dị ứng thành công");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator).customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';");
        verify(dbOperator).customInsertion("UPDATE patient SET drug_allergies_and_reactions = 'Penicillin,Peanuts' WHERE patient_id = '" + patientID + "';");
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: ALG_06
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn dị ứng hiện tại
     * Input: allergies = "Peanuts", patientID = "pat001"
     * Expected Output: Trả về false, không thực hiện cập nhật
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database, sử dụng BDDMockito
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenDatabaseErrorWhenAddingAllergiesThenReturnFalse() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String allergies = "Peanuts";
        String patientID = "pat001";
        given(dbOperator.customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';"))
                .willThrow(new SQLException("Database error"));

        // Act: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức allergies phải trả về false khi ném ngoại lệ SQLException");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator).customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';");
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: ALG_07
     * Mục tiêu: Kiểm tra xử lý khi dữ liệu trả về không hợp lệ (chỉ có header), gây IndexOutOfBoundsException
     * Input: allergies = "Peanuts", patientID = "pat001", dữ liệu trả về chỉ có header
     * Expected Output: Trả về false, không thực hiện cập nhật
     * Ghi chú: Phủ nhánh lỗi không được bắt IndexOutOfBoundsException khi dữ liệu không hợp lệ
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenInvalidDataWhenAddingAllergiesThenThrowExceptionAndReturnFalse() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String allergies = "Peanuts";
        String patientID = "pat001";
        ArrayList<ArrayList<String>> emptyData = new ArrayList<>();
        emptyData.add(new ArrayList<>(Arrays.asList("drug_allergies_and_reactions"))); // Chỉ có header
        given(dbOperator.customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';"))
                .willReturn(emptyData);

        // Act: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức allergies phải trả về false khi dữ liệu không hợp lệ gây IndexOutOfBoundsException");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator).customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';");
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: ALG_08
     * Mục tiêu: Kiểm tra xử lý khi đầu vào allergies là null, gây NullPointerException khi nối chuỗi
     * Input: allergies = null, patientID = "pat001", dữ liệu hiện tại trong DB là "Penicillin"
     * Expected Output: Trả về false, không thực hiện cập nhật
     * Ghi chú: Phủ nhánh lỗi không được bắt NullPointerException khi đầu vào null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenNullInputWhenAddingAllergiesThenHandleErrorGracefully() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String allergies = null;
        String patientID = "pat001";
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList("drug_allergies_and_reactions")));
        data.add(new ArrayList<>(Arrays.asList("Penicillin")));
        given(dbOperator.customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';"))
                .willReturn(data);

        // Act: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức allergies phải trả về false khi đầu vào null gây NullPointerException");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator).customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';");
        verifyNoMoreInteractions(dbOperator);
    }
}