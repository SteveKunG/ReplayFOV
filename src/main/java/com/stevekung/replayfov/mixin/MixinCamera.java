package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.stevekung.replayfov.ReplayFOV;

import net.minecraft.client.Camera;

@Mixin(Camera.class)
public class MixinCamera
{
    @ModifyVariable(method = "getNearPlane", at = @At(value = "STORE"), index = 4, ordinal = 1)
    private double useFloatFov(double defaultValue)
    {
        if (ReplayFOV.fov != null)
        {
            return Math.tan(ReplayFOV.fov * (float)(Math.PI / 180.0) / 2.0) * 0.05F;
        }
        return defaultValue;
    }
}