package Cashier;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class CashierRemoveFromTempBillIntegrationTest {

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
                "VALUES ('hms020', 'user020', 'cashier', '1234', 'user020ProfPic.png') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user020', user_type = 'cashier', " +
                "password = '1234', profile_pic = 'user020ProfPic.png', person_id = NULL");
        stmt.executeUpdate("INSERT INTO person (person_id, user_id, nic, gender, date_of_birth, address, mobile, " +
                "first_name, last_name, email, nationality, religion) " +
                "VALUES ('p020', 'hms020', '872984565V', 'M', '1987-10-24', '123/f Yanthampalawa Kurunegala', " +
                "'0713457779', 'Lakshitha', 'Rangana', 'lakshithaasd@gmail.com', 'Sri Lankan', 'Buddhism') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms020', nic = '872984565V', gender = 'M', " +
                "date_of_birth = '1987-10-24', address = '123/f Yanthampalawa Kurunegala', mobile = '0713457779', " +
                "first_name = 'Lakshitha', last_name = 'Rangana', email = 'lakshithaasd@gmail.com', " +
                "nationality = 'Sri Lankan', religion = 'Buddhism'");
        stmt.executeUpdate("UPDATE sys_user SET person_id = 'p020' WHERE user_id = 'hms020'");
        stmt.executeUpdate("INSERT INTO patient (patient_id, person_id) " +
                "VALUES ('hms0001pa', 'p020') " +
                "ON DUPLICATE KEY UPDATE person_id = 'p020'");
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type) " +
                "VALUES ('hms00081', 'user001', 'doctor') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user001', user_type = 'doctor'");
        stmt.executeUpdate("INSERT INTO doctor (slmc_reg_no, user_id) " +
                "VALUES ('doc001', 'hms00081') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms00081'");
        stmt.executeUpdate("INSERT INTO tmp_bill (tmp_bill_id, patient_id, pharmacy_fee, consultant_id) " +
                "VALUES ('hms0001tb', 'hms0001pa', 300, 'doc001') " +
                "ON DUPLICATE KEY UPDATE patient_id = 'hms0001pa', pharmacy_fee = 300, consultant_id = 'doc001'");
        stmt.close();

        cashierInstance = new Cashier("user020");
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
    public void testRemoveFromTempBill_SuccessfulDeletion() throws SQLException {
        boolean result = cashierInstance.removeFromTempBill("hms0001pa");

        assertTrue(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        assertFalse(rs.next());
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testRemoveFromTempBill_NoRecord() throws SQLException {
        boolean result = cashierInstance.removeFromTempBill("invalid");

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM tmp_bill WHERE tmp_bill_id = 'hms0001tb'");
        assertTrue(rs.next());
        rs.close();
        verifyStmt.close();
    }
}