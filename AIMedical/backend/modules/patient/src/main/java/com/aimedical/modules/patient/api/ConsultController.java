package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/patient/consult")
public class ConsultController {

    private final AuthService authService;

    public ConsultController(AuthService authService) {
        this.authService = authService;
    }

    public static class ConsultRequest {
        @NotBlank
        private String question;
        private String sessionId;

        public String getQuestion() { return question; }
        public void setQuestion(String v) { this.question = v; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String v) { this.sessionId = v; }
    }

    public static class ConsultResponse {
        private String answer;
        private List<String> relatedQuestions;
        private boolean disclaimerRequired;
        private String sessionId;
        private boolean isMock;

        public String getAnswer() { return answer; }
        public void setAnswer(String v) { this.answer = v; }
        public List<String> getRelatedQuestions() { return relatedQuestions; }
        public void setRelatedQuestions(List<String> v) { this.relatedQuestions = v; }
        public boolean isDisclaimerRequired() { return disclaimerRequired; }
        public void setDisclaimerRequired(boolean v) { this.disclaimerRequired = v; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String v) { this.sessionId = v; }
        public boolean isMock() { return isMock; }
        public void setMock(boolean v) { this.isMock = v; }
    }

    @PostMapping
    public Result<ConsultResponse> ask(@Valid @RequestBody ConsultRequest request) {
        authService.getCurrentUser();

        ConsultResponse response = new ConsultResponse();
        response.setAnswer(generateSafeResponse(request.getQuestion()));
        response.setRelatedQuestions(generateSafeRelatedQuestions(request.getQuestion()));
        response.setDisclaimerRequired(true);
        response.setSessionId("consult-" + UUID.randomUUID().toString().substring(0, 8));
        response.setMock(true);
        return Result.success(response);
    }

    private String generateSafeResponse(String question) {
        return "感谢您的提问。" + getContextualGuidance(question)
                + "需要说明的是，当前为 Mock 模式回复（is_mock=true），最终诊断请以医院医生面诊为准。如有紧急情况请立即就医。";
    }

    private List<String> generateSafeRelatedQuestions(String question) {
        String q = question != null ? question.toLowerCase() : "";
        List<String> base = new ArrayList<>();
        if (q.contains("头痛") || q.contains("头疼")) {
            base.add("头痛时是否伴有恶心或视力模糊？");
        }
        base.add("需要补充哪些信息有助于判断病情？");
        base.add("什么情况下应该去医院而不是自行处理？");
        return base;
    }

    private String getContextualGuidance(String question) {
        if (question == null) return "建议您在医生指导下进行专业诊疗。";
        String q = question.toLowerCase();
        if (q.contains("头痛") || q.contains("头疼")) return "头痛症状可能涉及神经内科、眼科等多个科室，建议在专业医生指导下进行鉴别。";
        if (q.contains("发烧") || q.contains("发热")) return "发热可能由多种原因引起，建议结合体温监测和伴随症状综合评估。";
        if (q.contains("咳嗽")) return "咳嗽原因多样，建议根据持续时间、痰液性状等综合判断。";
        if (q.contains("胃") || q.contains("腹") || q.contains("肚")) return "腹痛可能涉及消化系统多个器官，建议在医生指导下排查。";
        return "建议您补充症状详情（持续时间、部位、伴随症状等），以便更好为您分析。";
    }
}
