package com.benjoe.addon.modules;

import com.benjoe.addon.BenjoeAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class FreeCamera extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Camera movement speed.")
        .defaultValue(1.0)
        .min(0.1)
        .sliderMax(10.0)
        .build()
    );

    private final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov")
        .description("Field of view for the free camera (normal = 70, high = 110+).")
        .defaultValue(110.0)
        .min(30.0)
        .sliderMax(160.0)
        .build()
    );

    private final Setting<Boolean> noClip = sgGeneral.add(new BoolSetting.Builder()
        .name("no-clip")
        .description("Let the camera pass through blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> freezePlayer = sgGeneral.add(new BoolSetting.Builder()
        .name("freeze-player")
        .description("Freeze your actual player body while the camera is active.")
        .defaultValue(true)
        .build()
    );

    // Camera entity that the view will follow
    private Entity cameraEntity;
    private double savedFov;
    private Vec3d frozenPos;

    public FreeCamera() {
        super(BenjoeAddon.CATEGORY, "free-camera",
            "Detaches your camera from your body with a high FOV. " +
            "Use WASD + look to fly the camera around.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;

        // Save current FOV and apply ours
        savedFov = mc.options.getFov().getValue();
        mc.options.getFov().setValue((int) Math.round(fov.get()));

        // Spawn a camera entity at the player's current position
        cameraEntity = new net.minecraft.entity.decoration.ArmorStandEntity(
            mc.world,
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ()
        );
        cameraEntity.noClip = noClip.get();
        cameraEntity.setYaw(mc.player.getYaw());
        cameraEntity.setPitch(mc.player.getPitch());

        mc.world.addEntity(cameraEntity);
        mc.setCameraEntity(cameraEntity);

        if (freezePlayer.get()) {
            frozenPos = mc.player.getPos();
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null) return;

        // Restore camera to player
        mc.setCameraEntity(mc.player);

        // Restore FOV
        mc.options.getFov().setValue((int) Math.round(savedFov));

        // Remove the camera entity
        if (cameraEntity != null) {
            cameraEntity.discard();
            cameraEntity = null;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || cameraEntity == null) return;

        // Keep player frozen
        if (freezePlayer.get() && frozenPos != null) {
            mc.player.setPosition(frozenPos);
            mc.player.setVelocity(Vec3d.ZERO);
        }

        // Update noClip in case setting changed
        cameraEntity.noClip = noClip.get();

        // FOV update if setting changed mid-session
        int targetFov = (int) Math.round(fov.get());
        if (mc.options.getFov().getValue() != targetFov) {
            mc.options.getFov().setValue(targetFov);
        }

        // Move camera entity based on player input
        float yaw   = cameraEntity.getYaw();
        float pitch = cameraEntity.getPitch();
        double spd  = speed.get();

        double dx = 0, dy = 0, dz = 0;
        double yawRad = Math.toRadians(yaw);

        if (mc.options.forwardKey.isPressed()) {
            dx -= Math.sin(yawRad) * spd * Math.cos(Math.toRadians(pitch));
            dy -= Math.sin(Math.toRadians(pitch)) * spd;
            dz += Math.cos(yawRad) * spd * Math.cos(Math.toRadians(pitch));
        }
        if (mc.options.backKey.isPressed()) {
            dx += Math.sin(yawRad) * spd * Math.cos(Math.toRadians(pitch));
            dy += Math.sin(Math.toRadians(pitch)) * spd;
            dz -= Math.cos(yawRad) * spd * Math.cos(Math.toRadians(pitch));
        }
        if (mc.options.leftKey.isPressed()) {
            dx -= Math.cos(yawRad) * spd;
            dz -= Math.sin(yawRad) * spd;
        }
        if (mc.options.rightKey.isPressed()) {
            dx += Math.cos(yawRad) * spd;
            dz += Math.sin(yawRad) * spd;
        }
        if (mc.options.jumpKey.isPressed())  dy += spd;
        if (mc.options.sneakKey.isPressed()) dy -= spd;

        cameraEntity.setPosition(
            cameraEntity.getX() + dx * 0.1,
            cameraEntity.getY() + dy * 0.1,
            cameraEntity.getZ() + dz * 0.1
        );

        // Mirror mouse look to camera entity
        cameraEntity.setYaw(mc.player.getYaw());
        cameraEntity.setPitch(mc.player.getPitch());
    }
}
