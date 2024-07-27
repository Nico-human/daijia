package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {

    /**
     * 上传文件到腾讯云存储桶中
     * 具体操作见: <a href="https://cloud.tencent.com/document/product/436/10199">...</a> SDK -> Java -> 快速入门
     * 返回封装了 Url(文件在腾讯云中的地址)和ShowUrl(临时显示地址) 的Vo对象
     */
    CosUploadVo upload(MultipartFile file, String path);

}
