package tn.naizo.smartvillagers.voice;

import tn.naizo.smartvillagers.Constants;

public final class CompositeTtsSynthesizer implements TtsSynthesizer {
    private final TtsSynthesizer windows = new WindowsSapiTts();
    private final TtsSynthesizer formant = new FormantTtsSynthesizer();

    @Override
    public short[] synthesize(TtsRequest request) {
        if (WindowsSapiTts.available()) {
            try {
                short[] spoken = windows.synthesize(request);
                if (spoken.length > 0) {
                    return spoken;
                }
            } catch (Exception e) {
                Constants.LOG.warn("Windows TTS unavailable, using built-in voice", e);
            }
        }
        return formant.synthesize(request);
    }
}
