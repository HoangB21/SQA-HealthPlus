package Doctor;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the prescribe method in the Doctor class.
 * This class tests various scenarios for prescribing drugs and tests to a patient.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class PrescribeTest {
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
     * Test case: PRE_01
     * Mục tiêu: Kiểm tra khả năng kê đơn thành công với dữ liệu hợp lệ
     * Input: drugs = "paracetamol", tests = "bloodTest", patientID = "pat001", dữ liệu mock trả về prescription_id = "pres00042"
     * Expected Output: Trả về true, lệnh INSERT được gọi với prescription_id = "pres00043"
     * Ghi chú: Phủ nhánh thành công khi kê đơn với dữ liệu hợp lệ
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_SuccessfulInsert() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00042");
        mockResult.add(new ArrayList<>()); // row 0
        mockResult.add(row);               // row 1
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("paracetamol", "bloodTest", "pat001");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator).customSelection(contains("SELECT prescription_id"));
        verify(dbOperator).addTableRow(eq("prescription"), anyString());
    }

    /**
     * Test case: PRE_02
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn prescription_id
     * Input: drugs = "drug1", tests = "test1", patientID = "pat001", dữ liệu mock ném SQLException
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_FailsOnSQLException() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("DB error"));

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("drug1", "test1", "pat001");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức prescribe phải trả về false khi có lỗi SQLException");
    }

    /**
     * Test case: PRE_03
     * Mục tiêu: Kiểm tra khả năng kê đơn khi cả drugs và tests đều rỗng
     * Input: drugs = "", tests = "", patientID = "pat002", dữ liệu mock trả về prescription_id = "pres00099"
     * Expected Output: Trả về true, lệnh INSERT được gọi với drugs và tests là "NULL"
     * Ghi chú: Phủ nhánh khi cả drugs và tests đều rỗng
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_EmptyDrugsAndTests() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00099");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("", "", "pat002");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với drugs và tests rỗng");

        // Verify: Đảm bảo lệnh INSERT được gọi với drugs và tests là "NULL"
        verify(dbOperator).addTableRow(eq("prescription"), contains("NULL"));
    }

    /**
     * Test case: PRE_04
     * Mục tiêu: Kiểm tra khả năng kê đơn khi drugs rỗng, tests hợp lệ
     * Input: drugs = "", tests = "xray", patientID = "pat003", dữ liệu mock trả về prescription_id = "pres00008"
     * Expected Output: Trả về true, lệnh INSERT được gọi với drugs là "NULL"
     * Ghi chú: Phủ nhánh khi drugs rỗng và tests hợp lệ
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_EmptyDrugs_ShouldSetTestsToNULL() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00008");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("", "xray", "pat003");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với drugs rỗng");

        // Verify: Đảm bảo lệnh INSERT được gọi với drugs là "NULL"
        verify(dbOperator).addTableRow(eq("prescription"), contains("NULL"));
    }

    /**
     * Test case: PRE_05
     * Mục tiêu: Kiểm tra xử lý khi customSelection trả về null
     * Input: drugs = "drugB", tests = "testB", patientID = "pat006", dữ liệu mock trả về null
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh khi customSelection trả về null
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_NullResultFromCustomSelection_ShouldFailGracefully() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("drugB", "testB", "pat006");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức prescribe phải trả về false khi customSelection trả về null");
    }

    /**
     * Test case: PRE_06
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi insert dữ liệu
     * Input: drugs = "drugD", tests = "testD", patientID = "pat008", dữ liệu mock trả về prescription_id = "pres00015", addTableRow ném SQLException
     * Expected Output: Trả về false, không hoàn thành insert
     * Ghi chú: Phủ nhánh lỗi SQLException khi insert dữ liệu vào database
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_SQLExceptionWhenAddTableRow_ShouldReturnFalse() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00015");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doThrow(new SQLException("Error")).when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("drugD", "testD", "pat008");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức prescribe phải trả về false khi addTableRow ném SQLException");
    }

    /**
     * Test case: PRE_07
     * Mục tiêu: Kiểm tra khả năng kê đơn khi tests rỗng, drugs hợp lệ
     * Input: drugs = "paracetamol", tests = "", patientID = "pat004", dữ liệu mock trả về prescription_id = "pres00008"
     * Expected Output: Trả về true, lệnh INSERT được gọi với tests là "NULL"
     * Ghi chú: Phủ nhánh khi tests rỗng và drugs hợp lệ
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_EmptyTests_ShouldSetTestsToNULL() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00008");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("paracetamol", "", "pat004");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với tests rỗng");

        // Verify: Đảm bảo lệnh INSERT được gọi với tests là "NULL"
        verify(dbOperator).addTableRow(eq("prescription"), contains("NULL"));
    }

    /**
     * Test case: PRE_08
     * Mục tiêu: Kiểm tra khả năng kê đơn khi drugs rỗng, tests hợp lệ
     * Input: drugs = "", tests = "blood test", patientID = "pat005", dữ liệu mock trả về prescription_id = "pres00009"
     * Expected Output: Trả về true, lệnh INSERT được gọi với drugs là "NULL"
     * Ghi chú: Phủ nhánh khi drugs rỗng và tests hợp lệ
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_EmptyDrugs_ShouldSetDrugsToNULL() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00009");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("", "blood test", "pat005");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với drugs rỗng");

        // Verify: Đảm bảo lệnh INSERT được gọi với drugs là "NULL"
        verify(dbOperator).addTableRow(eq("prescription"), contains("NULL"));
    }

    /**
     * Test case: PRE_09
     * Mục tiêu: Kiểm tra khả năng kê đơn khi cả drugs và tests đều rỗng
     * Input: drugs = "", tests = "", patientID = "pat006", dữ liệu mock trả về prescription_id = "pres00010"
     * Expected Output: Trả về true, lệnh INSERT được gọi với cả drugs và tests là "NULL"
     * Ghi chú: Phủ nhánh khi cả drugs và tests đều rỗng, kiểm tra SQL bằng ArgumentCaptor
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_BothEmpty_ShouldSetBothToNULL() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00010");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("", "", "pat006");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với cả drugs và tests rỗng");

        // Verify: Sử dụng ArgumentCaptor để kiểm tra SQL được truyền vào
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(dbOperator).addTableRow(eq("prescription"), sqlCaptor.capture());
        String capturedSql = sqlCaptor.getValue();
        assertTrue(capturedSql.contains("pat006"), "SQL phải chứa patientID 'pat006'");
        assertTrue(capturedSql.contains("NULL"), "SQL phải chứa 'NULL' cho drugs và tests");
        assertTrue(capturedSql.contains("pres"), "SQL phải chứa prefix 'pres' cho prescription_id");
    }

    /**
     * Test case: PRE_10
     * Mục tiêu: Kiểm tra khả năng kê đơn khi tests rỗng, drugs hợp lệ, kiểm tra SQL
     * Input: drugs = "Panadol", tests = "", patientID = "pat001", dữ liệu mock trả về prescription_id = "pres00010"
     * Expected Output: Trả về true, lệnh INSERT được gọi với tests là "NULL"
     * Ghi chú: Phủ nhánh khi tests rỗng và drugs hợp lệ, kiểm tra SQL bằng ArgumentCaptor
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_EmptyTestsOnly_ShouldSetTestsToNULL() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00010");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("Panadol", "", "pat001");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với tests rỗng");

        // Verify: Sử dụng ArgumentCaptor để kiểm tra SQL được truyền vào
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(dbOperator).addTableRow(eq("prescription"), sqlCaptor.capture());
        String capturedSql = sqlCaptor.getValue();
        assertTrue(capturedSql.contains("Panadol"), "SQL phải chứa drugs 'Panadol'");
        assertTrue(capturedSql.contains("NULL"), "SQL phải chứa 'NULL' cho tests");
    }

    /**
     * Test case: PRE_11
     * Mục tiêu: Kiểm tra khả năng kê đơn khi drugs rỗng, tests hợp lệ, kiểm tra SQL
     * Input: drugs = "", tests = "BloodTest", patientID = "pat002", dữ liệu mock trả về prescription_id = "pres00010"
     * Expected Output: Trả về true, lệnh INSERT được gọi với drugs là "NULL"
     * Ghi chú: Phủ nhánh khi drugs rỗng và tests hợp lệ, kiểm tra SQL bằng ArgumentCaptor
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_EmptyDrugsOnly_ShouldSetDrugsToNULL() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00010");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("", "BloodTest", "pat002");

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với drugs rỗng");

        // Verify: Sử dụng ArgumentCaptor để kiểm tra SQL được truyền vào
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(dbOperator).addTableRow(eq("prescription"), sqlCaptor.capture());
        String capturedSql = sqlCaptor.getValue();
        assertTrue(capturedSql.contains("BloodTest"), "SQL phải chứa tests 'BloodTest'");
        assertTrue(capturedSql.contains("NULL"), "SQL phải chứa 'NULL' cho drugs");
    }

    /**
     * Test case: PRE_12
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn prescription_id
     * Input: drugs = "A", tests = "B", patientID = "pat007", dữ liệu mock ném SQLException
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_DBException_ShouldReturnFalse() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("DB error"));

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("A", "B", "pat007");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức prescribe phải trả về false khi có lỗi SQLException");
    }

    /**
     * Test case: PRE_13
     * Mục tiêu: Kiểm tra xử lý khi prescription_id có định dạng không hợp lệ
     * Input: drugs = "drugA", tests = "testA", patientID = "pat005", dữ liệu mock trả về prescription_id = "invalidFormat"
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh lỗi NumberFormatException khi định dạng prescription_id không hợp lệ
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_InvalidPrescIDFormat_ShouldFailGracefully() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>());
        mockResult.add(new ArrayList<>(Arrays.asList("invalidFormat"))); // Định dạng không hợp lệ
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("drugA", "testA", "pat005");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức prescribe phải trả về false khi prescription_id có định dạng không hợp lệ");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1)).customSelection(anyString());
    }

    /**
     * Test case: PRE_14
     * Mục tiêu: Kiểm tra xử lý khi danh sách kết quả rỗng (chỉ có header)
     * Input: drugs = "drugC", tests = "testC", patientID = "pat007", dữ liệu mock trả về chỉ có header
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh lỗi IndexOutOfBoundsException khi danh sách kết quả rỗng
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_EmptyResultList_ShouldFailGracefully() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>()); // Chỉ có hàng tiêu đề, không có dữ liệu
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("drugC", "testC", "pat007");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức prescribe phải trả về false khi danh sách kết quả rỗng gây IndexOutOfBoundsException");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1)).customSelection(anyString());
    }

    /**
     * Test case: PRE_15
     * Mục tiêu: Kiểm tra xử lý lỗi ClassNotFoundException khi truy vấn prescription_id
     * Input: drugs = "drugE", tests = "testE", patientID = "pat009", dữ liệu mock ném ClassNotFoundException
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh lỗi ClassNotFoundException khi truy vấn dữ liệu từ database
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testPrescribe_ClassNotFoundException() throws Exception {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new ClassNotFoundException("Driver not found"));

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe("drugE", "testE", "pat009");

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức prescribe phải trả về false khi có lỗi ClassNotFoundException");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1)).customSelection(anyString());
    }

    /**
     * Test case: PRE_16
     * Mục tiêu: Kiểm tra khả năng kê đơn với prescription_id không hợp lệ (độ dài nhỏ hơn 5)
     * Input: drugs = "DrugA", tests = "TestA", patientID = "pat001", dữ liệu mock trả về prescription_id = "pres"
     * Expected Output: Trả về true, lệnh INSERT được gọi với prescription_id mặc định "pres00001"
     * Ghi chú: Phủ nhánh xử lý khi prescription_id không hợp lệ, sử dụng giá trị mặc định
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testPrescribeInvalidPrescIDLength() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String drugs = "DrugA";
        String tests = "TestA";
        String patientID = "pat001";
        ArrayList<ArrayList<String>> prescIDResult = new ArrayList<>();
        prescIDResult.add(new ArrayList<>(Arrays.asList("prescription_id")));
        prescIDResult.add(new ArrayList<>(Arrays.asList("pres"))); // Độ dài 4
        when(dbOperator.customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);"))
                .thenReturn(prescIDResult);
        String expectedSql = "pres00001,pat001," + doctorInstance.slmcRegNo + ","
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + ",DrugA,TestA";
        when(dbOperator.addTableRow("prescription", expectedSql)).thenReturn(true);

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe(drugs, tests, patientID);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với prescription_id không hợp lệ");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);");
        verify(dbOperator, times(1)).addTableRow("prescription", expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: PRE_17
     * Mục tiêu: Kiểm tra khả năng kê đơn khi tests rỗng, drugs hợp lệ
     * Input: drugs = "DrugB", tests = "", patientID = "pat002", dữ liệu mock trả về prescription_id = "pres00001"
     * Expected Output: Trả về true, lệnh INSERT được gọi với tests là "NULL"
     * Ghi chú: Phủ nhánh khi tests rỗng và drugs hợp lệ
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testPrescribeEmptyTests() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String drugs = "DrugB";
        String tests = ""; // tests rỗng
        String patientID = "pat002";
        ArrayList<ArrayList<String>> prescIDResult = new ArrayList<>();
        prescIDResult.add(new ArrayList<>(Arrays.asList("prescription_id")));
        prescIDResult.add(new ArrayList<>(Arrays.asList("pres00001")));
        when(dbOperator.customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);"))
                .thenReturn(prescIDResult);
        String expectedSql = "pres00002,pat002," + doctorInstance.slmcRegNo + ","
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + ",DrugB,NULL";
        when(dbOperator.addTableRow("prescription", expectedSql)).thenReturn(true);

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe(drugs, tests, patientID);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với tests rỗng");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);");
        verify(dbOperator, times(1)).addTableRow("prescription", expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: PRE_18
     * Mục tiêu: Kiểm tra khả năng kê đơn khi drugs rỗng, tests hợp lệ
     * Input: drugs = "", tests = "TestC", patientID = "pat003", dữ liệu mock trả về prescription_id = "pres00002"
     * Expected Output: Trả về true, lệnh INSERT được gọi với drugs là "NULL"
     * Ghi chú: Phủ nhánh khi drugs rỗng và tests hợp lệ
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testPrescribeEmptyDrugs() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String drugs = ""; // drugs rỗng
        String tests = "TestC";
        String patientID = "pat003";
        ArrayList<ArrayList<String>> prescIDResult = new ArrayList<>();
        prescIDResult.add(new ArrayList<>(Arrays.asList("prescription_id")));
        prescIDResult.add(new ArrayList<>(Arrays.asList("pres00002")));
        when(dbOperator.customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);"))
                .thenReturn(prescIDResult);
        String expectedSql = "pres00003,pat003," + doctorInstance.slmcRegNo + ","
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + ",NULL,TestC";
        when(dbOperator.addTableRow("prescription", expectedSql)).thenReturn(true);

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe(drugs, tests, patientID);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức prescribe phải trả về true khi kê đơn thành công với drugs rỗng");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);");
        verify(dbOperator, times(1)).addTableRow("prescription", expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: PRE_19
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn prescription_id
     * Input: drugs = "DrugD", tests = "TestD", patientID = "pat004", dữ liệu mock ném SQLException
     * Expected Output: Trả về false, không thực hiện insert
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testPrescribeSQLException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String drugs = "DrugD";
        String tests = "TestD";
        String patientID = "pat004";
        when(dbOperator.customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);"))
                .thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức prescribe
        boolean result = doctorInstance.prescribe(drugs, tests, patientID);

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức prescribe phải trả về false khi có lỗi SQLException");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);");
        verifyNoMoreInteractions(dbOperator);
    }
}