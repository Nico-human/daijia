package com.atguigu.daijia.rules.service;

import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;

public interface RewardRuleService {

    /**
     * 计算订单奖励费用(司机端)
     * @param rewardRuleRequestForm
     * @return
     */
    RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm);
}
