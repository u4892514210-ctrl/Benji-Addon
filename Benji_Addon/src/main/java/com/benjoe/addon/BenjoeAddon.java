package com.benjoe.addon;

import com.benjoe.addon.modules.*;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenjoeAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("BenjoeAddon");
    public static final Category CATEGORY = new Category("Benjoe");

    @Override
    public void onInitialize() {
        LOG.info("Initializing BenjoeAddon");

        Modules.get().add(new BenjiESP());
        Modules.get().add(new ChestESP());
        Modules.get().add(new SpawnerESP());
        Modules.get().add(new OrbitalCamera());
        Modules.get().add(new CoordsLogger());
        Modules.get().add(new ChatAlerts());
        Modules.get().add(new PlayerList());
        Modules.get().add(new AntiHunger());
        Modules.get().add(new AutoTotem());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() { return "com.benjoe.addon"; }

    @Override
    public GithubRepo getRepo() { return new GithubRepo("yourusername", "BenjoeAddon"); }
}
