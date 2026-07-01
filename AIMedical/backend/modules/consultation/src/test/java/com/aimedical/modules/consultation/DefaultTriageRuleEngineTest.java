package com.aimedical.modules.consultation;

import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import com.aimedical.modules.consultation.repository.TriageRuleRepository;
import com.aimedical.modules.consultation.rule.DefaultTriageRuleEngine;
import com.aimedical.modules.consultation.rule.MatchResult;
import com.aimedical.modules.consultation.rule.entity.TriageRule;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultTriageRuleEngineTest {

    @Test
    void shouldReturnEmptyResultWhenNoRulesMatch() {
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(new ArrayList<>()));
        MatchResult mr = engine.match("头痛", null, null);
        List<RecommendedDepartment> result = mr.getDepartments();
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFilterRulesByRuleVersion() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));
        rules.add(rule("R2", "RS001", "v2", "dept-02", "外科", true));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", "v1", null);
        List<RecommendedDepartment> result = mr.getDepartments();
        assertEquals(1, result.size());
        assertEquals("dept-01", result.get(0).getDepartmentId());
    }

    @Test
    void shouldFilterRulesByRuleSetId() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));
        rules.add(rule("R2", "RS002", "v1", "dept-02", "外科", true));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", null, "RS002");
        List<RecommendedDepartment> result = mr.getDepartments();
        assertEquals(1, result.size());
        assertEquals("dept-02", result.get(0).getDepartmentId());
    }

    @Test
    void shouldFilterOutDisabledRules() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", false));
        rules.add(rule("R2", "RS001", "v1", "dept-02", "外科", true));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", "v1", "RS001");
        List<RecommendedDepartment> result = mr.getDepartments();
        assertEquals(1, result.size());
        assertEquals("dept-02", result.get(0).getDepartmentId());
    }

    @Test
    void shouldReturnLatestRuleVersion() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));
        rules.add(rule("R2", "RS001", "v2", "dept-02", "外科", true));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        assertEquals("v2", engine.currentRuleVersion());
    }

    @Test
    void shouldReturnLatestForRuleVersionWhenNoRules() {
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(new ArrayList<>()));
        assertEquals("latest", engine.currentRuleVersion());
    }

    @Test
    void shouldReturnDefaultForRuleSetIdWhenNoRules() {
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(new ArrayList<>()));
        assertEquals("default", engine.currentRuleSetId());
    }

    @Test
    void shouldFallbackWhenVersionFilterEmpty() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", "v2", null);
        assertFalse(mr.getDepartments().isEmpty());
        assertTrue(mr.isRuleVersionMismatch());
    }

    @Test
    void shouldNotFallbackWhenVersionMatches() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", "v1", null);
        assertFalse(mr.getDepartments().isEmpty());
        assertFalse(mr.isRuleVersionMismatch());
    }

    @Test
    void shouldFallbackWhenSetIdFilterEmpty() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", null, "RS999");
        assertFalse(mr.getDepartments().isEmpty());
        assertTrue(mr.isRuleVersionMismatch());
    }

    @Test
    void shouldNotFallbackWhenBothVersionAndSetIdAreNull() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", null, null);
        assertFalse(mr.getDepartments().isEmpty());
        assertFalse(mr.isRuleVersionMismatch());
    }

    @Test
    void shouldMatchWithAndLogicWhenAllKeywordsPresent() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions("{\"keywords\": [\"胸痛\", \"胸闷\"], \"logic\": \"AND\"}");
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("胸痛伴随胸闷", null, null);
        assertEquals(1, mr.getDepartments().size());
    }

    @Test
    void shouldNotMatchWithAndLogicWhenKeywordMissing() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions("{\"keywords\": [\"胸痛\", \"胸闷\"], \"logic\": \"AND\"}");
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("只有胸痛", null, null);
        assertTrue(mr.getDepartments().isEmpty());
    }

    @Test
    void shouldMatchWithOrLogicWhenAnyKeywordPresent() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions("{\"keywords\": [\"胸痛\", \"胸闷\"], \"logic\": \"OR\"}");
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("胸痛", null, null);
        assertEquals(1, mr.getDepartments().size());
    }

    @Test
    void shouldNotMatchWithOrLogicWhenNoKeywordPresent() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions("{\"keywords\": [\"胸痛\", \"胸闷\"], \"logic\": \"OR\"}");
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", null, null);
        assertTrue(mr.getDepartments().isEmpty());
    }

    @Test
    void shouldDefaultToAndLogicWhenLogicFieldMissing() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions("{\"keywords\": [\"胸痛\", \"胸闷\"]}");
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("胸痛伴随胸闷", null, null);
        assertEquals(1, mr.getDepartments().size());
    }

    @Test
    void shouldMatchCaseInsensitively() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions("{\"keywords\": [\"chest pain\"], \"logic\": \"OR\"}");
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("CHEST PAIN", null, null);
        assertEquals(1, mr.getDepartments().size());
    }

    @Test
    void shouldPassRuleWhenConditionsNull() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions(null);
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("任何症状", null, null);
        assertEquals(1, mr.getDepartments().size());
    }

    @Test
    void shouldPassRuleWhenConditionsEmpty() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions("");
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("任何症状", null, null);
        assertEquals(1, mr.getDepartments().size());
    }

    @Test
    void shouldPassRuleWhenConditionsInvalidJson() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r.setConditions("not valid json");
        rules.add(r);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("任何症状", null, null);
        assertTrue(mr.getDepartments().isEmpty());
    }

    @Test
    void shouldSortMatchedRulesByScoreDescending() {
        List<TriageRule> rules = new ArrayList<>();
        TriageRule r1 = rule("R1", "RS001", "v1", "dept-01", "内科", true);
        r1.setScore(0.3f);
        TriageRule r2 = rule("R2", "RS001", "v1", "dept-02", "外科", true);
        r2.setScore(0.9f);
        TriageRule r3 = rule("R3", "RS001", "v1", "dept-03", "儿科", true);
        r3.setScore(0.6f);
        rules.add(r1);
        rules.add(r2);
        rules.add(r3);
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));

        MatchResult mr = engine.match("头痛", null, null);
        assertEquals(3, mr.getDepartments().size());
        assertEquals("dept-02", mr.getDepartments().get(0).getDepartmentId());
        assertEquals("dept-03", mr.getDepartments().get(1).getDepartmentId());
        assertEquals("dept-01", mr.getDepartments().get(2).getDepartmentId());
    }

    @Test
    void shouldLogWarningWhenRuleVersionMismatch() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));

        Logger engineLogger = (Logger) LoggerFactory.getLogger(DefaultTriageRuleEngine.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        engineLogger.addAppender(appender);

        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(rules));
        engine.match("头痛", "v2", null);

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("Rule version mismatch"));
        assertTrue(event.getFormattedMessage().contains("v2"));

        engineLogger.detachAppender(appender);
    }

    @Test
    void shouldExpireCacheAfterWriteDuration() {
        List<TriageRule> rules = new ArrayList<>();
        rules.add(rule("R1", "RS001", "v1", "dept-01", "内科", true));
        MockTicker ticker = new MockTicker();
        TriageRuleRepository repo = spy(stubRepo(rules));
        DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(repo, ticker);

        engine.match("a", null, null);
        engine.match("a", null, null);

        ticker.advance(31_000_000_000L);
        engine.match("a", null, null);

        verify(repo, times(2)).findByEnabledTrue();
    }

    private static class MockTicker implements Ticker {
        private long nanos = 0;
        @Override
        public long read() { return nanos; }
        void advance(long nanos) { this.nanos += nanos; }
    }

    private static TriageRule rule(String ruleId, String ruleSetId, String version,
                                    String deptId, String deptName, boolean enabled) {
        TriageRule rule = new TriageRule();
        rule.setRuleId(ruleId);
        rule.setRuleSetId(ruleSetId);
        rule.setRuleVersion(version);
        rule.setResultDepartmentId(deptId);
        rule.setResultDepartmentName(deptName);
        rule.setEnabled(enabled);
        rule.setScore(0.5f);
        return rule;
    }

    private static TriageRuleRepository stubRepo(List<TriageRule> rules) {
        return new TriageRuleRepository() {
            @Override
            public List<TriageRule> findByRuleSetIdAndRuleVersion(String ruleSetId, String ruleVersion) {
                return rules;
            }

            @Override
            public List<TriageRule> findByEnabledTrue() {
                List<TriageRule> enabled = new ArrayList<>();
                for (TriageRule r : rules) {
                    if (Boolean.TRUE.equals(r.getEnabled())) {
                        enabled.add(r);
                    }
                }
                return enabled;
            }

            @Override
            public List<TriageRule> findAll() { return rules; }

            @Override
            public List<TriageRule> findAll(Sort sort) { return rules; }

            @Override
            public List<TriageRule> findAllById(Iterable<Long> longs) { return rules; }

            @Override
            public <S extends TriageRule> List<S> saveAll(Iterable<S> entities) { return null; }

            @Override
            public void flush() {}

            @Override
            public <S extends TriageRule> S saveAndFlush(S entity) { return entity; }

            @Override
            public <S extends TriageRule> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }

            @Override
            public void deleteAllInBatch(Iterable<TriageRule> entities) {}

            @Override
            public void deleteAllByIdInBatch(Iterable<Long> longs) {}

            @Override
            public void deleteAllInBatch() {}

            @Override
            public TriageRule getOne(Long aLong) { return null; }

            @Override
            public TriageRule getById(Long aLong) { return null; }

            @Override
            public TriageRule getReferenceById(Long aLong) { return null; }

            @Override
            public <S extends TriageRule> Optional<S> findOne(Example<S> example) { return Optional.empty(); }

            @Override
            public <S extends TriageRule> List<S> findAll(Example<S> example) { return null; }

            @Override
            public <S extends TriageRule> List<S> findAll(Example<S> example, Sort sort) { return null; }

            @Override
            public <S extends TriageRule> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }

            @Override
            public <S extends TriageRule> long count(Example<S> example) { return 0; }

            @Override
            public <S extends TriageRule> boolean exists(Example<S> example) { return false; }

            @Override
            public <S extends TriageRule, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }

            @Override
            public TriageRule save(TriageRule entity) { return entity; }

            @Override
            public Optional<TriageRule> findById(Long aLong) { return Optional.empty(); }

            @Override
            public boolean existsById(Long aLong) { return false; }

            @Override
            public long count() { return rules.size(); }

            @Override
            public void deleteById(Long aLong) {}

            @Override
            public void delete(TriageRule entity) {}

            @Override
            public void deleteAllById(Iterable<? extends Long> longs) {}

            @Override
            public void deleteAll(Iterable<? extends TriageRule> entities) {}

            @Override
            public void deleteAll() {}

            @Override
            public Page<TriageRule> findAll(Pageable pageable) { return null; }
        };
    }
}
