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

public class AllergiesTest {
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
    public void testAllergies_Success() throws Exception {
        String patientID = "P001";
        String allergies = "Penicillin";

        // Giả lập DB trả về allergy hiện tại là "Dust"
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>()); // Header row
        ArrayList<String> row = new ArrayList<>();
        row.add("Dust"); // Giá trị hiện tại
        mockResult.add(row);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        doNothing().when(dbOperator).customInsertion(anyString());

        boolean result = doctorInstance.allergies(allergies, patientID);

        assertTrue(result);
        // Kiểm tra SQL đã được gọi đúng
        verify(dbOperator).customInsertion(contains("Dust,Penicillin"));
    }

    @Test
    public void testAllergies_DBException() throws Exception {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("DB error"));

        boolean result = doctorInstance.allergies("Peanuts", "P002");

        assertFalse(result);
    }
    @Test
    public void testAllergies_ClassNotFoundException() throws Exception {
        when(dbOperator.customSelection(anyString())).thenThrow(new ClassNotFoundException("Driver not found"));

        boolean result = doctorInstance.allergies("Peanuts", "P003");

        assertFalse(result);
        verify(dbOperator, times(1)).customSelection(anyString());
    }
    @Test
    public void testAllergiesInvalidInput() {
        String allergies = null;
        String patientID = "pat001";

        boolean result = doctorInstance.allergies(allergies, patientID);

        assertFalse(result); // Kỳ vọng trả về false vì allergies là null
        verifyNoInteractions(dbOperator); // Không gọi dbOperator
    }
    @Test
    public void givenValidDataWhenAddingAllergiesThenUpdateSuccessfully() throws SQLException, ClassNotFoundException {
        // Given: Dữ liệu hợp lệ
        String allergies = "Peanuts";
        String patientID = "pat001";
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList("drug_allergies_and_reactions")));
        data.add(new ArrayList<>(Arrays.asList("Penicillin")));
        given(dbOperator.customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';"))
                .willReturn(data);
        willDoNothing().given(dbOperator)
                .customInsertion("UPDATE patient SET drug_allergies_and_reactions = 'Penicillin,Peanuts' WHERE patient_id = '" + patientID + "';");

        // When: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Then: Kỳ vọng trả về true và gọi đúng các phương thức
        assertTrue(result, "Phải trả về true khi thêm dị ứng thành công");
        verify(dbOperator).customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';");
        verify(dbOperator).customInsertion("UPDATE patient SET drug_allergies_and_reactions = 'Penicillin,Peanuts' WHERE patient_id = '" + patientID + "';");
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void givenDatabaseErrorWhenAddingAllergiesThenReturnFalse() throws SQLException, ClassNotFoundException {
        // Given: Database ném ngoại lệ
        String allergies = "Peanuts";
        String patientID = "pat001";
        given(dbOperator.customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';"))
                .willThrow(new SQLException("Database error"));

        // When: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Then: Kỳ vọng trả về false
        assertFalse(result, "Phải trả về false khi ném ngoại lệ");
        verify(dbOperator).customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';");
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void givenInvalidDataWhenAddingAllergiesThenThrowExceptionAndReturnFalse() throws SQLException, ClassNotFoundException {
        // Given: Dữ liệu không hợp lệ (data rỗng)
        String allergies = "Peanuts";
        String patientID = "pat001";
        ArrayList<ArrayList<String>> emptyData = new ArrayList<>();
        emptyData.add(new ArrayList<>(Arrays.asList("drug_allergies_and_reactions"))); // Chỉ có header
        given(dbOperator.customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';"))
                .willReturn(emptyData);

        // When: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Then: Kỳ vọng trả về false vì ném IndexOutOfBoundsException
        assertFalse(result, "Phải trả về false khi dữ liệu không hợp lệ");
        verify(dbOperator).customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';");
        verifyNoMoreInteractions(dbOperator);
    }
    @Test
    public void givenNullInputWhenAddingAllergiesThenHandleErrorGracefully() throws SQLException, ClassNotFoundException {
        // Given: Đầu vào null
        String allergies = null;
        String patientID = "pat001";
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList("drug_allergies_and_reactions")));
        data.add(new ArrayList<>(Arrays.asList("Penicillin")));
        given(dbOperator.customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';"))
                .willReturn(data);

        // When: Gọi phương thức allergies
        boolean result = doctorInstance.allergies(allergies, patientID);

        // Then: Kỳ vọng trả về false vì ném NullPointerException khi nối chuỗi
        assertFalse(result, "Phải trả về false khi đầu vào null");
        verify(dbOperator).customSelection("SELECT drug_allergies_and_reactions FROM patient WHERE patient_id = '" + patientID + "';");
        verifyNoMoreInteractions(dbOperator);
    }

}