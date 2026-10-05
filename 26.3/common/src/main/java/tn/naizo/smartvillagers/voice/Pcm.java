package tn.naizo.smartvillagers.voice;

import java.util.ArrayList;
import java.util.List;

public final class Pcm {
    public static final int FRAME_SAMPLES = 960;

    private Pcm() {
    }

    public static List<short[]> frames960(short[] pcm) {
        List<short[]> frames = new ArrayList<>();
        if (pcm == null || pcm.length == 0) {
            return frames;
        }
        for (int offset = 0; offset < pcm.length; offset += FRAME_SAMPLES) {
            short[] frame = new short[FRAME_SAMPLES];
            int copy = Math.min(FRAME_SAMPLES, pcm.length - offset);
            System.arraycopy(pcm, offset, frame, 0, copy);
            frames.add(frame);
        }
        return frames;
    }

    public static short[] resample(short[] input, int sourceRate, int targetRate) {
        if (input == null || input.length == 0 || sourceRate <= 0 || targetRate <= 0) {
            return new short[0];
        }
        if (sourceRate == targetRate) {
            return input.clone();
        }
        int outLength = Math.max(1, (int) Math.round(input.length * (double) targetRate / sourceRate));
        short[] out = new short[outLength];
        double step = (double) sourceRate / targetRate;
        for (int i = 0; i < outLength; i++) {
            double src = i * step;
            int i0 = (int) Math.floor(src);
            int i1 = Math.min(input.length - 1, i0 + 1);
            double t = src - i0;
            out[i] = (short) Math.round(input[i0] * (1.0 - t) + input[i1] * t);
        }
        return out;
    }

    public static short[] scale(short[] input, double volume) {
        if (input == null || input.length == 0) {
            return new short[0];
        }
        double gain = Math.max(0.0, Math.min(1.0, volume));
        if (gain == 1.0) {
            return input;
        }
        short[] out = new short[input.length];
        for (int i = 0; i < input.length; i++) {
            int v = (int) Math.round(input[i] * gain);
            if (v > Short.MAX_VALUE) {
                v = Short.MAX_VALUE;
            } else if (v < Short.MIN_VALUE) {
                v = Short.MIN_VALUE;
            }
            out[i] = (short) v;
        }
        return out;
    }
}
