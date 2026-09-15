package tn.naizo.smartvillagers.chat;

import tn.naizo.smartvillagers.ActivationMode;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;

public final class ActivationDetector {
    private ActivationDetector() {
    }

    public static boolean isActivated(ActivationMode mode, String message, MessageSignals signals, boolean lookingAtVillager) {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        String text = message == null ? "" : message;

        return switch (mode) {
            case SMART -> smartActivate(text, signals, lookingAtVillager, config);
            case LOOK_AT -> lookingAtVillager;
            case PREFIX -> {
                String prefix = config.chatPrefix();
                yield !prefix.isEmpty() && text.startsWith(prefix);
            }
            case NAME -> signals.nameMention();
            case ALWAYS_NEARBY -> true;
        };
    }

    private static boolean smartActivate(String message, MessageSignals signals, boolean lookingAtVillager,
                                         SmartVillagersConfig.Snapshot config) {
        String prefix = config.chatPrefix();
        if (config.requirePrefix()) {
            if (prefix.isEmpty() || !message.startsWith(prefix)) {
                return false;
            }
            message = message.substring(prefix.length()).trim();
            if (message.isEmpty()) {
                return false;
            }
        } else if (!prefix.isEmpty() && message.startsWith(prefix)) {
            return true;
        }

        if (signals.nameMention()) {
            return true;
        }
        if (lookingAtVillager && signals.conversational()) {
            return true;
        }
        if (lookingAtVillager && message.length() <= 48) {
            return true;
        }
        return signals.greeting() || signals.question() || signals.farewell();
    }

    public static String stripPrefix(String message) {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        String prefix = config.chatPrefix();
        if (prefix != null && !prefix.isEmpty() && message.startsWith(prefix)) {
            return message.substring(prefix.length()).trim();
        }
        return message;
    }
}
