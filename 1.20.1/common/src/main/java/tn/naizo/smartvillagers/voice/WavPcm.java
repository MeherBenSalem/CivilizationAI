package tn.naizo.smartvillagers.voice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class WavPcm {
    private WavPcm() {
    }

    public record Clip(int sampleRate, int channels, short[] samples) {
    }

    public static Clip read(byte[] wav) {
        if (wav == null || wav.length < 44) {
            throw new IllegalArgumentException("WAV too small");
        }
        ByteBuffer buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        if (!ascii(buf, 4).equals("RIFF") || !ascii(skip(buf, 4), 4).equals("WAVE")) {
            throw new IllegalArgumentException("Not a RIFF WAVE file");
        }
        int sampleRate = 0;
        int channels = 1;
        int bits = 16;
        short[] samples = new short[0];
        while (buf.remaining() >= 8) {
            String id = ascii(buf, 4);
            int size = buf.getInt();
            if (size < 0 || size > buf.remaining()) {
                break;
            }
            int start = buf.position();
            if ("fmt ".equals(id) && size >= 16) {
                int format = buf.getShort() & 0xFFFF;
                channels = buf.getShort() & 0xFFFF;
                sampleRate = buf.getInt();
                buf.getInt();
                buf.getShort();
                bits = buf.getShort() & 0xFFFF;
                if (format != 1 || bits != 16) {
                    throw new IllegalArgumentException("Only PCM 16-bit WAV is supported");
                }
            } else if ("data".equals(id)) {
                int count = size / 2;
                samples = new short[count];
                for (int i = 0; i < count; i++) {
                    samples[i] = buf.getShort();
                }
                if (channels > 1) {
                    samples = downmixToMono(samples, channels);
                }
            }
            buf.position(start + size + (size & 1));
        }
        if (sampleRate <= 0 || samples.length == 0) {
            throw new IllegalArgumentException("WAV missing fmt/data");
        }
        return new Clip(sampleRate, 1, samples);
    }

    private static short[] downmixToMono(short[] interleaved, int channels) {
        int frames = interleaved.length / channels;
        short[] mono = new short[frames];
        for (int i = 0; i < frames; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                sum += interleaved[i * channels + c];
            }
            mono[i] = (short) (sum / channels);
        }
        return mono;
    }

    private static ByteBuffer skip(ByteBuffer buf, int bytes) {
        buf.position(buf.position() + bytes);
        return buf;
    }

    private static String ascii(ByteBuffer buf, int n) {
        byte[] bytes = new byte[n];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}
