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

public class GetTestResultsTest {
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
    public void testGetTestResultsByPatientID() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> testData = new ArrayList<>();
        testData.add(new ArrayList<>(Arrays.asList("first_name", "last_name")));
        testData.add(new ArrayList<>(Arrays.asList("John", "Doe")));
        when(dbOperator.customSelection(anyString())).thenReturn(testData);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults("patientID", "pat001");
        assertEquals("John", result.get(1).get(0));
    }

    @Test
    public void testGetTestResultsByNic() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> testData = new ArrayList<>();
        testData.add(new ArrayList<>(Arrays.asList("first_name", "last_name")));
        testData.add(new ArrayList<>(Arrays.asList("John", "Doe")));
        when(dbOperator.customSelection(anyString())).thenReturn(testData);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults("nic", "123456789V");
        assertEquals("John", result.get(1).get(0));
    }

    @Test
    public void testGetTestResultsByTestIDSearch() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> testData = new ArrayList<>();
        testData.add(new ArrayList<>(Arrays.asList("first_name", "last_name")));
        testData.add(new ArrayList<>(Arrays.asList("John", "Doe")));
        when(dbOperator.customSelection(anyString())).thenReturn(testData);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults("testID", "tst001");
        assertEquals("John", result.get(1).get(0));
    }

    @Test
    public void testGetTestResultsBySearchException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults("patientID", "pat001");
        assertNull(result);
    }
    @Test
    public void testGetTestResultsInvalidSearchType() throws SQLException, ClassNotFoundException {
        String searchType = "invalid"; // searchType không hợp lệ
        String searchWord = "123";

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(searchType, searchWord);

        assertNull(result); // Kỳ vọng trả về null vì searchType không hợp lệ
        verifyNoInteractions(dbOperator); // Không gọi customSelection
    }
    @Test
    public void testGetTestResultsByTestID() throws SQLException, ClassNotFoundException {
        String searchType = "testID";
        String searchWord = "test001";

        // Mock kết quả trả về từ customSelection
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>(Arrays.asList("first_name", "last_name", "date_of_birth", "gender", "date", "patient_id", "lab_assistant", "consultant_id", "test_result")));
        mockResult.add(new ArrayList<>(Arrays.asList("John", "Doe", "1990-01-01", "Male", "2023-01-01", "pat001", "lab001", "con001", "Positive")));
        when(dbOperator.customSelection("SELECT person.first_name, person.last_name, person.date_of_birth, person.gender, " +
                "tests.date, tests.patient_id, tests.lab_assistant, tests.consultant_id, tests.test_result " +
                "FROM person " +
                "JOIN patient " +
                "ON person.person_id=patient.person_id " +
                "JOIN tests " +
                "ON patient.patient_id=tests.patient_id " +
                "WHERE patient.patient_id = (SELECT tests.patient_id FROM tests WHERE tests.test_id = '" + searchWord + "');"))
                .thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(searchType, searchWord);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(1).get(0));
        assertEquals("Doe", result.get(1).get(1));
        assertEquals("1990-01-01", result.get(1).get(2));
        assertEquals("Male", result.get(1).get(3));
        assertEquals("2023-01-01", result.get(1).get(4));
        assertEquals("pat001", result.get(1).get(5));
        assertEquals("lab001", result.get(1).get(6));
        assertEquals("con001", result.get(1).get(7));
        assertEquals("Positive", result.get(1).get(8));

        verify(dbOperator, times(1))
                .customSelection("SELECT person.first_name, person.last_name, person.date_of_birth, person.gender, " +
                        "tests.date, tests.patient_id, tests.lab_assistant, tests.consultant_id, tests.test_result " +
                        "FROM person " +
                        "JOIN patient " +
                        "ON person.person_id=patient.person_id " +
                        "JOIN tests " +
                        "ON patient.patient_id=tests.patient_id " +
                        "WHERE patient.patient_id = (SELECT tests.patient_id FROM tests WHERE tests.test_id = '" + searchWord + "');");
        verifyNoMoreInteractions(dbOperator);
    }
    //////////////////////////////////////////////////
    @Test
    public void testGetTestResultsLiverFunctionSuccess() throws SQLException, ClassNotFoundException {
        String testID = "lv001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_liver_id", "result1", "result2")));
        testResult.add(new ArrayList<>(Arrays.asList("lv001", "value1", "value2")));

        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("lv", result.get(2).get(0));
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';");
    }

    @Test
    public void testGetTestResultsBloodGroupingSuccess() throws SQLException, ClassNotFoundException {
        String testID = "bg001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_bloodG_id", "blood_type")));
        testResult.add(new ArrayList<>(Arrays.asList("bg001", "A+")));

        when(dbOperator.customSelection("SELECT * FROM BloodGroupingRh WHERE tst_bloodG_id = 'bg001';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("bg", result.get(2).get(0));
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM BloodGroupingRh WHERE tst_bloodG_id = 'bg001';");
    }

    @Test
    public void testGetTestResultsSCPTSuccess() throws SQLException, ClassNotFoundException {
        String testID = "scpt001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_SCPT_id", "value")));
        testResult.add(new ArrayList<>(Arrays.asList("scpt001", "100")));

        when(dbOperator.customSelection("SELECT * FROM SeriumCreatinePhosphokinaseTotal WHERE tst_SCPT_id = 'scpt001';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("scpt", result.get(2).get(0));
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM SeriumCreatinePhosphokinaseTotal WHERE tst_SCPT_id = 'scpt001';");
    }

    @Test
    public void testGetTestResultsCBCSuccess() throws SQLException, ClassNotFoundException {
        String testID = "cbc001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_CBC_id", "wbc", "rbc")));
        testResult.add(new ArrayList<>(Arrays.asList("cbc001", "5.5", "4.5")));

        when(dbOperator.customSelection("SELECT * FROM completeBloodCount WHERE tst_CBC_id = 'cbc001';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("cbc", result.get(2).get(0));
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM completeBloodCount WHERE tst_CBC_id = 'cbc001';");
    }

    @Test
    public void testGetTestResultsSQLException() throws SQLException, ClassNotFoundException {
        String testID = "lv001";
        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';"))
                .thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNull(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';");
    }

    @Test
    public void testGetTestResultsDatabaseReturnsNull() throws SQLException, ClassNotFoundException {
        String testID = "lv001";
        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';"))
                .thenReturn(null);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNull(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';");
    }


    @Test
    public void testGetTestResultsInvalidTestID() throws SQLException, ClassNotFoundException {
        String testID = "xx001";

        // Không có bảng nào khớp với prefix "xx", truy vấn sẽ thất bại
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNull(result); // Kết quả phải là null vì không có bảng nào khớp
        verify(dbOperator, times(1)).customSelection(anyString());
    }

    @Test
    public void testGetTestResultsEmptyResult() throws SQLException, ClassNotFoundException {
        String testID = "lv999";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_liver_id", "result1", "result2")));

        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv999';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("lv", result.get(1).get(0)); // meta được thêm vào
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv999';");
    }
    @Test
    public void testGetTestResultsLipidTestSuccess() throws SQLException, ClassNotFoundException {
        String testID = "li001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_li_id", "result")));
        testResult.add(new ArrayList<>(Arrays.asList("li001", "normal")));

        when(dbOperator.customSelection("SELECT * FROM LipidTest WHERE tst_li_id = 'li001';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("li", result.get(2).get(0));
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LipidTest WHERE tst_li_id = 'li001';");
    }

    @Test
    public void testGetTestResultsRenalFunctionSuccess() throws SQLException, ClassNotFoundException {
        String testID = "re001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_renal_id", "result")));
        testResult.add(new ArrayList<>(Arrays.asList("re001", "normal")));

        when(dbOperator.customSelection("SELECT * FROM RenalFunctionTest WHERE tst_renal_id = 're001';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("re", result.get(2).get(0));
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM RenalFunctionTest WHERE tst_renal_id = 're001';");
    }

    @Test
    public void testGetTestResultsUrineFullReportSuccess() throws SQLException, ClassNotFoundException {
        String testID = "ur001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_ur_id", "result")));
        testResult.add(new ArrayList<>(Arrays.asList("ur001", "normal")));

        when(dbOperator.customSelection("SELECT * FROM UrineFullReport WHERE tst_ur_id = 'ur001';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("ur", result.get(2).get(0));
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM UrineFullReport WHERE tst_ur_id = 'ur001';");
    }

    @Test
    public void testGetTestResultsSCPSuccess() throws SQLException, ClassNotFoundException {
        String testID = "scp001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_SCP_id", "result")));
        testResult.add(new ArrayList<>(Arrays.asList("scp001", "normal")));

        when(dbOperator.customSelection("SELECT * FROM SeriumCreatinePhosphokinase WHERE tst_SCP_id = 'scp001';"))
                .thenReturn(testResult);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("scp", result.get(2).get(0));
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM SeriumCreatinePhosphokinase WHERE tst_SCP_id = 'scp001';");
    }

    @Test
    public void testGetTestResultsClassNotFoundException() throws SQLException, ClassNotFoundException {
        String testID = "lv001";
        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';"))
                .thenThrow(new ClassNotFoundException("Driver not found"));

        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        assertNull(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';");
    }
}
