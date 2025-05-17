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

public class DoctorConstructorTest {
    @Mock
    private DatabaseOperator dbOperator;

    private Doctor doctor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void givenNonNullUsernameWhenConstructingDoctorThenSetSlmcRegNoSuccessfully() throws SQLException, ClassNotFoundException {
        // Given: username không null và showTableData trả về dữ liệu hợp lệ
        String username = "user001";
        ArrayList<ArrayList<String>> slmcData = new ArrayList<>();
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("22387")));
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user001'")))
                .thenReturn(slmcData);

        // When: Tạo instance của Doctor
        doctor = new Doctor(username);
        doctor.dbOperator = dbOperator;
        doctor.userID = "user001"; // Gán userID để khớp với truy vấn

        // Then: Kiểm tra slmcRegNo
        assertEquals("22387", doctor.slmcRegNo, "slmcRegNo phải được gán giá trị từ mock khi truy vấn thành công");

        // Verify rằng showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user001'"));
        verifyNoMoreInteractions(dbOperator);
    }

    @Test
    public void givenNullUsernameWhenConstructingDoctorThenSetSlmcRegNoSuccessfully() throws SQLException, ClassNotFoundException {
        // Given: username là null và showTableData trả về dữ liệu hợp lệ
        ArrayList<ArrayList<String>> slmcData = new ArrayList<>();
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        slmcData.add(new ArrayList<>(java.util.Arrays.asList("22388")));
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user002'")))
                .thenReturn(slmcData);

        // When: Tạo instance của Doctor
        doctor = new Doctor(null);
        doctor.dbOperator = dbOperator;
        doctor.userID = "user002"; // Gán userID sau khi constructor chạy để truy vấn thành công

        // Then: Kiểm tra slmcRegNo
        assertEquals("22388", doctor.slmcRegNo, "slmcRegNo phải được gán giá trị từ mock khi truy vấn thành công, ngay cả khi username là null");

        // Verify rằng showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user002'"));
        verifyNoMoreInteractions(dbOperator);
    }

    @Test
    public void givenDatabaseErrorWhenConstructingDoctorThenHandleSQLException() throws SQLException, ClassNotFoundException {
        // Given: showTableData ném SQLException
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), anyString()))
                .thenThrow(new SQLException("Database error"));

        // When: Tạo instance của Doctor
        doctor = new Doctor("user001");
        doctor.dbOperator = dbOperator;
        doctor.userID = "user001";

        // Then: Kiểm tra slmcRegNo
        assertNull(doctor.slmcRegNo, "slmcRegNo phải là null khi showTableData ném SQLException");

        // Verify rằng showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user001'"));
        verifyNoMoreInteractions(dbOperator);
    }

    @Test
    public void givenDriverErrorWhenConstructingDoctorThenHandleClassNotFoundException() throws SQLException, ClassNotFoundException {
        // Given: showTableData ném ClassNotFoundException
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), anyString()))
                .thenThrow(new ClassNotFoundException("Driver not found"));

        // When: Tạo instance của Doctor
        doctor = new Doctor("user001");
        doctor.dbOperator = dbOperator;
        doctor.userID = "user001";

        // Then: Kiểm tra slmcRegNo
        assertNull(doctor.slmcRegNo, "slmcRegNo phải là null khi showTableData ném ClassNotFoundException");

        // Verify rằng showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user001'"));
        verifyNoMoreInteractions(dbOperator);
    }

    @Test
    public void givenEmptyDataWhenConstructingDoctorThenHandleEmptyResult() throws SQLException, ClassNotFoundException {
        // Given: showTableData trả về danh sách rỗng
        ArrayList<ArrayList<String>> emptyData = new ArrayList<>();
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), anyString()))
                .thenReturn(emptyData);

        // When: Tạo instance của Doctor
        doctor = new Doctor("user003");
        doctor.dbOperator = dbOperator;
        doctor.userID = "user003";

        // Then: Kiểm tra slmcRegNo
        assertNull(doctor.slmcRegNo, "slmcRegNo phải là null khi showTableData trả về danh sách rỗng, gây IndexOutOfBoundsException");

        // Verify rằng showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user003'"));
        verifyNoMoreInteractions(dbOperator);
    }

    @Test
    public void givenHeaderOnlyDataWhenConstructingDoctorThenHandleIndexOutOfBoundsException() throws SQLException, ClassNotFoundException {
        // Given: showTableData trả về danh sách chỉ có header
        ArrayList<ArrayList<String>> headerOnlyData = new ArrayList<>();
        headerOnlyData.add(new ArrayList<>(java.util.Arrays.asList("slmc_reg_no")));
        when(dbOperator.showTableData(eq("doctor"), eq("slmc_reg_no"), anyString()))
                .thenReturn(headerOnlyData);

        // When: Tạo instance của Doctor
        doctor = new Doctor("user004");
        doctor.dbOperator = dbOperator;
        doctor.userID = "user004";

        // Then: Kiểm tra slmcRegNo
        assertNull(doctor.slmcRegNo, "slmcRegNo phải là null khi showTableData trả về danh sách chỉ có header, gây IndexOutOfBoundsException");

        // Verify rằng showTableData được gọi đúng
        verify(dbOperator, times(1))
                .showTableData(eq("doctor"), eq("slmc_reg_no"), eq("user_id = 'user004'"));
        verifyNoMoreInteractions(dbOperator);
    }
}