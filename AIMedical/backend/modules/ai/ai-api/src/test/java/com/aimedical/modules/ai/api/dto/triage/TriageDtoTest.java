package com.aimedical.modules.ai.api.dto.triage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriageDtoTest {

    @Test
    void shouldCreateTriageRequestWithDefaultConstructor() {
        TriageRequest request = new TriageRequest();
        assertNull(request.getChiefComplaint());
    }

    @Test
    void shouldSetAndGetChiefComplaint() {
        TriageRequest request = new TriageRequest();
        request.setChiefComplaint("头痛三天");
        assertEquals("头痛三天", request.getChiefComplaint());
    }

    @Test
    void shouldCreateTriageResponseWithDefaultConstructor() {
        TriageResponse response = new TriageResponse();
        assertNull(response.getRecommendedDepartments());
        assertNull(response.getReason());
    }

    @Test
    void shouldSetAndGetRecommendedDepartments() {
        TriageResponse response = new TriageResponse();
        List<RecommendedDepartment> depts = new ArrayList<>();
        depts.add(new RecommendedDepartment());
        response.setRecommendedDepartments(depts);
        assertEquals(1, response.getRecommendedDepartments().size());
    }

    @Test
    void shouldSetAndGetReason() {
        TriageResponse response = new TriageResponse();
        response.setReason("根据主诉推测为神经系统问题");
        assertEquals("根据主诉推测为神经系统问题", response.getReason());
    }

    @Test
    void shouldCreateRecommendedDepartmentWithDefaultConstructor() {
        RecommendedDepartment dept = new RecommendedDepartment();
        assertNull(dept.getDepartmentName());
    }

    @Test
    void shouldSetAndGetDepartmentName() {
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentName("神经内科");
        assertEquals("神经内科", dept.getDepartmentName());
    }

    @Test
    void shouldBuildFullTriageResponseWithDepartments() {
        RecommendedDepartment neurology = new RecommendedDepartment();
        neurology.setDepartmentName("神经内科");

        RecommendedDepartment ent = new RecommendedDepartment();
        ent.setDepartmentName("耳鼻喉科");

        List<RecommendedDepartment> depts = new ArrayList<>();
        depts.add(neurology);
        depts.add(ent);

        TriageResponse response = new TriageResponse();
        response.setRecommendedDepartments(depts);
        response.setReason("根据主诉头痛，建议优先就诊神经内科");

        assertEquals(2, response.getRecommendedDepartments().size());
        assertEquals("神经内科", response.getRecommendedDepartments().get(0).getDepartmentName());
        assertEquals("耳鼻喉科", response.getRecommendedDepartments().get(1).getDepartmentName());
        assertEquals("根据主诉头痛，建议优先就诊神经内科", response.getReason());
    }
}
