package Doctor;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class GetUsernameTest {
    private DatabaseOperator dbOperator;
    private Doctor doctorInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        // Kết nối đến cơ sở dữ liệu MySQL thật
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true",
                "root",
                "Huycode12003."
        );
        connection.setAutoCommit(false);

        dbOperator = new DatabaseOperator(); // Sử dụng dbOperator thật
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // Rollback giao dịch và đóng kết nối
        if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    @Test
    public void testGetUsernameSuccessWithNonNullUsername() throws SQLException, ClassNotFoundException {
        // Chuẩn bị dữ liệu cho constructor của Doctor
        ArrayList<ArrayList<String>> slmcData = new ArrayList<>();
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("22387")));
        // Giả lập dữ liệu trả về từ database khi Doctor khởi tạo
        dbOperator.customSelection("SELECT slmc_reg_no FROM doctor WHERE user_id = 'user001';");

        // Tạo instance của Doctor với username không null
        doctorInstance = new Doctor("user001");
        doctorInstance.dbOperator = dbOperator;
        doctorInstance.slmcRegNo = "22387";

        // Gọi phương thức getUsername
        String result = doctorInstance.getUsername();

        // Kiểm tra kết quả
        assertEquals("user001", result, "Phương thức getUsername phải trả về username đúng khi username không null");
    }

    @Test
    public void testGetUsernameWithNullUsername() throws SQLException, ClassNotFoundException {
        // Chuẩn bị dữ liệu cho constructor của Doctor
        ArrayList<ArrayList<String>> slmcData = new ArrayList<>();
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("22387")));
        // Giả lập dữ liệu trả về từ database khi Doctor khởi tạo
        dbOperator.customSelection("SELECT slmc_reg_no FROM doctor WHERE user_id = 'user001';");

        // Tạo instance của Doctor với username là null
        doctorInstance = new Doctor(null); // Truyền null vào constructor
        doctorInstance.dbOperator = dbOperator;
        doctorInstance.slmcRegNo = "22387";

        // Gọi phương thức getUsername
        String result = doctorInstance.getUsername();

        // Kiểm tra kết quả
        assertNull(result, "Phương thức getUsername phải trả về null khi username là null");
    }
}