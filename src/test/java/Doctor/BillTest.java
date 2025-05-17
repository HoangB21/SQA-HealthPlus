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

public class BillTest {
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
        doctorInstance.userID = "user001"; // Đảm bảo userID được thiết lập
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    @Test
    public void testBillUpdateExistingPatient() throws SQLException, ClassNotFoundException {
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

        // Act
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert
        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';");
        verify(dbOperator, times(1))
                .customInsertion("UPDATE tmp_bill SET laboratory_fee = '1000' WHERE tmp_bill_id = 'hms0001tb';");
        verifyNoMoreInteractions(dbOperator);
    }

    // BILL_03: Xử lý SQLException từ truy vấn đầu tiên nhưng vẫn tạo mới thành công
    @Test
    public void testBillSQLExceptionOnFirstQuery() throws SQLException, ClassNotFoundException {
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

        // Act
        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        // Assert
        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat002';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    // BILL_04: Xử lý SQLException từ truy vấn chính, trả về true mặc định

    // BILL_05: Tạo tmp_bill_id với giá trị lớn nhất có số không đầu (hms0000tb)
    @Test
    public void testBillCreateNewWithZeroPadding() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat004, doctor_fee 2000";
        String patientID = "hms0001pa";
        String labFee = "2500";

        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'hms0001pa';"))
                .thenThrow(new SQLException("No record"));

        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0000tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        String expectedSql = "INSERT INTO tmp_bill (patient_id,doctor_fee,tmp_bill_id) " +
                "VALUES ('pat004','2000','hms0001tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'hms0001pa';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testBillCreateNewWithNonZeroBillID() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat005, doctor_fee 1500";
        String patientID = "pat005";
        String labFee = "2000";

        // Mock: Không tìm thấy bill cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat005';"))
                .thenThrow(new SQLException("No record"));

        // Mock: billID lớn nhất là "hms0010tb"
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0010tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Kỳ vọng câu lệnh SQL với tmpID2 = "hms0011tb"
        String expectedSql = "INSERT INTO tmp_bill (patient_id,doctor_fee,tmp_bill_id) " +
                "VALUES ('pat005','1500','hms0011tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat005';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testBillCreateNewPatient() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat001, appointment_fee 500, doctor_fee 1000"; // 3 cặp giá trị
        String patientID = "pat001";
        String labFee = "2000";

        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"))
                .thenThrow(new SQLException("No record found"));

        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        String expectedSql = "INSERT INTO tmp_bill (patient_id,appointment_fee,doctor_fee,tmp_bill_id) " +
                "VALUES ('pat001','500','1000','hms0002tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testBillWithTwoFields() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat006, appointment_fee 600"; // 2 cặp giá trị
        String patientID = "pat006";
        String labFee = "1500";

        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat006';"))
                .thenThrow(new SQLException("No record"));

        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0005tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        String expectedSql = "INSERT INTO tmp_bill (patient_id,appointment_fee,tmp_bill_id) " +
                "VALUES ('pat006','600','hms0006tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat006';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }

    @Test
    public void testBillInvalidBillInfoMissingValue() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat012, appointment_fee "; // Thiếu giá trị sau "appointment_fee"
        String patientID = "pat012";
        String labFee = "2000";

        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat012';"))
                .thenThrow(new SQLException("No record"));

        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0004tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        String expectedSql = "INSERT INTO tmp_bill (patient_id,tmp_bill_id) VALUES ('pat012','hms0005tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat012';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testBillSQLExceptionOnInsert() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat013, doctor_fee 3000";
        String patientID = "pat013";
        String labFee = "2500";

        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat013';"))
                .thenThrow(new SQLException("No record"));

        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0005tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        String expectedSql = "INSERT INTO tmp_bill (patient_id,doctor_fee,tmp_bill_id) VALUES ('pat013','3000','hms0006tb');";
        when(dbOperator.customInsertion(expectedSql)).thenThrow(new SQLException("Insert failed"));

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertFalse(result); // Kỳ vọng trả về false
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat013';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testBillSQLExceptionOnMainBlock() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat003";
        String patientID = "pat003";
        String labFee = "3000";

        // Ném SQLException ở cấp độ cao nhất
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Critical database error"));

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result); // Mã nguồn luôn trả về true nếu có ngoại lệ ở cấp độ cao nhất
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat003';");
    }

    @Test
    public void testBillUpdateExistingPatientFailed() throws SQLException, ClassNotFoundException {
        String billInfo = "dummy_field dummy_value";
        String patientID = "pat011";
        String labFee = "1200";

        ArrayList<ArrayList<String>> tmpBillResult = new ArrayList<>();
        tmpBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        tmpBillResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat011';"))
                .thenReturn(tmpBillResult);

        when(dbOperator.customInsertion("UPDATE tmp_bill SET laboratory_fee = '1200' WHERE tmp_bill_id = 'hms0001tb';"))
                .thenReturn(false);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result); // Mã nguồn không gán result = false trong trường hợp này
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat011';");
        verify(dbOperator, times(1))
                .customInsertion("UPDATE tmp_bill SET laboratory_fee = '1200' WHERE tmp_bill_id = 'hms0001tb';");
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testBillEmptyBillInfo() throws SQLException, ClassNotFoundException {
        String billInfo = "";
        String patientID = "pat008";
        String labFee = "2200";

        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat008';"))
                .thenThrow(new SQLException("No record"));

        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0003tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        String expectedSql = "INSERT INTO tmp_bill (tmp_bill_id) VALUES ('hms0004tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat008';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
    }

    @Test
    public void testBillWithOneField() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat009";
        String patientID = "pat009";
        String labFee = "1800";

        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat009';"))
                .thenThrow(new SQLException("No record"));

        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0004tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        String expectedSql = "INSERT INTO tmp_bill (patient_id,tmp_bill_id) VALUES ('pat009','hms0005tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat009';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
    }

    @Test
    public void testBillWithFourFields() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat010, appointment_fee 500, doctor_fee 1000, extra_fee 200";
        String patientID = "pat010";
        String labFee = "2000";

        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat010';"))
                .thenThrow(new SQLException("No record"));

        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms0005tb")));
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        String expectedSql = "INSERT INTO tmp_bill (patient_id,appointment_fee,doctor_fee,extra_fee,tmp_bill_id) " +
                "VALUES ('pat010','500','1000',200,'hms0006tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat010';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
    }

    @Test
    public void testBillClassNotFoundException() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat014, doctor_fee 3000";
        String patientID = "pat014";
        String labFee = "2500";

        when(dbOperator.customSelection(anyString())).thenThrow(new ClassNotFoundException("Driver not found"));

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result); // Mã nguồn luôn trả về true nếu có ngoại lệ
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat014';");
    }
    @Test
    public void testBillInvalidBillIDLength() throws SQLException, ClassNotFoundException {
        String billInfo = "patient_id pat014, doctor_fee 3500";
        String patientID = "pat014";
        String labFee = "2800";

        // Mock: Không tìm thấy bill cho patientID
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat014';"))
                .thenThrow(new SQLException("No record"));

        // Mock: billID không hợp lệ (độ dài nhỏ hơn 4)
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        maxBillResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
        maxBillResult.add(new ArrayList<>(Arrays.asList("hms"))); // billID không hợp lệ
        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Kỳ vọng câu lệnh SQL với tmpID2 = "hms0001tb" (do billID không hợp lệ, dùng giá trị mặc định)
        String expectedSql = "INSERT INTO tmp_bill (patient_id,doctor_fee,tmp_bill_id) VALUES ('pat014','3500','hms0001tb');";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.bill(billInfo, patientID, labFee);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat014';");
        verify(dbOperator, times(1))
                .customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
}