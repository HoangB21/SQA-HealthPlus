//package Receptionist;
//
//import com.hms.hms_test_2.DatabaseOperator;
//import java.sql.SQLException;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//public class ReceptionistTest {
//
//    @Mock
//    private DatabaseOperator dbOperator;
//
//    @InjectMocks
//    private final Receptionist receptionistInstance = new Receptionist("user018");
//
//    @BeforeEach
//    public void setUp() {
//        // Khởi tạo các mock
//        MockitoAnnotations.openMocks(this);
//        receptionistInstance.dbOperator = dbOperator;
//    }
//
//    // ----- TEST CASES FOR makingAppointment -----
//    
//    // RE_MA_01: Đặt lịch trong tuần hiện tại, bệnh nhân đã có hóa đơn tạm thời
//    @Test
//    public void testMakeAppointmentCurrentWeekWithExistingBill() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat001";
//        String doctorID = "doc001";
//        String day = "5"; // Giả sử hôm nay là thứ 2 (day of week = 2)
//        String timeSlot = "09:00-10:00";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Lấy appointment_id lớn nhất
//        ArrayList<ArrayList<String>> appointmentResult = new ArrayList<>();
//        appointmentResult.add(new ArrayList<>(Arrays.asList("appointment_id")));
//        appointmentResult.add(new ArrayList<>(Arrays.asList("app001")));
//        when(dbOperator.customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);"))
//                .thenReturn(appointmentResult);
//
//        // 2. Lấy tmp_bill_id của patientID
//        ArrayList<ArrayList<String>> billResult = new ArrayList<>();
//        billResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
//        billResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
//        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"))
//                .thenReturn(billResult);
//
//        // Mock các lệnh insert/update không ném lỗi
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeAppointment(patientID, doctorID, day, timeSlot);
//
//        // Assert
//        assertEquals("app002", result); // Kiểm tra appointment_id mới được tạo
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);");
//        verify(dbOperator, times(1)).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';");
//
//        // Verify insert vào bảng appointment
//        verify(dbOperator, times(1)).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO appointment") && sql.contains("app002") && sql.contains("pat001") && sql.contains("doc001")
//        ));
//
//        // Verify update current_week_appointments trong doctor_availability
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE doctor_availability SET current_week_appointments = current_week_appointments + 1 WHERE " +
//            "time_slot = '09:00-10:00' AND slmc_reg_no = 'doc001' AND day = '3';"
//        );
//
//        // Verify update appointment_fee trong tmp_bill
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE tmp_bill SET appointment_fee = ' 500 ' WHERE tmp_bill_id = 'hms0001tb';"
//        );
//
//        // Verify không có lệnh nào liên quan đến next_week_appointments
//        verify(dbOperator, never()).customInsertion(argThat(sql -> sql.contains("next_week_appointments")));
//    }
//    
//    // RE_MA_02 Đặt lịch tuần sau, bệnh nhân chưa có hóa đơn tạm thời, tạo billID mới
//    @Test
//    public void testMakeAppointmentNextWeekNewBill() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat002";
//        String doctorID = "doc002";
//        String day = "12"; // > 7, nextWeek = true
//        String timeSlot = "14:00-15:00";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Lấy appointment_id lớn nhất
//        ArrayList<ArrayList<String>> appointmentResult = new ArrayList<>();
//        appointmentResult.add(new ArrayList<>(Arrays.asList("appointment_id")));
//        appointmentResult.add(new ArrayList<>(Arrays.asList("app005")));
//        when(dbOperator.customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);"))
//                .thenReturn(appointmentResult);
//
//        // 2. Lấy tmp_bill_id của patientID (không tồn tại)
//        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat002';"))
//                .thenThrow(new SQLException("No bill found"));
//
//        // 3. Lấy tmp_bill_id lớn nhất
//        ArrayList<ArrayList<String>> billResult = new ArrayList<>();
//        billResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
//        billResult.add(new ArrayList<>(Arrays.asList("hms0002tb")));
//        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
//                .thenReturn(billResult);
//
//        // Mock các lệnh insert không ném lỗi
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeAppointment(patientID, doctorID, day, timeSlot);
//
//        // Assert
//        assertEquals("app006", result);
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);");
//        verify(dbOperator, times(1)).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat002';");
//        verify(dbOperator, times(1)).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
//
//        // Verify insert vào bảng appointment
//        verify(dbOperator, times(1)).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO appointment") && sql.contains("app006") && sql.contains("pat002") && sql.contains("doc002")
//        ));
//
//        // Verify update next_week_appointments
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE doctor_availability SET next_week_appointments = next_week_appointments + 1 WHERE " +
//            "time_slot = '14:00-15:00' AND slmc_reg_no = 'doc002' AND day = '3';"
//        );
//
//        // Verify insert vào tmp_bill
//        verify(dbOperator, times(1)).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO tmp_bill") && sql.contains("hms0003tb") && sql.contains("patient_id 'pat002'") && sql.contains("appointment_fee '500'")
//        ));
//    }
//    // RE_MA_03 Đặt lịch trong tuần, lỗi khi lấy appointment_id
//    @Test
//    public void testMakeAppointmentCurrentWeekAppointmentIdError() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat003";
//        String doctorID = "doc003";
//        String day = "4";
//        String timeSlot = "10:00-11:00";
//
//        // Mock lỗi khi lấy appointment_id
//        when(dbOperator.customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);"))
//                .thenThrow(new SQLException("Database error"));
//
//        // Act
//        String result = receptionistInstance.makeAppointment(patientID, doctorID, day, timeSlot);
//
//        // Assert
//        assertEquals("false", result);
//
//        // Verify chỉ gọi lệnh lấy appointment_id
//        verify(dbOperator, times(1)).customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);");
//        verify(dbOperator, never()).customInsertion(anyString()); // Không có insert nào được gọi
//    }
//    
//    // RE_MA_04: Đặt lịch tuần sau, lỗi khi tạo tmp_bill_id, dùng giá trị mặc định
//    @Test
//    public void testMakeAppointmentNextWeekDefaultBillId() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat004";
//        String doctorID = "doc004";
//        String day = "9"; // > 7, nextWeek = true
//        String timeSlot = "15:00-16:00";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Lấy appointment_id lớn nhất
//        ArrayList<ArrayList<String>> appointmentResult = new ArrayList<>();
//        appointmentResult.add(new ArrayList<>(Arrays.asList("appointment_id")));
//        appointmentResult.add(new ArrayList<>(Arrays.asList("app003")));
//        when(dbOperator.customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);"))
//                .thenReturn(appointmentResult);
//
//        // 2. Lấy tmp_bill_id của patientID (không tồn tại)
//        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat004';"))
//                .thenThrow(new SQLException("No bill found"));
//
//        // 3. Lỗi khi lấy tmp_bill_id lớn nhất
//        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
//                .thenThrow(new SQLException("Database error"));
//
//        // Mock các lệnh insert không ném lỗi
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeAppointment(patientID, doctorID, day, timeSlot);
//
//        // Assert
//        assertEquals("app004", result);
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);");
//        verify(dbOperator, times(1)).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat004';");
//        verify(dbOperator, times(1)).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
//
//        // Verify insert vào bảng appointment
//        verify(dbOperator, times(1)).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO appointment") && sql.contains("app004") && sql.contains("pat004") && sql.contains("doc004")
//        ));
//
//        // Verify update next_week_appointments
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE doctor_availability SET next_week_appointments = next_week_appointments + 1 WHERE " +
//            "time_slot = '15:00-16:00' AND slmc_reg_no = 'doc004' AND day = '2';"
//        );
//
//        // Verify insert vào tmp_bill với giá trị mặc định
//        verify(dbOperator, times(1)).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO tmp_bill") && sql.contains("hms0001tb") && sql.contains("patient_id 'pat004'") && sql.contains("appointment_fee '500'")
//        ));
//    }
//    
//    //RE_MA_05: Đặt lịch trong tuần, lỗi khi tính ngày hẹn
//    @Test
//    public void testMakeAppointmentCurrentWeekDateCalculationError() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat005";
//        String doctorID = "doc005";
//        String day = "6";
//        String timeSlot = "11:00-12:00";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Lấy appointment_id lớn nhất
//        ArrayList<ArrayList<String>> appointmentResult = new ArrayList<>();
//        appointmentResult.add(new ArrayList<>(Arrays.asList("appointment_id")));
//        appointmentResult.add(new ArrayList<>(Arrays.asList("app002")));
//        when(dbOperator.customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);"))
//                .thenReturn(appointmentResult);
//
//        // 2. Lấy tmp_bill_id của patientID
//        ArrayList<ArrayList<String>> billResult = new ArrayList<>();
//        billResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
//        billResult.add(new ArrayList<>(Arrays.asList("hms0005tb")));
//        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat005';"))
//                .thenReturn(billResult);
//
//        // Mock các lệnh insert không ném lỗi
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeAppointment(patientID, doctorID, day, timeSlot);
//
//        // Assert
//        assertEquals("app003", result);
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);");
//        verify(dbOperator, times(1)).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat005';");
//
//        // Verify insert vào bảng appointment (appDate có thể rỗng hoặc mặc định)
//        verify(dbOperator, times(1)).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO appointment") && sql.contains("app003") && sql.contains("pat005") && sql.contains("doc005")
//        ));
//
//        // Verify update current_week_appointments
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE doctor_availability SET current_week_appointments = current_week_appointments + 1 WHERE " +
//            "time_slot = '11:00-12:00' AND slmc_reg_no = 'doc005' AND day = '6';"
//        );
//
//        // Verify update tmp_bill
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE tmp_bill SET appointment_fee = ' 500 ' WHERE tmp_bill_id = 'hms0005tb';"
//        );
//    }
//    //RE_MA_06: Appointment id trả về có độ dài ngắn
//    @Test
//    public void testMakeAppointmentShortAppointmentId() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat008";
//        String doctorID = "doc008";
//        String day = "4";
//        String timeSlot = "10:00-11:00";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Lấy appointment_id lớn nhất với độ dài <= 3
//        ArrayList<ArrayList<String>> appointmentResult = new ArrayList<>();
//        appointmentResult.add(new ArrayList<>(Arrays.asList("appointment_id")));
//        appointmentResult.add(new ArrayList<>(Arrays.asList("ap"))); // Độ dài = 2
//        when(dbOperator.customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);"))
//                .thenReturn(appointmentResult);
//
//        // Act
//        String result = receptionistInstance.makeAppointment(patientID, doctorID, day, timeSlot);
//
//        // Assert
//        assertEquals("false", result);
//
//        // Verify chỉ gọi lệnh lấy appointment_id, không có insert/update
//        verify(dbOperator, times(1)).customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);");
//        verify(dbOperator, never()).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat008';");
//        verify(dbOperator, never()).customInsertion(anyString());
//    }
//    //RE_MA_07: Bill id trả về có độ dài ngắn
//    @Test
//    public void testMakeAppointmentShortBillIdInCatch() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat009";
//        String doctorID = "doc009";
//        String day = "5";
//        String timeSlot = "12:00-13:00";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Lấy appointment_id lớn nhất
//        ArrayList<ArrayList<String>> appointmentResult = new ArrayList<>();
//        appointmentResult.add(new ArrayList<>(Arrays.asList("appointment_id")));
//        appointmentResult.add(new ArrayList<>(Arrays.asList("app001")));
//        when(dbOperator.customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);"))
//                .thenReturn(appointmentResult);
//
//        // 2. Truy vấn tmp_bill_id của patientID ném lỗi
//        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat009';"))
//                .thenThrow(new SQLException("No bill found"));
//
//        // 3. Lấy tmp_bill_id lớn nhất với độ dài <= 3
//        ArrayList<ArrayList<String>> billResult = new ArrayList<>();
//        billResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
//        billResult.add(new ArrayList<>(Arrays.asList("hm"))); // Độ dài = 2
//        when(dbOperator.customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
//                .thenReturn(billResult);
//
//        // Mock các lệnh insert không ném lỗi
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeAppointment(patientID, doctorID, day, timeSlot);
//
//        // Assert
//        assertEquals("app002", result);
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection("SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);");
//        verify(dbOperator, times(1)).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat009';");
//        verify(dbOperator, times(1)).customSelection("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);");
//
//        // Verify insert vào bảng appointment
//        verify(dbOperator, times(1)).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO appointment") && sql.contains("app002") && sql.contains("pat009") && sql.contains("doc009")
//        ));
//
//        // Verify update current_week_appointments
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE doctor_availability SET current_week_appointments = current_week_appointments + 1 WHERE " +
//            "time_slot = '12:00-13:00' AND slmc_reg_no = 'doc009' AND day = '5';"
//        );
//
//        // Verify insert vào tmp_bill với giá trị mặc định
//        verify(dbOperator, times(1)).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO tmp_bill") && sql.contains("hms0001tb") && sql.contains("patient_id 'pat009'") && sql.contains("appointment_fee '500'")
//        ));
//
//        // Verify không gọi update tmp_bill
//        verify(dbOperator, never()).customInsertion(argThat(sql -> sql.contains("UPDATE tmp_bill")));
//    }
//    
//    // ----- TEST CASES FOR setPatientInfo -----
//    // RE_SP_01: Thành công với dữ liệu hợp lệ
//    @Test
//    public void testSetPatientInfoSuccess() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientInfo = "nic 123456789V,gender M,date_of_birth 19900101,first_name John,last_name Doe";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Lấy patient_id lớn nhất
//        ArrayList<ArrayList<String>> patientIdResult = new ArrayList<>();
//        patientIdResult.add(new ArrayList<>(Arrays.asList("patient_id")));
//        patientIdResult.add(new ArrayList<>(Arrays.asList("hms0001pa")));
//        when(dbOperator.customSelection("SELECT patient_id FROM patient WHERE patient_id = (SELECT MAX(patient_id) FROM patient);"))
//                .thenReturn(patientIdResult);
//
//        // 2. Lấy person_id lớn nhất
//        ArrayList<ArrayList<String>> personIdResult = new ArrayList<>();
//        personIdResult.add(new ArrayList<>(Arrays.asList("person_id")));
//        personIdResult.add(new ArrayList<>(Arrays.asList("hms00001")));
//        when(dbOperator.customSelection("SELECT person_id FROM person WHERE person_id = (SELECT MAX(person_id) FROM person);"))
//                .thenReturn(personIdResult);
//
//        // Mock các lệnh insert không ném lỗi
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.setPatientInfo(patientInfo);
//
//        // Assert
//        assertEquals("hms00002", result); // Kiểm tra person_id mới được tạo
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT patient_id FROM patient WHERE patient_id = (SELECT MAX(patient_id) FROM patient);"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT person_id FROM person WHERE person_id = (SELECT MAX(person_id) FROM person);"
//        );
//
//        // Verify insert vào bảng person
//        verify(dbOperator, times(1)).customInsertion(
//            "INSERT INTO person (nic,gender,date_of_birth,first_name,last_name,person_id) " +
//            "VALUES ('123456789V','M',19900101,'John','Doe','hms00002');"
//        );
//
//        // Verify insert vào bảng patient
//        verify(dbOperator, times(1)).customInsertion(
//            "INSERT INTO patient (patient_id,person_id) VALUES ('hms0002pa','hms00002');"
//        );
//
//        // Verify số lần gọi customInsertion (chỉ 2 lần cho person và patient)
//        verify(dbOperator, times(2)).customInsertion(anyString());
//    }
//    
//    // RE_SP_02: patientID.length() <= 3
//    @Test
//    public void testSetPatientInfoWithShortPatientId() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientInfo = "nic 123456789V,gender M,date_of_birth 19900101,first_name John,last_name Doe";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Lấy patient_id lớn nhất với độ dài <= 3
//        ArrayList<ArrayList<String>> patientIdResult = new ArrayList<>();
//        patientIdResult.add(new ArrayList<>(Arrays.asList("patient_id")));
//        patientIdResult.add(new ArrayList<>(Arrays.asList("hm"))); // Độ dài = 2
//        when(dbOperator.customSelection("SELECT patient_id FROM patient WHERE patient_id = (SELECT MAX(patient_id) FROM patient);"))
//                .thenReturn(patientIdResult);
//
//        // Act
//        String result = receptionistInstance.setPatientInfo(patientInfo);
//
//        // Assert
//        assertEquals("false", result); // Kiểm tra trả về "false" do ngoại lệ
//
//        // Verify các lệnh SQL được gọi
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT patient_id FROM patient WHERE patient_id = (SELECT MAX(patient_id) FROM patient);"
//        );
//
//        // Verify không có lệnh nào khác được gọi do lỗi xảy ra sớm
//        verify(dbOperator, never()).customSelection(
//            "SELECT person_id FROM person WHERE person_id = (SELECT MAX(person_id) FROM person);"
//        );
//        verify(dbOperator, never()).customInsertion(anyString());
//    }
//    // RE_SP_03: SQLException khi lấy patient_id lớn nhất
//    @Test
//    public void testSetPatientInfoWithSQLExceptionOnPatientIdQuery() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientInfo = "nic 123456789V,gender M,date_of_birth 19900101,first_name John,last_name Doe";
//
//        // Mock dữ liệu trả về từ CSDL
//        // 1. Truy vấn patient_id lớn nhất ném SQLException
//        when(dbOperator.customSelection("SELECT patient_id FROM patient WHERE patient_id = (SELECT MAX(patient_id) FROM patient);"))
//                .thenThrow(new SQLException("Database connection failed"));
//
//        // Act
//        String result = receptionistInstance.setPatientInfo(patientInfo);
//
//        // Assert
//        assertEquals("false", result); // Kiểm tra trả về "false" do SQLException
//
//        // Verify các lệnh SQL được gọi
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT patient_id FROM patient WHERE patient_id = (SELECT MAX(patient_id) FROM patient);"
//        );
//
//        // Verify không có lệnh nào khác được gọi do lỗi xảy ra sớm
//        verify(dbOperator, never()).customSelection(
//            "SELECT person_id FROM person WHERE person_id = (SELECT MAX(person_id) FROM person);"
//        );
//        verify(dbOperator, never()).customInsertion(anyString());
//    }
//    
//    
//    // Test Case: Thành công với dữ liệu hợp lệ
//    @Test
//    public void testDoctorAppointmentAvailableTimeSuccessNoCalendarMock() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String doctorID = "doc001";
//
//        // Mock dữ liệu trả về từ cơ sở dữ liệu
//        ArrayList<ArrayList<String>> dbResult = new ArrayList<>();
//        dbResult.add(new ArrayList<>(Arrays.asList("day", "time_slot", "current_week_appointments")));
//        dbResult.add(new ArrayList<>(Arrays.asList("6", "09:00-10:00", "2"))); // Thứ Sáu, 2 cuộc hẹn
//        when(dbOperator.customSelection(
//            "SELECT doctor_availability.day, doctor_availability.time_slot, doctor_availability.current_week_appointments " +
//            "FROM doctor_availability WHERE slmc_reg_no = 'doc001' ORDER BY day;"
//        )).thenReturn(dbResult);
//
//        // Giả sử ngày hiện tại là thứ Tư (09/04/2025) để tính toán
//        
//        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
//
//        // Act
//        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime(doctorID);
//
//        // Assert
//        assertEquals(2, result.size()); // 1 hàng tiêu đề + 1 hàng dữ liệu
//        assertEquals(Arrays.asList("day", "session_start", "app_time"), result.get(0)); // Tiêu đề
//
//        // Tính toán ngày mong đợi dựa trên dayOfWeek thực tế
//        int availableDay = 6; // Từ dữ liệu cơ sở dữ liệu
//        int nextDay = (availableDay > dayOfWeek) ? (availableDay - dayOfWeek) : (7 - dayOfWeek + availableDay);
//        Calendar expectedCalendar = Calendar.getInstance();
//        expectedCalendar.add(Calendar.DATE, nextDay);
//        String expectedDay = new SimpleDateFormat("EEEE MMM dd").format(expectedCalendar.getTime());
//
//        assertEquals(Arrays.asList(expectedDay, "09:00", "09:10"), result.get(1)); // Dữ liệu động dựa trên ngày hiện tại
//
//        // Verify lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT doctor_availability.day, doctor_availability.time_slot, doctor_availability.current_week_appointments " +
//            "FROM doctor_availability WHERE slmc_reg_no = 'doc001' ORDER BY day;"
//        );
//    }
//    // Test Case: SQLException trong khối try
//    @Test
//    public void testDoctorAppointmentAvailableTimeWithSQLException() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String doctorID = "doc001";
//
//        // Mock dữ liệu trả về từ cơ sở dữ liệu ném SQLException
//        when(dbOperator.customSelection(
//            "SELECT doctor_availability.day, doctor_availability.time_slot, doctor_availability.current_week_appointments " +
//            "FROM doctor_availability WHERE slmc_reg_no = 'doc001' ORDER BY day;"
//        )).thenThrow(new SQLException("Database connection failed"));
//
//        // Act
//        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime(doctorID);
//
//        // Assert
//        assertEquals(1, result.size()); // Chỉ có hàng tiêu đề
//        assertEquals(Arrays.asList("day", "session_start", "app_time"), result.get(0)); // Tiêu đề
//
//        // Verify lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT doctor_availability.day, doctor_availability.time_slot, doctor_availability.current_week_appointments " +
//            "FROM doctor_availability WHERE slmc_reg_no = 'doc001' ORDER BY day;"
//        );
//    }
//    // Test Case: Data nhận về là null
//    @Test
//    public void testDoctorAppointmentAvailableTimeWithNullData() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String doctorID = "doc001";
//
//        // Mock dữ liệu trả về từ cơ sở dữ liệu là null
//        when(dbOperator.customSelection(
//            "SELECT doctor_availability.day, doctor_availability.time_slot, doctor_availability.current_week_appointments " +
//            "FROM doctor_availability WHERE slmc_reg_no = 'doc001' ORDER BY day;"
//        )).thenReturn(null);
//
//        // Act
//        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime(doctorID);
//
//        // Assert
//        assertEquals(1, result.size()); // Chỉ có hàng tiêu đề
//        assertEquals(Arrays.asList("day", "session_start", "app_time"), result.get(0)); // Tiêu đề
//
//        // Verify lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT doctor_availability.day, doctor_availability.time_slot, doctor_availability.current_week_appointments " +
//            "FROM doctor_availability WHERE slmc_reg_no = 'doc001' ORDER BY day;"
//        );
//    }
//    
//    // Test Case: Phủ nhánh else khi availableDay <= dayOfWeek
//    @Test
//    public void testDoctorAppointmentAvailableTimeWithElseBranch() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String doctorID = "doc001";
//
//        // Mock dữ liệu trả về từ cơ sở dữ liệu
//        ArrayList<ArrayList<String>> dbResult = new ArrayList<>();
//        dbResult.add(new ArrayList<>(Arrays.asList("day", "time_slot", "current_week_appointments")));
//        dbResult.add(new ArrayList<>(Arrays.asList("3", "09:00-10:00", "2"))); // Thứ Ba, 2 cuộc hẹn
//        when(dbOperator.customSelection(
//            "SELECT doctor_availability.day, doctor_availability.time_slot, doctor_availability.current_week_appointments " +
//            "FROM doctor_availability WHERE slmc_reg_no = 'doc001' ORDER BY day;"
//        )).thenReturn(dbResult);
//
//        // Giả sử ngày hiện tại là Thứ Sáu (06/04/2025, dayOfWeek = 6)
//        // Không mock Calendar, tính toán động dựa trên ngày thực tế
//        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
//
//        // Act
//        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime(doctorID);
//
//        // Assert
//        assertEquals(2, result.size()); // 1 hàng tiêu đề + 1 hàng dữ liệu
//        assertEquals(Arrays.asList("day", "session_start", "app_time"), result.get(0)); // Tiêu đề
//
//        // Tính toán ngày mong đợi dựa trên dayOfWeek thực tế
//        int availableDay = 3; // Từ dữ liệu cơ sở dữ liệu
//        int nextDay = (availableDay > dayOfWeek) ? (availableDay - dayOfWeek) : (7 - dayOfWeek + availableDay);
//        Calendar expectedCalendar = Calendar.getInstance();
//        expectedCalendar.add(Calendar.DATE, nextDay);
//        String expectedDay = new SimpleDateFormat("EEEE MMM dd").format(expectedCalendar.getTime());
//
//        assertEquals(Arrays.asList(expectedDay, "09:00", "09:10"), result.get(1)); // Dữ liệu động
//
//        // Verify lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT doctor_availability.day, doctor_availability.time_slot, doctor_availability.current_week_appointments " +
//            "FROM doctor_availability WHERE slmc_reg_no = 'doc001' ORDER BY day;"
//        );
//    }
//    
//    // Test Case: Thành công với dữ liệu hợp lệ
//    @Test
//    public void testRefundSuccess() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String refundInfo = "bill_id b001,patient_id pat001,amount 500";
//
//        // Mock dữ liệu trả về từ cơ sở dữ liệu
//        ArrayList<ArrayList<String>> refundIdResult = new ArrayList<>();
//        refundIdResult.add(new ArrayList<>(Arrays.asList("refund_id")));
//        refundIdResult.add(new ArrayList<>(Arrays.asList("r0001")));
//        when(dbOperator.customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        )).thenReturn(refundIdResult);
//
//        // Mock lệnh chèn dữ liệu thành công
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        boolean result = receptionistInstance.refund(refundInfo);
//
//        // Assert
//        assertTrue(result); // Kiểm tra trả về true
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        );
//
//        // Verify lệnh chèn dữ liệu vào bảng refund
//        String expectedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
//        verify(dbOperator, times(1)).customInsertion(
//            "INSERT INTO refund (bill_id,patient_id,amount,refund_id,date) " +
//            "VALUES ('b001','pat001','500','r0002','" + expectedDate + "');"
//        );
//    }
//    
//    // Test Case: refundID.length() <= 1
//    @Test
//    public void testRefundWithShortRefundId() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String refundInfo = "bill_id b001,patient_id pat001,amount 500";
//
//        // Mock dữ liệu trả về từ cơ sở dữ liệu
//        ArrayList<ArrayList<String>> refundIdResult = new ArrayList<>();
//        refundIdResult.add(new ArrayList<>(Arrays.asList("refund_id")));
//        refundIdResult.add(new ArrayList<>(Arrays.asList("r"))); // Độ dài = 1
//        when(dbOperator.customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        )).thenReturn(refundIdResult);
//
//        // Act
//        boolean result = receptionistInstance.refund(refundInfo);
//
//        // Assert
//        assertFalse(result); // Kiểm tra trả về false do lỗi
//
//        // Verify các lệnh SQL được gọi
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        );
//        verify(dbOperator, never()).customInsertion(anyString()); // Không chèn dữ liệu vì lỗi xảy ra sớm
//    }
//    // Test Case: refundInfo với field kích thước >= 3
//    @Test
//    public void testRefundWithFieldSizeAtLeastThree() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String refundInfo = "bill_id b001,patient_id pat001,amount 500,reason cancellation"; // 4 trường
//
//        // Mock dữ liệu trả về từ cơ sở dữ liệu
//        ArrayList<ArrayList<String>> refundIdResult = new ArrayList<>();
//        refundIdResult.add(new ArrayList<>(Arrays.asList("refund_id")));
//        refundIdResult.add(new ArrayList<>(Arrays.asList("r0001")));
//        when(dbOperator.customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        )).thenReturn(refundIdResult);
//
//        // Mock lệnh chèn dữ liệu thành công
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        boolean result = receptionistInstance.refund(refundInfo);
//
//        // Assert
//        assertTrue(result); // Kiểm tra trả về true
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        );
//
//        // Verify lệnh chèn dữ liệu vào bảng refund
//        String expectedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
//        verify(dbOperator, times(1)).customInsertion(
//            "INSERT INTO refund (bill_id,patient_id,amount,reason,refund_id,date) " +
//            "VALUES ('b001','pat001','500','cancellation','r0002','" + expectedDate + "');"
//        );
//    }
//    
//    // Test Case: SQLException trong khối try
//    @Test
//    public void testRefundWithSQLException() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String refundInfo = "bill_id b001,patient_id pat001,amount 500";
//
//        // Mock dữ liệu trả về từ cơ sở dữ liệu ném SQLException
//        when(dbOperator.customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        )).thenThrow(new SQLException("Database connection failed"));
//
//        // Act
//        boolean result = receptionistInstance.refund(refundInfo);
//
//        // Assert
//        assertFalse(result); // Kiểm tra trả về false do SQLException
//
//        // Verify các lệnh SQL được gọi
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        );
//        verify(dbOperator, never()).customInsertion(anyString()); // Không chèn dữ liệu vì lỗi xảy ra sớm
//    }
//    
//    // Test Case: Thành công với dữ liệu hợp lệ
//    @Test
//    public void testGetDoctorSummarySuccess() throws SQLException, ClassNotFoundException {
//        // Arrange
//        // Mock dữ liệu trả về từ truy vấn bác sĩ
//        ArrayList<ArrayList<String>> doctorResult = new ArrayList<>();
//        doctorResult.add(new ArrayList<>(Arrays.asList("slmc_reg_no", "experienced_areas", "first_name", "last_name")));
//        doctorResult.add(new ArrayList<>(Arrays.asList("doc001", "Cardiology", "John", "Doe")));
//        doctorResult.add(new ArrayList<>(Arrays.asList("doc002", "Neurology", "Jane", "Smith")));
//        when(dbOperator.customSelection(
//            "SELECT doctor.slmc_reg_no, doctor.experienced_areas, person.first_name, person.last_name " +
//            "FROM doctor INNER JOIN person ON doctor.user_id = person.user_id;"
//        )).thenReturn(doctorResult);
//
//        // Mock dữ liệu trả về từ truy vấn ngày làm việc cho doc001
//        ArrayList<ArrayList<String>> daysDoc001 = new ArrayList<>();
//        daysDoc001.add(new ArrayList<>(Arrays.asList("day")));
//        daysDoc001.add(new ArrayList<>(Arrays.asList("1")));
//        daysDoc001.add(new ArrayList<>(Arrays.asList("2")));
//        when(dbOperator.customSelection(
//            "SELECT day FROM doctor_availability WHERE slmc_reg_no = 'doc001';"
//        )).thenReturn(daysDoc001);
//
//        // Mock dữ liệu trả về từ truy vấn ngày làm việc cho doc002
//        ArrayList<ArrayList<String>> daysDoc002 = new ArrayList<>();
//        daysDoc002.add(new ArrayList<>(Arrays.asList("day")));
//        daysDoc002.add(new ArrayList<>(Arrays.asList("3")));
//        daysDoc002.add(new ArrayList<>(Arrays.asList("4")));
//        daysDoc002.add(new ArrayList<>(Arrays.asList("3"))); // Ngày trùng
//        when(dbOperator.customSelection(
//            "SELECT day FROM doctor_availability WHERE slmc_reg_no = 'doc002';"
//        )).thenReturn(daysDoc002);
//
//        // Act
//        ArrayList<ArrayList<String>> result = receptionistInstance.getDoctorSummary();
//
//        // Assert
//        assertEquals(3, result.size()); // 1 hàng tiêu đề + 2 hàng dữ liệu
//        assertEquals(Arrays.asList("slmc_reg_no", "experienced_areas", "first_name", "last_name"), result.get(0)); // Tiêu đề
//        assertEquals(Arrays.asList("doc001", "Cardiology", "John", "Doe", "2", "1 2 "), result.get(1)); // doc001
//        assertEquals(Arrays.asList("doc002", "Neurology", "Jane", "Smith", "2", "3 4 "), result.get(2)); // doc002
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT doctor.slmc_reg_no, doctor.experienced_areas, person.first_name, person.last_name " +
//            "FROM doctor INNER JOIN person ON doctor.user_id = person.user_id;"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT day FROM doctor_availability WHERE slmc_reg_no = 'doc001';"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT day FROM doctor_availability WHERE slmc_reg_no = 'doc002';"
//        );
//    }
//    // Test Case: SQLException trong khối try
//    @Test
//    public void testGetDoctorSummaryWithSQLException() throws SQLException, ClassNotFoundException {
//        // Arrange
//        // Mock dữ liệu trả về từ truy vấn bác sĩ ném SQLException
//        when(dbOperator.customSelection(
//            "SELECT doctor.slmc_reg_no, doctor.experienced_areas, person.first_name, person.last_name " +
//            "FROM doctor INNER JOIN person ON doctor.user_id = person.user_id;"
//        )).thenThrow(new SQLException("Database connection failed"));
//
//        // Act
//        ArrayList<ArrayList<String>> result = receptionistInstance.getDoctorSummary();
//
//        // Assert
//        assertNull(result); // Kiểm tra trả về null do SQLException
//
//        // Verify các lệnh SQL được gọi
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT doctor.slmc_reg_no, doctor.experienced_areas, person.first_name, person.last_name " +
//            "FROM doctor INNER JOIN person ON doctor.user_id = person.user_id;"
//        );
//        verify(dbOperator, never()).customSelection(contains("SELECT day FROM doctor_availability")); // Không gọi truy vấn ngày
//    }
//    
//    // Test Case: Thành công với dữ liệu hợp lệ
//    @Test
//    public void testMakeLabAppointmentSuccess() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat001";
//        String doctorID = "doc001";
//        String testID = "test001";
//        String day = "6"; // Thứ Sáu
//        String timeSlot = "09:00-10:00";
//
//        // Mock dữ liệu trả về từ truy vấn lab_appointment_id lớn nhất
//        ArrayList<ArrayList<String>> labAppIdResult = new ArrayList<>();
//        labAppIdResult.add(new ArrayList<>(Arrays.asList("lab_appointment_id")));
//        labAppIdResult.add(new ArrayList<>(Arrays.asList("lapp001")));
//        when(dbOperator.customSelection(
//            "SELECT lab_appointment_id FROM lab_appointment WHERE lab_appointment_id = (SELECT MAX(lab_appointment_id) FROM lab_appointment);"
//        )).thenReturn(labAppIdResult);
//
//        // Mock dữ liệu trả về từ truy vấn phí xét nghiệm
//        ArrayList<ArrayList<String>> testFeeResult = new ArrayList<>();
//        testFeeResult.add(new ArrayList<>(Arrays.asList("test_fee")));
//        testFeeResult.add(new ArrayList<>(Arrays.asList("300")));
//        when(dbOperator.customSelection(
//            "SELECT test_fee FROM lab_test WHERE test_id = 'test001';"
//        )).thenReturn(testFeeResult);
//
//        // Mock dữ liệu trả về từ truy vấn tmp_bill_id (bệnh nhân đã có hóa đơn)
//        ArrayList<ArrayList<String>> tmpBillIdResult = new ArrayList<>();
//        tmpBillIdResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
//        tmpBillIdResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
//        when(dbOperator.customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"
//        )).thenReturn(tmpBillIdResult);
//
//        // Mock các lệnh chèn/cập nhật thành công
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeLabAppointment(patientID, doctorID, testID, day, timeSlot);
//
//        // Assert
//        assertEquals("lapp002", result); // Kiểm tra trả về lab_appointment_id mới
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT lab_appointment_id FROM lab_appointment WHERE lab_appointment_id = (SELECT MAX(lab_appointment_id) FROM lab_appointment);"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT test_fee FROM lab_test WHERE test_id = 'test001';"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"
//        );
//
//        // Verify lệnh chèn lab_appointment
//        String expectedAppDate = new SimpleDateFormat("yyyy-MM-dd").format(
//            Calendar.getInstance().getTime() // Giả sử hôm nay là 08/04/2025
//        ).replace("08", "10"); // Điều chỉnh từ Thứ Tư (08) sang Thứ Sáu (10)
//        verify(dbOperator, times(1)).customInsertion(
//            "INSERT INTO lab_appointment (lab_appointment_id,test_id,patient_id,doctor_id,date,cancelled) " +
//            "VALUES ('lapp002' , 'test001' , 'pat001' , 'doc001' , '" + expectedAppDate + " 09:00:00' , false );"
//        );
//
//        // Verify lệnh cập nhật lab_appointment_timetable
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE lab_appointment_timetable SET current_week_appointments = current_week_appointments + 1 WHERE " +
//            "time_slot = '09:00-10:00' AND app_test_id = 'test001' AND app_day = '6';"
//        );
//
//        // Verify lệnh cập nhật tmp_bill
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE tmp_bill SET laboratory_fee = ' 300 ' WHERE tmp_bill_id = 'hms0001tb';"
//        );
//
//        // Verify không tạo tmp_bill mới
//        verify(dbOperator, never()).customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"
//        );
//    }
//    // Test Case: appointmentID.length() <= 4
//    @Test
//    public void testMakeLabAppointmentWithShortAppointmentId() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat001";
//        String doctorID = "doc001";
//        String testID = "test001";
//        String day = "6"; // Thứ Sáu
//        String timeSlot = "09:00-10:00";
//
//        // Mock dữ liệu trả về từ truy vấn lab_appointment_id lớn nhất
//        ArrayList<ArrayList<String>> labAppIdResult = new ArrayList<>();
//        labAppIdResult.add(new ArrayList<>(Arrays.asList("lab_appointment_id")));
//        labAppIdResult.add(new ArrayList<>(Arrays.asList("lap"))); // Độ dài = 3
//        when(dbOperator.customSelection(
//            "SELECT lab_appointment_id FROM lab_appointment WHERE lab_appointment_id = (SELECT MAX(lab_appointment_id) FROM lab_appointment);"
//        )).thenReturn(labAppIdResult);
//
//        // Mock dữ liệu trả về từ truy vấn phí xét nghiệm
//        ArrayList<ArrayList<String>> testFeeResult = new ArrayList<>();
//        testFeeResult.add(new ArrayList<>(Arrays.asList("test_fee")));
//        testFeeResult.add(new ArrayList<>(Arrays.asList("300")));
//        when(dbOperator.customSelection(
//            "SELECT test_fee FROM lab_test WHERE test_id = 'test001';"
//        )).thenReturn(testFeeResult);
//
//        // Mock dữ liệu trả về từ truy vấn tmp_bill_id (bệnh nhân đã có hóa đơn)
//        ArrayList<ArrayList<String>> tmpBillIdResult = new ArrayList<>();
//        tmpBillIdResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
//        tmpBillIdResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
//        when(dbOperator.customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"
//        )).thenReturn(tmpBillIdResult);
//
//        // Mock các lệnh chèn/cập nhật thành công
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeLabAppointment(patientID, doctorID, testID, day, timeSlot);
//
//        // Assert
//        assertEquals("lapp001", result); // Kiểm tra trả về "lapp001" do lỗi trong khối try đầu tiên
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT lab_appointment_id FROM lab_appointment WHERE lab_appointment_id = (SELECT MAX(lab_appointment_id) FROM lab_appointment);"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT test_fee FROM lab_test WHERE test_id = 'test001';"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"
//        );
//
//        // Verify lệnh chèn lab_appointment
//        String expectedAppDate = new SimpleDateFormat("yyyy-MM-dd").format(
//            Calendar.getInstance().getTime() // Giả sử hôm nay là 08/04/2025
//        ).replace("08", "10"); // Điều chỉnh từ Thứ Tư (08) sang Thứ Sáu (10)
//        verify(dbOperator, times(1)).customInsertion(
//            "INSERT INTO lab_appointment (lab_appointment_id,test_id,patient_id,doctor_id,date,cancelled) " +
//            "VALUES ('lapp001' , 'test001' , 'pat001' , 'doc001' , '" + expectedAppDate + " 09:00:00' , false );"
//        );
//
//        // Verify lệnh cập nhật lab_appointment_timetable
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE lab_appointment_timetable SET current_week_appointments = current_week_appointments + 1 WHERE " +
//            "time_slot = '09:00-10:00' AND app_test_id = 'test001' AND app_day = '6';"
//        );
//
//        // Verify lệnh cập nhật tmp_bill
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE tmp_bill SET laboratory_fee = ' 300 ' WHERE tmp_bill_id = 'hms0001tb';"
//        );
//
//        // Verify không tạo tmp_bill mới
//        verify(dbOperator, never()).customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"
//        );
//    }
//    // Test Case: tmpDay <= today
//    @Test
//    public void testMakeLabAppointmentWithTmpDaySmallerThanToday() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat001";
//        String doctorID = "doc001";
//        String testID = "test001";
//        String day = "1"; // Thứ Sáu
//        String timeSlot = "09:00-10:00";
//
//        // Mock dữ liệu trả về từ truy vấn lab_appointment_id lớn nhất
//        ArrayList<ArrayList<String>> labAppIdResult = new ArrayList<>();
//        labAppIdResult.add(new ArrayList<>(Arrays.asList("lab_appointment_id")));
//        labAppIdResult.add(new ArrayList<>(Arrays.asList("lapp001")));
//        when(dbOperator.customSelection(
//            "SELECT lab_appointment_id FROM lab_appointment WHERE lab_appointment_id = (SELECT MAX(lab_appointment_id) FROM lab_appointment);"
//        )).thenReturn(labAppIdResult);
//
//        // Mock dữ liệu trả về từ truy vấn phí xét nghiệm
//        ArrayList<ArrayList<String>> testFeeResult = new ArrayList<>();
//        testFeeResult.add(new ArrayList<>(Arrays.asList("test_fee")));
//        testFeeResult.add(new ArrayList<>(Arrays.asList("300")));
//        when(dbOperator.customSelection(
//            "SELECT test_fee FROM lab_test WHERE test_id = 'test001';"
//        )).thenReturn(testFeeResult);
//
//        // Mock dữ liệu trả về từ truy vấn tmp_bill_id (bệnh nhân đã có hóa đơn)
//        ArrayList<ArrayList<String>> tmpBillIdResult = new ArrayList<>();
//        tmpBillIdResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
//        tmpBillIdResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
//        when(dbOperator.customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"
//        )).thenReturn(tmpBillIdResult);
//
//        // Mock các lệnh chèn/cập nhật thành công
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeLabAppointment(patientID, doctorID, testID, day, timeSlot);
//
//        // Assert
//        assertEquals("lapp002", result); // Kiểm tra trả về lab_appointment_id mới
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT lab_appointment_id FROM lab_appointment WHERE lab_appointment_id = (SELECT MAX(lab_appointment_id) FROM lab_appointment);"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT test_fee FROM lab_test WHERE test_id = 'test001';"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat001';"
//        );
//
//        // Verify lệnh chèn lab_appointment
//        String expectedAppDate = new SimpleDateFormat("yyyy-MM-dd").format(
//            Calendar.getInstance().getTime() // Giả sử hôm nay là 08/04/2025
//        ).replace("08", "10"); // Điều chỉnh từ Thứ Tư (08) sang Thứ Sáu (10)
//        verify(dbOperator, times(1)).customInsertion(
//            "INSERT INTO lab_appointment (lab_appointment_id,test_id,patient_id,doctor_id,date,cancelled) " +
//            "VALUES ('lapp002' , 'test001' , 'pat001' , 'doc001' , '" + expectedAppDate + " 09:00:00' , false );"
//        );
//
//        // Verify lệnh cập nhật lab_appointment_timetable
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE lab_appointment_timetable SET current_week_appointments = current_week_appointments + 1 WHERE " +
//            "time_slot = '09:00-10:00' AND app_test_id = 'test001' AND app_day = '6';"
//        );
//
//        // Verify lệnh cập nhật tmp_bill
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE tmp_bill SET laboratory_fee = ' 300 ' WHERE tmp_bill_id = 'hms0001tb';"
//        );
//
//        // Verify không tạo tmp_bill mới
//        verify(dbOperator, never()).customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"
//        );
//    }
//    @Test
//    public void testMakeLabAppointment_CreateNewTmpBillWhenNotFound() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat002";
//        String doctorID = "doc001";
//        String testID = "test001";
//        String day = "6"; // Thứ Sáu
//        String timeSlot = "09:00-10:00";
//
//        // Mock dữ liệu trả về từ truy vấn lab_appointment_id lớn nhất
//        ArrayList<ArrayList<String>> labAppIdResult = new ArrayList<>();
//        labAppIdResult.add(new ArrayList<>(Arrays.asList("lab_appointment_id")));
//        labAppIdResult.add(new ArrayList<>(Arrays.asList("lapp001")));
//        when(dbOperator.customSelection(
//            "SELECT lab_appointment_id FROM lab_appointment WHERE lab_appointment_id = (SELECT MAX(lab_appointment_id) FROM lab_appointment);"
//        )).thenReturn(labAppIdResult);
//
//        // Mock dữ liệu trả về từ truy vấn phí xét nghiệm
//        ArrayList<ArrayList<String>> testFeeResult = new ArrayList<>();
//        testFeeResult.add(new ArrayList<>(Arrays.asList("test_fee")));
//        testFeeResult.add(new ArrayList<>(Arrays.asList("300")));
//        when(dbOperator.customSelection(
//            "SELECT test_fee FROM lab_test WHERE test_id = 'test001';"
//        )).thenReturn(testFeeResult);
//
//        // *** Giả lập Exception để rơi vào catch ***
//        when(dbOperator.customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat002';"
//        )).thenThrow(new RuntimeException("Patient has no existing tmp_bill"));
//
//        // Mock dữ liệu trả về từ truy vấn MAX tmp_bill_id (để tạo mới)
//        ArrayList<ArrayList<String>> maxTmpBillIdResult = new ArrayList<>();
//        maxTmpBillIdResult.add(new ArrayList<>(Arrays.asList("tmp_bill_id")));
//        maxTmpBillIdResult.add(new ArrayList<>(Arrays.asList("hms0001tb")));
//        when(dbOperator.customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"
//        )).thenReturn(maxTmpBillIdResult);
//
//        // Mock customInsertion cho tất cả các insert/update
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeLabAppointment(patientID, doctorID, testID, day, timeSlot);
//
//        // Assert
//        assertEquals("lapp002", result);
//
//        // Kiểm tra customSelection đã bị ném exception đúng chỗ
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'pat002';"
//        );
//
//        // Kiểm tra đã truy vấn để tạo mới tmp_bill
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"
//        );
//
//        // Kiểm tra đã gọi customInsertion để chèn tmp_bill mới
//        verify(dbOperator).customInsertion(argThat(sql ->
//            sql.contains("INSERT INTO tmp_bill") &&
//            sql.contains("patient_id") &&
//            sql.contains("laboratory_fee") &&
//            sql.contains("hms0002tb") // Tăng từ hms0001tb
//        ));
//    }
//    @Test
//    public void testMakeLabAppointment_BillIDTooShort_TriggersFallback() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat002";
//        String doctorID = "doc001";
//        String testID = "test001";
//        String day = "6"; // Thứ Sáu
//        String timeSlot = "09:00-10:00";
//
//        // Mock lab_appointment_id
//        when(dbOperator.customSelection(anyString()))
//            .thenAnswer(invocation -> {
//                String query = invocation.getArgument(0);
//                if (query.contains("SELECT lab_appointment_id")) {
//                    return new ArrayList<>(Arrays.asList(
//                        new ArrayList<>(List.of("lab_appointment_id")),
//                        new ArrayList<>(List.of("lapp001"))
//                    ));
//                }
//                if (query.contains("SELECT test_fee")) {
//                    return new ArrayList<>(Arrays.asList(
//                        new ArrayList<>(List.of("test_fee")),
//                        new ArrayList<>(List.of("500"))
//                    ));
//                }
//                if (query.contains("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id")) {
//                    throw new SQLException("Simulated error: No tmp_bill for patient");
//                }
//                if (query.contains("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id =")) {
//                    // Return a billID with length <= 3 to skip the for loop
//                    return new ArrayList<>(Arrays.asList(
//                        new ArrayList<>(List.of("tmp_bill_id")),
//                        new ArrayList<>(List.of("hm")) // length = 2
//                    ));
//                }
//                return null;
//            });
//
//        when(dbOperator.customInsertion(anyString())).thenReturn(true);
//
//        // Act
//        String result = receptionistInstance.makeLabAppointment(patientID, doctorID, testID, day, timeSlot);
//
//        // Assert
//        assertEquals("lapp002", result); // Vẫn tạo được cuộc hẹn
//        verify(dbOperator).customInsertion(contains("INSERT INTO tmp_bill"));
//    }
//    @Test
//    public void testMakeLabAppointment_CatchSQLExceptionFromInsertion() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String patientID = "pat005";
//        String doctorID = "doc001";
//        String testID = "test002";
//        String day = "6"; // Friday
//        String timeSlot = "11:00-12:00";
//
//        // Giả lập kết quả SELECT lab_appointment_id
//        when(dbOperator.customSelection(contains("SELECT lab_appointment_id")))
//            .thenReturn(new ArrayList<>(Arrays.asList(
//                new ArrayList<>(List.of("lab_appointment_id")),
//                new ArrayList<>(List.of("lapp010"))
//            )));
//
//        // Giả lập SELECT test_fee
//        when(dbOperator.customSelection(contains("SELECT test_fee")))
//            .thenReturn(new ArrayList<>(Arrays.asList(
//                new ArrayList<>(List.of("test_fee")),
//                new ArrayList<>(List.of("500"))
//            )));
//
//        // Giả lập không có tmp_bill => tạo mới
//        when(dbOperator.customSelection(contains("SELECT tmp_bill_id FROM tmp_bill WHERE patient_id")))
//            .thenReturn(new ArrayList<>(List.of(
//                new ArrayList<>(List.of("tmp_bill_id")) // Header only = no result
//            )));
//
//        // Có billID cũ để tạo mới
//        when(dbOperator.customSelection(contains("SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id")))
//            .thenReturn(new ArrayList<>(List.of(
//                new ArrayList<>(List.of("tmp_bill_id")),
//                new ArrayList<>(List.of("hms0003tb"))
//            )));
//
//        // Chèn tmp_bill thành công
//        when(dbOperator.customInsertion(contains("INSERT INTO tmp_bill")))
//            .thenReturn(true);
//
//        // Insert thất bại
//        when(dbOperator.customInsertion(contains("INSERT INTO lab_appointment")))
//            .thenThrow(new SQLException("Simulated failure in lab_appointment"));
//
//        // Act
//        String result = receptionistInstance.makeLabAppointment(patientID, doctorID, testID, day, timeSlot);
//
//        // Assert
//        assertEquals("lapp011", result); // Dù lỗi vẫn trả về appointment ID
//    }
//
//    // Test Case: Thành công với dữ liệu hợp lệ
//    @Test
//    public void testCancelAppointmentSuccess() throws SQLException, ClassNotFoundException {
//        // Arrange
//        String appointmentID = "app001";
//
//        // Mock cập nhật appointment thành công
//        when(dbOperator.customInsertion(
//            "UPDATE appointment SET cancelled = true WHERE appointment.appointment_id = 'app001';"
//        )).thenReturn(true);
//
//        // Mock dữ liệu trả về từ truy vấn bill
//        ArrayList<ArrayList<String>> billResult = new ArrayList<>();
//        billResult.add(new ArrayList<>(Arrays.asList("bill_id", "total")));
//        billResult.add(new ArrayList<>(Arrays.asList("b001", "500")));
//        when(dbOperator.customSelection(
//            "SELECT appointment.bill_id, bill.total FROM appointment INNER JOIN bill ON " +
//            "appointment.bill_id = bill.bill_id WHERE appointment_id = 'app001'"
//        )).thenReturn(billResult);
//
//        // Mock logic trong refund
//        // Mock truy vấn refund_id lớn nhất
//        ArrayList<ArrayList<String>> refundIdResult = new ArrayList<>();
//        refundIdResult.add(new ArrayList<>(Arrays.asList("refund_id")));
//        refundIdResult.add(new ArrayList<>(Arrays.asList("r0001")));
//        when(dbOperator.customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        )).thenReturn(refundIdResult);
//
//        // Mock chèn refund thành công
//        when(dbOperator.customInsertion(
//            eq("INSERT INTO refund (bill_id,payment_type,reason,amount,refund_id,date) " +
//            "VALUES ('b001','docApp','no_reason',500,'r0002','" + 
//            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + "');")
//        )).thenReturn(true);
//
//        // Mock cập nhật bill thành công
//        when(dbOperator.customInsertion(
//            "UPDATE bill SET refund = 1 WHERE bill_id = 'b001'"
//        )).thenReturn(true);
//
//        // Act
//        boolean result = receptionistInstance.cancelAppointment(appointmentID);
//
//        // Assert
//        assertTrue(result); // Kiểm tra trả về true
//
//        // Verify các lệnh SQL được gọi đúng
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE appointment SET cancelled = true WHERE appointment.appointment_id = 'app001';"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT appointment.bill_id, bill.total FROM appointment INNER JOIN bill ON " +
//            "appointment.bill_id = bill.bill_id WHERE appointment_id = 'app001'"
//        );
//        verify(dbOperator, times(1)).customSelection(
//            "SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"
//        );
//        verify(dbOperator, times(1)).customInsertion(
//            eq("INSERT INTO refund (bill_id,payment_type,reason,amount,refund_id,date) " +
//            "VALUES ('b001','docApp','no_reason',500,'r0002','" + 
//            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + "');")
//        );
//        verify(dbOperator, times(1)).customInsertion(
//            "UPDATE bill SET refund = 1 WHERE bill_id = 'b001'"
//        );
//    }
//}