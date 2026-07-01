package com.aimedical.modules.consultation.rule;

import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import com.aimedical.modules.consultation.rule.entity.TriageRule;
import com.aimedical.modules.consultation.repository.TriageRuleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class DefaultTriageRuleEngine implements TriageRuleEngine {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(DefaultTriageRuleEngine.class);

    private final TriageRuleRepository triageRuleRepository;
    private final LoadingCache<String, List<TriageRule>> ruleCache;

    public DefaultTriageRuleEngine(TriageRuleRepository triageRuleRepository) {
        this(triageRuleRepository, Ticker.systemTicker());
    }

    public DefaultTriageRuleEngine(TriageRuleRepository triageRuleRepository, Ticker ticker) {
        this.triageRuleRepository = triageRuleRepository;
        this.ruleCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .ticker(ticker)
                .build(new CacheLoader<String, List<TriageRule>>() {
                    @Override
                    public List<TriageRule> load(String key) {
                        return triageRuleRepository.findByEnabledTrue();
                    }
                });
    }

    @Override
    public MatchResult match(String chiefComplaint, String ruleVersion, String ruleSetId) {
        List<TriageRule> rules = ruleCache.get("all_enabled");

        String version = ruleVersion;
        String setId = ruleSetId;

        List<TriageRule> versionFiltered = rules.stream()
                .filter(r -> (version == null || version.equals(r.getRuleVersion()))
                        && (setId == null || setId.equals(r.getRuleSetId()))
                        && Boolean.TRUE.equals(r.getEnabled()))
                .collect(Collectors.toList());

        boolean ruleVersionMismatch = false;

        if (versionFiltered.isEmpty() && (version != null || setId != null)) {
            versionFiltered = rules.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                    .collect(Collectors.toList());
            ruleVersionMismatch = true;
            log.warn("Rule version mismatch, falling back to all enabled rules. requested version={}, setId={}", version, setId);
        }

        List<TriageRule> matched = new ArrayList<>();
        for (TriageRule rule : versionFiltered) {
            if (matchesConditions(chiefComplaint, rule.getConditions())) {
                matched.add(rule);
            }
        }

        matched.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        List<RecommendedDepartment> departments = new ArrayList<>();
        for (TriageRule rule : matched) {
            departments.add(new RecommendedDepartment(
                    rule.getResultDepartmentId(),
                    rule.getResultDepartmentName(),
                    rule.getScore()));
        }

        return new MatchResult(departments, ruleVersionMismatch);
    }

    private boolean matchesConditions(String chiefComplaint, String conditionsJson) {
        if (conditionsJson == null || conditionsJson.trim().isEmpty()) {
            return true;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(conditionsJson);
            JsonNode keywordsNode = node.get("keywords");
            if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.isEmpty()) {
                return true;
            }
            List<String> keywords = new ArrayList<>();
            for (JsonNode kw : keywordsNode) {
                keywords.add(kw.asText());
            }
            String logic = node.has("logic") ? node.get("logic").asText("AND") : "AND";
            String complaint = chiefComplaint.toLowerCase();
            if ("OR".equalsIgnoreCase(logic)) {
                for (String keyword : keywords) {
                    if (complaint.contains(keyword.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            } else {
                for (String keyword : keywords) {
                    if (!complaint.contains(keyword.toLowerCase())) {
                        return false;
                    }
                }
                return true;
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse conditions JSON for rule, skipping: {}", conditionsJson, e);
            return false;
        }
    }

    @Override
    public String currentRuleVersion() {
        List<TriageRule> rules = ruleCache.get("all_enabled");
        if (rules == null || rules.isEmpty()) {
            return "latest";
        }
        return rules.stream()
                .map(TriageRule::getRuleVersion)
                .filter(v -> v != null)
                .distinct()
                .sorted(Collections.reverseOrder())
                .findFirst()
                .orElse("latest");
    }

    @Override
    public String currentRuleSetId() {
        List<TriageRule> rules = ruleCache.get("all_enabled");
        if (rules == null || rules.isEmpty()) {
            return "default";
        }
        return rules.stream()
                .map(TriageRule::getRuleSetId)
                .filter(s -> s != null)
                .distinct()
                .findFirst()
                .orElse("default");
    }
}
