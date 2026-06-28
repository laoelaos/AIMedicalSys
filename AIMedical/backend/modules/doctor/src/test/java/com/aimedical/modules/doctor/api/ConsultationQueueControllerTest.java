package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.doctor.dto.response.ConsultationQueueResponse;
import com.aimedical.modules.doctor.service.ConsultationQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ConsultationQueueController} 单元测试。
 *
 * <p>验证控制器正确委托给 service 并传递当前医生 ID；
 * 覆盖 currentDoctorId() 空值保护分支。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ConsultationQueueControllerTest {

    @Mock
    private ConsultationQueueService queueService;

    @Mock
    private CurrentUser currentUser;

    private ConsultationQueueController controller;

    private static final Long DOCTOR_ID = 200L;

    @BeforeEach
    void setUp() {
        controller = new ConsultationQueueController(queueService, currentUser);
    }

    private ConsultationQueueResponse buildResponse() {
        return new ConsultationQueueResponse(1L, 100L, "张三", DOCTOR_ID, "内科",
                "A001", "WAITING", LocalDateTime.now(), null, null, null);
    }

    @Test
    void listMyQueue_shouldDelegateToServiceWithCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(queueService.listMyQueue(DOCTOR_ID)).thenReturn(Result.success(List.of(buildResponse())));

        Result<List<ConsultationQueueResponse>> result = controller.listMyQueue();

        assertEquals("SUCCESS", result.getCode());
        verify(queueService).listMyQueue(DOCTOR_ID);
    }

    @Test
    void listWaiting_shouldDelegateToServiceWithCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(queueService.listWaiting(DOCTOR_ID)).thenReturn(Result.success(List.of()));

        Result<List<ConsultationQueueResponse>> result = controller.listWaiting();

        assertEquals("SUCCESS", result.getCode());
        verify(queueService).listWaiting(DOCTOR_ID);
    }

    @Test
    void callNext_shouldDelegateToServiceWithCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(queueService.callNext(DOCTOR_ID)).thenReturn(Result.success(buildResponse()));

        Result<ConsultationQueueResponse> result = controller.callNext();

        assertEquals("SUCCESS", result.getCode());
        verify(queueService).callNext(DOCTOR_ID);
    }

    @Test
    void startConsultation_shouldDelegateToServiceWithIdAndCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(queueService.startConsultation(1L, DOCTOR_ID)).thenReturn(Result.success(buildResponse()));

        Result<ConsultationQueueResponse> result = controller.startConsultation(1L);

        assertEquals("SUCCESS", result.getCode());
        verify(queueService).startConsultation(1L, DOCTOR_ID);
    }

    @Test
    void finishConsultation_shouldDelegateToServiceWithIdAndCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(queueService.finishConsultation(1L, DOCTOR_ID)).thenReturn(Result.success(buildResponse()));

        Result<ConsultationQueueResponse> result = controller.finishConsultation(1L);

        assertEquals("SUCCESS", result.getCode());
        verify(queueService).finishConsultation(1L, DOCTOR_ID);
    }

    @Test
    void skip_shouldDelegateToServiceWithIdAndCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(queueService.skip(1L, DOCTOR_ID)).thenReturn(Result.success(buildResponse()));

        Result<ConsultationQueueResponse> result = controller.skip(1L);

        assertEquals("SUCCESS", result.getCode());
        verify(queueService).skip(1L, DOCTOR_ID);
    }

    @Test
    void anyEndpoint_shouldThrowWhenUserIdIsNull() {
        when(currentUser.getUserId()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> controller.listMyQueue());
    }
}
