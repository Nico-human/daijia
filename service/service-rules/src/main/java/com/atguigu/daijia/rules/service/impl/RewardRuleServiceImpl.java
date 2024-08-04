package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.form.rules.RewardRuleRequest;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponse;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.rules.config.DroolsHelper;
import com.atguigu.daijia.rules.mapper.RewardRuleMapper;
import com.atguigu.daijia.rules.service.RewardRuleService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RewardRuleServiceImpl implements RewardRuleService {

    @Override
    public RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm) {

        //封装传入对象
        RewardRuleRequest rewardRuleRequest = new RewardRuleRequest();
        Date startTime = rewardRuleRequestForm.getStartTime();
        rewardRuleRequest.setStartTime(new DateTime(startTime).toString("HH:mm:ss"));
        rewardRuleRequest.setOrderNum(rewardRuleRequestForm.getOrderNum());
        //封装返回对象
        RewardRuleResponse rewardRuleResponse = new RewardRuleResponse();

        KieSession kieSession = DroolsHelper.loadForRule(DroolsHelper.Reward_RULES_DRL);
        kieSession.setGlobal("rewardRuleResponse", rewardRuleResponse);
        kieSession.insert(rewardRuleRequest);
        kieSession.fireAllRules();
        kieSession.dispose();

        RewardRuleResponseVo rewardRuleResponseVo = new RewardRuleResponseVo();
        rewardRuleResponseVo.setRewardAmount(rewardRuleResponse.getRewardAmount());
        return rewardRuleResponseVo;
    }
}
