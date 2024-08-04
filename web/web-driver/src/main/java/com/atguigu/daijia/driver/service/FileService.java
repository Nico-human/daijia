package com.atguigu.daijia.driver.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 上传文件到Minio
     * @param multipartFile
     * @return
     */
    String uploadMinio(MultipartFile multipartFile);
}
