package com.lotusplugins.sheriff;

public class SheriffPlugin extends JavaPlugin {

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);

    }

}
