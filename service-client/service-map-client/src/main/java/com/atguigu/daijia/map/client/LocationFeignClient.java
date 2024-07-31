package com.atguigu.daijia.map.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
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
}