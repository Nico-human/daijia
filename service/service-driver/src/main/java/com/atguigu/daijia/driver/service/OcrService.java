package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {


    /**
     * 根据上传的文件(身份证图片), 调用腾讯云相关接口完成认证, 并返回验证信息
     */
    IdCardOcrVo idCardOcr(MultipartFile file);

    /**
     * 根据上传的文件(驾驶证图片), 调用腾讯云相关接口完成认证, 并返回验证信息
     * @param file
     * @return
     */
    DriverLicenseOcrVo driverLicenseOcr(MultipartFile file);
}
