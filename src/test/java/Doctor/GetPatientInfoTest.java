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

public class GetPatientInfoTest {
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
    public void testGetPatientInfoByNic() throws SQLException, ClassNotFoundException {
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

        ArrayList<ArrayList<ArrayList<String>>> result = doctorInstance.getPatientInfo("nic", "123456789V");
        assertEquals("John", result.get(0).get(1).get(0));
        assertEquals("Penicillin", result.get(1).get(1).get(0));
        assertEquals("Diabetes", result.get(2).get(1).get(0));
    }


    @Test
    public void testGetPatientInfoException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<ArrayList<String>>> result = doctorInstance.getPatientInfo("nic", "123456789V");
        assertNull(result.get(0));
        assertNull(result.get(1));
        assertNull(result.get(2));
    }
    @Test
    public void testGetPatientInfoById() throws SQLException, ClassNotFoundException {
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

        ArrayList<ArrayList<ArrayList<String>>> result = doctorInstance.getPatientInfo(searchType, searchWord);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(personalDataMock, result.get(0));
        assertEquals(medicalDataMock, result.get(1));
        assertEquals(historyDataMock, result.get(2));

        verify(dbOperator, times(1))
                .customSelection("SELECT * FROM person WHERE person_id = (SELECT patient.person_id FROM patient WHERE patient_id = '" + searchWord + "')");
        verify(dbOperator, times(1))
                .customSelection("SELECT patient.drug_allergies_and_reactions FROM patient WHERE patient_id = '" + searchWord + "'");
        verify(dbOperator, times(1))
                .customSelection("SELECT medical_history.date,medical_history.history FROM medical_history WHERE patient_id = '" + searchWord + "' ORDER BY date DESC");
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void givenInvalidSearchTypeWhenGettingPatientInfoThenReturnNullData() throws SQLException, ClassNotFoundException {
        // Given: searchType không hợp lệ
        String searchType = "invalid";
        String searchWord = "123";

        // When: Gọi phương thức getPatientInfo
        ArrayList<ArrayList<ArrayList<String>>> result = doctorInstance.getPatientInfo(searchType, searchWord);

        // Then: Kỳ vọng trả về danh sách chứa các phần tử null
        assertNotNull(result, "Kết quả không được null");
        assertEquals(3, result.size(), "Kết quả phải chứa 3 phần tử");
        assertNull(result.get(0), "personalData phải là null");
        assertNull(result.get(1), "medicalData phải là null");
        assertNull(result.get(2), "historyData phải là null");
        verifyNoInteractions(dbOperator); // Không gọi customSelection vì không có case nào khớp
    }
}
