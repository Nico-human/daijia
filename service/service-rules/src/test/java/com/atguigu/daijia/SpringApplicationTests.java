package com.atguigu.daijia;

import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

/**
 * @Description:
 * @Author: dong
 * @Date: 2024/7/28
 */
@SpringBootTest
public class SpringApplicationTests {

    @Autowired
    private KieContainer kieContainer;

    @Test
    void test() {

        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setStartTime("01:45:00");
        feeRuleRequest.setDistance(new BigDecimal("15.0"));
        feeRuleRequest.setWaitMinute(20);
        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();

        KieSession kieSession = kieContainer.newKieSession();
        kieSession.setGlobal("feeRuleResponse", feeRuleResponse);
        kieSession.insert(feeRuleRequest);
        kieSession.fireAllRules();
        kieSession.dispose();

        System.out.println("后: " + JSON.toJSONString(feeRuleResponse));
    }


}
