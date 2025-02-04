package com.atguigu.daijia.payment.service;

import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;

public interface WxPayService {

    /**
     * 微信支付
     * @param paymentInfoForm
     * @return
     */
    WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm);

    /**
     * 支付状态查询
     * @param orderNo
     * @return
     */
    Boolean queryPayStatus(String orderNo);

    /**
     * 支付成功后续处理(消息接受端)
     * @param orderNo
     */
    void handleOrder(String orderNo);
}
