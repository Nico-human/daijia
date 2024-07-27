package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {

    /**
     * 远程调用
     * @param file 前端传入的文件
     * @param path 文件在tx云中的存储路径
     * @return CosUploadVo对象
     */
    CosUploadVo uploadFile(MultipartFile file, String path);
}
