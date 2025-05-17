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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

public class GetTestInfoTest {
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
    public void testGetTestInfoSuccess() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> testData = new ArrayList<>();
        testData.add(new ArrayList<>(Arrays.asList("test_id", "test_name", "test_description")));
        testData.add(new ArrayList<>(Arrays.asList("tst001", "BloodTest", "Complete Blood Count")));
        when(dbOperator.customSelection(anyString())).thenReturn(testData);

        ArrayList<ArrayList<String>> result = doctorInstance.getTestInfo();
        assertEquals("BloodTest", result.get(1).get(1));
    }

    @Test
    public void testGetTestInfoException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = doctorInstance.getTestInfo();
        assertNull(result);
    }
}