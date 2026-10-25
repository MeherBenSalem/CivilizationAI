package tn.naizo.smartvillagers.voice;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavPcmTest {
    @Test
    void readsPcm16MonoWav() {
        short[] samples = new short[]{0, 1234, -2000, 32767};
        byte[] wav = writeWav(22050, samples);
        WavPcm.Clip clip = WavPcm.read(wav);
        assertEquals(22050, clip.sampleRate());
        assertEquals(1, clip.channels());
        assertEquals(4, clip.samples().length);
        assertEquals(1234, clip.samples()[1]);
        assertEquals(-2000, clip.samples()[2]);
        assertEquals(32767, clip.samples()[3]);
    }

    @Test
    void sapiScriptDoesNotEmbedRawUserText() {
        String script = WindowsSapiTts.scriptFor("Hello \"there\"; Stop-Process", "C:\\tmp\\out.wav", 170);
        assertTrue(script.contains("FromBase64String"));
        assertFalse(script.contains("Hello"));
        assertFalse(script.contains("Stop-Process"));
        assertTrue(script.contains("Female") || script.contains("Female"));
    }

    private static byte[] writeWav(int sampleRate, short[] samples) {
        int dataBytes = samples.length * 2;
        ByteBuffer buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(36 + dataBytes);
        buf.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        buf.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(16);
        buf.putShort((short) 1);
        buf.putShort((short) 1);
        buf.putInt(sampleRate);
        buf.putInt(sampleRate * 2);
        buf.putShort((short) 2);
        buf.putShort((short) 16);
        buf.put("data".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(dataBytes);
        for (short sample : samples) {
            buf.putShort(sample);
        }
        return buf.array();
    }
}
