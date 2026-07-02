package com.aimedical.modules.ai.api.dto.examination;

import java.util.List;

/**
 * AI 检查推荐响应 DTO。
 *
 * <p>承载 AI 推荐的检查检验项目列表。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class ExaminationRecommendResponse {

    private List<ExaminationItem> items;

    public ExaminationRecommendResponse() {
    }

    public List<ExaminationItem> getItems() {
        return items;
    }

    public void setItems(List<ExaminationItem> items) {
        this.items = items;
    }

    /**
     * 检查推荐项。
     */
    public static class ExaminationItem {

        private String name;
        private String category;
        private String reason;

        public ExaminationItem() {
        }

        public ExaminationItem(String name, String category, String reason) {
            this.name = name;
            this.category = category;
            this.reason = reason;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
