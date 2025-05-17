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

public class SearchByNameTest {
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
    public void testSearchByNameSinglePart() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> nameData = new ArrayList<>();
        nameData.add(new ArrayList<>(Arrays.asList("patient_id", "first_name", "last_name", "date_of_birth")));
        nameData.add(new ArrayList<>(Arrays.asList("pat001", "John", "Doe", "1990-01-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(nameData);

        ArrayList<ArrayList<String>> result = doctorInstance.searchByName("John");
        assertEquals("pat001", result.get(1).get(0));
    }

    @Test
    public void testSearchByNameTwoParts() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> nameData = new ArrayList<>();
        nameData.add(new ArrayList<>(Arrays.asList("patient_id", "first_name", "last_name", "date_of_birth")));
        nameData.add(new ArrayList<>(Arrays.asList("pat001", "John", "Doe", "1990-01-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(nameData);

        ArrayList<ArrayList<String>> result = doctorInstance.searchByName("John Doe");
        assertEquals("pat001", result.get(1).get(0));
    }

    @Test
    public void testSearchByNameMoreThanTwoParts() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> nameData = new ArrayList<>();
        nameData.add(new ArrayList<>(Arrays.asList("patient_id", "first_name", "last_name", "date_of_birth")));
        nameData.add(new ArrayList<>(Arrays.asList("pat001", "John", "Doe", "1990-01-01")));
        when(dbOperator.customSelection(anyString())).thenReturn(nameData);

        ArrayList<ArrayList<String>> result = doctorInstance.searchByName("John Doe Smith");
        assertEquals("pat001", result.get(1).get(0));
    }

    @Test
    public void testSearchByNameException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = doctorInstance.searchByName("John");
        assertNull(result);
    }
    @Test
    public void testSearchByNameOnlyWhitespace() throws SQLException, ClassNotFoundException {
        String namePart = "  "; // Chỉ chứa khoảng trắng

        ArrayList<ArrayList<String>> result = doctorInstance.searchByName(namePart);

        assertNull(result); // Kỳ vọng trả về null vì namePart chỉ chứa khoảng trắng
        verifyNoInteractions(dbOperator); // Không gọi customSelection
    }
    @Test
    public void testSearchByNameMoreThanTwoWords() throws SQLException, ClassNotFoundException {
        String namePart = "John Doe Extra"; // Hơn 2 từ

        // Mock kết quả trả về từ customSelection
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>(Arrays.asList("patient_id", "first_name", "last_name", "date_of_birth")));
        mockResult.add(new ArrayList<>(Arrays.asList("pat001", "John", "Doe", "1990-01-01")));
        when(dbOperator.customSelection("SELECT patient.patient_id, person.first_name, person.last_name, person.date_of_birth " +
                "FROM person INNER JOIN patient ON person.person_id = patient.person_id " +
                "WHERE person.first_name LIKE 'John%' AND person.last_name LIKE 'Doe%' " +
                "LIMIT 10;"))
                .thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = doctorInstance.searchByName(namePart);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("pat001", result.get(1).get(0));
        assertEquals("John", result.get(1).get(1));
        assertEquals("Doe", result.get(1).get(2));
        assertEquals("1990-01-01", result.get(1).get(3));

        verify(dbOperator, times(1))
                .customSelection("SELECT patient.patient_id, person.first_name, person.last_name, person.date_of_birth " +
                        "FROM person INNER JOIN patient ON person.person_id = patient.person_id " +
                        "WHERE person.first_name LIKE 'John%' AND person.last_name LIKE 'Doe%' " +
                        "LIMIT 10;");
        verifyNoMoreInteractions(dbOperator);
    }
}
