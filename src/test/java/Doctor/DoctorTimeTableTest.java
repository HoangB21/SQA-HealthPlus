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
import static org.mockito.Mockito.*;

public class DoctorTimeTableTest {
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
    public void testDoctorTimeTableSuccess() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> timeTableData = new ArrayList<>();
        timeTableData.add(new ArrayList<>(Arrays.asList("day", "time_slot", "time_slot_id")));
        timeTableData.add(new ArrayList<>(Arrays.asList("Monday", "09:00-10:00", "t0001")));
        when(dbOperator.showTableData(anyString(), anyString(), anyString())).thenReturn(timeTableData);

        ArrayList<ArrayList<String>> result = doctorInstance.doctorTimeTable();
        assertEquals("day", result.get(0).get(0));
        assertEquals("Monday", result.get(1).get(0));
    }

    @Test
    public void testDoctorTimeTableNullData() throws SQLException, ClassNotFoundException {
        when(dbOperator.showTableData(anyString(), anyString(), anyString())).thenReturn(null);

        ArrayList<ArrayList<String>> result = doctorInstance.doctorTimeTable();
        assertNull(result);
    }

    @Test
    public void testDoctorTimeTableException() throws SQLException, ClassNotFoundException {
        when(dbOperator.showTableData(anyString(), anyString(), anyString()))
                .thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = doctorInstance.doctorTimeTable();
        assertNull(result);
    }
    @Test
    public void testDoctorTimeTableAddSlotInvalidTimeSlotIDLength() throws SQLException, ClassNotFoundException {
        String day = "Monday";
        String timeSlot = "09:00-10:00";

        // Mock: timeSlotID không hợp lệ (độ dài nhỏ hơn 2)
        ArrayList<ArrayList<String>> timeSlotIDResult = new ArrayList<>();
        timeSlotIDResult.add(new ArrayList<>(Arrays.asList("time_slot_id")));
        timeSlotIDResult.add(new ArrayList<>(Arrays.asList("t"))); // Độ dài 1
        when(dbOperator.customSelection("SELECT time_slot_id FROM doctor_availability WHERE time_slot_id = (SELECT MAX(time_slot_id) FROM doctor_availability);"))
                .thenReturn(timeSlotIDResult);

        // Kỳ vọng tmpID2 = "t0001" (do timeSlotID không hợp lệ, dùng giá trị mặc định)
        String expectedSql = "t0001," + doctorInstance.slmcRegNo + "," + day + "," + timeSlot + ",0,0";
        when(dbOperator.addTableRow("doctor_availability", expectedSql)).thenReturn(true);

        boolean result = doctorInstance.doctorTimeTableAddSlot(day, timeSlot);

        assertTrue(result);
        verify(dbOperator, times(1))
                .customSelection("SELECT time_slot_id FROM doctor_availability WHERE time_slot_id = (SELECT MAX(time_slot_id) FROM doctor_availability);");
        verify(dbOperator, times(1)).addTableRow("doctor_availability", expectedSql);
        verifyNoMoreInteractions(dbOperator);
    }
}