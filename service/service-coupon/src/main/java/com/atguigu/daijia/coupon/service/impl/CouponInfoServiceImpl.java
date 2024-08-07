package com.atguigu.daijia.coupon.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.coupon.mapper.CouponInfoMapper;
import com.atguigu.daijia.coupon.mapper.CustomerCouponMapper;
import com.atguigu.daijia.coupon.service.CouponInfoService;
import com.atguigu.daijia.model.entity.coupon.CouponInfo;
import com.atguigu.daijia.model.entity.coupon.CustomerCoupon;
import com.atguigu.daijia.model.form.coupon.UseCouponForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.coupon.AvailableCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoUseCouponVo;
import com.atguigu.daijia.model.vo.coupon.UsedCouponVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {

    @Autowired
    private CouponInfoMapper couponInfoMapper;
    @Autowired
    private CustomerCouponMapper customerCouponMapper;
    @Autowired
    private RedissonClient redissonClient;

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

        RLock lock = null;
        try {
            lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + customerId);
            boolean isLock = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME,
                                          RedisConstant.COUPON_LOCK_LEASE_TIME,
                                          TimeUnit.SECONDS);
            if (isLock) {
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

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
        throw new GuiguException(ResultCodeEnum.COUPON_LESS);
    }

    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount) {

        //1. 定义符合条件的优惠券信息容器
        ArrayList<AvailableCouponVo> availableCouponVoList = new ArrayList<>();

        //2. 获取未使用的优惠券列表
        List<NoUseCouponVo> list = couponInfoMapper.findNoUseList(customerId);

        //3. 遍历乘客未使用优惠券列表, 得到每个优惠券类型, 按类型计算优惠后的订单价格

        //3.1 现金券
        List<NoUseCouponVo> typeList = list.stream().filter(item -> item.getCouponType() == 1).toList();

        for (NoUseCouponVo noUseCouponVo: typeList) {
            BigDecimal reduceAmount = noUseCouponVo.getAmount();
            if (noUseCouponVo.getConditionAmount().doubleValue() == 0 // 无门槛优惠券
                    && orderAmount.subtract(reduceAmount).doubleValue() > 0){
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
            if (noUseCouponVo.getConditionAmount().doubleValue() == 0 // 无门槛优惠券, 优惠券金额比订单价格高
                    && orderAmount.subtract(reduceAmount).doubleValue() <= 0){
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, orderAmount));
            }
            if (noUseCouponVo.getConditionAmount().doubleValue() > 0 // 有门槛优惠券
                    && orderAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
        }

        //3.2 折扣券
        List<NoUseCouponVo> typeList2 = list.stream().filter(item -> item.getCouponType() == 2).toList();
        for (NoUseCouponVo noUseCouponVo: typeList2) {
            BigDecimal discountAmount = orderAmount.multiply(noUseCouponVo.getDiscount())
                                                .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            BigDecimal reduceAmount = orderAmount.subtract(discountAmount);
            if (noUseCouponVo.getConditionAmount().doubleValue() == 0) { // 无门槛
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
            if (noUseCouponVo.getConditionAmount().doubleValue() > 0 // 有门槛
                    && orderAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
        }

        //4. 把满足条件的优惠券放入最终list集合, 并根据金额排序
        if (!CollectionUtils.isEmpty(availableCouponVoList)) {
            availableCouponVoList.sort(new Comparator<AvailableCouponVo>() {
                @Override
                public int compare(AvailableCouponVo o1, AvailableCouponVo o2) {
                    return o1.getReduceAmount().compareTo(o2.getReduceAmount());
                }
            });
        }

        return availableCouponVoList;

    }

    private AvailableCouponVo buildBestNoUseCouponVo(NoUseCouponVo noUseCouponVo, BigDecimal reduceAmount) {
        AvailableCouponVo availableCouponVo = new AvailableCouponVo();
        BeanUtils.copyProperties(noUseCouponVo, availableCouponVo);
        availableCouponVo.setCouponId(noUseCouponVo.getId());
        availableCouponVo.setReduceAmount(reduceAmount);
        return availableCouponVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public BigDecimal useCoupon(UseCouponForm useCouponForm) {
        //1. 获取乘客优惠券信息(customer_coupon)
        CustomerCoupon customerCoupon =
                customerCouponMapper.selectById(useCouponForm.getCustomerCouponId());
        if (customerCoupon == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //2. 获取优惠券信息(coupon_info)
        CouponInfo couponInfo = couponInfoMapper.selectById(useCouponForm.getOrderId());
        if (couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        }

        //3. 判断该乘客是否未该乘客所有
        if (customerCoupon.getCustomerId() != useCouponForm.getCustomerId()) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //4. 判断是否达到使用该优惠券门槛, 若达到则计算减免金额
        if (couponInfo.getConditionAmount().doubleValue() > 0
                && useCouponForm.getOrderAmount().subtract(couponInfo.getConditionAmount()).doubleValue() < 0) {
            throw new GuiguException(ResultCodeEnum.COUPON_NOT_ACHIEVE_CONDITION_AMOUNT);
        }
        BigDecimal reduceAmount;
        if (couponInfo.getCouponType() == 1) { //现金券
            if (useCouponForm.getOrderAmount().subtract(couponInfo.getAmount()).doubleValue() > 0){
                reduceAmount = couponInfo.getAmount();
            } else {
                reduceAmount = useCouponForm.getOrderAmount();
            }
        } else {  //折扣券
            BigDecimal discountAmount = useCouponForm.getOrderAmount()
                    .multiply(couponInfo.getDiscount())
                    .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            reduceAmount = useCouponForm.getOrderAmount().subtract(discountAmount);
        }

        //5. 如果满足条件, 更新数据(coupon_info, customer_coupon)
        if (reduceAmount.doubleValue() > 0) {
            //更新coupon_info
            couponInfo.setUseCount(couponInfo.getUseCount()+1);
            couponInfoMapper.updateById(couponInfo);

            //更新customer_coupon
            CustomerCoupon updateCustomerCoupon = new CustomerCoupon();
            updateCustomerCoupon.setId(customerCoupon.getId());
            updateCustomerCoupon.setUsedTime(new Date());
            updateCustomerCoupon.setOrderId(useCouponForm.getOrderId());
            updateCustomerCoupon.setStatus(2);
            customerCouponMapper.updateById(updateCustomerCoupon);

            return reduceAmount;
        }

        return null;
    }

}
