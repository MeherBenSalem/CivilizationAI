package tn.naizo.smartvillagers.voice;

import tn.naizo.smartvillagers.Constants;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Uses Windows SAPI (System.Speech) so villagers read the reply in a real TTS voice.
 * Cross-platform callers get an empty buffer and should fall back to formant TTS.
 */
public final class WindowsSapiTts implements TtsSynthesizer {
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).contains("win");

    @Override
    public short[] synthesize(TtsRequest request) {
        if (!available() || request == null || request.text().isBlank() || request.volume() <= 0) {
            return new short[0];
        }
        Path wav = null;
        try {
            wav = Files.createTempFile("smartvillagers-tts-", ".wav");
            Path scriptFile = Files.createTempFile("smartvillagers-tts-", ".ps1");
            String script = scriptFor(request.text(), wav.toAbsolutePath().toString(), request.pitchHz());
            Files.writeString(scriptFile, script, StandardCharsets.UTF_8);
            Process process = new ProcessBuilder(powershellExe(),
                    "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.toAbsolutePath().toString())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(12, TimeUnit.SECONDS);
            Files.deleteIfExists(scriptFile);
            if (!finished) {
                process.destroyForcibly();
                Constants.LOG.warn("Windows TTS timed out");
                return new short[0];
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(wav) || Files.size(wav) < 44) {
                Constants.LOG.warn("Windows TTS failed (exit {})", process.exitValue());
                return new short[0];
            }
            WavPcm.Clip clip = WavPcm.read(Files.readAllBytes(wav));
            short[] pcm48 = Pcm.resample(clip.samples(), clip.sampleRate(), TtsRequest.SAMPLE_RATE_HZ);
            return Pcm.scale(pcm48, request.volume());
        } catch (Exception e) {
            Constants.LOG.warn("Windows TTS failed", e);
            return new short[0];
        } finally {
            if (wav != null) {
                try {
                    Files.deleteIfExists(wav);
                } catch (Exception ignored) {
                }
            }
        }
    }

    static boolean available() {
        return WINDOWS;
    }

    static String scriptFor(String text, String wavPath, int pitchHz) {
        String payload = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        String escapedPath = wavPath.replace("'", "''");
        String gender = pitchHz >= 160 ? "Female" : "Male";
        return """
                Add-Type -AssemblyName System.Speech
                $bytes = [Convert]::FromBase64String('%s')
                $text = [Text.Encoding]::UTF8.GetString($bytes)
                $synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
                try {
                  $synth.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::%s) | Out-Null
                } catch {}
                $synth.Rate = 1
                $synth.Volume = 100
                $synth.SetOutputToWaveFile('%s')
                $synth.Speak($text)
                $synth.Dispose()
                """.formatted(payload, gender, escapedPath);
    }

    private static String powershellExe() {
        String root = System.getenv("SystemRoot");
        if (root != null && !root.isBlank()) {
            return root + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
        }
        return "powershell.exe";
    }
}
