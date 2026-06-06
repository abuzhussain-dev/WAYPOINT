package com.example.waypointtracker;

import net.fabricmc.api.ClientModInitializer;

public class StandaloneWaypointTracker implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WaypointTracker.getInstance().init();
    }
}
