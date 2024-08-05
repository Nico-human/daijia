package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private MongoTemplate mongoTemplate;

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

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {

        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());

        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();

        redisTemplate.opsForValue().set(key, orderLocationVo, 60, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        String key = RedisConstant.UPDATE_ORDER_LOCATION + orderId;

        OrderLocationVo orderLocationVo = (OrderLocationVo) redisTemplate.opsForValue().get(key);
        if (orderLocationVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        return orderLocationVo;
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderServiceLocationFormList) {

        List<OrderServiceLocation> list = new ArrayList<>();

        orderServiceLocationFormList.forEach(orderServiceLocationForm -> {
            OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
            // orderServiceLocationFormList --> orderServiceLocation
            BeanUtils.copyProperties(orderServiceLocationForm, orderServiceLocation);
            orderServiceLocation.setId(ObjectId.get().toString());
            orderServiceLocation.setCreateTime(new Date());

            list.add(orderServiceLocation);
        });
        mongoTemplate.insertAll(list);
        return true;
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        // select * from _ where order_id = ? order by DESC limit 1;
        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        query.limit(1);

        OrderServiceLocation orderServiceLocation = mongoTemplate.findOne(query, OrderServiceLocation.class);

        if (orderServiceLocation == null) { // TODO: 优化这段代码
            log.info("MongoDB中没有此对象 orderServiceLocation == null ");
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
        orderServiceLastLocationVo.setLatitude(orderServiceLocation.getLatitude());
        orderServiceLastLocationVo.setLongitude(orderServiceLocation.getLongitude());
        return orderServiceLastLocationVo;
    }

    @Override
    public BigDecimal calculateOrderRealDistance(Long orderId) {

        //1. 根据订单id获取代驾订单位置信息list集合, 根据创建时间排序(升序)
        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Direction.ASC, "createTime"));
        List<OrderServiceLocation> orderServiceLocationList = mongoTemplate.find(query, OrderServiceLocation.class);

        //2. 遍历位置信息list集合, 计算每两个位置之间的距离, 相加得到订单总距离
        double distance = 0;
        if (!CollectionUtils.isEmpty(orderServiceLocationList)) {

            for (int i = 0; i < orderServiceLocationList.size() - 1; i++) {
                OrderServiceLocation locationA = orderServiceLocationList.get(i);
                OrderServiceLocation locationB = orderServiceLocationList.get(i + 1);

                // A -> B 的距离
                double _Distance = LocationUtil.getDistance(locationA.getLatitude().doubleValue(),
                                                            locationA.getLongitude().doubleValue(),
                                                            locationB.getLatitude().doubleValue(),
                                                            locationB.getLongitude().doubleValue());

                distance += _Distance;
            }

        }
        return new BigDecimal(distance).setScale(2, RoundingMode.HALF_UP);
    }
}
