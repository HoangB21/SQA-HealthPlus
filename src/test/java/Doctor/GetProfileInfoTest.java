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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class GetProfileInfoTest {
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
    public void testGetProfileInfoSuccess() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> profileData = new ArrayList<>();
        profileData.add(new ArrayList<>(Arrays.asList("first_name", "last_name")));
        profileData.add(new ArrayList<>(Arrays.asList("John", "Doe")));
        when(dbOperator.customSelection(anyString())).thenReturn(profileData);

        HashMap<String, String> result = doctorInstance.getProfileInfo();
        assertEquals("John", result.get("first_name"));
        assertEquals("Doe", result.get("last_name"));
    }
    @Test
    public void testGetProfileInfoNullData() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        HashMap<String, String> result = doctorInstance.getProfileInfo();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetProfileInfoException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        HashMap<String, String> result = doctorInstance.getProfileInfo();
        assertTrue(result.isEmpty());
    }
}
