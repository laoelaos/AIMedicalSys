package com.aimedical.modules.consultation.repository;

import com.aimedical.modules.consultation.rule.entity.TriageRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriageRuleRepository extends JpaRepository<TriageRule, Long> {

    List<TriageRule> findByRuleSetIdAndRuleVersion(String ruleSetId, String ruleVersion);

    List<TriageRule> findByEnabledTrue();
}
