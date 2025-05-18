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
 * Unit test class for testing the getPatientInfo method in the Doctor class.
 * This class tests various scenarios for retrieving patient information from the database based on different search types.
 * Uses Mockito for mocking database interactions and direct database access for validation.
 */
public class GetPatientInfoTest {
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
     * Test case: GPI_01
     * Mục tiêu: Kiểm tra khả năng lấy thông tin bệnh nhân theo NIC khi truy vấn thành công
     * Input: searchType = "nic", searchWord = "123456789V", dữ liệu mock trả về thông tin cá nhân, y tế và lịch sử
     * Expected Output: Trả về danh sách chứa thông tin cá nhân, y tế và lịch sử, ví dụ "John", "Penicillin", "Diabetes"
     * Ghi chú: Phủ nhánh thành công khi truy vấn thông tin bệnh nhân theo NIC
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetPatientInfoByNic() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> personalData = new ArrayList<>();
        personalData.add(new ArrayList<>(Arrays.asList("first_name")));
        personalData.add(new ArrayList<>(Arrays.asList("John")));
        ArrayList<ArrayList<String>> medicalData = new ArrayList<>();
        medicalData.add(new ArrayList<>(Arrays.asList("drug_allergies_and_reactions", "patient_id")));
        medicalData.add(new ArrayList<>(Arrays.asList("Penicillin", "pat001")));
        ArrayList<ArrayList<String>> historyData = new ArrayList<>();
        historyData.add(new ArrayList<>(Arrays.asList("history")));
        historyData.add(new ArrayList<>(Arrays.asList("Diabetes")));
        when(dbOperator.customSelection(anyString())).thenReturn(personalData, medicalData, historyData);

        // Act: Gọi phương thức getPatientInfo
        ArrayList<ArrayList<ArrayList<String>>> result = doctorInstance.getPatientInfo("nic", "123456789V");

        // Assert: Kiểm tra kết quả
        assertEquals("John", result.get(0).get(1).get(0), "Personal data đầu tiên phải có first_name là 'John'");
        assertEquals("Penicillin", result.get(1).get(1).get(0), "Medical data đầu tiên phải có drug_allergies_and_reactions là 'Penicillin'");
        assertEquals("Diabetes", result.get(2).get(1).get(0), "History data đầu tiên phải có history là 'Diabetes'");
    }

    /**
     * Test case: GPI_02
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn thông tin bệnh nhân
     * Input: searchType = "nic", searchWord = "123456789V", dữ liệu mock ném SQLException
     * Expected Output: Trả về danh sách với các phần tử null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetPatientInfoException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Act: Gọi phương thức getPatientInfo
        ArrayList<ArrayList<ArrayList<String>>> result = doctorInstance.getPatientInfo("nic", "123456789V");

        // Assert: Kiểm tra kết quả
        assertNull(result.get(0), "Personal data phải là null khi có lỗi SQLException");
        assertNull(result.get(1), "Medical data phải là null khi có lỗi SQLException");
        assertNull(result.get(2), "History data phải là null khi có lỗi SQLException");
    }

    /**
     * Test case: GPI_03
     * Mục tiêu: Kiểm tra khả năng lấy thông tin bệnh nhân theo ID khi truy vấn thành công
     * Input: searchType = "id", searchWord = "pat001", dữ liệu mock trả về thông tin cá nhân, y tế và lịch sử
     * Expected Output: Trả về danh sách chứa thông tin cá nhân, y tế và lịch sử, ví dụ "John", "Doe", "Penicillin", "Flu"
     * Ghi chú: Phủ nhánh thành công khi truy vấn thông tin bệnh nhân theo ID
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void testGetPatientInfoById() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String searchType = "id";
        String searchWord = "pat001";

        // Mock kết quả trả về từ customSelection cho personalData
        ArrayList<ArrayList<String>> personalDataMock = new ArrayList<>();
        personalDataMock.add(new ArrayList<>(Arrays.asList("person_id", "first_name", "last_name", "date_of_birth", "gender", "nic")));
        personalDataMock.add(new ArrayList<>(Arrays.asList("per001", "John", "Doe", "1990-01-01", "Male", "123456789V")));
        when(dbOperator.customSelection("SELECT * FROM person WHERE person_id = (SELECT patient.person_id FROM patient WHERE patient_id = '" + searchWord + "')"))
                .thenReturn(personalDataMock);

        // Mock kết quả trả về từ customSelection cho medicalData
        ArrayList<ArrayList<String>> medicalDataMock = new ArrayList<>();
        medicalDataMock.add(new ArrayList<>(Arrays.asList("drug_allergies_and_reactions")));
        medicalDataMock.add(new ArrayList<>(Arrays.asList("Penicillin")));
        when(dbOperator.customSelection("SELECT patient.drug_allergies_and_reactions FROM patient WHERE patient_id = '" + searchWord + "'"))
                .thenReturn(medicalDataMock);

        // Mock kết quả trả về từ customSelection cho historyData
        ArrayList<ArrayList<String>> historyDataMock = new ArrayList<>();
        historyDataMock.add(new ArrayList<>(Arrays.asList("date", "history")));
        historyDataMock.add(new ArrayList<>(Arrays.asList("2023-01-01", "Flu")));
        when(dbOperator.customSelection("SELECT medical_history.date,medical_history.history FROM medical_history WHERE patient_id = '" + searchWord + "' ORDER BY date DESC"))
                .thenReturn(historyDataMock);

        // Act: Gọi phương thức getPatientInfo
        ArrayList<ArrayList<ArrayList<String>>> result = doctorInstance.getPatientInfo(searchType, searchWord);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 phần tử (personalData, medicalData, historyData)");
        assertEquals(personalDataMock, result.get(0), "Personal data phải khớp với dữ liệu mock");
        assertEquals(medicalDataMock, result.get(1), "Medical data phải khớp với dữ liệu mock");
        assertEquals(historyDataMock, result.get(2), "History data phải khớp với dữ liệu mock");

        // Verify: Đảm bảo các lệnh SQL được gọi đúng
        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM person WHERE person_id = (SELECT patient.person_id FROM patient WHERE patient_id = '" + searchWord + "')");
        verify(dbOperator, times(1))
                .customSelection("SELECT patient.drug_allergies_and_reactions FROM patient WHERE patient_id = '" + searchWord + "'");
        verify(dbOperator, times(1))
                .customSelection("SELECT medical_history.date,medical_history.history FROM medical_history WHERE patient_id = '" + searchWord + "' ORDER BY date DESC");
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: GPI_04
     * Mục tiêu: Kiểm tra xử lý khi searchType không hợp lệ
     * Input: searchType = "invalid", searchWord = "123"
     * Expected Output: Trả về danh sách với các phần tử null, không gọi database
     * Ghi chú: Phủ nhánh xử lý đầu vào không hợp lệ (searchType không được hỗ trợ)
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenInvalidSearchTypeWhenGettingPatientInfoThenReturnNullData() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào
        String searchType = "invalid";
        String searchWord = "123";

        // Act: Gọi phương thức getPatientInfo
        ArrayList<ArrayList<ArrayList<String>>> result = doctorInstance.getPatientInfo(searchType, searchWord);

        // Assert: Kiểm tra kết quả
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 phần tử");
        assertNull(result.get(0), "personalData phải là null");
        assertNull(result.get(1), "medicalData phải là null");
        assertNull(result.get(2), "historyData phải là null");

        // Verify: Đảm bảo không có tương tác với database
        verifyNoInteractions(dbOperator);
    }
}