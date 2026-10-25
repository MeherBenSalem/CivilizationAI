package tn.naizo.smartvillagers.voice;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PcmTest {
    @Test
    void splitsInto960SampleFramesAndPadsTheLast() {
        short[] pcm = new short[2000];
        pcm[0] = 12;
        pcm[959] = 34;
        pcm[960] = 56;
        List<short[]> frames = Pcm.frames960(pcm);
        assertEquals(3, frames.size());
        assertEquals(960, frames.get(0).length);
        assertEquals(960, frames.get(1).length);
        assertEquals(960, frames.get(2).length);
        assertEquals(12, frames.get(0)[0]);
        assertEquals(34, frames.get(0)[959]);
        assertEquals(56, frames.get(1)[0]);
        assertEquals(0, frames.get(2)[200]);
    }

    @Test
    void emptyInputYieldsNoFrames() {
        assertTrue(Pcm.frames960(new short[0]).isEmpty());
        assertTrue(Pcm.frames960(null).isEmpty());
    }

    @Test
    void resamplesMonoPcmByLinearInterpolation() {
        short[] src = new short[]{0, 10000, 0};
        short[] dst = Pcm.resample(src, 3, 6);
        assertEquals(6, dst.length);
        assertEquals(0, dst[0]);
        assertTrue(dst[1] > 0 && dst[1] < 10000);
        assertEquals(10000, dst[2]);
    }
}
