package com.atguigu.daijia.coupon.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.coupon.mapper.CouponInfoMapper;
import com.atguigu.daijia.coupon.mapper.CustomerCouponMapper;
import com.atguigu.daijia.coupon.service.CouponInfoService;
import com.atguigu.daijia.model.entity.coupon.CouponInfo;
import com.atguigu.daijia.model.entity.coupon.CustomerCoupon;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoUseCouponVo;
import com.atguigu.daijia.model.vo.coupon.UsedCouponVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {

    @Autowired
    private CouponInfoMapper couponInfoMapper;
    @Autowired
    private CustomerCouponMapper customerCouponMapper;

    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoReceiveCouponVo> pageInfo = couponInfoMapper.findNoReceivePage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoUseCouponVo> pageInfo = couponInfoMapper.findNoUsePage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<UsedCouponVo> pageInfo = couponInfoMapper.findUsedPage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean receive(Long customerId, Long couponId) {
        //1. 查询优惠券, 优惠券是否存在判断
        CouponInfo couponInfo = couponInfoMapper.selectById(couponId);
        if (couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        }

        //2. 优惠券过期日期判断
        if (couponInfo.getExpireTime().before(new Date())) {
            throw new GuiguException(ResultCodeEnum.COUPON_EXPIRE);
        }

        //3. 校验库存, 优惠券发行数量和领取数量判断
        if(couponInfo.getPublishCount() != 0 &&
                couponInfo.getReceiveCount() >= couponInfo.getPublishCount()) {
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        }

        //4. 校验每人限领的数量
        if (couponInfo.getPerLimit() > 0) {
            Long count = customerCouponMapper.selectCount(
                    new LambdaQueryWrapper<CustomerCoupon>().eq(CustomerCoupon::getCustomerId, customerId)
                                                            .eq(CustomerCoupon::getCouponId, couponId));
            if (count >= couponInfo.getPerLimit()) {
                throw new GuiguException(ResultCodeEnum.COUPON_USER_LIMIT);
            }
        }

        //5. 领取优惠券
        int row = couponInfoMapper.updateReceiveCount(couponId);
        if (row != 1) {
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        }
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setCouponId(couponId);
        customerCoupon.setStatus(1);
        customerCoupon.setReceiveTime(new Date());
        customerCoupon.setExpireTime(couponInfo.getExpireTime());
        customerCouponMapper.insert(customerCoupon);
        return true;
    }

}
