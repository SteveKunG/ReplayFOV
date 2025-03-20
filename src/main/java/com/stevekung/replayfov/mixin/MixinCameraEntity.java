package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraEntity;
import com.stevekung.replayfov.extender.CameraEntityExtender;
import com.stevekung.replayfov.ReplayFov;

@Mixin(CameraEntity.class)
public class MixinCameraEntity implements CameraEntityExtender
{
    @Override
    public void replayfov$setFov(Float fov)
    {
        ReplayFov.fov = fov;
        MCVer.getMinecraft().levelRenderer.needsUpdate();
    }
}