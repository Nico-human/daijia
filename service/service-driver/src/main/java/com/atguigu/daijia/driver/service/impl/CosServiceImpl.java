package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Autowired
    private TencentCloudProperties tencentCloudProperties;

    @Override
    public CosUploadVo upload(MultipartFile file, String path) {

        //获取COSClient对象
        COSClient cosClient = this.getCosClient();

        //元数据信息
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentEncoding("UTF-8");
        metadata.setContentType(file.getContentType());

        //上传路径
        String originalFilename = file.getOriginalFilename();
        String fileType = Objects.requireNonNull(originalFilename).substring(originalFilename.lastIndexOf("."));
        String uploadPath = "/driver/" + path + "/" +
                UUID.randomUUID().toString().replaceAll("-", "") + fileType;

        //上传文件
        PutObjectRequest putObjectRequest;
        try {
            putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(),
                                                    uploadPath,
                                                    file.getInputStream(),
                                                    metadata);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        putObjectRequest.setStorageClass(StorageClass.Standard);
        cosClient.putObject(putObjectRequest);
        cosClient.shutdown();

        //返回Vo对象
        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);
        cosUploadVo.setShowUrl(this.getImageUrl(uploadPath)); // 图片临时访问url, 回显使用
        return cosUploadVo;
    }

    private COSClient getCosClient() {
        String secretId = tencentCloudProperties.getSecretId();
        String secretKey = tencentCloudProperties.getSecretKey();
        BasicCOSCredentials cred = new BasicCOSCredentials(secretId, secretKey);

        Region region = new Region(tencentCloudProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(cred, clientConfig);
    }

    private String getImageUrl(String path) {

        if(!StringUtils.hasText(path)){
            return "";
        }

        COSClient cosClient = this.getCosClient();
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(tencentCloudProperties.getBucketPrivate(),
                        path, HttpMethodName.GET);

        Date date = new DateTime().plusMinutes(15).toDate();
        request.setExpiration(date); //设置过期时间: 👆 15分钟

        URL url = cosClient.generatePresignedUrl(request);
        cosClient.shutdown();
        return url.toString();
    }
}
