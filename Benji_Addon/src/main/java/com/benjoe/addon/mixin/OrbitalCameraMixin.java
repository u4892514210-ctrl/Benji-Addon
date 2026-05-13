package com.benjoe.addon.mixin;

import com.benjoe.addon.BenjoeAddon;
import com.benjoe.addon.modules.OrbitalCamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class OrbitalCameraMixin {

    @Inject(method = "update", at = @At("TAIL"))
    private void onCameraUpdate(CallbackInfo ci) {
        OrbitalCamera mod = Modules.get().get(OrbitalCamera.class);
        if (mod == null || !mod.isActive()) return;

        Vec3d pos = mod.getCameraPos();
        if (pos == null) return;

        Camera camera = (Camera)(Object)this;
        // Reflect-set the camera position and rotation
        try {
            // pos field
            var posField = Camera.class.getDeclaredField("pos");
            posField.setAccessible(true);
            posField.set(camera, pos);

            // yaw field
            var yawField = Camera.class.getDeclaredField("yaw");
            yawField.setAccessible(true);
            yawField.set(camera, mod.getCamYaw());

            // pitch field
            var pitchField = Camera.class.getDeclaredField("pitch");
            pitchField.setAccessible(true);
            pitchField.set(camera, mod.getCamPitch());

        } catch (Exception e) {
            BenjoeAddon.LOG.error("OrbitalCamera mixin error: {}", e.getMessage());
        }
    }
}
