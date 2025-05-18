package Cashier;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CashierGetCancelledDocAppointmentsIntegrationTest {

    private Cashier cashierInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "123456");
        connection.setAutoCommit(false);

        Statement stmt = connection.createStatement();
        // Insert sys_user first to satisfy person.user_id
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type, password, profile_pic) " +
                "VALUES ('hms020', 'user020', 'cashier', '1234', 'user020ProfPic.png') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user020', user_type = 'cashier', " +
                "password = '1234', profile_pic = 'user020ProfPic.png', person_id = NULL");
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type, password, profile_pic) " +
                "VALUES ('hms00081', 'user001', 'doctor', '1234', 'user001ProfPic.png') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user001', user_type = 'doctor', " +
                "password = '1234', profile_pic = 'user001ProfPic.png', person_id = NULL");
        // Insert person
        stmt.executeUpdate("INSERT INTO person (person_id, user_id, nic, gender, date_of_birth, address, mobile, " +
                "first_name, last_name, email, nationality, religion) " +
                "VALUES ('hms00001', 'hms020', '652489712V', 'M', '1965-09-04', 'No 26 Galla Estate Rajamawatha JaEla', " +
                "'0774523687', 'Kasun', 'Weerasekara', 'kasun35@gmail.com', 'Sri Lankan', 'Buddhism') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms020', nic = '652489712V', gender = 'M', " +
                "date_of_birth = '1965-09-04', address = 'No 26 Galla Estate Rajamawatha JaEla', mobile = '0774523687', " +
                "first_name = 'Kasun', last_name = 'Weerasekara', email = 'kasun35@gmail.com', " +
                "nationality = 'Sri Lankan', religion = 'Buddhism'");
        stmt.executeUpdate("INSERT INTO person (person_id, user_id, nic, gender, date_of_birth, address, mobile, " +
                "first_name, last_name, email, nationality, religion) " +
                "VALUES ('hms00081', 'hms00081', '723452312V', 'M', '1972-12-10', '67/a Jambugasmulla Lane Nugegoda', " +
                "'0772343544', 'Keerthi', 'Perera', 'keerthiperera72@yahoo.com', 'Sri Lankan', 'Buddhism') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms00081', nic = '723452312V', gender = 'M', " +
                "date_of_birth = '1972-12-10', address = '67/a Jambugasmulla Lane Nugegoda', mobile = '0772343544', " +
                "first_name = 'Keerthi', last_name = 'Perera', email = 'keerthiperera72@yahoo.com', " +
                "nationality = 'Sri Lankan', religion = 'Buddhism'");
        // Update sys_user with person_id
        stmt.executeUpdate("UPDATE sys_user SET person_id = 'hms00001' WHERE user_id = 'hms020'");
        stmt.executeUpdate("UPDATE sys_user SET person_id = 'hms00081' WHERE user_id = 'hms00081'");
        // Insert patient
        stmt.executeUpdate("INSERT INTO patient (patient_id, person_id) " +
                "VALUES ('hms0001pa', 'hms00001') " +
                "ON DUPLICATE KEY UPDATE person_id = 'hms00001'");
        // Insert doctor
        stmt.executeUpdate("INSERT INTO doctor (slmc_reg_no, user_id) " +
                "VALUES ('22387', 'hms00081') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms00081'");
        // Insert appointments
        stmt.executeUpdate("INSERT INTO appointment (appointment_id, date, cancelled, patient_id, consultant_id) " +
                "VALUES ('app001', '2024-11-01 10:00:00', 1, 'hms0001pa', '22387') " +
                "ON DUPLICATE KEY UPDATE date = '2024-11-01 10:00:00', cancelled = 1, patient_id = 'hms0001pa', consultant_id = '22387'");
        stmt.executeUpdate("INSERT INTO appointment (appointment_id, date, cancelled, patient_id, consultant_id) " +
                "VALUES ('app002', '2025-01-15 14:00:00', 1, 'hms0001pa', '22387') " +
                "ON DUPLICATE KEY UPDATE date = '2025-01-15 14:00:00', cancelled = 1, patient_id = 'hms0001pa', consultant_id = '22387'");
        stmt.executeUpdate("INSERT INTO appointment (appointment_id, date, cancelled, patient_id, consultant_id) " +
                "VALUES ('app003', '2023-10-01 09:00:00', 1, 'hms0001pa', '22387') " +
                "ON DUPLICATE KEY UPDATE date = '2023-10-01 09:00:00', cancelled = 1, patient_id = 'hms0001pa', consultant_id = '22387'");
        stmt.executeUpdate("INSERT INTO appointment (appointment_id, date, cancelled, patient_id, consultant_id) " +
                "VALUES ('app004', '2024-12-01 11:00:00', 0, 'hms0001pa', '22387') " +
                "ON DUPLICATE KEY UPDATE date = '2024-12-01 11:00:00', cancelled = 0, patient_id = 'hms0001pa', consultant_id = '22387'");
        stmt.close();

        cashierInstance = new Cashier("user020");
        cashierInstance.dbOperator = new DatabaseOperator() {
            @Override
            public ArrayList<ArrayList<String>> customSelection(String query) throws SQLException, ClassNotFoundException {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                ResultSetMetaData rsmd = rs.getMetaData();
                int noOfColumns = rsmd.getColumnCount();

                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= noOfColumns; i++) {
                    columnNames.add(rsmd.getColumnName(i));
                }
                result.add(columnNames);

                while (rs.next()) {
                    ArrayList<String> row = new ArrayList<>();
                    for (int i = 1; i <= noOfColumns; i++) {
                        row.add(rs.getString(i));
                    }
                    result.add(row);
                }
                rs.close();
                stmt.close();
                return result;
            }

            @Override
            public ArrayList<ArrayList<String>> showTableData(String table, String columns, String condition)
                    throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columnNames = new ArrayList<>(Arrays.asList(columns.split(",")));
                result.add(columnNames);
                if (table.equals("sys_user") && columns.equals("user_id,user_type") && condition.contains("user_name = 'user020'")) {
                    ArrayList<String> row = new ArrayList<>();
                    row.add("hms020");
                    row.add("cashier");
                    result.add(row);
                }
                return result;
            }
        };
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
    public void testGetCancelledDocAppointments_SuccessfulRetrieval() throws SQLException {
        ArrayList<ArrayList<String>> result = cashierInstance.getCancelledDocAppointments();

        assertNotNull(result);
        assertEquals(3, result.size()); // 1 header + 2 data rows within 12 months
        assertEquals("date", result.get(0).get(0));
        assertEquals("2024-11-01 10:00:00", result.get(1).get(0));
        assertEquals("2025-01-15 14:00:00", result.get(2).get(0));
    }

    @Test
    public void testGetCancelledDocAppointments_NoCancelledAppointments() throws SQLException {
        // Delete cancelled appointments within 12 months
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM appointment WHERE cancelled = 1 AND date > (CURRENT_DATE - INTERVAL 12 MONTH)");
        stmt.close();

        ArrayList<ArrayList<String>> result = cashierInstance.getCancelledDocAppointments();

        assertNotNull(result);
        assertEquals(1, result.size()); // Only header row
        assertEquals("date", result.get(0).get(0));
    }

    @Test
    public void testGetCancelledDocAppointments_NoAppointments() throws SQLException {
        // Delete all appointments
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM appointment");
        stmt.close();

        ArrayList<ArrayList<String>> result = cashierInstance.getCancelledDocAppointments();

        assertNotNull(result);
        assertEquals(1, result.size()); // Only header row
        assertEquals("date", result.get(0).get(0));
    }

    @Test
    public void testGetCancelledDocAppointments_OldAppointmentsOnly() throws SQLException {
        // Delete appointments within 12 months
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM appointment WHERE date > (CURRENT_DATE - INTERVAL 12 MONTH)");
        stmt.close();

        ArrayList<ArrayList<String>> result = cashierInstance.getCancelledDocAppointments();

        assertNotNull(result);
        assertEquals(1, result.size()); // Only header row
        assertEquals("date", result.get(0).get(0));
    }

    @Test
    public void testGetCancelledDocAppointments_NonCancelledExcluded() throws SQLException {
        // Ensure non-cancelled appointments are excluded
        ArrayList<ArrayList<String>> result = cashierInstance.getCancelledDocAppointments();

        assertNotNull(result);
        assertEquals(3, result.size()); // 1 header + 2 cancelled appointments
        assertFalse(result.stream().anyMatch(row -> row.get(0).equals("2024-12-01 11:00:00"))); // Non-cancelled
    }
}