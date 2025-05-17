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

public class DiagnoseTest {
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
    public void testDiagnoseSuccessWithZeroPadding() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> maxHistoryData = new ArrayList<>();
        maxHistoryData.add(new ArrayList<>(Arrays.asList("history_id")));
        maxHistoryData.add(new ArrayList<>(Arrays.asList("his0001")));
        when(dbOperator.customSelection(anyString())).thenReturn(maxHistoryData);
        when(dbOperator.customInsertion(anyString())).thenReturn(true);

        boolean result = doctorInstance.diagnose("Diabetes", "pat001");
        assertTrue(result);
        verify(dbOperator, times(1)).customInsertion(contains("his0002"));
    }

    @Test
    public void testDiagnoseSuccessWithoutZeroPadding() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> maxHistoryData = new ArrayList<>();
        maxHistoryData.add(new ArrayList<>(Arrays.asList("history_id")));
        maxHistoryData.add(new ArrayList<>(Arrays.asList("his0010")));
        when(dbOperator.customSelection(anyString())).thenReturn(maxHistoryData);
        when(dbOperator.customInsertion(anyString())).thenReturn(true);

        boolean result = doctorInstance.diagnose("Diabetes", "pat001");
        assertTrue(result);
        verify(dbOperator, times(1)).customInsertion(contains("his0011"));
    }

    @Test
    public void testDiagnoseException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        boolean result = doctorInstance.diagnose("Diabetes", "pat001");
        assertFalse(result);
    }
    @Test
    public void testDiagnoseInvalidHistoryIDLength() throws SQLException, ClassNotFoundException {
        String diagnostic = "Flu";
        String patientID = "pat001";

        // Mock: historyID không hợp lệ (độ dài nhỏ hơn 4)
        ArrayList<ArrayList<String>> historyIDResult = new ArrayList<>();
        historyIDResult.add(new ArrayList<>(Arrays.asList("history_id")));
        historyIDResult.add(new ArrayList<>(Arrays.asList("his"))); // Độ dài 3
        when(dbOperator.customSelection("SELECT history_id FROM medical_history WHERE history_id = (SELECT MAX(history_id) FROM medical_history);"))
                .thenReturn(historyIDResult);

        // Kỳ vọng tmpID2 = "his0001" (do historyID không hợp lệ, dùng giá trị mặc định)
        String expectedSql = "INSERT INTO medical_history VALUES (" +
                "'his0001'," + patientID + "," + doctorInstance.slmcRegNo + "," +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) +
                "," + diagnostic + ");";
        when(dbOperator.customInsertion(expectedSql)).thenReturn(true);

        boolean result = doctorInstance.diagnose(diagnostic, patientID);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT history_id FROM medical_history WHERE history_id = (SELECT MAX(history_id) FROM medical_history);");
        verify(dbOperator, times(1)).customInsertion(expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
}