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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the diagnose method in the Doctor class.
 * This class tests various scenarios for diagnosing a patient and adding the diagnosis to the medical history.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class DiagnoseTest {
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
     * Test case: DIAG_01
     * Mục tiêu: Kiểm tra khả năng chẩn đoán và thêm vào lịch sử y tế khi history_id cần đệm số 0
     * Input: diagnostic = "Diabetes", patientID = "pat001", dữ liệu mock trả về history_id = "his0001"
     * Expected Output: Trả về true, lệnh INSERT được gọi với history_id = "his0002"
     * Ghi chú: Phủ nhánh thành công với trường hợp history_id cần đệm số 0
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDiagnoseSuccessWithZeroPadding() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> maxHistoryData = new ArrayList<>();
        maxHistoryData.add(new ArrayList<>(Arrays.asList("history_id")));
        maxHistoryData.add(new ArrayList<>(Arrays.asList("his0001")));
        when(dbOperator.customSelection(anyString())).thenReturn(maxHistoryData);
        when(dbOperator.customInsertion(anyString())).thenReturn(true);

        // Act: Gọi phương thức diagnose
        boolean result = doctorInstance.diagnose("Diabetes", "pat001");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức diagnose phải trả về true khi thêm chẩn đoán thành công");

        // Verify: Đảm bảo lệnh INSERT được gọi với history_id mới
        verify(dbOperator, times(1)).customInsertion(contains("his0002"));
    }

    /**
     * Test case: DIAG_02
     * Mục tiêu: Kiểm tra khả năng chẩn đoán và thêm vào lịch sử y tế khi history_id không cần đệm số 0
     * Input: diagnostic = "Diabetes", patientID = "pat001", dữ liệu mock trả về history_id = "his0010"
     * Expected Output: Trả về true, lệnh INSERT được gọi với history_id = "his0011"
     * Ghi chú: Phủ nhánh thành công với trường hợp history_id không cần đệm số 0
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDiagnoseSuccessWithoutZeroPadding() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> maxHistoryData = new ArrayList<>();
        maxHistoryData.add(new ArrayList<>(Arrays.asList("history_id")));
        maxHistoryData.add(new ArrayList<>(Arrays.asList("his0010")));
        when(dbOperator.customSelection(anyString())).thenReturn(maxHistoryData);
        when(dbOperator.customInsertion(anyString())).thenReturn(true);

        // Act: Gọi phương thức diagnose
        boolean result = doctorInstance.diagnose("Diabetes", "pat001");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức diagnose phải trả về true khi thêm chẩn đoán thành công");

        // Verify: Đảm bảo lệnh INSERT được gọi với history_id mới
        verify(dbOperator, times(1)).customInsertion(contains("his0011"));
    }

    /**
     * Test case: DIAG_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn history_id
     * Input: diagnostic = "Diabetes", patientID = "pat001", dữ liệu mock ném SQLException
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDiagnoseException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức diagnose
        boolean result = doctorInstance.diagnose("Diabetes", "pat001");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức diagnose phải trả về false khi có lỗi SQLException");
    }

    /**
     * Test case: DIAG_04
     * Mục tiêu: Kiểm tra khả năng chẩn đoán khi history_id không hợp lệ (độ dài nhỏ hơn 4)
     * Input: diagnostic = "Flu", patientID = "pat001", dữ liệu mock trả về history_id = "his"
     * Expected Output: Trả về true, lệnh INSERT được gọi với history_id mặc định "his0001"
     * Ghi chú: Phủ nhánh xử lý khi history_id không hợp lệ, sử dụng giá trị mặc định
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testDiagnoseInvalidHistoryIDLength() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String diagnostic = "Flu";
        String patientID = "pat001";

        // Mock: historyID không hợp lệ (độ dài nhỏ hơn 4)
        ArrayList<ArrayList<String>> historyIDResult = new ArrayList<>();
        historyIDResult.add(new ArrayList<>(Arrays.asList("history_id")));
        historyIDResult.add(new ArrayList<>(Arrays.asList("his"))); // Độ dài 3
        when(dbOperator.customSelection("SELECT history_id FROM medical_history WHERE history_id = (SELECT MAX(history_id) FROM medical_history);"))
                .thenReturn(historyIDResult);

        // Mock lệnh insert thành công với history_id mặc định "his0001"
        String expectedSql = "INSERT INTO medical_history VALUES (" +
                "'his0001'," + patientID + "," + doctorInstance.slmcRegNo + "," +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) +
                "," + diagnostic + ");";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức diagnose
        boolean result = doctorInstance.diagnose(diagnostic, patientID);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức diagnose phải trả về true khi thêm chẩn đoán thành công với history_id không hợp lệ");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT history_id FROM medical_history WHERE history_id = (SELECT MAX(history_id) FROM medical_history);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
}