package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraEntity;
import com.stevekung.replayfov.FovCamera;
import com.stevekung.replayfov.ReplayFOV;

@Mixin(CameraEntity.class)
public class MixinCameraEntity implements FovCamera
{
    @Override
    public void setFOV(Float fov)
    {
        ReplayFOV.fov = fov;
        MCVer.getMinecraft().levelRenderer.needsUpdate();
    }
}