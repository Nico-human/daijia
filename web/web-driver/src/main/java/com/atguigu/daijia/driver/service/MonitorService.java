package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import org.springframework.web.multipart.MultipartFile;

public interface MonitorService {

    /**
     * 将录音文件file存入Minio中, 对文本内容进行审核, 将文本文件存入MongoDB中
     * @param file 录音文件
     * @param orderMonitorForm 文本(orderMonitorForm.content)文件(由前端传入, 通过语音转文本得到)
     * @return
     */
    Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm);
}
