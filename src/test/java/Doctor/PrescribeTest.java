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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

public class PrescribeTest {
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
    public void testPrescribe_SuccessfulInsert() throws Exception {
        // Setup giả lập prescription_id hiện tại
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00042");
        mockResult.add(new ArrayList<>()); // row 0
        mockResult.add(row);               // row 1
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Không làm gì khi addTableRow được gọi
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        // Gọi phương thức cần test
        boolean result = doctorInstance.prescribe("paracetamol", "bloodTest", "pat001");

        assertTrue(result); // Kết quả phải là true

        // Kiểm tra hàm được gọi đúng
        verify(dbOperator).customSelection(contains("SELECT prescription_id"));
        verify(dbOperator).addTableRow(eq("prescription"), anyString());
    }

    @Test
    public void testPrescribe_FailsOnSQLException() throws Exception {
        // Giả lập customSelection ném SQLException
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("DB error"));

        boolean result = doctorInstance.prescribe("drug1", "test1", "pat001");

        assertFalse(result); // Phải false do lỗi DB
    }

    @Test
    public void testPrescribe_EmptyDrugsAndTests() throws Exception {
        // Setup prescription_id hiện tại
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00099");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        boolean result = doctorInstance.prescribe("", "", "pat002");

        assertTrue(result);
        // Có thể kiểm tra thêm: verify dữ liệu chèn vào có "NULL"
        verify(dbOperator).addTableRow(eq("prescription"), contains("NULL"));
    }
    @Test
    public void testPrescribe_EmptyDrugs_ShouldSetTestsToNULL() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00008");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        boolean result = doctorInstance.prescribe("", "xray", "pat003");

        assertTrue(result);
        verify(dbOperator).addTableRow(eq("prescription"), contains("NULL")); // drugs được set thành NULL
    }


    @Test
    public void testPrescribe_NullResultFromCustomSelection_ShouldFailGracefully() throws Exception {
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        boolean result = doctorInstance.prescribe("drugB", "testB", "pat006");

        assertFalse(result); // Không lấy được prescription_id
    }


    @Test
    public void testPrescribe_SQLExceptionWhenAddTableRow_ShouldReturnFalse() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00015");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doThrow(new SQLException("Error")).when(dbOperator).addTableRow(anyString(), anyString());

        boolean result = doctorInstance.prescribe("drugD", "testD", "pat008");

        assertFalse(result); // Bị lỗi khi insert
    }
    @Test
    public void testPrescribe_EmptyTests_ShouldSetTestsToNULL() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00008");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        boolean result = doctorInstance.prescribe("paracetamol", "", "pat004");

        assertTrue(result);
        verify(dbOperator).addTableRow(eq("prescription"), contains("NULL"));
    }
    @Test
    public void testPrescribe_EmptyDrugs_ShouldSetDrugsToNULL() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00009");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        boolean result = doctorInstance.prescribe("", "blood test", "pat005");

        assertTrue(result);
        verify(dbOperator).addTableRow(eq("prescription"), contains("NULL")); // Chắc chắn xuất hiện NULL
    }
    @Test
    public void testPrescribe_BothEmpty_ShouldSetBothToNULL() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00010");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        boolean result = doctorInstance.prescribe("", "", "pat006");

        assertTrue(result);

        // Capture giá trị sql được truyền vào
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(dbOperator).addTableRow(eq("prescription"), sqlCaptor.capture());

        String capturedSql = sqlCaptor.getValue();
        assertTrue(capturedSql.contains("pat006"));
        assertTrue(capturedSql.contains("NULL"));
        assertTrue(capturedSql.contains("pres"));
    }
    @Test
    public void testPrescribe_EmptyTestsOnly_ShouldSetTestsToNULL() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00010");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        boolean result = doctorInstance.prescribe("Panadol", "", "pat001");

        assertTrue(result);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(dbOperator).addTableRow(eq("prescription"), sqlCaptor.capture());

        String capturedSql = sqlCaptor.getValue();
        assertTrue(capturedSql.contains("Panadol"));
        assertTrue(capturedSql.contains("NULL")); // test phần tests được set NULL
    }
    @Test
    public void testPrescribe_EmptyDrugsOnly_ShouldSetDrugsToNULL() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        row.add("pres00010");
        mockResult.add(new ArrayList<>());
        mockResult.add(row);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).addTableRow(anyString(), anyString());

        boolean result = doctorInstance.prescribe("", "BloodTest", "pat002");

        assertTrue(result);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(dbOperator).addTableRow(eq("prescription"), sqlCaptor.capture());

        String capturedSql = sqlCaptor.getValue();
        assertTrue(capturedSql.contains("BloodTest"));
        assertTrue(capturedSql.contains("NULL")); // test phần drugs được set NULL
    }
    @Test
    public void testPrescribe_DBException_ShouldReturnFalse() throws Exception {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("DB error"));

        boolean result = doctorInstance.prescribe("A", "B", "pat007");

        assertFalse(result); // vì bị catch -> false
    }
    @Test
    public void testPrescribe_InvalidPrescIDFormat_ShouldFailGracefully() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>());
        mockResult.add(new ArrayList<>(Arrays.asList("invalidFormat"))); // Định dạng không hợp lệ

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        boolean result = doctorInstance.prescribe("drugA", "testA", "pat005");

        assertFalse(result); // NumberFormatException -> return false
        verify(dbOperator, times(1)).customSelection(anyString());
    }

    @Test
    public void testPrescribe_EmptyResultList_ShouldFailGracefully() throws Exception {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>()); // Chỉ có hàng tiêu đề, không có dữ liệu

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        boolean result = doctorInstance.prescribe("drugC", "testC", "pat007");

        assertFalse(result); // IndexOutOfBoundsException -> return false
        verify(dbOperator, times(1)).customSelection(anyString());
    }
    @Test
    public void testPrescribe_ClassNotFoundException() throws Exception {
        when(dbOperator.customSelection(anyString())).thenThrow(new ClassNotFoundException("Driver not found"));

        boolean result = doctorInstance.prescribe("drugE", "testE", "pat009");

        assertFalse(result); // ClassNotFoundException -> return false
        verify(dbOperator, times(1)).customSelection(anyString());
    }
    @Test
    public void testPrescribeInvalidPrescIDLength() throws SQLException, ClassNotFoundException {
        String drugs = "DrugA";
        String tests = "TestA";
        String patientID = "pat001";

        // Mock: prescID không hợp lệ (độ dài nhỏ hơn 5)
        ArrayList<ArrayList<String>> prescIDResult = new ArrayList<>();
        prescIDResult.add(new ArrayList<>(Arrays.asList("prescription_id")));
        prescIDResult.add(new ArrayList<>(Arrays.asList("pres"))); // Độ dài 4
        when(dbOperator.customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);"))
                .thenReturn(prescIDResult);

        // Kỳ vọng tmpID2 = "pres00001" (do prescID không hợp lệ, dùng giá trị mặc định)
        String expectedSql = "pres00001,pat001," + doctorInstance.slmcRegNo + ","
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + ",DrugA,TestA";
        when(dbOperator.addTableRow("prescription", expectedSql)).thenReturn(true);

        boolean result = doctorInstance.prescribe(drugs, tests, patientID);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);");
        verify(dbOperator, times(1)).addTableRow("prescription", expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testPrescribeEmptyTests() throws SQLException, ClassNotFoundException {
        String drugs = "DrugB";
        String tests = ""; // tests rỗng
        String patientID = "pat002";

        // Mock: prescID hợp lệ
        ArrayList<ArrayList<String>> prescIDResult = new ArrayList<>();
        prescIDResult.add(new ArrayList<>(Arrays.asList("prescription_id")));
        prescIDResult.add(new ArrayList<>(Arrays.asList("pres00001")));
        when(dbOperator.customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);"))
                .thenReturn(prescIDResult);

        // Kỳ vọng tmpID2 = "pres00002", tests = "NULL"
        String expectedSql = "pres00002,pat002," + doctorInstance.slmcRegNo + ","
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + ",DrugB,NULL";
        when(dbOperator.addTableRow("prescription", expectedSql)).thenReturn(true);

        boolean result = doctorInstance.prescribe(drugs, tests, patientID);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);");
        verify(dbOperator, times(1)).addTableRow("prescription", expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testPrescribeEmptyDrugs() throws SQLException, ClassNotFoundException {
        String drugs = ""; // drugs rỗng
        String tests = "TestC";
        String patientID = "pat003";

        // Mock: prescID hợp lệ
        ArrayList<ArrayList<String>> prescIDResult = new ArrayList<>();
        prescIDResult.add(new ArrayList<>(Arrays.asList("prescription_id")));
        prescIDResult.add(new ArrayList<>(Arrays.asList("pres00002")));
        when(dbOperator.customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);"))
                .thenReturn(prescIDResult);

        // Kỳ vọng tmpID2 = "pres00003", drugs = "NULL"
        String expectedSql = "pres00003,pat003," + doctorInstance.slmcRegNo + ","
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + ",NULL,TestC";
        when(dbOperator.addTableRow("prescription", expectedSql)).thenReturn(true);

        boolean result = doctorInstance.prescribe(drugs, tests, patientID);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);");
        verify(dbOperator, times(1)).addTableRow("prescription", expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void testPrescribeSQLException() throws SQLException, ClassNotFoundException {
        String drugs = "DrugD";
        String tests = "TestD";
        String patientID = "pat004";

        // Mock: customSelection ném SQLException
        when(dbOperator.customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);"))
                .thenThrow(new SQLException("Database error"));

        boolean result = doctorInstance.prescribe(drugs, tests, patientID);

        assertFalse(result); // Kỳ vọng trả về false
        verify(dbOperator, times(1))
                .customSelection("SELECT prescription_id FROM prescription WHERE prescription_id = (SELECT MAX(prescription_id) FROM prescription);");
        verifyNoMoreInteractions(dbOperator);
    }
}