package Doctor;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for testing the constructor of the Doctor class.
 * This class tests various scenarios for constructing a Doctor instance, including successful initialization and error handling.
 * Uses Mockito for mocking database interactions.
 */
public class DoctorConstructorTest {
    @Mock
    private DatabaseOperator dbOperator; // Mocked DatabaseOperator for simulating database interactions

    private Doctor doctor; // Doctor instance under test

    /**
     * Set up the test environment before each test case.
     * Initializes the Mockito mocks.
     */
    @BeforeEach
    public void setUp() {
        // Initialize Mockito mocks
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test case: DOC_01
     * Mục tiêu: Kiểm tra khả năng khởi tạo Doctor với username không null và truy vấn thành công
     * Input: username = "user001", dữ liệu mock trả về slmc_reg_no = "22387"
     * Expected Output: slmcRegNo được gán giá trị "22387"
     * Ghi chú: Phủ nhánh thành công khi khởi tạo Doctor với username không null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenNonNullUsernameWhenConstructingDoctorThenSetSlmcRegNoSuccessfully() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        String username = "user001";
        ArrayList<ArrayList<String>> slmcData = new ArrayList<>();
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("22387")));
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user001'")))
                .thenReturn(slmcData);

        // Act: Tạo instance của Doctor
        doctor = new Doctor(username);
        doctor.dbOperator = dbOperator;
        doctor.userID = "user001"; // Gán userID để khớp với truy vấn

        // Assert: Kiểm tra slmcRegNo
        assertEquals("22387", doctor.slmcRegNo, "slmcRegNo phải được gán giá trị từ mock khi truy vấn thành công");

        // Verify: Đảm bảo showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user001'"));
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: DOC_02
     * Mục tiêu: Kiểm tra khả năng khởi tạo Doctor với username là null và truy vấn thành công
     * Input: username = null, dữ liệu mock trả về slmc_reg_no = "22388"
     * Expected Output: slmcRegNo được gán giá trị "22388"
     * Ghi chú: Phủ nhánh thành công khi khởi tạo Doctor với username là null
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenNullUsernameWhenConstructingDoctorThenSetSlmcRegNoSuccessfully() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu đầu vào và mock
        ArrayList<ArrayList<String>> slmcData = new ArrayList<>();
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("22388")));
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user002'")))
                .thenReturn(slmcData);

        // Act: Tạo instance của Doctor
        doctor = new Doctor(null);
        doctor.dbOperator = dbOperator;
        doctor.userID = "user002"; // Gán userID sau khi constructor chạy để truy vấn thành công

        // Assert: Kiểm tra slmcRegNo
        assertEquals("22388", doctor.slmcRegNo, "slmcRegNo phải được gán giá trị từ mock khi truy vấn thành công, ngay cả khi username là null");

        // Verify: Đảm bảo showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user002'"));
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: DOC_03
     * Mục tiêu: Kiểm tra xử lý lỗi SQLException khi truy vấn slmc_reg_no
     * Input: username = "user001", dữ liệu mock ném SQLException
     * Expected Output: slmcRegNo giữ giá trị null
     * Ghi chú: Phủ nhánh lỗi SQLException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenDatabaseErrorWhenConstructingDoctorThenHandleSQLException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), anyString()))
                .thenThrow(new SQLException("Database error"));

        // Act: Tạo instance của Doctor
        doctor = new Doctor("user001");
        doctor.dbOperator = dbOperator;
        doctor.userID = "user001";

        // Assert: Kiểm tra slmcRegNo
        assertNull(doctor.slmcRegNo, "slmcRegNo phải là null khi showTableData ném SQLException");

        // Verify: Đảm bảo showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user001'"));
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: DOC_04
     * Mục tiêu: Kiểm tra xử lý lỗi ClassNotFoundException khi truy vấn slmc_reg_no
     * Input: username = "user001", dữ liệu mock ném ClassNotFoundException
     * Expected Output: slmcRegNo giữ giá trị null
     * Ghi chú: Phủ nhánh lỗi ClassNotFoundException khi truy vấn dữ liệu từ database
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenDriverErrorWhenConstructingDoctorThenHandleClassNotFoundException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), anyString()))
                .thenThrow(new ClassNotFoundException("Driver not found"));

        // Act: Tạo instance của Doctor
        doctor = new Doctor("user001");
        doctor.dbOperator = dbOperator;
        doctor.userID = "user001";

        // Assert: Kiểm tra slmcRegNo
        assertNull(doctor.slmcRegNo, "slmcRegNo phải là null khi showTableData ném ClassNotFoundException");

        // Verify: Đảm bảo showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user001'"));
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: DOC_05
     * Mục tiêu: Kiểm tra xử lý khi dữ liệu trả về rỗng, gây IndexOutOfBoundsException
     * Input: username = "user003", dữ liệu mock trả về danh sách rỗng
     * Expected Output: slmcRegNo giữ giá trị null
     * Ghi chú: Phủ nhánh lỗi không được bắt IndexOutOfBoundsException khi dữ liệu rỗng
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenEmptyDataWhenConstructingDoctorThenHandleEmptyResult() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> emptyData = new ArrayList<>();
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), anyString()))
                .thenReturn(emptyData);

        // Act: Tạo instance của Doctor
        doctor = new Doctor("user003");
        doctor.dbOperator = dbOperator;
        doctor.userID = "user003";

        // Assert: Kiểm tra slmcRegNo
        assertNull(doctor.slmcRegNo, "slmcRegNo phải là null khi showTableData trả về danh sách rỗng, gây IndexOutOfBoundsException");

        // Verify: Đảm bảo showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user003'"));
        verifyNoMoreInteractions(dbOperator);
    }

    /**
     * Test case: DOC_06
     * Mục tiêu: Kiểm tra xử lý khi dữ liệu trả về chỉ có header, gây IndexOutOfBoundsException
     * Input: username = "user004", dữ liệu mock trả về danh sách chỉ có header
     * Expected Output: slmcRegNo giữ giá trị null
     * Ghi chú: Phủ nhánh lỗi không được bắt IndexOutOfBoundsException khi dữ liệu không hợp lệ
     * @throws SQLException if a database access error occurs
     * @throws ClassNotFoundException if the JDBC driver is not found
     */
    @Test
    public void givenHeaderOnlyDataWhenConstructingDoctorThenHandleIndexOutOfBoundsException() throws SQLException, ClassNotFoundException {
        // Arrange: Chuẩn bị dữ liệu mock
        ArrayList<ArrayList<String>> headerOnlyData = new ArrayList<>();
        headerOnlyData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), anyString()))
                .thenReturn(headerOnlyData);

        // Act: Tạo instance của Doctor
        doctor = new Doctor("user004");
        doctor.dbOperator = dbOperator;
        doctor.userID = "user004";

        // Assert: Kiểm tra slmcRegNo
        assertNull(doctor.slmcRegNo, "slmcRegNo phải là null khi showTableData trả về danh sách chỉ có header, gây IndexOutOfBoundsException");

        // Verify: Đảm bảo showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user004'"));
        verifyNoMoreInteractions(dbOperator);
    }
}