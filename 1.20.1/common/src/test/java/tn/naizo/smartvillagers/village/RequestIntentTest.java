package tn.naizo.smartvillagers.village;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIntentTest {
    @Test
    void recognisesRequestsForWork() {
        for (String message : new String[]{"Do you have any work for me?", "got any jobs?", "I need a quest",
                "Can I help you with anything?", "any errands today", "Need help?", "As-tu une quête pour moi ?",
                "¿Tienes trabajo?", "Hast du eine Aufgabe für mich?"}) {
            assertTrue(RequestIntent.asksForWork(message), message);
        }
    }

    @Test
    void ordinaryChatIsNotAskingForWork() {
        for (String message : new String[]{"hello", "What do you sell?", "does this work?", "nice weather",
                "where is the nearest village?"}) {
            assertFalse(RequestIntent.asksForWork(message), message);
        }
    }

    @Test
    void acceptAndDecline() {
        assertTrue(RequestIntent.accepts("Yes!"));
        assertTrue(RequestIntent.accepts("sure, I'll do it"));
        assertTrue(RequestIntent.accepts("ok"));
        assertTrue(RequestIntent.accepts("Oui"));
        assertFalse(RequestIntent.accepts("I don't know yet"));
        assertTrue(RequestIntent.declines("no thanks"));
        assertTrue(RequestIntent.declines("Not now"));
        assertFalse(RequestIntent.declines("know what?"));
    }

    @Test
    void progressReports() {
        assertTrue(RequestIntent.reportsProgress("Done!"));
        assertTrue(RequestIntent.reportsProgress("here you go"));
        assertTrue(RequestIntent.reportsProgress("how many do you still need?"));
        assertFalse(RequestIntent.reportsProgress("hello"));
    }

    @Test
    void abandoning() {
        assertTrue(RequestIntent.abandons("Never mind, I give up"));
        assertTrue(RequestIntent.abandons("nevermind"));
        assertTrue(RequestIntent.abandons("forget it"));
        assertFalse(RequestIntent.abandons("hello"));
        assertFalse(RequestIntent.abandons("done"));
    }
}
