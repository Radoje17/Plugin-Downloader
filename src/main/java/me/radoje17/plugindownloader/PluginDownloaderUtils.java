package me.radoje17.plugindownloader;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class PluginDownloaderUtils {

    public static ArrayList<File> plugins;

    public PluginDownloaderUtils() {
        plugins = new ArrayList<>();
    }

    private static String removeLast(String s) {

        String newString = "";

        String[] list = s.split("/");
        for (int i = 0; i < list.length-1; i++) {
            newString+=list[i];
        }

        return newString;
    }

    static void downloadPlugin(String name, boolean deleteOnDisable) {
        File file = new File(Bukkit.getWorldContainer() + "/plugins/" + name);
        new File("./plugins/" + removeLast(name)).mkdirs();
        try {
            PluginDownloader.getInstance().getLogger().info("Downloading " + name + ".");
            FileUtils.copyURLToFile(new URL(PluginDownloader.getInstance().getConfig().getString("download-server") + "/" + name), file);
        } catch (IOException e) {
            PluginDownloader.getInstance().getLogger().severe("Plugin " + name + " could not be downloaded: " + e.getLocalizedMessage() + ".");
            throw new PluginNotDownloadedException("Plugin " + name +  " could not be found on the server.");
        }

        PluginDownloader.getInstance().getLogger().info("Plugin " + name + " has been downloaded.");
        if (name.endsWith(".jar") && deleteOnDisable) {
            plugins.add(new File("./plugins/" + name));
        }
    }

    static void loadPlugins() {
        for (File f : plugins) {
            try {
                PluginDownloader.getInstance().getLogger().info("Trying to load " + f.getName() + ".");
                Bukkit.getPluginManager().loadPlugin(f);
            } catch (Exception e) {
                PluginDownloader.getInstance().getLogger().severe("Plugin " + f.getName() + " could not be loaded: " + e.getLocalizedMessage()  + ".");
                return;
            }

            PluginDownloader.getInstance().getLogger().info("Plugin " + f.getName() + " has been loaded successfully.");
        }
    }

    public static void addToConfig(String plugin, boolean oneTime) {
        String list = oneTime ? "one-time-plugins" : "plugins";
        String list2 = oneTime ? "plugins" : "one-time-plugins";
        List<String> lista = PluginDownloader.getInstance().data.getStringList(list);
        if (lista == null)
            lista = new ArrayList<>();
        List<String> lista2 = PluginDownloader.getInstance().data.getStringList(list2);
        if (lista2 != null && lista2.contains(plugin) || lista.contains(plugin)) {
            throw new PluginNotDownloadedException("Plugin " + plugin + " has already been downloaded.");
        }
        if (!lista.contains(plugin))
            lista.add(plugin);
        PluginDownloader.getInstance().data.set(list, lista);
        PluginDownloader.getInstance().saveConfig();
    }

    public static void unload(Plugin plugin) {
        String name = plugin.getName();
        PluginManager pluginManager = Bukkit.getPluginManager();
        SimpleCommandMap commandMap = null;
        List<Plugin> plugins = null;
        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;
        boolean reloadlisteners = true;
        if (pluginManager != null) {
            pluginManager.disablePlugin(plugin);
            try {
                Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
                pluginsField.setAccessible(true);
                plugins = (List<Plugin>)pluginsField.get(pluginManager);
                Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
                lookupNamesField.setAccessible(true);
                names = (Map<String, Plugin>)lookupNamesField.get(pluginManager);
                try {
                    Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                    listenersField.setAccessible(true);
                    listeners = (Map<Event, SortedSet<RegisteredListener>>)listenersField.get(pluginManager);
                } catch (Exception e) {
                    reloadlisteners = false;
                }
                Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                commandMap = (SimpleCommandMap)commandMapField.get(pluginManager);
                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                commands = (Map<String, Command>)knownCommandsField.get(commandMap);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                //return PlugMan.getInstance().getMessageFormatter().format("unload.failed", new Object[] { name });
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                //return PlugMan.getInstance().getMessageFormatter().format("unload.failed", new Object[] { name });
            }
        }
        pluginManager.disablePlugin(plugin);
        if (plugins != null && plugins.contains(plugin))
            plugins.remove(plugin);
        if (names != null && names.containsKey(name))
            names.remove(name);
        if (listeners != null && reloadlisteners)
            for (SortedSet<RegisteredListener> set : listeners.values()) {
                for (Iterator<RegisteredListener> it = set.iterator(); it.hasNext(); ) {
                    RegisteredListener value = it.next();
                    if (value.getPlugin() == plugin)
                        it.remove();
                }
            }
        if (commandMap != null)
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand c = (PluginCommand)entry.getValue();
                    if (c.getPlugin() == plugin) {
                        c.unregister((CommandMap)commandMap);
                        it.remove();
                    }
                }
            }
        ClassLoader cl = plugin.getClass().getClassLoader();
        if (cl instanceof URLClassLoader) {
            try {
                Field pluginField = cl.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(cl, (Object)null);
                Field pluginInitField = cl.getClass().getDeclaredField("pluginInit");
                pluginInitField.setAccessible(true);
                pluginInitField.set(cl, (Object)null);
            } catch (NoSuchFieldException|SecurityException|IllegalArgumentException|IllegalAccessException ex) {
                //Logger.getLogger(PluginUtil.class.getName()).log(Level.SEVERE, (String)null, ex);
            }
            try {
                ((URLClassLoader)cl).close();
            } catch (IOException ex) {
                //Logger.getLogger(PluginUtil.class.getName()).log(Level.SEVERE, (String)null, ex);
            }
        }
        System.gc();
    }
}
