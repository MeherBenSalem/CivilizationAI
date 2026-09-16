package tn.naizo.smartvillagers.chat;

import org.junit.jupiter.api.Test;
import tn.naizo.smartvillagers.ActivationMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivationDetectorTest {
    @Test
    void smartActivatesOnGreeting() {
        MessageSignals signals = MessageSignals.analyze("Hello there", "Aldric");
        assertTrue(ActivationDetector.isActivated(ActivationMode.SMART, "Hello there", signals, false));
    }

    @Test
    void lookAtRequiresLooking() {
        MessageSignals signals = MessageSignals.analyze("hi", "Aldric");
        assertFalse(ActivationDetector.isActivated(ActivationMode.LOOK_AT, "hi", signals, false));
        assertTrue(ActivationDetector.isActivated(ActivationMode.LOOK_AT, "hi", signals, true));
    }

    @Test
    void alwaysNearbyAlwaysTrue() {
        MessageSignals signals = MessageSignals.analyze("anything", null);
        assertTrue(ActivationDetector.isActivated(ActivationMode.ALWAYS_NEARBY, "anything", signals, false));
    }

    @Test
    void smartActivatesOnShortMessageWhenLookingOrClose() {
        MessageSignals signals = MessageSignals.analyze("test", "Aldric");
        assertTrue(ActivationDetector.isActivated(ActivationMode.SMART, "test", signals, true));
        assertFalse(ActivationDetector.isActivated(ActivationMode.SMART, "test", signals, false));
    }

    @Test
    void closeDistanceCountsAsAddressing() {
        assertTrue(ActivationDetector.isCloseEnough(3.0));
        assertTrue(ActivationDetector.isCloseEnough(6.0));
        assertFalse(ActivationDetector.isCloseEnough(6.1));
    }
}
