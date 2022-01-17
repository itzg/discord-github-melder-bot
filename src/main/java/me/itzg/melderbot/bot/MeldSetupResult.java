package me.itzg.melderbot.bot;

public record MeldSetupResult(boolean needingMeld, String meldUrl) {

    public static MeldSetupResult needsMeld(String url) {
        return new MeldSetupResult(true, url);
    }

    public static MeldSetupResult noMeldNeeded() {
        return new MeldSetupResult(false, null);
    }
}
