package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.stevekung.replayfov.ReplayFOV;

import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer
{
    @ModifyVariable(method = "getFov", at = @At(value = "STORE", ordinal = 1), index = 4, ordinal = 0)
    private double useFloatFov(double defaultValue)
    {
        if (ReplayFOV.fov != null)
        {
            return ReplayFOV.fov;
        }
        return defaultValue;
    }

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "java/lang/Math.max(DD)D"), index = 1)
    private double getFloatFov(double defaultValue)
    {
        if (ReplayFOV.fov != null)
        {
            return ReplayFOV.fov;
        }
        return defaultValue;
    }
}