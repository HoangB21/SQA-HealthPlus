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

public class CashierRefundIntegrationTest {

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
    public void testRefund_SuccessfulInsertion() throws SQLException {
        String refundInfo = "bill_id hms0007b,payment_type cash,amount 200,reason return";
        boolean result = cashierInstance.refund(refundInfo);

        assertTrue(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM refund WHERE refund_id = 'r0002'");
        assertTrue(rs.next());
        assertEquals("hms0007b", rs.getString("bill_id"));
        assertEquals("cash", rs.getString("payment_type"));
        assertEquals(200, rs.getInt("amount"));
        assertEquals("return", rs.getString("reason"));
        assertNotNull(rs.getString("date"));
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testRefund_EmptyRefundInfo() throws SQLException {
        String refundInfo = "";
        boolean result = cashierInstance.refund(refundInfo);

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM refund WHERE refund_id = 'r0002'");
        assertFalse(rs.next());
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testRefund_NonExistentBill() throws SQLException {
        String refundInfo = "bill_id invalid,payment_type cash,amount 200,reason return";
        boolean result = cashierInstance.refund(refundInfo);

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM refund WHERE refund_id = 'r0002'");
        assertFalse(rs.next());
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testRefund_MalformedRefundInfo() throws SQLException {
        String refundInfo = "bill_id hms0007b,amount 200";
        boolean result = cashierInstance.refund(refundInfo);

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM refund WHERE refund_id = 'r0002'");
        assertFalse(rs.next());
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testRefund_NullRefundInfo() throws SQLException {
        boolean result = cashierInstance.refund(null);

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM refund WHERE refund_id = 'r0002'");
        assertFalse(rs.next());
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testRefund_DuplicateRefundID() throws SQLException {
        String refundInfo = "bill_id hms0007b,payment_type cash,amount 200,reason return";
        cashierInstance.refund(refundInfo); // Chèn refund_id = 'r0002'

        boolean result = cashierInstance.refund(refundInfo); // Thử chèn lại với cùng refund_id

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM refund WHERE refund_id = 'r0002'");
        assertTrue(rs.next());
        assertEquals("hms0007b", rs.getString("bill_id"));
        assertEquals(200, rs.getInt("amount"));
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testRefund_DifferentPaymentType() throws SQLException {
        String refundInfo = "bill_id hms0007b,payment_type card,amount 150,reason error";
        boolean result = cashierInstance.refund(refundInfo);

        assertTrue(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM refund WHERE refund_id = 'r0002'");
        assertTrue(rs.next());
        assertEquals("hms0007b", rs.getString("bill_id"));
        assertEquals("card", rs.getString("payment_type"));
        assertEquals(150, rs.getInt("amount"));
        assertEquals("error", rs.getString("reason"));
        assertNotNull(rs.getString("date"));
        rs.close();
        verifyStmt.close();
    }
}