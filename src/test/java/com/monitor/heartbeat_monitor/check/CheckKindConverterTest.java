package com.monitor.heartbeat_monitor.check;

import com.monitor.heartbeat_monitor.check.enums.CheckKind;
import com.monitor.heartbeat_monitor.check.enums.CheckKindConverter;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.enums.CheckStatusConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CheckKindConverterTest {

    private final CheckKindConverter kindConverter = new CheckKindConverter();
    private final CheckStatusConverter statusConverter = new CheckStatusConverter();

    @Test
    void testCheckKindCaseInsensitiveConversion() {
        assertEquals(CheckKind.WEBSITE, kindConverter.convertToEntityAttribute("website"));
        assertEquals(CheckKind.WEBSITE, kindConverter.convertToEntityAttribute("WEBSITE"));
        assertEquals(CheckKind.WEBSITE, kindConverter.convertToEntityAttribute("Website"));

        assertEquals(CheckKind.HEARTBEAT, kindConverter.convertToEntityAttribute("heartbeat"));
        assertEquals(CheckKind.HEARTBEAT, kindConverter.convertToEntityAttribute("HEARTBEAT"));
        assertEquals(CheckKind.HEARTBEAT, kindConverter.convertToEntityAttribute("Heartbeat"));

        assertNull(kindConverter.convertToEntityAttribute(null));
        assertNull(kindConverter.convertToEntityAttribute("unknown"));
    }

    @Test
    void testCheckStatusCaseInsensitiveConversion() {
        assertEquals(CheckStatus.UP, statusConverter.convertToEntityAttribute("up"));
        assertEquals(CheckStatus.UP, statusConverter.convertToEntityAttribute("UP"));
        assertEquals(CheckStatus.DOWN, statusConverter.convertToEntityAttribute("down"));
        assertEquals(CheckStatus.DOWN, statusConverter.convertToEntityAttribute("DOWN"));
        assertEquals(CheckStatus.LATE, statusConverter.convertToEntityAttribute("late"));
        assertEquals(CheckStatus.PAUSED, statusConverter.convertToEntityAttribute("paused"));

        assertNull(statusConverter.convertToEntityAttribute(null));
        assertNull(statusConverter.convertToEntityAttribute("invalid"));
    }
}
