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
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class GetAppointmentsTest {
    @Mock
    private DatabaseOperator dbOperator;
    private Connection connection;

    private Doctor doctorInstance;
    private AutoCloseable closeable;
    @BeforeEach
    public void setUp() throws SQLException {
        closeable = MockitoAnnotations.openMocks(this);

        connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true", "root", "Huycode12003.");

        // Start a transaction to allow rollback after the test
        connection.setAutoCommit(false);
        doctorInstance = new Doctor("user001");
        MockitoAnnotations.openMocks(this);
        doctorInstance.dbOperator = dbOperator;
        doctorInstance.slmcRegNo = "22387";

    }
    @AfterEach
    public void tearDown() throws Exception {
        // Rollback database changes to restore the original state
        connection.rollback();
        connection.setAutoCommit(true);
        connection.close();

        // Close Mockito resources
        closeable.close();
    }
    @Test
    public void testGetAppointmentsSuccess() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> appointmentData = new ArrayList<>();
        appointmentData.add(new ArrayList<>(Arrays.asList("patient_id", "date")));
        appointmentData.add(new ArrayList<>(Arrays.asList("pat001", "2023-10-01")));
        when(dbOperator.showTableData(anyString(), anyString(), anyString())).thenReturn(appointmentData);

        ArrayList<ArrayList<String>> result = doctorInstance.getAppointments();
        assertEquals("patient", result.get(0).get(0));
        assertEquals("pat001", result.get(1).get(0));
    }

    @Test
    public void testGetAppointmentsNullData() throws SQLException, ClassNotFoundException {
        when(dbOperator.showTableData(anyString(), anyString(), anyString())).thenReturn(null);

        ArrayList<ArrayList<String>> result = doctorInstance.getAppointments();
        assertNull(result);
    }

    @Test
    public void testGetAppointmentsException() throws SQLException, ClassNotFoundException {
        when(dbOperator.showTableData(anyString(), anyString(), anyString()))
                .thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = doctorInstance.getAppointments();
        assertNull(result);
    }
}
