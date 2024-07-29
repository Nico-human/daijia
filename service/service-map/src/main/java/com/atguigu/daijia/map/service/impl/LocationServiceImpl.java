package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {

        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(),
                                updateDriverLocationForm.getLatitude().doubleValue());

        Long added = redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION,
                point,
                updateDriverLocationForm.getDriverId().toString());

        if (added == null || added == 0){
            return false;
        }

        return true;
    }

    @Override
    public Boolean removeDriverLocation(Long driverId) {
        Long removed = redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());

        if (removed == null || removed == 0){
            return false;
        }

        return true;
    }

    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {

        // 乘客所在的经纬度
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(),
                                searchNearByDriverForm.getLatitude().doubleValue());

        // 距离(值, 单位)
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS, RedisGeoCommands.DistanceUnit.KILOMETERS);

        //根据 经纬度信息和距离 可以得到一个圈
        Circle circle = new Circle(point, distance);

        // 定位GEO参数, 设置返回结果包含内容
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates().sortAscending(); // 返回结果包含司机的距离和坐标. 按升序排列

        // 查询Redis, 返回list结果(包含接单距离: 当前司机距离乘客xx米/千米)
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                redisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = geoResults.getContent();

        //遍历集合, 获取每个司机的信息
        List<NearByDriverVo> nearByDriverVos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(content)){

            for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : content) {
                long driverId = Long.parseLong(item.getContent().getName());

                // 远程调用获取司机id个性化设置信息(订单里程, 接单里程)
                Result<DriverSet> driverSetResult = driverInfoFeignClient.getDriverSet(driverId);
                if (driverSetResult.getCode() != 200) {
                    System.out.println("获取司机个性化设置失败");
                    continue;
                }

                DriverSet driverSet = driverSetResult.getData();

                // 判断订单里程order_distance
                BigDecimal orderDistance = driverSet.getOrderDistance();
                if (orderDistance.doubleValue() != 0.0 &&
                        orderDistance.subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
                    continue;
                }

                //判断接单里程accept_distance
                BigDecimal acceptDistance = driverSet.getAcceptDistance();
                BigDecimal currentDistance = BigDecimal.valueOf(item.getDistance().getValue()).
                        setScale(2, RoundingMode.HALF_UP); // 当前接单距离 (司机距离乘客多少米/千米)
                if (acceptDistance.doubleValue() != 0.0 &&
                        acceptDistance.subtract(currentDistance).doubleValue() < 0) {
                    continue;
                }

                // 封装符合条件的司机信息
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                nearByDriverVos.add(nearByDriverVo);
            }

        }

        return nearByDriverVos;
    }


}
