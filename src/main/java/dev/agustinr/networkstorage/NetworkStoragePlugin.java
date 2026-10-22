package dev.agustinr.networkstorage;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.agustinr.networkstorage.storage.OpenTerminalInteraction;
import dev.agustinr.networkstorage.system.NetworkBreakSystem;
import dev.agustinr.networkstorage.system.NetworkEnvironmentBreakSystem;
import dev.agustinr.networkstorage.system.NetworkPlaceSystem;

import javax.annotation.Nonnull;

public class NetworkStoragePlugin extends JavaPlugin {

    public NetworkStoragePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getCodecRegistry(Interaction.CODEC).register(
                OpenTerminalInteraction.TYPE, OpenTerminalInteraction.class, OpenTerminalInteraction.CODEC);
        getEntityStoreRegistry().registerSystem(new NetworkPlaceSystem());
        getEntityStoreRegistry().registerSystem(new NetworkBreakSystem());
        getEntityStoreRegistry().registerSystem(new NetworkEnvironmentBreakSystem());
        getLogger().atInfo().log("Network Storage cargado");
    }
}
