package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.coupon.AvailableCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoUseCouponVo;
import com.atguigu.daijia.model.vo.coupon.UsedCouponVo;

import java.util.List;

public interface CouponService  {

    /**
     * 查询未领取的优惠券
     * @param customerId
     * @param page
     * @param limit
     * @return
     */
    PageVo<NoReceiveCouponVo> findNoReceivePage(Long customerId, Long page, Long limit);

    /**
     * 查询未使用的优惠券
     * @param customerId
     * @param page
     * @param limit
     * @return
     */
    PageVo<NoUseCouponVo> findNoUsePage(Long customerId, Long page, Long limit);

    /**
     * 查询使用过的优惠券
     * @param customerId
     * @param page
     * @param limit
     * @return
     */
    PageVo<UsedCouponVo> findUsedPage(Long customerId, Long page, Long limit);

    /**
     * 领取优惠券
     * @param customerId
     * @param couponId
     * @return
     */
    Boolean receive(Long customerId, Long couponId);

    /**
     * 获取未使用的最佳优惠券信息
     * @param customerId
     * @param orderId
     * @return
     */
    List<AvailableCouponVo> findAvailableCoupon(Long customerId, Long orderId);
}
