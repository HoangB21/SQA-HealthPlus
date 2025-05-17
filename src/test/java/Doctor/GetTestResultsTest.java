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
 * Unit test class for testing the getTestResults method in the Doctor class.
 * This class tests various scenarios for retrieving test results from the database based on different search types and test IDs.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetTestResultsTest {
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
     * Test case: GTR_01
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm theo patientID khi truy vấn thành công
     * Input: searchType = "patientID", searchWord = "pat001", dữ liệu mock trả về với các cột "first_name", "last_name"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "John", "Doe"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm theo patientID
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsByPatientID() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> testData = new ArrayList<>();
        testData.add(new ArrayList<>(Arrays.asList("first_name", "last_name")));
        testData.add(new ArrayList<>(Arrays.asList("John", "Doe")));
        when(dbOperator.customSelection(anyString())).thenReturn(testData);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults("patientID", "pat001");

        // Assert: Kiểm tra kết quả
        assertEquals("John", result.get(1).get(0), "Dữ liệu kết quả xét nghiệm đầu tiên phải có first_name là 'John'");
    }

    /**
     * Test case: GTR_02
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm theo NIC khi truy vấn thành công
     * Input: searchType = "nic", searchWord = "123456789V", dữ liệu mock trả về với các cột "first_name", "last_name"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "John", "Doe"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm theo NIC
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsByNic() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> testData = new ArrayList<>();
        testData.add(new ArrayList<>(Arrays.asList("first_name", "last_name")));
        testData.add(new ArrayList<>(Arrays.asList("John", "Doe")));
        when(dbOperator.customSelection(anyString())).thenReturn(testData);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults("nic", "123456789V");

        // Assert: Kiểm tra kết quả
        assertEquals("John", result.get(1).get(0), "Dữ liệu kết quả xét nghiệm đầu tiên phải có first_name là 'John'");
    }

    /**
     * Test case: GTR_03
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm theo testID khi truy vấn thành công
     * Input: searchType = "testID", searchWord = "tst001", dữ liệu mock trả về với các cột "first_name", "last_name"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "John", "Doe"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm theo testID
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsByTestIDSearch() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> testData = new ArrayList<>();
        testData.add(new ArrayList<>(Arrays.asList("first_name", "last_name")));
        testData.add(new ArrayList<>(Arrays.asList("John", "Doe")));
        when(dbOperator.customSelection(anyString())).thenReturn(testData);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults("testID", "tst001");

        // Assert: Kiểm tra kết quả
        assertEquals("John", result.get(1).get(0), "Dữ liệu kết quả xét nghiệm đầu tiên phải có first_name là 'John'");
    }

    /**
     * Test case: GTR_04
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn kết quả xét nghiệm
     * Input: searchType = "patientID", searchWord = "pat001", dữ liệu mock ném SQLException
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsBySearchException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults("patientID", "pat001");

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getTestResults phải trả về null khi có lỗi SQLException");
    }

    /**
     * Test case: GTR_05
     * Mục tiêu: Kiểm tra xử lý khi searchType không hợp lệ
     * Input: searchType = "invalid", searchWord = "123"
     * Expected Output: Trả về null, không gọi database
     * Ghi chú: Phủ nhánh xử lý đầu vào không hợp lệ (searchType không được hỗ trợ)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsInvalidSearchType() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào
        String searchType = "invalid"; // searchType không hợp lệ
        String searchWord = "123";

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(searchType, searchWord);

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getTestResults phải trả về null vì searchType không hợp lệ");

        // Verify: Đảm bảo không có tương tác với database
        verifyNoInteractions(dbOperator);
    }

    /**
     * Test case: GTR_06
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm chi tiết theo testID khi truy vấn thành công
     * Input: searchType = "testID", searchWord = "test001", dữ liệu mock trả về với các cột chi tiết
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "John", "Doe", "1990-01-01", "Male", "2023-01-01", "pat001", "lab001", "con001", "Positive"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm chi tiết theo testID
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsByTestID() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String searchType = "testID";
        String searchWord = "test001";
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

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(searchType, searchWord);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(2, result.size(), "Kết quả phải chứa 2 hàng (header và dữ liệu)");
        assertEquals("John", result.get(1).get(0), "First_name phải là 'John'");
        assertEquals("Doe", result.get(1).get(1), "Last_name phải là 'Doe'");
        assertEquals("1990-01-01", result.get(1).get(2), "Date_of_birth phải là '1990-01-01'");
        assertEquals("Male", result.get(1).get(3), "Gender phải là 'Male'");
        assertEquals("2023-01-01", result.get(1).get(4), "Date phải là '2023-01-01'");
        assertEquals("pat001", result.get(1).get(5), "Patient_id phải là 'pat001'");
        assertEquals("lab001", result.get(1).get(6), "Lab_assistant phải là 'lab001'");
        assertEquals("con001", result.get(1).get(7), "Consultant_id phải là 'con001'");
        assertEquals("Positive", result.get(1).get(8), "Test_result phải là 'Positive'");

        // Verify: Đảm bảo customSelection được gọi đúng
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

    /**
     * Test case: GTR_07
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm Liver Function khi truy vấn thành công
     * Input: testID = "lv001", dữ liệu mock trả về với các cột "tst_liver_id", "result1", "result2"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "lv001", "value1", "value2"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm Liver Function
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsLiverFunctionSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "lv001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_liver_id", "result1", "result2")));
        testResult.add(new ArrayList<>(Arrays.asList("lv001", "value1", "value2")));
        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 hàng (header, dữ liệu, và meta)");
        assertEquals("lv", result.get(2).get(0), "Meta phải là 'lv' cho Liver Function Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';");
    }

    /**
     * Test case: GTR_08
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm Blood Grouping khi truy vấn thành công
     * Input: testID = "bg001", dữ liệu mock trả về với các cột "tst_bloodG_id", "blood_type"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "bg001", "A+"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm Blood Grouping
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsBloodGroupingSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "bg001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_bloodG_id", "blood_type")));
        testResult.add(new ArrayList<>(Arrays.asList("bg001", "A+")));
        when(dbOperator.customSelection("SELECT * FROM BloodGroupingRh WHERE tst_bloodG_id = 'bg001';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 hàng (header, dữ liệu, và meta)");
        assertEquals("bg", result.get(2).get(0), "Meta phải là 'bg' cho Blood Grouping Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM BloodGroupingRh WHERE tst_bloodG_id = 'bg001';");
    }

    /**
     * Test case: GTR_09
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm SCPT khi truy vấn thành công
     * Input: testID = "scpt001", dữ liệu mock trả về với các cột "tst_SCPT_id", "value"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "scpt001", "100"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm SCPT
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsSCPTSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "scpt001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_SCPT_id", "value")));
        testResult.add(new ArrayList<>(Arrays.asList("scpt001", "100")));
        when(dbOperator.customSelection("SELECT * FROM SeriumCreatinePhosphokinaseTotal WHERE tst_SCPT_id = 'scpt001';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 hàng (header, dữ liệu, và meta)");
        assertEquals("scpt", result.get(2).get(0), "Meta phải là 'scpt' cho SCPT Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM SeriumCreatinePhosphokinaseTotal WHERE tst_SCPT_id = 'scpt001';");
    }

    /**
     * Test case: GTR_10
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm CBC khi truy vấn thành công
     * Input: testID = "cbc001", dữ liệu mock trả về với các cột "tst_CBC_id", "wbc", "rbc"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "cbc001", "5.5", "4.5"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm CBC
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsCBCSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "cbc001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_CBC_id", "wbc", "rbc")));
        testResult.add(new ArrayList<>(Arrays.asList("cbc001", "5.5", "4.5")));
        when(dbOperator.customSelection("SELECT * FROM completeBloodCount WHERE tst_CBC_id = 'cbc001';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 hàng (header, dữ liệu, và meta)");
        assertEquals("cbc", result.get(2).get(0), "Meta phải là 'cbc' cho CBC Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM completeBloodCount WHERE tst_CBC_id = 'cbc001';");
    }

    /**
     * Test case: GTR_11
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn kết quả xét nghiệm Liver Function
     * Input: testID = "lv001", dữ liệu mock ném SQLException
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsSQLException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "lv001";
        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';"))
                .thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getTestResults phải trả về null khi có lỗi SQLException");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';");
    }

    /**
     * Test case: GTR_12
     * Mục tiêu: Kiểm tra xử lý khi truy vấn kết quả xét nghiệm trả về null
     * Input: testID = "lv001", dữ liệu mock trả về null
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh khi truy vấn trả về null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsDatabaseReturnsNull() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "lv001";
        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';"))
                .thenReturn(null);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getTestResults phải trả về null khi dữ liệu trả về là null");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';");
    }

    /**
     * Test case: GTR_13
     * Mục tiêu: Kiểm tra xử lý khi testID không hợp lệ
     * Input: testID = "xx001", dữ liệu mock trả về null
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh khi testID không hợp lệ, không có bảng nào khớp
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsInvalidTestID() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "xx001";
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getTestResults phải trả về null vì không có bảng nào khớp với testID");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1)).customSelection(anyString());
    }

    /**
     * Test case: GTR_14
     * Mục tiêu: Kiểm tra xử lý khi kết quả xét nghiệm rỗng (chỉ có header)
     * Input: testID = "lv999", dữ liệu mock trả về chỉ có header "tst_liver_id", "result1", "result2"
     * Expected Output: Trả về danh sách với header và meta, không có dữ liệu xét nghiệm
     * Ghi chú: Phủ nhánh khi kết quả xét nghiệm rỗng (không có dữ liệu)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsEmptyResult() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "lv999";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_liver_id", "result1", "result2")));
        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv999';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(2, result.size(), "Kết quả phải chứa 2 hàng (header và meta)");
        assertEquals("lv", result.get(1).get(0), "Meta phải là 'lv' cho Liver Function Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv999';");
    }

    /**
     * Test case: GTR_15
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm Lipid Test khi truy vấn thành công
     * Input: testID = "li001", dữ liệu mock trả về với các cột "tst_li_id", "result"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "li001", "normal"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm Lipid Test
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsLipidTestSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "li001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_li_id", "result")));
        testResult.add(new ArrayList<>(Arrays.asList("li001", "normal")));
        when(dbOperator.customSelection("SELECT * FROM LipidTest WHERE tst_li_id = 'li001';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 hàng (header, dữ liệu, và meta)");
        assertEquals("li", result.get(2).get(0), "Meta phải là 'li' cho Lipid Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LipidTest WHERE tst_li_id = 'li001';");
    }

    /**
     * Test case: GTR_16
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm Renal Function khi truy vấn thành công
     * Input: testID = "re001", dữ liệu mock trả về với các cột "tst_renal_id", "result"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "re001", "normal"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm Renal Function
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsRenalFunctionSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "re001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_renal_id", "result")));
        testResult.add(new ArrayList<>(Arrays.asList("re001", "normal")));
        when(dbOperator.customSelection("SELECT * FROM RenalFunctionTest WHERE tst_renal_id = 're001';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 hàng (header, dữ liệu, và meta)");
        assertEquals("re", result.get(2).get(0), "Meta phải là 're' cho Renal Function Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM RenalFunctionTest WHERE tst_renal_id = 're001';");
    }

    /**
     * Test case: GTR_17
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm Urine Full Report khi truy vấn thành công
     * Input: testID = "ur001", dữ liệu mock trả về với các cột "tst_ur_id", "result"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "ur001", "normal"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm Urine Full Report
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsUrineFullReportSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "ur001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_ur_id", "result")));

        testResult.add(new ArrayList<>(Arrays.asList("tst_ur_id", "result")));
        testResult.add(new ArrayList<>(Arrays.asList("ur001", "normal")));
        when(dbOperator.customSelection("SELECT * FROM UrineFullReport WHERE tst_ur_id = 'ur001';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 hàng (header, dữ liệu, và meta)");
        assertEquals("ur", result.get(2).get(0), "Meta phải là 'ur' cho Urine Full Report Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM UrineFullReport WHERE tst_ur_id = 'ur001';");
    }

    /**
     * Test case: GTR_18
     * Mục tiêu: Kiểm tra khả năng lấy kết quả xét nghiệm SCP khi truy vấn thành công
     * Input: testID = "scp001", dữ liệu mock trả về với các cột "tst_SCP_id", "result"
     * Expected Output: Trả về danh sách kết quả xét nghiệm với dữ liệu hợp lệ, ví dụ "scp001", "normal"
     * Ghi chú: Phủ nhánh thành công khi truy vấn kết quả xét nghiệm SCP
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsSCPSuccess() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "scp001";
        ArrayList<ArrayList<String>> testResult = new ArrayList<>();
        testResult.add(new ArrayList<>(Arrays.asList("tst_SCP_id", "result")));
        testResult.add(new ArrayList<>(Arrays.asList("scp001", "normal")));
        when(dbOperator.customSelection("SELECT * FROM SeriumCreatinePhosphokinase WHERE tst_SCP_id = 'scp001';"))
                .thenReturn(testResult);

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 hàng (header, dữ liệu, và meta)");
        assertEquals("scp", result.get(2).get(0), "Meta phải là 'scp' cho SCP Test");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM SeriumCreatinePhosphokinase WHERE tst_SCP_id = 'scp001';");
    }

    /**
     * Test case: GTR_19
     * Mục tiêu: Kiểm tra xử lý lỗi ClassNotFoundException khi truy vấn kết quả xét nghiệm
     * Input: testID = "lv001", dữ liệu mock ném ClassNotFoundException
     * Expected Output: Trả về null
     * Ghi chú: Phủ nhánh lỗi ClassNotFoundException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetTestResultsClassNotFoundException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        String testID = "lv001";
        when(dbOperator.customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';"))
                .thenThrow(new ClassNotFoundException("Driver not found"));

        // Act: Gọi phương thức getTestResults
        ArrayList<ArrayList<String>> result = doctorInstance.getTestResults(testID);

        // Assert: Kiểm tra kết quả
        assertNull(result, "Phương thức getTestResults phải trả về null khi có lỗi ClassNotFoundException");

        // Verify: Đảm bảo customSelection được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM LiverFunctionTest WHERE tst_liver_id = 'lv001';");
    }
}