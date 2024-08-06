package com.atguigu.daijia.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.common.service.RabbitService;
import com.atguigu.daijia.driver.client.DriverAccountFeignClient;
import com.atguigu.daijia.model.entity.payment.PaymentInfo;
import com.atguigu.daijia.model.enums.TradeTypeEnum;
import com.atguigu.daijia.model.form.driver.TransferForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.order.OrderRewardVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.payment.config.WxPayV3Properties;
import com.atguigu.daijia.payment.mapper.PaymentInfoMapper;
import com.atguigu.daijia.payment.mapper.ProfitsharingInfoMapper;
import com.atguigu.daijia.payment.service.WxPayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private ProfitsharingInfoMapper profitsharingInfoMapper;
    @Autowired
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;
    @Autowired
    private WxPayV3Properties wxPayV3Properties;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
    private DriverAccountFeignClient driverAccountFeignClient;

    @Override
    public WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm) {
        try {
            //1. 添加支付记录到支付表中(payment_info)
            LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentInfo::getOrderNo, paymentInfoForm.getOrderNo());
            PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);
            if (paymentInfo == null) {
                paymentInfo = new PaymentInfo();
                BeanUtils.copyProperties(paymentInfoForm, paymentInfo);
                paymentInfo.setPaymentStatus(0); // 0: 未支付
                paymentInfoMapper.insert(paymentInfo);
            }

            //2. 创建微信支付使用对象
            JsapiServiceExtension service =
                    new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();

            //3. 创建request对象, 封装微信支付接口的入参
            PrepayRequest request = new PrepayRequest();
            Amount amount = new Amount();
            // amount.setTotal(paymentInfoForm.getAmount().multiply(new BigDecimal(100)).intValue());
            //TODO: 测试使用, 支付1分钱
            amount.setTotal(1);
            request.setAmount(amount);
            request.setAppid(wxPayV3Properties.getAppid());
            request.setMchid(wxPayV3Properties.getMerchantId());
            //string[1,127]
            String description = paymentInfo.getContent();
            if(description.length() > 127) {
                description = description.substring(0, 127);
            }
            request.setDescription(description);
            request.setNotifyUrl(wxPayV3Properties.getNotifyUrl());
            request.setOutTradeNo(paymentInfo.getOrderNo());

            //获取用户信息
            Payer payer = new Payer();
            payer.setOpenid(paymentInfoForm.getCustomerOpenId());
            request.setPayer(payer);

            //是否指定分账，不指定不能分账
            SettleInfo settleInfo = new SettleInfo();
            settleInfo.setProfitSharing(true);
            request.setSettleInfo(settleInfo);

            //4. 调用微信支付使用对象里面的方法实现微信支付调用
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);

            //5. 封装返回结果
            WxPrepayVo wxPrepayVo = new WxPrepayVo();
            BeanUtils.copyProperties(response, wxPrepayVo);
            wxPrepayVo.setTimeStamp(response.getTimeStamp());

            return wxPrepayVo;
        } catch (BeansException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean queryPayStatus(String orderNo) {
        //1 创建微信操作对象
        JsapiServiceExtension service =
                new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();

        //2 封装查询支付状态需要参数
        QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
        queryRequest.setMchid(wxPayV3Properties.getMerchantId());
        queryRequest.setOutTradeNo(orderNo);

        //3 调用微信操作对象里面方法实现查询操作
        Transaction transaction = service.queryOrderByOutTradeNo(queryRequest);

        //4 查询返回结果，根据结果判断
        if(transaction != null
                && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
            //5 如果支付成功，调用其他方法实现支付后处理逻辑
            this.handlePayment(transaction);

            return true;
        }
        return false;
    }

    // 如果支付成功, 调用其他方法实现支付后处理逻辑
    public void handlePayment(Transaction transaction) {

        String orderNo = transaction.getOutTradeNo();

        //1 更新支付记录, 状态修改为 已经支付
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);
        if (paymentInfo.getPaymentStatus() == 1) {
            return;
        }
        paymentInfo.setPaymentStatus(1); // 1: 已支付
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setTransactionId(transaction.getTransactionId());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString(transaction));
        paymentInfoMapper.updateById(paymentInfo);

        //2 发送端: 发送mq消息，传递 订单编号
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, MqConst.ROUTING_PAY_SUCCESS, orderNo);
    }

    @GlobalTransactional
    @Override
    public void handleOrder(String orderNo) {

        //1 远程调用：更新订单状态为 已经支付
        orderInfoFeignClient.updateOrderPayStatus(orderNo);

        //2 远程调用：获取系统奖励，打入到司机账户(转账)
        OrderRewardVo orderRewardVo = orderInfoFeignClient.getOrderRewardFee(orderNo).getData();
        if (orderRewardVo != null && orderRewardVo.getRewardFee().doubleValue() > 0) {
            TransferForm transferForm = new TransferForm();
            transferForm.setTradeNo(orderNo);
            transferForm.setTradeType(TradeTypeEnum.REWARD.getType());
            transferForm.setContent(TradeTypeEnum.REWARD.getContent());
            transferForm.setAmount(orderRewardVo.getRewardFee());
            transferForm.setDriverId(orderRewardVo.getDriverId());
            driverAccountFeignClient.transfer(transferForm);
        }

        //3 TODO 其他

    }

}
