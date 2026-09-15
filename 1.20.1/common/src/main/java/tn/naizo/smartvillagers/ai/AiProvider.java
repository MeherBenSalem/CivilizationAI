package tn.naizo.smartvillagers.ai;

import java.util.concurrent.CompletableFuture;

public interface AiProvider {
    String name();

    boolean isConfigured();

    CompletableFuture<AiResponse> complete(AiRequest request);
}
