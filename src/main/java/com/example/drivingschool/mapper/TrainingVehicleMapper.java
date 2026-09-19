package com.example.drivingschool.mapper;
import com.example.drivingschool.entity.TrainingVehicle;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 培训预约与学时模块：车辆映射器
public interface TrainingVehicleMapper {
    TrainingVehicle selectById(@Param("vehicleId") Long vehicleId);
    List<TrainingVehicle> selectList(@Param("enabled") Boolean enabled,
                                     @Param("licenseType") String licenseType);
    int countByPlateNo(@Param("plateNo") String plateNo);
    int insert(TrainingVehicle trainingVehicle);
    int updateById(TrainingVehicle trainingVehicle);
    int updateEnabled(@Param("vehicleId") Long vehicleId, @Param("enabled") Boolean enabled);
    int countBookingReference(@Param("vehicleId") Long vehicleId);
    int deleteById(@Param("vehicleId") Long vehicleId);
}
