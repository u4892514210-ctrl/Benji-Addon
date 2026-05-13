package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class OrbitalCamera extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("How far the camera pulls back from your body (blocks).")
        .defaultValue(20.0).min(5.0).sliderMax(100.0)
        .build()
    );

    private final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov")
        .description("Field of view. 110 is wide, 150 is extreme fisheye-style.")
        .defaultValue(120.0).min(40.0).sliderMax(160.0)
        .build()
    );

    private final Setting<Double> sensitivity = sgGeneral.add(new DoubleSetting.Builder()
        .name("look-sensitivity")
        .description("Mouse sensitivity multiplier while orbital cam is active.")
        .defaultValue(1.0).min(0.1).sliderMax(3.0)
        .build()
    );

    private final Setting<Boolean> freezePlayer = sgGeneral.add(new BoolSetting.Builder()
        .name("freeze-player")
        .description("Stop your player from moving while looking around.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smoothFollow = sgGeneral.add(new BoolSetting.Builder()
        .name("smooth-follow")
        .description("Camera smoothly lerps to the target position.")
        .defaultValue(true)
        .build()
    );

    // Camera yaw/pitch independent from player
    private float camYaw;
    private float camPitch;
    private Vec3d camPos;
    private Vec3d frozenPos;
    private int savedFov;

    public OrbitalCamera() {
        super(BenjoeAddon.CATEGORY, "orbital-camera",
            "Pulls the camera far back from your body. You can look anywhere freely. " +
            "Your player stays frozen. Great for scouting and scanning.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        camYaw   = mc.player.getYaw();
        camPitch = mc.player.getPitch();
        camPos   = mc.player.getEyePos();
        frozenPos = mc.player.getPos();
        savedFov = mc.options.getFov().getValue();
        mc.options.getFov().setValue((int) Math.round(fov.get()));
    }

    @Override
    public void onDeactivate() {
        mc.options.getFov().setValue(savedFov);
        camPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Freeze player body
        if (freezePlayer.get() && frozenPos != null) {
            mc.player.setPosition(frozenPos);
            mc.player.setVelocity(Vec3d.ZERO);
        }

        // Sync camera yaw/pitch from player mouse look
        camYaw   = mc.player.getYaw();
        camPitch = mc.player.getPitch();

        // Clamp pitch so camera doesn't flip
        camPitch = MathHelper.clamp(camPitch, -89.9f, 89.9f);

        // Compute offset: pull backwards and upwards from the direction we're looking
        double yawRad   = Math.toRadians(camYaw);
        double pitchRad = Math.toRadians(camPitch);
        double dist     = distance.get();

        // Direction vector the camera looks along (forward vector)
        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);
        double dz =  Math.cos(yawRad) * Math.cos(pitchRad);

        // Camera sits BEHIND the player (opposite of forward vector)
        Vec3d target = mc.player.getEyePos().add(-dx * dist, -dy * dist, -dz * dist);

        if (smoothFollow.get() && camPos != null) {
            double alpha = 0.15; // lerp factor
            camPos = camPos.lerp(target, alpha);
        } else {
            camPos = target;
        }

        // FOV update if slider changed live
        int targetFov = (int) Math.round(fov.get());
        if (mc.options.getFov().getValue() != targetFov) {
            mc.options.getFov().setValue(targetFov);
        }
    }

    /**
     * Called by the mixin to override camera position each frame.
     * Returns the computed orbital position, or null if inactive.
     */
    public Vec3d getCameraPos() {
        return camPos;
    }

    public float getCamYaw()   { return camYaw;   }
    public float getCamPitch() { return camPitch; }
}
