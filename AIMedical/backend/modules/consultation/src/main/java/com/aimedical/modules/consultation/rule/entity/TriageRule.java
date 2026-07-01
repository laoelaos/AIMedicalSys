package com.aimedical.modules.consultation.rule.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "triage_rule")
public class TriageRule extends BaseEntity {

    private String ruleId;
    private String ruleSetId;
    private String ruleVersion;
    private String conditions;
    private String resultDepartmentId;
    private String resultDepartmentName;
    private float score;
    private Boolean enabled = true;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(String ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getResultDepartmentId() {
        return resultDepartmentId;
    }

    public void setResultDepartmentId(String resultDepartmentId) {
        this.resultDepartmentId = resultDepartmentId;
    }

    public String getResultDepartmentName() {
        return resultDepartmentName;
    }

    public void setResultDepartmentName(String resultDepartmentName) {
        this.resultDepartmentName = resultDepartmentName;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
