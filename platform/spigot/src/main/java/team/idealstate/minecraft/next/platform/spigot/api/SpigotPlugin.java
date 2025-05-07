/*
 *    Copyright 2025 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.minecraft.next.platform.spigot.api;

import java.io.File;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import team.idealstate.minecraft.next.common.command.Command;
import team.idealstate.minecraft.next.common.context.Bean;
import team.idealstate.minecraft.next.common.context.Context;
import team.idealstate.minecraft.next.common.context.ContextHolder;
import team.idealstate.minecraft.next.common.context.ContextLifecycle;
import team.idealstate.minecraft.next.common.context.annotation.component.Controller;
import team.idealstate.minecraft.next.common.context.annotation.component.Subscriber;
import team.idealstate.minecraft.next.common.eventbus.EventBus;
import team.idealstate.minecraft.next.platform.spigot.api.command.SpigotCommand;
import team.idealstate.minecraft.next.platform.spigot.api.context.factory.SpigotSubscriberBeanFactory;
import team.idealstate.minecraft.next.platform.spigot.api.placeholder.Placeholder;
import team.idealstate.minecraft.next.platform.spigot.api.placeholder.SpigotPlaceholderExpansion;

public abstract class SpigotPlugin extends JavaPlugin implements ContextHolder, ContextLifecycle {

    private final Context context = Context.of(this, this, EventBus.instance());

    public SpigotPlugin() {
        context.initialize();
    }

    protected SpigotPlugin(
            JavaPluginLoader loader,
            PluginDescriptionFile description,
            File dataFolder,
            File file) {
        super(loader, description, dataFolder, file);
        context.initialize();
    }

    @Override
    public final String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public final void onLoad() {
        super.onLoad();
        context.setBeanFactory(Subscriber.class, new SpigotSubscriberBeanFactory());
        context.load();
    }

    @Override
    public final void onEnable() {
        super.onEnable();
        context.enable();
        registerCommands();
        PluginManager pluginManager = getServer().getPluginManager();
        hookPlaceholderAPI(pluginManager);
        registerEventListeners(pluginManager);
    }

    @Override
    public final void onDisable() {
        super.onDisable();
        context.disable();
        context.destroy();
    }

    private void registerCommands() {
        List<Bean<Controller, Command>> beans =
                context.getBeans(Controller.class, Command.class);
        if (beans.isEmpty()) {
            return;
        }
        for (Bean<Controller, Command> bean : beans) {
            Controller metadata = bean.getMetadata();
            String name = metadata.value();
            PluginCommand pluginCommand = getCommand(name);
            if (pluginCommand == null) {
                continue;
            }
            Command instance = bean.getInstance();
            SpigotCommand spigotCommand = SpigotCommand.of(name, instance);
            pluginCommand.setExecutor(spigotCommand);
            pluginCommand.setTabCompleter(spigotCommand);
        }
    }

    private void hookPlaceholderAPI(PluginManager pluginManager) {
        pluginManager.registerEvents(new PlaceholderAPIHook(context), this);
    }

    @RequiredArgsConstructor
    private static final class PlaceholderAPIHook implements Listener {
        private static final String PLACEHOLDER_API = "PlaceholderAPI";
        @NonNull private final Context context;

        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlaceholderAPIEnabled(PluginEnableEvent event) {
            if (PLACEHOLDER_API.equals(event.getPlugin().getName())) {
                List<Bean<Controller, Placeholder>> beans =
                        context.getBeans(Controller.class, Placeholder.class);
                if (beans.isEmpty()) {
                    return;
                }
                String author = context.getName();
                String version = context.getVersion();
                for (Bean<Controller, Placeholder> bean : beans) {
                    Controller metadata = bean.getMetadata();
                    Placeholder instance = bean.getInstance();
                    SpigotPlaceholderExpansion.of(metadata.value(), author, version, instance)
                            .register();
                }
            }
        }
    }

    private void registerEventListeners(PluginManager pluginManager) {
        List<Bean<Subscriber, Listener>> beans =
                context.getBeans(Subscriber.class, Listener.class);
        if (beans.isEmpty()) {
            return;
        }
        for (Bean<Subscriber, Listener> bean : beans) {
            Listener instance = bean.getInstance();
            pluginManager.registerEvents(instance, this);
        }
    }
}
