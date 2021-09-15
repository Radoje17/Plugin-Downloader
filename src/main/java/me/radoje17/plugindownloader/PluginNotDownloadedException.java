package me.radoje17.plugindownloader;

public class PluginNotDownloadedException extends RuntimeException {

    private String message;

    public PluginNotDownloadedException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
