package me.radoje17.plugindownloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class PluginDownloader extends JavaPlugin {

    private boolean restart = false;

    private static PluginDownloader pluginDownloader;

    public static PluginDownloader getInstance() {
        return pluginDownloader;
    }

    private File file;
    FileConfiguration data;

    public FileConfiguration getConfig() {
        return data;
    }

    public void saveConfig() {
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        new PluginDownloaderUtils();
        this.pluginDownloader = this;

        file = new File(getDataFolder() + "/config.yml");
        if (!file.exists()) {
            saveResource("config.yml", false);
        }

        data = YamlConfiguration.loadConfiguration(file);
        Bukkit.getWorldContainer();

        if (data.isList("one-time-plugins")) {
            for (Object o : data.getList("one-time-plugins")) {
                if (o instanceof String) {
                    if (!new File("./plugins/" + o).exists()) {
                        PluginDownloaderUtils.downloadPlugin("" + o, false);
                        if (((String) o).endsWith(".jar")) {
                            restart = true;
                        }
                    }
                }
            }
            if (restart) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                return;
            }
        }
        if (data.isList("plugins")) {
            for (Object o : data.getList("plugins")) {
                if (o instanceof String) {
                    if (Bukkit.getPluginManager().getPlugin("" + o) == null) {
                        PluginDownloaderUtils.downloadPlugin("" + o, true);
                    }
                }
            }
        }
        PluginDownloaderUtils.loadPlugins();
        //PluginDownloaderUtils.downloadFile("http://theparca.de:2500/nesto.jar", getDataFolder() + "/nesto.jar");
    }

    @Override
    public void onDisable() {
        if (!restart && data.isList("plugins")) {
            for (Object o : data.getList("plugins")) {
            //for (File f : PluginDownloaderUtils.plugins) {
                /*if (f.exists()) {
                    String pluginName = f.getName().replaceAll(".jar", "");
                    if (Bukkit.getPluginManager().getPlugin(pluginName) != null)
                        PluginDownloaderUtils.unload(Bukkit.getPluginManager().getPlugin(pluginName));
                    try {
                        Files.delete(Paths.get(f.toString()));
                        getLogger().info("Plugin " + pluginName + " has been deleted successfully.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }*/
                if (o instanceof String && !((String) o).contains("/") && ((String) o).endsWith(".jar")) {
                    getLogger().info("Trying to delete " + o + ".");
                    String name = ((String) o).split(".jar")[0];
                    if (Bukkit.getPluginManager().getPlugin("" + name) != null) {
                        PluginDownloaderUtils.unload(Bukkit.getPluginManager().getPlugin("" + name));
                    }
                    try {
                        Files.delete(Paths.get("./plugins/" + o));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getLogger().info("Plugin " + o + " has been deleted successfully.");
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("plugindownloader")) {
            if (args.length == 0) {
                sender.sendMessage("Plugin Downloader" + ChatColor.BLUE + " v1.0 " + ChatColor.WHITE + "by " + ChatColor.BLUE +"Radoje17");
                sender.sendMessage("Type " + ChatColor.BLUE + "/" + label + " help" + ChatColor.WHITE + " for list of commands.");
                return false;
            }

            if (args[0].equalsIgnoreCase("download") || args[0].equalsIgnoreCase("redownload")) {
                if (args.length == 1) {
                    if (data.isList("one-time-plugins")) {
                        for (Object o : data.getList("one-time-plugins")) {
                            if (o instanceof String) {
                                PluginDownloaderUtils.downloadPlugin("" + o, false);
                            }
                        }

                    }
                    sender.sendMessage("One time plugins have been re-downloaded.");
                } else {
                    StringBuilder builder = new StringBuilder(args[1]);
                    for (int i = 2; i < args.length-1; i++)
                        builder.append(args[i]);
                    String s = args[args.length-1];
                    boolean deleteOnDisable = false;
                    if (s.equalsIgnoreCase("true"))
                        deleteOnDisable = true;
                    else if (!s.equalsIgnoreCase("false")) {
                        sender.sendMessage(ChatColor.DARK_GRAY + "[pd] " + ChatColor.RED + "Invalid syntax, please use:\n"
                                            + ChatColor.RED + "  /pd [plugin] [deleteOnDisable] <- (true/false)");
                        return false;
                    }
                    try {
                        PluginDownloaderUtils.downloadPlugin(builder.toString(), deleteOnDisable);
                        PluginDownloaderUtils.addToConfig(builder.toString(), deleteOnDisable);
                        sender.sendMessage(ChatColor.DARK_GRAY + "[pc] " + ChatColor.GRAY + "Plugin " + ChatColor.YELLOW + builder.toString() +
                                ChatColor.GRAY + " has been downloaded successfully.");
                        if (builder.toString().endsWith(".jar")) {
                            sender.sendMessage(ChatColor.DARK_GRAY + "[pc] " + ChatColor.GRAY + "Trying to load plugin " + ChatColor.YELLOW + builder.toString() +
                                    ChatColor.GRAY + ".");
                            Bukkit.getPluginManager().loadPlugin(new File("./plugins/" + builder.toString()));
                            sender.sendMessage(ChatColor.DARK_GRAY + "[pc] " + ChatColor.GRAY + "Plugin " + ChatColor.YELLOW + builder.toString() +
                                    ChatColor.GRAY + " has been loaded successfully.");
                        }
                    } catch (PluginNotDownloadedException e) {
                        sender.sendMessage(ChatColor.DARK_GRAY + "[pc] " + ChatColor.RED + e.getMessage());
                    } catch (InvalidDescriptionException | InvalidPluginException e) {
                        sender.sendMessage(ChatColor.DARK_GRAY + "[pc] " + ChatColor.RED + "Plugin " + builder.toString() + " could not be loaded.");
                    }
                }
                //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                return false;
            }
        }
        return false;
    }
}

/*for (Object o : data.getList("one-time-plugins")) {
                        if (o instanceof String && !((String) o).contains("/") && ((String) o).endsWith(".jar")) {
                            getLogger().info("Trying to delete " + o + ".");
                            String name = ((String) o).split(".")[0];
                            if (Bukkit.getPluginManager().getPlugin("" + name) != null) {
                                PluginDownloaderUtils.unload(Bukkit.getPluginManager().getPlugin("" + name));
                            }
                            if (new File("./plugins/" + o).exists()) {
                                try {
                                    Files.delete(Paths.get("./plugins/" + o));
                                    getLogger().info("Plugin " + o + " has been deleted successfully.");
                                } catch (IOException e) {
                                    getLogger().severe("File " + o + " could not be deleted: " + e.getLocalizedMessage());
                                }
                            }
                        }
                    }*/
