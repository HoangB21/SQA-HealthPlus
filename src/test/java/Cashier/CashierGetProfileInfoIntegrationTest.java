package Cashier;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class CashierGetProfileInfoIntegrationTest {

    private Cashier cashierInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "123456");
        connection.setAutoCommit(false);

        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type, password, profile_pic) " +
                "VALUES ('hms0020u', 'user020', 'cashier', '1234', 'user020ProfPic.png') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user020', user_type = 'cashier', " +
                "password = '1234', profile_pic = 'user020ProfPic.png', person_id = NULL");
        stmt.executeUpdate("INSERT INTO person (person_id, user_id, first_name, last_name, nic, gender, date_of_birth, address, mobile, email, nationality, religion) " +
                "VALUES ('hms00001', 'hms0020u', 'Kasun', 'Weerasekara', '652489712V', 'M', '1965-09-04', 'No 26 Galla Estate Rajamawatha JaEla', '0774523687', 'kasun35@gmail.com', 'Sri Lankan', 'Buddhism') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms0020u', first_name = 'Kasun', last_name = 'Weerasekara', nic = '652489712V', gender = 'M', " +
                "date_of_birth = '1965-09-04', address = 'No 26 Galla Estate Rajamawatha JaEla', mobile = '0774523687', email = 'kasun35@gmail.com', nationality = 'Sri Lankan', religion = 'Buddhism'");
        stmt.executeUpdate("UPDATE sys_user SET person_id = 'hms00001' WHERE user_id = 'hms0020u'");
        stmt.close();

        cashierInstance = new Cashier("user020");
        cashierInstance.dbOperator = new DatabaseOperator() {
            @Override
            public ArrayList<ArrayList<String>> showTableData(String table, String columns, String condition)
                    throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columnNames = new ArrayList<>(Arrays.asList(columns.split(",")));
                result.add(columnNames);
                if (table.equals("sys_user") && columns.equals("user_id,user_type") && condition.contains("user_name = 'user020'")) {
                    ArrayList<String> row = new ArrayList<>();
                    row.add("hms0020u");
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
    public void testGetProfileInfo_Success() {
        HashMap<String, String> result = cashierInstance.getProfileInfo();

        assertNotNull(result, "Result should not be null");
        assertEquals("hms0020u", result.get("user_id"), "User ID should match");
        assertEquals("Kasun", result.get("first_name"), "First name should match");
        assertEquals("Weerasekara", result.get("last_name"), "Last name should match");
        assertEquals("652489712V", result.get("nic"), "NIC should match");
        assertEquals("M", result.get("gender"), "Gender should match");
    }

    @Test
    public void testGetProfileInfo_DatabaseError() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("ALTER TABLE person RENAME TO person_backup");
        stmt.close();

        HashMap<String, String> result = cashierInstance.getProfileInfo();

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty on database error");

        // Restore table for rollback
        Statement restoreStmt = connection.createStatement();
        restoreStmt.executeUpdate("ALTER TABLE person_backup RENAME TO person");
        restoreStmt.close();
    }

    @Test
    public void testGetProfileInfo_AdditionalFields() {
        HashMap<String, String> result = cashierInstance.getProfileInfo();

        assertNotNull(result, "Result should not be null");
        assertEquals("No 26 Galla Estate Rajamawatha JaEla", result.get("address"), "Address should match");
        assertEquals("0774523687", result.get("mobile"), "Mobile should match");
        assertEquals("kasun35@gmail.com", result.get("email"), "Email should match");
        assertEquals("Sri Lankan", result.get("nationality"), "Nationality should match");
        assertEquals("Buddhism", result.get("religion"), "Religion should match");
    }

    @Test
    public void testGetProfileInfo_ConsistentUserType() {
        HashMap<String, String> result = cashierInstance.getProfileInfo();

        assertNotNull(result, "Result should not be null");
        assertEquals("cashier", result.get("user_type"), "User type should match");
        assertEquals("user020ProfPic.png", result.get("profile_pic"), "Profile picture should match");
    }

    @Test
    public void testGetProfileInfo_ValidPersonId() {
        HashMap<String, String> result = cashierInstance.getProfileInfo();

        assertNotNull(result, "Result should not be null");
        assertEquals("hms00001", result.get("person_id"), "Person ID should match");
        assertEquals("1965-09-04", result.get("date_of_birth"), "Date of birth should match");
    }
}