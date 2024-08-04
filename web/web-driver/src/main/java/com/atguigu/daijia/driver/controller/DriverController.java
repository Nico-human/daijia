package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.annotation.Log;
import com.atguigu.daijia.common.login.LoginAuth;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable String code) {
        String token = driverService.login(code);
        return Result.ok(token);
    }

    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo")
    @LoginAuth
    public Result<DriverLoginVo> getDriverInfo() {
        Long driverId = AuthContextHolder.getUserId();
        DriverLoginVo driverLoginVo = driverService.getDriverLoginInfo(driverId);
        return Result.ok(driverLoginVo);
    }

    @Operation(summary = "获取司机认证信息")
    @GetMapping("/getDriverAuthInfo")
    @LoginAuth
    public Result<DriverAuthInfoVo> getDriverAuthInfo() {
        Long driverId = AuthContextHolder.getUserId();
        DriverAuthInfoVo driverAuthInfoVo = driverService.getDriverAuthInfo(driverId);
        return Result.ok(driverAuthInfoVo);
    }

    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    @LoginAuth
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm){
        Long driverId = AuthContextHolder.getUserId();
        updateDriverAuthInfoForm.setDriverId(driverId);
        return Result.ok(driverService.updateDriverAuthInfo(updateDriverAuthInfoForm));
    }

    @Operation(summary = "创建司机人脸模型")
    @PostMapping("/creatDriverFaceModel")
    @LoginAuth
    public Result<Boolean> createDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        Long driverId = AuthContextHolder.getUserId();
        driverFaceModelForm.setDriverId(driverId);
        return Result.ok(driverService.createDriverFaceModel(driverFaceModelForm));
    }

    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @GetMapping("/isFaceRecognition")
    @LoginAuth
    public Result<Boolean> isFaceRecognition() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @PostMapping("/verifyDriverFace")
    @LoginAuth
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.verifyDriverFace(driverFaceModelForm));
    }

    @Operation(summary = "司机开启接单服务")
    @GetMapping("/startService")
    @LoginAuth
    public Result<Boolean> startService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.startService(driverId));
    }

    @Operation(summary = "司机停止接单服务")
    @GetMapping("/stopService")
    @LoginAuth
    public Result<Boolean> stopService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.stopService(driverId));
    }

}

