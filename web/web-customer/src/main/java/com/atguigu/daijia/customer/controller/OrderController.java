package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.LoginAuth;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import io.swagger.v3.oas.annotations.Operation;
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

    //TODO 后续完善, 目前假设乘客当前没有订单
    @Operation(summary = "查找乘客端当前订单")
    @GetMapping("/searchCustomerCurrentOrder")
    @LoginAuth
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(){
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        currentOrderInfoVo.setIsHasCurrentOrder(false);
        return Result.ok(currentOrderInfoVo);
    }

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

}

