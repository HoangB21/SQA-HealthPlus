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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

public class GetDrugBrandInfoTest {
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
    public void testGetDrugBrandInfoSuccess() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> brandData = new ArrayList<>();
        brandData.add(new ArrayList<>(Arrays.asList("brand_name")));
        brandData.add(new ArrayList<>(Arrays.asList("Panadol")));
        brandData.add(new ArrayList<>(Arrays.asList("Calpol")));
        when(dbOperator.customSelection(anyString())).thenReturn(brandData);

        ArrayList<String> result = doctorInstance.getDrugBrandInfo("Paracetamol");
        assertEquals(2, result.size());
        assertTrue(result.contains("Panadol"));
        assertTrue(result.contains("Calpol"));
    }

    @Test
    public void testGetDrugBrandInfoNullData() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        ArrayList<String> result = doctorInstance.getDrugBrandInfo("Paracetamol");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetDrugBrandInfoException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<String> result = doctorInstance.getDrugBrandInfo("Paracetamol");
        assertTrue(result.isEmpty());
    }
}