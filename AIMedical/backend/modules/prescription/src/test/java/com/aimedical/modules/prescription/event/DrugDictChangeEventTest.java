package com.aimedical.modules.prescription.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrugDictChangeEventTest {

    @Test
    void shouldCreateBaseEventWithChangeTypeAndDrugCode() {
        DrugDictChangeEvent event = new DrugContraindicationChangeEvent(
                DrugDictChangeEvent.ChangeType.UPDATE, "drug-001");

        assertEquals(DrugDictChangeEvent.ChangeType.UPDATE, event.getChangeType());
        assertEquals("drug-001", event.getDrugCode());
    }

    @Test
    void shouldCreateDrugContraindicationChangeEvent() {
        DrugContraindicationChangeEvent event = new DrugContraindicationChangeEvent(
                DrugDictChangeEvent.ChangeType.CREATE, "drug-001");

        assertInstanceOf(DrugDictChangeEvent.class, event);
        assertEquals(DrugDictChangeEvent.ChangeType.CREATE, event.getChangeType());
        assertEquals("drug-001", event.getDrugCode());
    }

    @Test
    void shouldCreateDrugAllergyMappingChangeEvent() {
        DrugAllergyMappingChangeEvent event = new DrugAllergyMappingChangeEvent(
                DrugDictChangeEvent.ChangeType.DELETE, "drug-002");

        assertInstanceOf(DrugDictChangeEvent.class, event);
        assertEquals(DrugDictChangeEvent.ChangeType.DELETE, event.getChangeType());
        assertEquals("drug-002", event.getDrugCode());
    }

    @Test
    void shouldCreateDrugCompositionDictChangeEvent() {
        DrugCompositionDictChangeEvent event = new DrugCompositionDictChangeEvent(
                DrugDictChangeEvent.ChangeType.UPDATE, "drug-003");

        assertInstanceOf(DrugDictChangeEvent.class, event);
        assertEquals(DrugDictChangeEvent.ChangeType.UPDATE, event.getChangeType());
        assertEquals("drug-003", event.getDrugCode());
    }

    @Test
    void changeTypeShouldContainAllExpectedValues() {
        assertEquals(3, DrugDictChangeEvent.ChangeType.values().length);
        assertTrue(Enum.valueOf(DrugDictChangeEvent.ChangeType.class, "CREATE") != null);
        assertTrue(Enum.valueOf(DrugDictChangeEvent.ChangeType.class, "UPDATE") != null);
        assertTrue(Enum.valueOf(DrugDictChangeEvent.ChangeType.class, "DELETE") != null);
    }

    @Test
    void shouldHandleDifferentChangeTypesForEachEvent() {
        DrugContraindicationChangeEvent createEvent = new DrugContraindicationChangeEvent(
                DrugDictChangeEvent.ChangeType.CREATE, "drug-001");
        DrugAllergyMappingChangeEvent updateEvent = new DrugAllergyMappingChangeEvent(
                DrugDictChangeEvent.ChangeType.UPDATE, "drug-002");
        DrugCompositionDictChangeEvent deleteEvent = new DrugCompositionDictChangeEvent(
                DrugDictChangeEvent.ChangeType.DELETE, "drug-003");

        assertEquals(DrugDictChangeEvent.ChangeType.CREATE, createEvent.getChangeType());
        assertEquals(DrugDictChangeEvent.ChangeType.UPDATE, updateEvent.getChangeType());
        assertEquals(DrugDictChangeEvent.ChangeType.DELETE, deleteEvent.getChangeType());
    }
}
