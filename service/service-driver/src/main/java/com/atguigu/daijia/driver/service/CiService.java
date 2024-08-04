package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.order.TextAuditingVo;

public interface CiService {

    /**
     * 图片审核
     * @param path
     * @return
     */
    Boolean imageAuditing(String path);

    /**
     * 文本审核
     * @param content
     * @return
     */
    TextAuditingVo textAuditing(String content);
}
