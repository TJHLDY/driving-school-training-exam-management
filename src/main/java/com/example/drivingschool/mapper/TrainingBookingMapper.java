package com.example.drivingschool.mapper;
import com.example.drivingschool.dto.TrainingBookingDetailDto;
import com.example.drivingschool.entity.TrainingBooking;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 培训预约与学时模块：时段、预约、分配、训练结果都在这一张表上
public interface TrainingBookingMapper {
    TrainingBooking selectById(@Param("bookingId") Long bookingId);
    // 加行锁读取，认领和分配前先锁住这一行
    TrainingBooking selectByIdForUpdate(@Param("bookingId") Long bookingId);
    TrainingBookingDetailDto selectDetailById(@Param("bookingId") Long bookingId);
    List<TrainingBookingDetailDto> selectDetailList(@Param("studentUserId") Long studentUserId,
                                                    @Param("coachUserId") Long coachUserId,
                                                    @Param("status") String status);
    // 学员可认领的时段：状态 OPEN、未开始
    List<TrainingBooking> selectOpenSlots(@Param("fromTime") LocalDateTime fromTime);
    int insertOpenSlot(TrainingBooking trainingBooking);
    // 认领时段：只允许 OPEN 且未被认领的行，返回 0 说明被别人抢先，防止两人抢同一时段
    int claimOpenSlot(@Param("bookingId") Long bookingId, @Param("studentUserId") Long studentUserId);
    // 分配教练和车辆，只允许 PENDING 状态
    int assign(@Param("bookingId") Long bookingId,
               @Param("coachUserId") Long coachUserId,
               @Param("vehicleId") Long vehicleId,
               @Param("assignedBy") Long assignedBy);
    // 教练登记训练结果，只允许 ASSIGNED 状态
    int complete(@Param("bookingId") Long bookingId,
                 @Param("actualStart") LocalDateTime actualStart,
                 @Param("actualEnd") LocalDateTime actualEnd,
                 @Param("validHours") BigDecimal validHours,
                 @Param("resultNote") String resultNote);
    // 开始前取消，只允许 PENDING / ASSIGNED 状态
    int cancel(@Param("bookingId") Long bookingId);
    // 退学申请通过后取消该学员未来的未完成预约
    int cancelFutureByStudentUserId(@Param("studentUserId") Long studentUserId);
    // 学员时间冲突：新开始 < 已有结束 且 新结束 > 已有开始
    int countStudentConflict(@Param("studentUserId") Long studentUserId,
                             @Param("plannedStart") LocalDateTime plannedStart,
                             @Param("plannedEnd") LocalDateTime plannedEnd);
    int countCoachConflict(@Param("coachUserId") Long coachUserId,
                           @Param("plannedStart") LocalDateTime plannedStart,
                           @Param("plannedEnd") LocalDateTime plannedEnd);
    int countVehicleConflict(@Param("vehicleId") Long vehicleId,
                             @Param("plannedStart") LocalDateTime plannedStart,
                             @Param("plannedEnd") LocalDateTime plannedEnd);
    // 累计有效学时只汇总 COMPLETED 行
    BigDecimal sumValidHoursByStudentUserId(@Param("studentUserId") Long studentUserId);
}
