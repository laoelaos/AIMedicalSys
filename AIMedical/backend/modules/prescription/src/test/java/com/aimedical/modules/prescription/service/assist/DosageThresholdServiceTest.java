package com.aimedical.modules.prescription.service.assist;

import com.aimedical.common.entity.DosageStandard;
import com.aimedical.modules.prescription.PrescriptionErrorCode;
import com.aimedical.modules.prescription.dto.assist.DosageAlert;
import com.aimedical.modules.prescription.dto.assist.DosageAlertLevel;
import com.aimedical.modules.prescription.dto.assist.DosageCheckRequest;
import com.aimedical.modules.prescription.dto.assist.DoseWarningType;
import com.aimedical.modules.prescription.repository.DosageStandardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DosageThresholdServiceTest {

    @Mock
    private DosageStandardRepository dosageStandardRepository;

    private DosageThresholdService service;
    private DosageCheckRequest request;

    @BeforeEach
    void setUp() {
        service = new DosageThresholdService(dosageStandardRepository);
        request = new DosageCheckRequest();
        request.setDrugCode("drug-001");
        request.setDosage(100);
        request.setUnit("mg");
        request.setRouteOfAdministration("oral");
    }

    private DosageStandard createStandard(BigDecimal singleMax, BigDecimal dailyMax, String unit) {
        DosageStandard ds = new DosageStandard();
        ds.setDrugCode("drug-001");
        ds.setRouteOfAdministration("oral");
        ds.setSingleMax(singleMax);
        ds.setDailyMax(dailyMax);
        ds.setUnit(unit);
        return ds;
    }

    @Test
    void shouldReturnCriticalAlertWhenNoStandardFound() {
        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of());

        List<DosageAlert> alerts = service.check(request);

        assertEquals(1, alerts.size());
        assertEquals(DosageAlertLevel.CRITICAL, alerts.get(0).getAlertLevel());
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, alerts.get(0).getWarningType());
        assertEquals(PrescriptionErrorCode.RX_ASSIST_DOSE_STANDARD_NOT_FOUND.getCode(), alerts.get(0).getErrorCode());
    }

    @Test
    void shouldReturnWarningWhenUnitMismatch() {
        DosageStandard ds = createStandard(BigDecimal.valueOf(200), null, "g");
        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ds));

        List<DosageAlert> alerts = service.check(request);

        assertEquals(1, alerts.size());
        assertEquals(DosageAlertLevel.WARNING, alerts.get(0).getAlertLevel());
    }

    @Test
    void shouldReturnCriticalWhenDosageExceedsDoubleSingleMax() {
        DosageStandard ds = createStandard(BigDecimal.valueOf(100), null, "mg");
        request.setDosage(250);
        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ds));

        List<DosageAlert> alerts = service.check(request);

        assertEquals(1, alerts.size());
        assertEquals(DosageAlertLevel.CRITICAL, alerts.get(0).getAlertLevel());
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, alerts.get(0).getWarningType());
    }

    @Test
    void shouldReturnWarningWhenDosageExceedsSingleMax() {
        DosageStandard ds = createStandard(BigDecimal.valueOf(100), null, "mg");
        request.setDosage(150);
        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ds));

        List<DosageAlert> alerts = service.check(request);

        assertEquals(1, alerts.size());
        assertEquals(DosageAlertLevel.WARNING, alerts.get(0).getAlertLevel());
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, alerts.get(0).getWarningType());
    }

    @Test
    void shouldReturnNoAlertWhenDosageWithinLimit() {
        DosageStandard ds = createStandard(BigDecimal.valueOf(200), null, "mg");
        request.setDosage(100);
        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ds));

        List<DosageAlert> alerts = service.check(request);

        assertTrue(alerts.isEmpty());
    }

    @Test
    void shouldAddDailyDoseWarningWhenExceedsDailyMax() {
        DosageStandard ds = createStandard(BigDecimal.valueOf(100), BigDecimal.valueOf(300), "mg");
        request.setDosage(150);
        request.setFrequency("3");
        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ds));

        List<DosageAlert> alerts = service.check(request);

        assertEquals(2, alerts.size());
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, alerts.get(0).getWarningType());
        assertEquals(DoseWarningType.OVER_DAILY_DOSE, alerts.get(1).getWarningType());
    }

    @Test
    void shouldMatchByExactAgeAndWeightPriority() {
        DosageStandard exact = createStandard(BigDecimal.valueOf(100), null, "mg");
        exact.setAgeRangeStart(20);
        exact.setAgeRangeEnd(40);
        exact.setWeightRangeStart(BigDecimal.valueOf(50));
        exact.setWeightRangeEnd(BigDecimal.valueOf(100));

        DosageStandard fallback = createStandard(BigDecimal.valueOf(200), null, "mg");
        request.setPatientAge(30);
        request.setPatientWeight(BigDecimal.valueOf(70));

        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(exact, fallback));

        List<DosageAlert> alerts = service.check(request);

        assertTrue(alerts.isEmpty(), "exact match should apply and dose is within limit");
    }

    @Test
    void shouldReturnStandardNotFoundWhenNoPriorityMatch() {
        DosageStandard ds = createStandard(BigDecimal.valueOf(100), null, "mg");
        ds.setAgeRangeStart(20);
        ds.setAgeRangeEnd(40);
        request.setPatientAge(50);

        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ds));

        List<DosageAlert> alerts = service.check(request);

        assertEquals(1, alerts.size());
        assertEquals(PrescriptionErrorCode.RX_ASSIST_DOSE_STANDARD_NOT_FOUND.getCode(), alerts.get(0).getErrorCode());
    }

    @Test
    void shouldLogWarnAndSkipDailyDoseCheckWhenFrequencyIsNonNumeric() {
        DosageStandard ds = createStandard(BigDecimal.valueOf(100), BigDecimal.valueOf(300), "mg");
        request.setDosage(150);
        request.setFrequency("tid");
        when(dosageStandardRepository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ds));

        Logger thresholdLogger = (Logger) org.slf4j.LoggerFactory.getLogger(DosageThresholdService.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        thresholdLogger.addAppender(logAppender);

        try {
            List<DosageAlert> alerts = service.check(request);

            assertEquals(1, alerts.size());
            assertEquals(DoseWarningType.OVER_SINGLE_DOSE, alerts.get(0).getWarningType());

            assertEquals(1, logAppender.list.size());
            ILoggingEvent event = logAppender.list.get(0);
            assertEquals(Level.WARN, event.getLevel());
            assertTrue(event.getFormattedMessage().contains("Non-numeric frequency"));
            assertTrue(event.getFormattedMessage().contains("tid"));
        } finally {
            thresholdLogger.detachAppender(logAppender);
        }
    }
}
