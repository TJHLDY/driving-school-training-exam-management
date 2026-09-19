package com.example.drivingschool.mapper;

import com.example.drivingschool.entity.TrainingBooking;
import com.example.drivingschool.entity.TrainingVehicle;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TrainingMapper {
    List<TrainingVehicle> findVehicles(@Param("enabled") Boolean enabled);
    TrainingVehicle findVehicleById(@Param("vehicleId") Long vehicleId);
    TrainingVehicle findVehicleByIdForUpdate(@Param("vehicleId") Long vehicleId);
    int insertVehicle(TrainingVehicle vehicle);
    int updateVehicle(TrainingVehicle vehicle);

    TrainingBooking findBookingById(@Param("bookingId") Long bookingId);
    TrainingBooking findBookingByIdForUpdate(@Param("bookingId") Long bookingId);
    int insertOpenBooking(TrainingBooking booking);
    int claim(@Param("bookingId") Long bookingId, @Param("studentUserId") Long studentUserId,
              @Param("requestedAt") LocalDateTime requestedAt);
    int assign(@Param("bookingId") Long bookingId, @Param("coachUserId") Long coachUserId,
               @Param("vehicleId") Long vehicleId, @Param("assignedBy") Long assignedBy,
               @Param("assignedAt") LocalDateTime assignedAt);
    int cancel(@Param("bookingId") Long bookingId, @Param("cancelledAt") LocalDateTime cancelledAt);
    int complete(@Param("bookingId") Long bookingId, @Param("actualStart") LocalDateTime actualStart,
                 @Param("actualEnd") LocalDateTime actualEnd, @Param("validHours") BigDecimal validHours,
                 @Param("resultNote") String resultNote);

    int countOverlaps(@Param("studentUserId") Long studentUserId, @Param("coachUserId") Long coachUserId,
                      @Param("vehicleId") Long vehicleId, @Param("plannedStart") LocalDateTime plannedStart,
                      @Param("plannedEnd") LocalDateTime plannedEnd, @Param("excludeBookingId") Long excludeBookingId);
    int cancelFutureBookings(@Param("studentUserId") Long studentUserId, @Param("now") LocalDateTime now);
    int countUnfinishedStarted(@Param("studentUserId") Long studentUserId, @Param("now") LocalDateTime now);
    BigDecimal sumCompletedHours(@Param("studentUserId") Long studentUserId);

    List<TrainingBooking> findPage(@Param("studentUserId") Long studentUserId,
                                   @Param("coachUserId") Long coachUserId,
                                   @Param("status") String status,
                                   @Param("offset") long offset, @Param("size") int size);
    long count(@Param("studentUserId") Long studentUserId, @Param("coachUserId") Long coachUserId,
               @Param("status") String status);
}
