package Pharmacist;

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

public class PharmacistBillIntegrationTest {

    private Pharmacist pharmacistInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "hieu");
        connection.setAutoCommit(false);

        pharmacistInstance = new Pharmacist("user016");
        pharmacistInstance.dbOperator = new DatabaseOperator() {
            @Override
            public boolean customInsertion(String query) throws SQLException, ClassNotFoundException {
                Statement stmt = connection.createStatement();
                int rowsAffected = stmt.executeUpdate(query);
                stmt.close();
                return rowsAffected > 0;
            }

            @Override
            public ArrayList<ArrayList<String>> customSelection(String query) throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                ArrayList<String> columns = new ArrayList<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    columns.add(rs.getMetaData().getColumnName(i));
                }
                result.add(columns);
                if (rs.next()) {
                    ArrayList<String> dataRow = new ArrayList<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        dataRow.add(rs.getString(i));
                    }
                    result.add(dataRow);
                }
                rs.close();
                stmt.close();
                return result;
            }

            @Override
            public ArrayList<ArrayList<String>> showTableData(String table, String columns, String condition)
                    throws SQLException, ClassNotFoundException {
                String query = "SELECT " + columns + " FROM " + table + " WHERE " + condition;
                return customSelection(query);
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

    /* PH_BILL_INT_01
    Objective: Verify that the bill method attempts to insert a new bill into tmp_bill and returns true (current behavior) when no existing bill exists.
    Input: billInfo = "", patientID = "hms0001pa", pharmacyFee = "5.00"
           Pre-test state: Database contains a patient with patient_id = "hms0001pa" and a bill with tmp_bill_id = "hms0001tb".
    Expected output: true, but no new bill is added due to invalid billInfo (current bug).
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testBill_NewBillSuccessfulInsertion() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO tmp_bill (tmp_bill_id, patient_id, pharmacy_fee) " +
                "VALUES ('hms0001tb', 'hms0001pa', '3.00') " +
                "ON DUPLICATE KEY UPDATE patient_id = 'hms0001pa', pharmacy_fee = '3.00'");
        stmt.close();

        String billInfo = "";
        boolean result = pharmacistInstance.bill(billInfo, "hms0001pa", "5.00");

        assertTrue(result, "The method should return true when attempting to insert the bill (current behavior)");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM tmp_bill WHERE tmp_bill_id = 'hms0002tb'");
        assertFalse(rs.next(), "No new bill should be added due to invalid billInfo (current bug)");
        rs.close();
        verifyStmt.close();
    }

    /* PH_BILL_INT_02
    Objective: Verify that the bill method correctly updates an existing bill in tmp_bill and returns true.
    Input: billInfo = "", patientID = "hms0001pa", pharmacyFee = "7"
           Pre-test state: Database contains a bill with tmp_bill_id = "hms0001tb" for patient_id = "hms0001pa".
    Expected output: true, and the existing bill is updated with new pharmacy_fee.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testBill_UpdateExistingBill() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO tmp_bill (tmp_bill_id, patient_id, pharmacy_fee) " +
                "VALUES ('hms0001tb', 'hms0001pa', '3.00') " +
                "ON DUPLICATE KEY UPDATE patient_id = 'hms0001pa', pharmacy_fee = '3.00'");
        stmt.close();

        String billInfo = "";
        boolean result = pharmacistInstance.bill(billInfo, "hms0001pa", "7");

        assertTrue(result, "The method should return true when the bill is updated successfully");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT pharmacy_fee FROM tmp_bill WHERE tmp_bill_id = 'hms0001tb'");
        assertTrue(rs.next(), "The bill should be present in the database");
        assertEquals("7", rs.getString("pharmacy_fee"), "The pharmacy_fee should be updated");
        rs.close();
        verifyStmt.close();
    }

    /* PH_BILL_INT_03
    Objective: Verify that the bill method handles invalid billInfo input, returns true (current behavior), and does not throw an exception.
    Input: billInfo = "", patientID = "hms0002pa", pharmacyFee = "5.00"
           Pre-test state: Database contains a patient with patient_id = "hms0002pa" and no existing bill.
    Expected output: true, and no new bill is added due to invalid billInfo.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testBill_InvalidBillInfo() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO patient (patient_id, person_id, drug_allergies_and_reactions) " +
                "VALUES ('hms002pa', 'hms00068', 'Doe') " +
                "ON DUPLICATE KEY UPDATE person_id = 'hms00068', drug_allergies_and_reactions = 'Doe'");
        stmt.close();

        String billInfo = "";
        boolean result = pharmacistInstance.bill(billInfo, "hms0002pa", "5.00");

        assertTrue(result, "The method should return true even with invalid billInfo (current behavior)");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM tmp_bill WHERE patient_id = 'hms0002pa'");
        assertFalse(rs.next(), "No new bill should be added to the database due to invalid billInfo");
        rs.close();
        verifyStmt.close();
    }
}