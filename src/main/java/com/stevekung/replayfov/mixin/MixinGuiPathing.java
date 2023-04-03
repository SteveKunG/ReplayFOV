package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.gui.GuiPathing;
import com.stevekung.replayfov.FovPositionKeyframe;
import com.stevekung.replayfov.GuiEditKeyframe;
import com.stevekung.replayfov.ReplayFov;

import net.minecraft.client.Minecraft;

@Mixin(value = GuiPathing.class, remap = false)
public abstract class MixinGuiPathing
{
    @Shadow
    @Final
    ReplayModSimplePathing mod;

    /**
     * @author SteveKunG
     * @reason Use new GuiEditKeyframe
     */
    @Overwrite
    public void openEditKeyframePopup(SPTimeline.SPPath path, long time)
    {
        var getThis = GuiPathing.class.cast(this);

        if (!getThis.loadEntityTracker(() -> this.openEditKeyframePopup(path, time)))
        {
            return;
        }

        var keyframe = this.mod.getCurrentTimeline().getKeyframe(path, time);

        if (keyframe.getProperties().contains(SpectatorProperty.PROPERTY))
        {
            new GuiEditKeyframe.Spectator(getThis, path, keyframe.getTime()).open();
        }
        else if (keyframe.getProperties().contains(CameraProperties.POSITION))
        {
            new GuiEditKeyframe.Position(getThis, path, keyframe.getTime()).open();
        }
        else
        {
            new GuiEditKeyframe.Time(getThis, path, keyframe.getTime()).open();
        }
    }

    @Redirect(method = "toggleKeyframe", at = @At(value = "INVOKE", target = "com/replaymod/simplepathing/SPTimeline.addPositionKeyframe(JDDDFFFI)V"))
    private void newMarkerPopup(SPTimeline timeline, long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, int spectated)
    {
        ((FovPositionKeyframe)timeline).addPositionKeyframe(time, posX, posY, posZ, yaw, pitch, roll, ReplayFov.fov == null ? Minecraft.getInstance().options.fov().get() : ReplayFov.fov, spectated);
    }
}