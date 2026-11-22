package com.iot.platform.service.control;

import com.iot.platform.service.control.ConflictChecker.Candidate;
import com.iot.platform.service.control.ConflictChecker.ConflictResult;
import com.iot.platform.service.control.ConflictChecker.ExistingRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 冲突校验单测（定时规则场景，复刻原型 conflictCheck）。 */
class ConflictCheckerTimedTest {

    private Candidate timedCandidate(String name, List<String> devices, String action,
                                     String time, String closeTime) {
        Candidate c = new Candidate();
        c.name = name;
        c.kind = ConflictChecker.KIND_TIMED;
        c.trigger = ConflictChecker.TRIGGER_TIMED;
        c.devices.addAll(devices);
        c.deviceTypes.add("风机");
        c.action = action;
        c.time = time;
        c.closeTime = closeTime;
        return c;
    }

    private ExistingRule timed(String id, String name, String action, String time, String closeTime) {
        return new ExistingRule(id, name, ConflictChecker.TRIGGER_TIMED, List.of("wind1"), action,
                time, closeTime, null, null, null, null, null, false, null, "每天 " + time + " " + action);
    }

    @Test
    void timedOppositeOverlappingWindowsIsHardConflict() {
        ExistingRule existing = timed("r1", "风机早间通风", "ON", "07:00", "07:20");
        Candidate c = timedCandidate("新规则", List.of("wind1"), "OFF", "07:00", "07:20");
        ConflictResult r = ConflictChecker.check(c, "", List.of(existing));
        assertTrue(r.blocked());
        assertTrue(r.getHits().get(0).isHard());
    }

    @Test
    void timedSameActionSameMinuteIsSoftDuplicate() {
        ExistingRule existing = timed("r1", "风机早间通风", "ON", "07:00", null);
        Candidate c = timedCandidate("同刻开启", List.of("wind1"), "ON", "07:00", null);
        ConflictResult r = ConflictChecker.check(c, "", List.of(existing));
        assertFalse(r.blocked());
        assertEquals(1, r.getHits().size());
        assertFalse(r.getHits().get(0).isHard());
    }

    @Test
    void timedDisjointWindowsPass() {
        ExistingRule existing = timed("r1", "风机早间通风", "ON", "07:00", "07:20");
        Candidate c = timedCandidate("园区通风", List.of("wind1"), "ON", "09:00", "09:30");
        ConflictResult r = ConflictChecker.check(c, "", List.of(existing));
        assertFalse(r.blocked());
        assertTrue(r.getHits().isEmpty());
    }

    @Test
    void ruleEventsContainOppositeCloseEvent() {
        var events = ConflictChecker.ruleEvents(ConflictChecker.TRIGGER_TIMED, "ON", "07:00", "07:20",
                null, null, null, null, null, false, null);
        assertEquals(2, events.size());
        assertEquals("OFF", events.get(1).action);
        assertTrue(ConflictChecker.ruleEvents(ConflictChecker.TRIGGER_MANUAL, "ON", null, null,
                null, null, null, null, null, false, null).isEmpty());
    }
}
