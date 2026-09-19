package com.example.drivingschool.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
// 培训记录详情：一条预约连同学员、教练和车辆的显示信息
@Data

public class TrainingBookingDetailDto {
    private Long bookingId;
    private Long studentUserId;
    private String studentName;
    private Long coachUserId;
    private String coachName;
    private Long vehicleId;
    private String plateNo;
    private String licenseType;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private String location;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private BigDecimal validHours;
    private String resultNote;
}
