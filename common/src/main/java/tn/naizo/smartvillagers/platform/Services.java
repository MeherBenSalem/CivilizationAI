package tn.naizo.smartvillagers.platform;

import tn.naizo.smartvillagers.Constants;

import java.util.ServiceLoader;

public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private Services() {
    }

    private static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No implementation found for " + type.getName() + " on " + Constants.MOD_ID));
    }
}
