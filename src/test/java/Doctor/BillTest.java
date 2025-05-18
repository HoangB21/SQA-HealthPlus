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
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the bill method in the Doctor class.
 * This class tests various scenarios for billing a patient, including updating existing bills and creating new ones.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class BillTest {
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
     * Test case: BILL_01
     * Mục tiêu: Kiểm tra khả năng cập nhật hóa đơn cho bệnh nhân hiện có
     * Input: billInfo = "dummy_field dummy_value", patientID = "pat001", labFee = "1000"
     * Expected Output: Trả về true, lệnh UPDATE được gọi để cập nhật laboratory_fee
     * Ghi chú: Phủ nhánh thành công khi bệnh nhân đã có hóa đơn và cập nhật phí phòng lab
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillUpdateExistingPatient() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "dummy_field dummy_value"; // Không dùng trong trường hợp update
        String patientID = "pat001";
        String labFee = "1000";

        // Mock truy vấn tmp_bill_id theo patientID
        ArrayList<ArrayList<String>> tmpBillResult = new ArrayList<>();
        tmpBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        tmpBillResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"))
                .thenReturn(tmpBillResult);

        // Mock lệnh update thành công
        when(dbOperator.customInsertion("UPDATE tmp_bill SET laboratory_fee = '1000' WHERE tmp_bill_id = 'hms0001tb';"))
                .thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi cập nhật hóa đơn thành công");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';");
        verify(dbOperator, times(1))
                .customInsertion("UPDATE tmp_bill SET laboratory_fee = '1000' WHERE tmp_bill_id = 'hms0001tb';");
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_03
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới khi truy vấn đầu tiên thất bại (SQLException) nhưng vẫn tạo mới thành công
     * Input: billInfo = "patient_id pat002, appointment_fee 600", patientID = "pat002", labFee = "1500"
     * Expected Output: Trả về true, lệnh INSERT được gọi để tạo hóa đơn mới
     * Ghi chú: Phủ nhánh xử lý SQLException từ truy vấn đầu tiên và tạo hóa đơn mới
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillSQLExceptionOnFirstQuery() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat002, appointment_fee 600";
        String patientID = "pat002";
        String labFee = "1500";

        // Mock SQLException cho truy vấn đầu tiên
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat002';"))
                .thenThrow(new SQLException("Database error"));

        // Mock truy vấn tmp_bill_id lớn nhất
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0010tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công
        String expectedSql = "INSERT INTO tmp_bill (patient_id,appointment_fee,tmp_bill_id) " +
                "VALUES ('pat002','600','hms0011tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công sau khi truy vấn đầu tiên thất bại");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat002';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_05
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới với tmp_bill_id có số không đầu (zero padding)
     * Input: billInfo = "patient_id pat004, doctor_fee 2000", patientID = "hms0001pa", labFee = "2500"
     * Expected Output: Trả về true, lệnh INSERT được gọi để tạo hóa đơn mới với tmp_bill_id = "hms0001tb"
     * Ghi chú: Phủ nhánh tạo tmp_bill_id với giá trị lớn nhất có số không đầu (hms0000tb)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillCreateNewWithZeroPadding() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat004, doctor_fee 2000";
        String patientID = "hms0001pa";
        String labFee = "2500";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'hms0001pa';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id lớn nhất là "hms0000tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0000tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0001tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,doctor_fee,tmp_bill_id) " +
                "VALUES ('pat004','2000','hms0001tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công với tmp_bill_id có số không đầu");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'hms0001pa';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_06
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới với tmp_bill_id không có số không đầu
     * Input: billInfo = "patient_id pat005, doctor_fee 1500", patientID = "pat005", labFee = "2000"
     * Expected Output: Trả về true, lệnh INSERT được gọi để tạo hóa đơn mới với tmp_bill_id = "hms0011tb"
     * Ghi chú: Phủ nhánh tạo tmp_bill_id không có số không đầu (zero padding)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillCreateNewWithNonZeroBillID() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat005, doctor_fee 1500";
        String patientID = "pat005";
        String labFee = "2000";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat005';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id lớn nhất là "hms0010tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0010tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0011tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,doctor_fee,tmp_bill_id) " +
                "VALUES ('pat005','1500','hms0011tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công với tmp_bill_id không có số không đầu");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat005';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_07
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới với nhiều trường dữ liệu
     * Input: billInfo = "patient_id pat001, appointment_fee 500, doctor_fee 1000", patientID = "pat001", labFee = "2000"
     * Expected Output: Trả về true, lệnh INSERT được gọi để tạo hóa đơn mới với nhiều trường
     * Ghi chú: Phủ nhánh tạo hóa đơn mới với nhiều trường dữ liệu (patient_id, appointment_fee, doctor_fee)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillCreateNewPatient() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat001, appointment_fee 500, doctor_fee 1000"; // 3 cặp giá trị
        String patientID = "pat001";
        String labFee = "2000";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"))
                .thenThrow(new SQLException("No record found"));

        // Mock: tmp_bill_id lớn nhất là "hms0001tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0002tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,appointment_fee,doctor_fee,tmp_bill_id) " +
                "VALUES ('pat001','500','1000','hms0002tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công với nhiều trường dữ liệu");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_08
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới với hai trường dữ liệu
     * Input: billInfo = "patient_id pat006, appointment_fee 600", patientID = "pat006", labFee = "1500"
     * Expected Output: Trả về true, lệnh INSERT được gọi để tạo hóa đơn mới với hai trường
     * Ghi chú: Phủ nhánh tạo hóa đơn mới với hai trường dữ liệu (patient_id, appointment_fee)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillWithTwoFields() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat006, appointment_fee 600"; // 2 cặp giá trị
        String patientID = "pat006";
        String labFee = "1500";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat006';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id lớn nhất là "hms0005tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0005tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0006tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,appointment_fee,tmp_bill_id) " +
                "VALUES ('pat006','600','hms0006tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công với hai trường dữ liệu");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat006';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_09
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới khi billInfo thiếu giá trị (missing value)
     * Input: billInfo = "patient_id pat012, appointment_fee ", patientID = "pat012", labFee = "2000"
     * Expected Output: Trả về true, lệnh INSERT được gọi chỉ với patient_id và tmp_bill_id
     * Ghi chú: Phủ nhánh xử lý khi billInfo thiếu giá trị, chỉ tạo hóa đơn với các trường hợp lệ
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillInvalidBillInfoMissingValue() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat012, appointment_fee "; // Thiếu giá trị sau "appointment_fee"
        String patientID = "pat012";
        String labFee = "2000";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat012';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id lớn nhất là "hms0004tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0004tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0005tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,tmp_bill_id) VALUES ('pat012','hms0005tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công mặc dù billInfo thiếu giá trị");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat012';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_10
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi insert hóa đơn mới
     * Input: billInfo = "patient_id pat013, doctor_fee 3000", patientID = "pat013", labFee = "2500"
     * Expected Output: Trả về false, không tạo hóa đơn mới
     * Ghi chú: Phủ nhánh lỗi SQLException khi insert hóa đơn mới
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillSQLExceptionOnInsert() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat013, doctor_fee 3000";
        String patientID = "pat013";
        String labFee = "2500";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat013';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id lớn nhất là "hms0005tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0005tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock: Lệnh insert ném SQLException
        String expectedSql = "INSERT INTO tmp_bill (patient_id,doctor_fee,tmp_bill_id) VALUES ('pat013','3000','hms0006tb');";
        when(dbOperator.customInsertion(expectedSql)).thenThrow(new SQLException("Insert failed"));

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertFalse(result, "Phương thức bill phải trả về false khi insert hóa đơn mới thất bại do SQLException");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat013';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_11
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException ở cấp độ chính (main block)
     * Input: billInfo = "patient_id pat003", patientID = "pat003", labFee = "3000"
     * Expected Output: Trả về true, không tạo hóa đơn mới
     * Ghi chú: Phủ nhánh lỗi SQLException ở cấp độ chính, trả về true theo logic mã nguồn
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillSQLExceptionOnMainBlock() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat003";
        String patientID = "pat003";
        String labFee = "3000";

        // Mock: Ném SQLException ở cấp độ cao nhất
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Critical database error"));

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true nếu có ngoại lệ ở cấp độ cao nhất theo logic mã nguồn");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat003';");
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_12
     * Mục tiêu: Kiểm tra khả năng cập nhật hóa đơn thất bại nhưng vẫn trả về true
     * Input: billInfo = "dummy_field dummy_value", patientID = "pat011", labFee = "1200"
     * Expected Output: Trả về true, lệnh UPDATE được gọi nhưng thất bại
     * Ghi chú: Phủ nhánh khi cập nhật hóa đơn thất bại, mã nguồn vẫn trả về true
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillUpdateExistingPatientFailed() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "dummy_field dummy_value";
        String patientID = "pat011";
        String labFee = "1200";

        // Mock truy vấn tmp_bill_id theo patientID
        ArrayList<ArrayList<String>> tmpBillResult = new ArrayList<>();
        tmpBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        tmpBillResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat011';"))
                .thenReturn(tmpBillResult);

        // Mock lệnh update thất bại
        when(dbOperator.customInsertion("UPDATE tmp_bill SET laboratory_fee = '1200' WHERE tmp_bill_id = 'hms0001tb';"))
                .thenReturn(false);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true ngay cả khi cập nhật hóa đơn thất bại theo logic mã nguồn");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat011';");
        verify(dbOperator, times(1))
                .customInsertion("UPDATE tmp_bill SET laboratory_fee = '1200' WHERE tmp_bill_id = 'hms0001tb';");
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_13
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới khi billInfo rỗng
     * Input: billInfo = "", patientID = "pat008", labFee = "2200"
     * Expected Output: Trả về true, lệnh INSERT được gọi chỉ với tmp_bill_id
     * Ghi chú: Phủ nhánh xử lý khi billInfo rỗng, chỉ tạo hóa đơn với tmp_bill_id
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillEmptyBillInfo() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "";
        String patientID = "pat008";
        String labFee = "2200";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat008';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id lớn nhất là "hms0003tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0003tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0004tb"
        String expectedSql = "INSERT INTO tmp_bill (tmp_bill_id) VALUES ('hms0004tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công với billInfo rỗng");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat008';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_14
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới với một trường dữ liệu
     * Input: billInfo = "patient_id pat009", patientID = "pat009", labFee = "1800"
     * Expected Output: Trả về true, lệnh INSERT được gọi chỉ với patient_id và tmp_bill_id
     * Ghi chú: Phủ nhánh tạo hóa đơn mới với một trường dữ liệu (patient_id)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillWithOneField() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat009";
        String patientID = "pat009";
        String labFee = "1800";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat009';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id lớn nhất là "hms0004tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0004tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0005tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,tmp_bill_id) VALUES ('pat009','hms0005tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công với một trường dữ liệu");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat009';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_15
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới với bốn trường dữ liệu
     * Input: billInfo = "patient_id pat010, appointment_fee 500, doctor_fee 1000, extra_fee 200", patientID = "pat010", labFee = "2000"
     * Expected Output: Trả về true, lệnh INSERT được gọi với bốn trường dữ liệu
     * Ghi chú: Phủ nhánh tạo hóa đơn mới với bốn trường dữ liệu (patient_id, appointment_fee, doctor_fee, extra_fee)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillWithFourFields() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat010, appointment_fee 500, doctor_fee 1000, extra_fee 200";
        String patientID = "pat010";
        String labFee = "2000";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat010';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id lớn nhất là "hms0005tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0005tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0006tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,appointment_fee,doctor_fee,extra_fee,tmp_bill_id) " +
                "VALUES ('pat010','500','1000',200,'hms0006tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công với bốn trường dữ liệu");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat010';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_16
     * Mục tiêu: Kiểm tra xử lý lỗi ClassNotFoundException khi truy vấn
     * Input: billInfo = "patient_id pat014, doctor_fee 3000", patientID = "pat014", labFee = "2500"
     * Expected Output: Trả về true, không tạo hóa đơn mới
     * Ghi chú: Phủ nhánh lỗi ClassNotFoundException, trả về true theo logic mã nguồn
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillClassNotFoundException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat014, doctor_fee 3000";
        String patientID = "pat014";
        String labFee = "2500";

        // Mock: Ném ClassNotFoundException khi truy vấn
        when(dbOperator.customSelection(anyString())).thenThrow(new ClassNotFoundException("Driver not found"));

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true nếu có ClassNotFoundException theo logic mã nguồn");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat014';");
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: BILL_17
     * Mục tiêu: Kiểm tra khả năng tạo hóa đơn mới khi tmp_bill_id không hợp lệ (độ dài nhỏ hơn 4)
     * Input: billInfo = "patient_id pat014, doctor_fee 3500", patientID = "pat014", labFee = "2800"
     * Expected Output: Trả về true, lệnh INSERT được gọi với tmp_bill_id mặc định "hms0001tb"
     * Ghi chú: Phủ nhánh xử lý khi tmp_bill_id không hợp lệ, sử dụng giá trị mặc định
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testBillInvalidBillIDLength() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String billInfo = "patient_id pat014, doctor_fee 3500";
        String patientID = "pat014";
        String labFee = "2800";

        // Mock: Không tìm thấy hóa đơn cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat014';"))
                .thenThrow(new SQLException("No record"));

        // Mock: tmp_bill_id không hợp lệ (độ dài nhỏ hơn 4)
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms"))); // tmp_bill_id không hợp lệ
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock lệnh insert thành công với tmpID2 = "hms0001tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,doctor_fee,tmp_bill_id) VALUES ('pat014','3500','hms0001tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        // Act: Gọi phương thức bill
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert: Kiểm tra kết quả
        assertTrue(result, "Phương thức bill phải trả về true khi tạo hóa đơn mới thành công với tmp_bill_id không hợp lệ");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat014';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
}