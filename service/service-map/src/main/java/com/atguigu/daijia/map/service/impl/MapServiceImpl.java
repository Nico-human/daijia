package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    @Autowired
    private RestTemplate restTemplate;

    @Value(value = "${tencent.map.key}")
    private String key;

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {

        //定义调用地址
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";

        // 封装传递参数
        Map<String, String> map = new HashMap<>();
        // 起点终点的经纬度信息
        map.put("from", calculateDrivingLineForm.getStartPointLatitude() + "," + calculateDrivingLineForm.getStartPointLongitude());
        map.put("to", calculateDrivingLineForm.getEndPointLatitude() + "," + calculateDrivingLineForm.getEndPointLongitude());
        map.put("key", key);

        //获取返回路线信息
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);

        //判断是否调用正常, status=0表示正常
        int status = result.getIntValue("status");
        if (status != 0) {
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }

        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);

        // 创建Vo对象, 封装预估时间, 预估距离, 预估路线
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        drivingLineVo.setDistance(route.getBigDecimal("distance")
                                .divide(new BigDecimal(1000)) // 单位转换: 米 -> 千米
                                .setScale(2, RoundingMode.HALF_UP)); // 四舍五入
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));


        return drivingLineVo;
    }
}
