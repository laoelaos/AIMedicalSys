package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/patient/consult")
public class ConsultController {

    private static final Logger log = LoggerFactory.getLogger(ConsultController.class);
    private static final AtomicBoolean faultMode = new AtomicBoolean(false);

    private final AuthService authService;

    public ConsultController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public Result<Map<String, Object>> ask(@RequestBody Map<String, Object> body) {
        authService.getCurrentUser();
        String question = (String) body.getOrDefault("question", "");
        if (question == null || question.isBlank()) {
            return Result.fail("PARAM_INVALID", "问题不能为空");
        }

        if (faultMode.get()) {
            return Result.fail("QA_AI_UNAVAILABLE", "暂不可用，请稍后重试");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", generateAnswer(question));
        response.put("related_questions", generateRelatedQuestions(question));
        response.put("disclaimer_required", true);
        response.put("session_id", "consult-" + UUID.randomUUID().toString().substring(0, 8));
        return Result.success(response);
    }

    @PostMapping("/mock-fault")
    public Result<Map<String, Boolean>> toggleFault() {
        boolean newState = !faultMode.get();
        faultMode.set(newState);
        log.info("Consult mock-fault toggled: {}", newState);
        return Result.success(Map.of("fault", newState));
    }

    private String generateAnswer(String question) {
        String q = question.toLowerCase();
        if (q.contains("头痛") || q.contains("头疼")) {
            return "根据您的描述，头痛可能由多种原因引起，如偏头痛、紧张性头痛、颈椎问题或高血压等。建议您记录头痛发作的频率和伴随症状，保持规律作息，避免过度劳累。如果头痛频繁或加重，请及时就医进行进一步检查。";
        }
        if (q.contains("发烧") || q.contains("发热") || q.contains("体温")) {
            return "发热是身体对抗感染的正常反应。体温38.5°C以下可先物理降温（温水擦浴、多饮水），超过38.5°C可考虑使用退烧药（如对乙酰氨基酚）。如果持续发热超过3天或伴有呼吸困难、意识模糊等症状，请立即就医。";
        }
        if (q.contains("咳嗽")) {
            return "咳嗽是呼吸道常见的防御反应。干咳可能由过敏、刺激引起，湿咳（有痰）多与感染有关。建议多饮温水，保持室内空气湿润，避免刺激性气体。如果咳嗽持续超过2周或伴有胸闷气短，建议到医院呼吸内科就诊。";
        }
        if (q.contains("胃") || q.contains("腹") || q.contains("肚")) {
            return "腹痛或胃部不适可能由饮食不当、胃炎、消化不良等引起。建议暂时进食清淡易消化的食物，避免辛辣刺激和生冷食物。如果疼痛剧烈或持续不缓解，请尽快就医排查是否存在更严重的问题。";
        }
        return "感谢您的提问。为了更好地为您分析，建议您补充以下信息：症状的持续时间、具体部位、有无伴随症状（如发热、恶心等），以及既往是否有相关病史。如有紧急情况请及时就医。";
    }

    private List<String> generateRelatedQuestions(String question) {
        String q = question.toLowerCase();
        if (q.contains("头痛") || q.contains("头疼")) {
            return List.of("头痛时是否伴有恶心或视力模糊？", "如何区分偏头痛和紧张性头痛？", "什么情况下头痛需要立即就医？");
        }
        if (q.contains("发烧") || q.contains("发热")) {
            return List.of("发烧多少度该吃退烧药？", "儿童发烧和成人发烧的处理方法一样吗？", "反复发烧是什么原因？");
        }
        return List.of("需要补充哪些信息有助于判断病情？", "什么情况下应该去医院而不是自行处理？", "如何预防这类症状的再次发生？");
    }
}
