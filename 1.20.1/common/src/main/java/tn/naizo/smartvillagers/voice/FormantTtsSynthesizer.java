package tn.naizo.smartvillagers.voice;

import java.util.List;

/**
 * Compact source-filter (formant) synthesizer producing 48 kHz 16-bit mono PCM
 * for Simple Voice Chat. Intelligibility is robotic on purpose — a villager voice,
 * not a cloud neural TTS.
 */
public final class FormantTtsSynthesizer implements TtsSynthesizer {
    private static final double TWO_PI = Math.PI * 2.0;

    @Override
    public short[] synthesize(TtsRequest request) {
        if (request == null || request.text().isBlank() || request.volume() <= 0) {
            return new short[0];
        }
        List<SimpleG2p.Phone> phones = SimpleG2p.transcribe(request.text(), request.locale());
        if (phones.isEmpty()) {
            return new short[0];
        }

        int sampleRate = TtsRequest.SAMPLE_RATE_HZ;
        int totalSamples = 0;
        for (SimpleG2p.Phone phone : phones) {
            totalSamples += Math.max(1, phone.durationMs) * sampleRate / 1000;
        }
        totalSamples += sampleRate / 20;

        double[] mix = new double[totalSamples];
        double y1a = 0;
        double y2a = 0;
        double y1b = 0;
        double y2b = 0;
        double y1c = 0;
        double y2c = 0;
        int cursor = 0;
        boolean question = request.text().trim().endsWith("?") || request.text().trim().endsWith("？");
        double baseF0 = request.pitchHz();
        long seed = request.text().hashCode();

        SimpleG2p.Phone prev = SimpleG2p.Phone.PAU;
        for (int p = 0; p < phones.size(); p++) {
            SimpleG2p.Phone phone = phones.get(p);
            int n = Math.max(1, phone.durationMs) * sampleRate / 1000;
            double progress = phones.size() == 1 ? 1.0 : (double) p / (phones.size() - 1);
            double f0 = baseF0 * (question ? (0.94 + 0.14 * progress) : (1.04 - 0.10 * progress));

            for (int i = 0; i < n && cursor + i < mix.length; i++) {
                double t = n == 1 ? 1.0 : (double) i / (n - 1);
                double f1 = lerp(prev.f1, phone.f1, ease(t));
                double f2 = lerp(prev.f2, phone.f2, ease(t));
                double f3 = lerp(prev.f3, phone.f3, ease(t));
                double source;
                if (phone == SimpleG2p.Phone.PAU) {
                    source = 0;
                } else if (phone.voiced) {
                    double phase = ((cursor + i) * f0 / sampleRate) % 1.0;
                    source = phase < 0.12 ? (1.0 - phase / 0.12) : 0.0;
                    source += 0.015 * noise(seed + cursor + i);
                } else {
                    source = 0.22 * noise(seed + cursor + i);
                }

                double[] r1 = resonator(source, f1, 90, sampleRate, y1a, y2a);
                y1a = r1[1];
                y2a = r1[2];
                double[] r2 = resonator(r1[0], f2, 110, sampleRate, y1b, y2b);
                y1b = r2[1];
                y2b = r2[2];
                double[] r3 = resonator(r2[0], f3, 150, sampleRate, y1c, y2c);
                y1c = r3[1];
                y2c = r3[2];

                double env = envelope(i, n);
                mix[cursor + i] = r3[0] * env;
            }
            cursor += n;
            prev = phone;
        }

        double peak = 1e-6;
        for (double sample : mix) {
            peak = Math.max(peak, Math.abs(sample));
        }
        double gain = 0.35 * request.volume() / peak;
        short[] pcm = new short[mix.length];
        for (int i = 0; i < mix.length; i++) {
            double v = mix[i] * gain;
            if (v > 1) {
                v = 1;
            } else if (v < -1) {
                v = -1;
            }
            pcm[i] = (short) Math.round(v * 32767);
        }
        return pcm;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double envelope(int i, int n) {
        int fade = Math.max(8, n / 10);
        if (i < fade) {
            return (double) i / fade;
        }
        if (i > n - fade) {
            return (double) (n - i) / fade;
        }
        return 1.0;
    }

    private static double noise(long seed) {
        long x = seed * 6364136223846793005L + 1;
        return ((x >>> 33) / (double) (1L << 31)) * 2.0 - 1.0;
    }

    private static double[] resonator(double x, double freq, double bandwidth, int sampleRate, double y1, double y2) {
        if (freq <= 0) {
            return new double[]{x, y1, y2};
        }
        double r = Math.exp(-Math.PI * bandwidth / sampleRate);
        double theta = TWO_PI * freq / sampleRate;
        double y = x + 2.0 * r * Math.cos(theta) * y1 - r * r * y2;
        return new double[]{y, y, y1};
    }
}
