package Doctor;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DoctorTimeTableAddSlotTest {
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
        doctorInstance.slmcRegNo = "22387";
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // Rollback giao dịch và đóng kết nối
        if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    @Test
    public void testDoctorTimeTableAddSlotSuccessWithZeroPadding() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> maxTimeSlotData = new ArrayList<>();
        maxTimeSlotData.add(new ArrayList<>(Arrays.asList("time_slot_id")));
        maxTimeSlotData.add(new ArrayList<>(Arrays.asList("t0001")));
        when(dbOperator.customSelection(anyString())).thenReturn(maxTimeSlotData);
        when(dbOperator.addTableRow(anyString(), anyString())).thenReturn(true);

        boolean result = doctorInstance.doctorTimeTableAddSlot("Monday", "09:00-10:00");
        assertTrue(result);
        verify(dbOperator, times(1)).addTableRow(eq("doctor_availability"), contains("t0002"));
    }

    @Test
    public void testDoctorTimeTableAddSlotSuccessWithoutZeroPadding() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> maxTimeSlotData = new ArrayList<>();
        maxTimeSlotData.add(new ArrayList<>(Arrays.asList("time_slot_id")));
        maxTimeSlotData.add(new ArrayList<>(Arrays.asList("t0010")));
        when(dbOperator.customSelection(anyString())).thenReturn(maxTimeSlotData);
        when(dbOperator.addTableRow(anyString(), anyString())).thenReturn(true);

        boolean result = doctorInstance.doctorTimeTableAddSlot("Monday", "09:00-10:00");
        assertTrue(result);
        verify(dbOperator, times(1)).addTableRow(eq("doctor_availability"), contains("t0011"));
    }

    @Test
    public void testDoctorTimeTableAddSlotException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        boolean result = doctorInstance.doctorTimeTableAddSlot("Monday", "09:00-10:00");
        assertFalse(result);
    }
}