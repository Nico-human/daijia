package com.atguigu.daijia.driver.mapper;

import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;

@Mapper
public interface DriverAccountMapper extends BaseMapper<DriverAccount> {

    /**
     * 更新司机账户余额
     * @param driverId
     * @param amount
     */
    void updateDriverAccount(Long driverId, BigDecimal amount);
}
