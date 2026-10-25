package tn.naizo.smartvillagers.ai;

public record AiResponse(String text, boolean ok, String error) {
    public static AiResponse success(String text) {
        return new AiResponse(text, true, null);
    }

    public static AiResponse failure(String error) {
        return new AiResponse("", false, error);
    }
}
