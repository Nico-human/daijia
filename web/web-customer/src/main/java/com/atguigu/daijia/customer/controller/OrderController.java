package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.LoginAuth;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "预估订单数据")
    @PostMapping("/expectOrder")
    @LoginAuth
    public Result<ExpectOrderVo> expectOrder(@RequestBody ExpectOrderForm expectOrderForm){
        ExpectOrderVo expectOrderVo = orderService.expectOrder(expectOrderForm);
        return Result.ok(expectOrderVo);
    }

    @Operation(summary = "乘客下单")
    @PutMapping("/submitOrder")
    @LoginAuth
    public Result<Long> submitOrder(@RequestBody SubmitOrderForm submitOrderForm){
        Long customerId = AuthContextHolder.getUserId();
        submitOrderForm.setCustomerId(customerId);
        return Result.ok(orderService.submitOrder(submitOrderForm));
    }

    @Operation(summary = "查询订单状态")
    @GetMapping("/getOrderStatus/{orderId}")
    @LoginAuth
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }

    @Operation(summary = "乘客端查找当前订单")
    @GetMapping("/searchCustomerCurrentOrder")
    @LoginAuth
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.searchCustomerCurrentOrder(customerId));
    }

    @Operation(summary = "乘客端获取订单信息")
    @GetMapping("/getOrderInfo/{orderId}")
    @LoginAuth
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getOrderInfo(orderId, customerId));
    }

    @Operation(summary = "司乘同显: 乘客根据订单Id获取司机基本信息")
    @GetMapping("/getDriverInfo/{orderId}")
    @LoginAuth
    public Result<DriverInfoVo> getDriverInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getDriverInfo(orderId, customerId));
    }

    @Operation(summary = "司乘同显: 乘客获取订单经纬度信息")
    @GetMapping("/getCacheOrderLocation/{orderId}")
    @LoginAuth
    public Result<OrderLocationVo> getCacheOrderLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getCacheOrderLocation(orderId));
    }

    @Operation(summary = "计算最佳驾驶线路")
    @PostMapping("/calculateDrivingLine")
    @LoginAuth
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "代驾服务: 获取订单服务最后一个位置信息")
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    @LoginAuth
    public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId){
        return Result.ok(orderService.getOrderServiceLastLocation(orderId));
    }

    @Operation(summary = "获取乘客订单分页列表")
    @GetMapping("findCustomerOrderPage/{page}/{limit}")
    @LoginAuth
    public Result<PageVo> findCustomerOrderPage(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit) {
        Long customerId = AuthContextHolder.getUserId();
        PageVo pageVo = orderService.findCustomerOrderPage(customerId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "微信支付")
    @PostMapping("/createWxPayment")
    @LoginAuth
    public Result<WxPrepayVo> createWxPayment(@RequestBody CreateWxPaymentForm createWxPaymentForm) {
        Long customerId = AuthContextHolder.getUserId();
        createWxPaymentForm.setCustomerId(customerId);
        return Result.ok(orderService.createWxPayment(createWxPaymentForm));
    }

    @Operation(summary = "支付状态查询")
    @GetMapping("/queryPayStatus/{orderNo}")
    @LoginAuth
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo) {
        return Result.ok(orderService.queryPayStatus(orderNo));
    }


}