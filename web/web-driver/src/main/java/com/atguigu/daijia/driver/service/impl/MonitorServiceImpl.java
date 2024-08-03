package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.CiFeignClient;
import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import com.atguigu.daijia.model.vo.order.TextAuditingVo;
import com.atguigu.daijia.order.client.OrderMonitorFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorServiceImpl implements MonitorService {

    @Autowired
    private FileService fileService;
    @Autowired
    private OrderMonitorFeignClient orderMonitorFeignClient;
    @Autowired
    private CiFeignClient ciFeignClient;

    @Override
    public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) {
        // 将录音文件存入Minio
        String url = fileService.uploadMinio(file);

        OrderMonitorRecord orderMonitorRecord = new OrderMonitorRecord();
        orderMonitorRecord.setOrderId(orderMonitorForm.getOrderId());
        orderMonitorRecord.setFileUrl(url);
        orderMonitorRecord.setContent(orderMonitorForm.getContent());

        //文本审核
        Result<TextAuditingVo> textAuditingVoResult = ciFeignClient.textAuditing(orderMonitorRecord.getContent());
        if (textAuditingVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        TextAuditingVo textAuditingVo = textAuditingVoResult.getData();
        orderMonitorRecord.setResult(textAuditingVo.getResult());
        orderMonitorRecord.setKeywords(textAuditingVo.getKeywords());

        //将文本文件存入MongoDB
        Result<Boolean> booleanResult = orderMonitorFeignClient.saveOrderMonitorRecord(orderMonitorRecord);
        if (booleanResult.getCode() != 200){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return booleanResult.getData();
    }
}
