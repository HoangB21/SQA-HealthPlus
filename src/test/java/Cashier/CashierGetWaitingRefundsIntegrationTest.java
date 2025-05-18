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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class CashierGetWaitingRefundsIntegrationTest {

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
        stmt.executeUpdate("INSERT INTO bill (bill_id, bill_date, doctor_fee, hospital_fee, pharmacy_fee, " +
                "laboratory_fee, appointment_fee, vat, discount, total, payment_method, patient_id, refund) " +
                "VALUES ('hms0007b', '2016-08-30 14:30:00', 200, 150, 300, 0, 500, 60, 0, 1210, 'pending', 'hms0001pa', 0) " +
                "ON DUPLICATE KEY UPDATE bill_date = '2016-08-30 14:30:00', doctor_fee = 200, hospital_fee = 150, " +
                "pharmacy_fee = 300, laboratory_fee = 0, appointment_fee = 500, vat = 60, discount = 0, total = 1210, " +
                "payment_method = 'pending', patient_id = 'hms0001pa', refund = 0");
        stmt.executeUpdate("INSERT INTO refund (refund_id, bill_id, payment_type, amount, reason, date) " +
                "VALUES ('r0001', 'hms0007b', 'cash', 100, 'overpayment', '2016-08-30 14:30:00') " +
                "ON DUPLICATE KEY UPDATE bill_id = 'hms0007b', payment_type = 'cash', amount = 100, " +
                "reason = 'overpayment', date = '2016-08-30 14:30:00'");
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
    public void testGetWaitingRefunds_SuccessfulRetrieval() throws SQLException {
        ArrayList<ArrayList<String>> result = cashierInstance.getWaitingRefunds();

        assertNotNull(result);
        assertTrue(result.size() > 1);
        assertEquals("refund_id", result.get(0).get(0));
        assertEquals("bill_id", result.get(0).get(1));
        assertEquals("payment_type", result.get(0).get(2));
        assertEquals("reason", result.get(0).get(3));
        assertEquals("amount", result.get(0).get(4));
        assertEquals("date", result.get(0).get(5));
        assertEquals("r0001", result.get(1).get(0));
        assertEquals("hms0007b", result.get(1).get(1));
        assertEquals("cash", result.get(1).get(2));
        assertEquals("overpayment", result.get(1).get(3));
        assertEquals("100", result.get(1).get(4));
        assertEquals("2016-08-30 14:30:00", result.get(1).get(5));
    }

    @Test
    public void testGetWaitingRefunds_NoData() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund WHERE refund_id = 'r0001'");
        stmt.close();

        ArrayList<ArrayList<String>> result = cashierInstance.getWaitingRefunds();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("refund_id", result.get(0).get(0));
        assertEquals("bill_id", result.get(0).get(1));
        assertEquals("payment_type", result.get(0).get(2));
        assertEquals("reason", result.get(0).get(3));
        assertEquals("amount", result.get(0).get(4));
        assertEquals("date", result.get(0).get(5));
    }

    @Test
    public void testGetWaitingRefunds_MultipleRefunds() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO refund (refund_id, bill_id, payment_type, amount, reason, date) " +
                "VALUES ('r0002', 'hms0007b', 'card', 200, 'return', '2016-08-31 10:00:00') " +
                "ON DUPLICATE KEY UPDATE bill_id = 'hms0007b', payment_type = 'card', amount = 200, " +
                "reason = 'return', date = '2016-08-31 10:00:00'");
        stmt.close();

        ArrayList<ArrayList<String>> result = cashierInstance.getWaitingRefunds();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("r0001", result.get(1).get(0));
        assertEquals("r0002", result.get(2).get(0));
    }
}