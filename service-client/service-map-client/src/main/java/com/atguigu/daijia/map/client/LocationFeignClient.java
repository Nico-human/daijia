package com.atguigu.daijia.map.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "service-map")
public interface LocationFeignClient {

    /**
     * 更新司机经纬度位置信息
     * @param updateDriverLocationForm
     * @return
     */
    @PostMapping("/map/location/updateDriverLocation")
    Result<Boolean> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm);

    /**
     * 删除司机经纬度位置信息
     * @param driverId
     * @return
     */
    @DeleteMapping("map/location/removeDriverLocation/{driverId}")
    Result<Boolean> removeDriverLocation(@PathVariable Long driverId);

    /**
     * 搜索附近满足条件的司机
     * @param searchNearByDriverForm
     * @return 所有满足条件的司机 (list)
     */
    @PostMapping("/map/location/searchNearByDriver")
    Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody SearchNearByDriverForm searchNearByDriverForm);

    /**
     * 司机赶往代驾起点, 更新订单位置到redis缓存中
     * @param updateOrderLocationForm
     * @return
     */
    @PostMapping("/map/location/updateOrderLocationToCache")
    Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm);

    /**
     * 司乘同显: 获取订单经纬度信息
     * @param orderId
     * @return
     */
    @GetMapping("/map/location/getCacheOrderLocation/{orderId}")
    Result<OrderLocationVo> getCacheOrderLocation(@PathVariable Long orderId);

    /**
     * 批量保存代驾订单位置
     * @param orderServiceLocationFormList
     * @return
     */
    @PostMapping("/map/location/saveOrderServiceLocation")
    Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderServiceLocationFormList);

    /**
     * 代驾服务: 获取订单服务最后一个位置信息
     * @param orderId
     * @return
     */
    @GetMapping("/map/location/getOrderServiceLastLocation/{orderId}")
    Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId);
}