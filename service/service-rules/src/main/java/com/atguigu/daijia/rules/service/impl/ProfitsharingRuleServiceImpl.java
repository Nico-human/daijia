package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequest;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponse;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.rules.config.DroolsHelper;
import com.atguigu.daijia.rules.service.ProfitsharingRuleService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProfitsharingRuleServiceImpl implements ProfitsharingRuleService {

    @Override
    public ProfitsharingRuleResponseVo calculateOrderProfitsharingFee(ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        // 封装输入, 返回 对象
        ProfitsharingRuleRequest profitsharingRuleRequest = new ProfitsharingRuleRequest();
        BeanUtils.copyProperties(profitsharingRuleRequestForm, profitsharingRuleRequest);
        ProfitsharingRuleResponse profitsharingRuleResponse = new ProfitsharingRuleResponse();

        KieSession kieSession = DroolsHelper.loadForRule(DroolsHelper.Profit_Sharing_RULES_DRL);
        kieSession.setGlobal("profitsharingRuleResponse", profitsharingRuleResponse);
        kieSession.insert(profitsharingRuleRequest);
        kieSession.fireAllRules();
        kieSession.dispose();

        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = new ProfitsharingRuleResponseVo();
        BeanUtils.copyProperties(profitsharingRuleResponse, profitsharingRuleResponseVo);

        return profitsharingRuleResponseVo;
    }
}

