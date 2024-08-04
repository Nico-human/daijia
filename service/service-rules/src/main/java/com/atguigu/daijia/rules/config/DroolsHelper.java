package com.atguigu.daijia.rules.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

/**
 * @Description: 规则引擎工具类
 * @Author: dong
 * @Date: 2024/8/4
 */
public class DroolsHelper {

    public static final String RULES_CUSTOMER_RULES_DRL = "rules/FeeRule.drl";
    public static final String Profit_Sharing_RULES_DRL = "rules/ProfitSharingRule.drl";
    public static final String Reward_RULES_DRL = "rules/RewardRule.drl";

    public static KieSession loadForRule(String drlStr) {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource(drlStr));
        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        kb.buildAll();

        KieModule kieModule = kb.getKieModule();
        KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        return kieContainer.newKieSession();
    }

}
