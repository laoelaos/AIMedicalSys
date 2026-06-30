package com.aimedical.modules.ai.api.dto.examination;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExaminationDtoTest {

    @Test
    void shouldCreateResponseWithDefaultConstructor() {
        ExaminationRecommendResponse response = new ExaminationRecommendResponse();
        assertNull(response.getItems());
    }

    @Test
    void shouldSetAndGetItems() {
        ExaminationRecommendResponse response = new ExaminationRecommendResponse();
        List<ExaminationRecommendResponse.ExaminationItem> items = new ArrayList<>();
        items.add(new ExaminationRecommendResponse.ExaminationItem());
        response.setItems(items);
        assertEquals(1, response.getItems().size());
    }

    @Test
    void shouldCreateExaminationItemWithDefaultConstructor() {
        ExaminationRecommendResponse.ExaminationItem item =
                new ExaminationRecommendResponse.ExaminationItem();
        assertNull(item.getName());
        assertNull(item.getCategory());
        assertNull(item.getReason());
    }

    @Test
    void shouldCreateExaminationItemWithAllArgsConstructor() {
        ExaminationRecommendResponse.ExaminationItem item =
                new ExaminationRecommendResponse.ExaminationItem(
                        "血常规", "检验", "排查感染");

        assertEquals("血常规", item.getName());
        assertEquals("检验", item.getCategory());
        assertEquals("排查感染", item.getReason());
    }

    @Test
    void shouldSetAndGetAllExaminationItemFields() {
        ExaminationRecommendResponse.ExaminationItem item =
                new ExaminationRecommendResponse.ExaminationItem();
        item.setName("心电图");
        item.setCategory("检查");
        item.setReason("排查心脏异常");

        assertEquals("心电图", item.getName());
        assertEquals("检查", item.getCategory());
        assertEquals("排查心脏异常", item.getReason());
    }

    @Test
    void shouldBuildFullExaminationRecommendResponse() {
        ExaminationRecommendResponse.ExaminationItem item1 =
                new ExaminationRecommendResponse.ExaminationItem(
                        "血常规", "检验", "排查感染");
        ExaminationRecommendResponse.ExaminationItem item2 =
                new ExaminationRecommendResponse.ExaminationItem();
        item2.setName("胸部X光");
        item2.setCategory("影像");
        item2.setReason("排查肺部病变");

        List<ExaminationRecommendResponse.ExaminationItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        ExaminationRecommendResponse response = new ExaminationRecommendResponse();
        response.setItems(items);

        assertEquals(2, response.getItems().size());
        assertEquals("血常规", response.getItems().get(0).getName());
        assertEquals("胸部X光", response.getItems().get(1).getName());
    }
}
