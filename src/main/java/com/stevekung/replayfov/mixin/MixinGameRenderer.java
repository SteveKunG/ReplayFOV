package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.stevekung.replayfov.ReplayFov;

import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer
{
    @ModifyVariable(method = "getFov", at = @At(value = "STORE", ordinal = 1), index = 4, ordinal = 1)
    private float useFloatFov(float defaultValue)
    {
        if (ReplayFov.fov != null)
        {
            return ReplayFov.fov;
        }
        return defaultValue;
    }

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "java/lang/Math.max(FF)F"), index = 1)
    private float getFloatFov(float defaultValue)
    {
        if (ReplayFov.fov != null)
        {
            return ReplayFov.fov;
        }
        return defaultValue;
    }
}