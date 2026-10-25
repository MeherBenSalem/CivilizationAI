package tn.naizo.smartvillagers.schedule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PendingResponseQueueTest {
    @Test
    void oneAdvanceDoesNotExpireATwoTickDelay() {
        ReplyDelayQueue queue = new ReplyDelayQueue();
        queue.enqueue(new ReplyDelayQueue.Task(
                UUID.randomUUID(), UUID.randomUUID(), "hi", "hello", 2));

        List<ReplyDelayQueue.Task> first = queue.takeReady();
        assertEquals(0, first.size());
        assertEquals(1, queue.size());

        List<ReplyDelayQueue.Task> second = queue.takeReady();
        assertEquals(1, second.size());
        assertEquals(0, queue.size());
    }

    @Test
    void mustNotTreatEachDimensionAsItsOwnGameTick() {
        ReplyDelayQueue queue = new ReplyDelayQueue();
        queue.enqueue(new ReplyDelayQueue.Task(
                UUID.randomUUID(), UUID.randomUUID(), "hi", "hello", 2));

        queue.takeReady();
        assertEquals(1, queue.size(), "must not treat overworld+nether+end as three game ticks");
    }
}
