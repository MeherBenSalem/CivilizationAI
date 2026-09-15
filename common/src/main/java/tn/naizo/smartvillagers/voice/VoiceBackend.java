package tn.naizo.smartvillagers.voice;

public interface VoiceBackend {
    boolean isReady();

    boolean play(VoicePlaybackRequest request);

    VoiceBackend NO_OP = new VoiceBackend() {
        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public boolean play(VoicePlaybackRequest request) {
            return false;
        }
    };
}
