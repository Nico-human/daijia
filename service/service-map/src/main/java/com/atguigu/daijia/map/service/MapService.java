package com.atguigu.daijia.map.service;

import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;

public interface MapService {

    /**
     * 请求腾讯地图服务得到预估路线, 距离, 时间
     * TODO: 需要先去腾讯位置服务 <a href="https://lbs.qq.com/dev/console/quota/account">...</a> 分配额度
     * 给 周边推荐(explore), 关键词输入提示, 驾车路线规划 三个接口分配额度, 否则无法使用
     * @param calculateDrivingLineForm
     * @return
     */
    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);
}
