package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.replaymod.core.versions.MCVer;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.gui.GuiPathing;
import com.stevekung.replayfov.extender.SPTimelineExtender;

@Mixin(value = GuiPathing.class, remap = false)
public abstract class MixinGuiPathing
{
    @WrapOperation(method = "toggleKeyframe", at = @At(value = "INVOKE", target = "com/replaymod/simplepathing/SPTimeline.addPositionKeyframe(JDDDFFFI)V"))
    private void newMarkerPopup(SPTimeline timeline, long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, int spectated, Operation<Void> operation)
    {
        ((SPTimelineExtender) timeline).addPositionKeyframe(time, posX, posY, posZ, yaw, pitch, roll, (float) MCVer.getMinecraft().options.fov, spectated);
    }
}