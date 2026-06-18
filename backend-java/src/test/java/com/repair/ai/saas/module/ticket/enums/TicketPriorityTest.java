package com.repair.ai.saas.module.ticket.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketPriorityTest {

    // ===== 正常解析 =====

    @Test
    void fromString_low() {
        assertEquals(TicketPriority.LOW, TicketPriority.fromString("LOW"));
    }

    @Test
    void fromString_normal() {
        assertEquals(TicketPriority.NORMAL, TicketPriority.fromString("NORMAL"));
    }

    @Test
    void fromString_high() {
        assertEquals(TicketPriority.HIGH, TicketPriority.fromString("HIGH"));
    }

    @Test
    void fromString_urgent() {
        assertEquals(TicketPriority.URGENT, TicketPriority.fromString("URGENT"));
    }

    // ===== 大小写和空格 =====

    @Test
    void fromString_lowerCase() {
        assertEquals(TicketPriority.HIGH, TicketPriority.fromString("high"));
    }

    @Test
    void fromString_mixedCase() {
        assertEquals(TicketPriority.URGENT, TicketPriority.fromString("Urgent"));
    }

    @Test
    void fromString_withSpaces() {
        assertEquals(TicketPriority.NORMAL, TicketPriority.fromString("  NORMAL  "));
    }

    // ===== 非法值 =====

    @Test
    void fromString_null_returnsNull() {
        assertNull(TicketPriority.fromString(null));
    }

    @Test
    void fromString_blank_returnsNull() {
        assertNull(TicketPriority.fromString("  "));
    }

    @Test
    void fromString_unknown_returnsNull() {
        assertNull(TicketPriority.fromString("UNKNOWN"));
    }

    // ===== label =====

    @Test
    void label_high() {
        assertEquals("高", TicketPriority.HIGH.getLabel());
    }

    @Test
    void label_urgent() {
        assertEquals("紧急", TicketPriority.URGENT.getLabel());
    }
}
