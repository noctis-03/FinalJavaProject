package com.blople.model.block;

import com.blople.model.enums.BlockType;

/**
 * 자동화 블록 (FR-13). 실제 동작은 ruleId 가 가리키는 AutomationRule 에 위임.
 */
public class AutomationBlock extends Block {

    private String ruleId;

    public AutomationBlock(String name, String color, String ruleId) {
        super(BlockType.AUTOMATION, name, color);
        this.ruleId = ruleId;
    }

    public String getRuleId()                  { return ruleId; }
    public void setRuleId(String ruleId)       { this.ruleId = ruleId; touch(); }
}
