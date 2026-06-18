package com.repair.ai.saas.module.ticket.enums;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TicketStatusTest {

    // ===== canTransitionTo: 正常流转 =====

    @Test
    void pending_canTransitionTo_assigned() {
        assertTrue(TicketStatus.PENDING.canTransitionTo(TicketStatus.ASSIGNED));
    }

    @Test
    void pending_canTransitionTo_cancelled() {
        assertTrue(TicketStatus.PENDING.canTransitionTo(TicketStatus.CANCELLED));
    }

    @Test
    void assigned_canTransitionTo_inProgress() {
        assertTrue(TicketStatus.ASSIGNED.canTransitionTo(TicketStatus.IN_PROGRESS));
    }

    @Test
    void assigned_canTransitionTo_cancelled() {
        assertTrue(TicketStatus.ASSIGNED.canTransitionTo(TicketStatus.CANCELLED));
    }

    @Test
    void inProgress_canTransitionTo_completed() {
        assertTrue(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.COMPLETED));
    }

    @Test
    void completed_canTransitionTo_closed() {
        assertTrue(TicketStatus.COMPLETED.canTransitionTo(TicketStatus.CLOSED));
    }

    @Test
    void completed_canTransitionTo_followedUp() {
        assertTrue(TicketStatus.COMPLETED.canTransitionTo(TicketStatus.FOLLOWED_UP));
    }

    @Test
    void followedUp_canTransitionTo_closed() {
        assertTrue(TicketStatus.FOLLOWED_UP.canTransitionTo(TicketStatus.CLOSED));
    }

    // ===== canTransitionTo: 非法转换 =====

    @Test
    void pending_cannotTransitionTo_inProgress() {
        assertFalse(TicketStatus.PENDING.canTransitionTo(TicketStatus.IN_PROGRESS));
    }

    @Test
    void pending_cannotTransitionTo_completed() {
        assertFalse(TicketStatus.PENDING.canTransitionTo(TicketStatus.COMPLETED));
    }

    @Test
    void assigned_cannotTransitionTo_completed() {
        assertFalse(TicketStatus.ASSIGNED.canTransitionTo(TicketStatus.COMPLETED));
    }

    @Test
    void inProgress_cannotTransitionTo_closed() {
        assertFalse(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.CLOSED));
    }

    @Test
    void inProgress_cannotTransitionTo_cancelled() {
        assertFalse(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.CANCELLED));
    }

    // ===== 终态不可流转 =====

    @Test
    void closed_cannotTransitionToAnything() {
        Set<TicketStatus> allowed = TicketStatus.getAllowedTargets(TicketStatus.CLOSED);
        assertEquals(EnumSet.noneOf(TicketStatus.class), allowed);
    }

    @Test
    void cancelled_cannotTransitionToAnything() {
        Set<TicketStatus> allowed = TicketStatus.getAllowedTargets(TicketStatus.CANCELLED);
        assertEquals(EnumSet.noneOf(TicketStatus.class), allowed);
    }

    @Test
    void closed_cannotTransitionTo_pending() {
        assertFalse(TicketStatus.CLOSED.canTransitionTo(TicketStatus.PENDING));
    }

    @Test
    void cancelled_cannotTransitionTo_assigned() {
        assertFalse(TicketStatus.CANCELLED.canTransitionTo(TicketStatus.ASSIGNED));
    }

    // ===== fromString =====

    @Test
    void fromString_validUpperCase() {
        assertEquals(TicketStatus.PENDING, TicketStatus.fromString("PENDING"));
    }

    @Test
    void fromString_validLowerCase() {
        assertEquals(TicketStatus.IN_PROGRESS, TicketStatus.fromString("in_progress"));
    }

    @Test
    void fromString_withSpaces() {
        assertEquals(TicketStatus.CLOSED, TicketStatus.fromString("  CLOSED  "));
    }

    @Test
    void fromString_null_returnsNull() {
        assertNull(TicketStatus.fromString(null));
    }

    @Test
    void fromString_blank_returnsNull() {
        assertNull(TicketStatus.fromString("  "));
    }

    @Test
    void fromString_unknown_returnsNull() {
        assertNull(TicketStatus.fromString("UNKNOWN"));
    }
}
