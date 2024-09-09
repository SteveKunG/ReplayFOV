package com.stevekung.replayfov.mixin;

import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiElement;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiLabel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiNumberField;
import com.replaymod.lib.de.johni0702.minecraft.gui.function.Focusable;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.gui.GuiEditKeyframe;
import com.replaymod.simplepathing.gui.GuiPathing;
import com.stevekung.replayfov.ReplayFov;
import com.stevekung.replayfov.extender.SPTimelineExtender;

@Mixin(value = GuiEditKeyframe.Position.class, remap = false)
public abstract class MixinGuiEditKeyframe_Position extends GuiEditKeyframe<GuiEditKeyframe.Position>
{
    MixinGuiEditKeyframe_Position()
    {
        super(null, null, 0, null);
    }

    @Unique
    private final GuiNumberField fovField = new GuiNumberField().setValidateOnFocusChange(true).setSize(60, 20).setPrecision(5);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setFovTextField(GuiPathing gui, SPTimeline.SPPath path, long keyframe, CallbackInfo info)
    {
        this.keyframe.getValue(ReplayFov.FOV).ifPresent(val ->
        {
            double fov;

            if (val.getLeft() < 0)
            {
                fov = Math.toDegrees(Math.atan(1 / val.getLeft()) + Math.PI);
            }
            else
            {
                fov = Math.toDegrees(Math.atan(1 / val.getLeft()));
            }

            this.fovField.setValue(fov);
        });
    }

    @WrapOperation(method = "save", at = @At(value = "INVOKE", target = "com/replaymod/simplepathing/SPTimeline.updatePositionKeyframe(JDDDFFF)Lcom/replaymod/replaystudio/pathing/change/Change;"))
    private Change updatePositionKeyframeWithFov(SPTimeline spTimeline, long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, Operation<Change> operation)
    {
        return ((SPTimelineExtender) spTimeline).updatePositionKeyframe(time, posX, posY, posZ, yaw, pitch, roll, (float) (1 / Math.tan(Math.toRadians(this.fovField.setPrecision(11).getFloat()))));
    }

    @ModifyArg(method = "<init>", at = @At(
            value = "INVOKE",
            target = "com/replaymod/lib/de/johni0702/minecraft/gui/container/GuiPanel.addElements(Lcom/replaymod/lib/de/johni0702/minecraft/gui/layout/LayoutData;[Lcom/replaymod/lib/de/johni0702/minecraft/gui/element/GuiElement;)Lcom/replaymod/lib/de/johni0702/minecraft/gui/container/AbstractGuiContainer;",
            ordinal = 0),
            index = 1)
    private GuiElement<?>[] addFovElement(GuiElement<?>[] elements)
    {
        return ArrayUtils.addAll(elements, new GuiLabel().setI18nText("replaymod.gui.editkeyframe.fov"), this.fovField);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "com/replaymod/lib/de/johni0702/minecraft/gui/utils/Utils.link([Lcom/replaymod/lib/de/johni0702/minecraft/gui/function/Focusable;)V"))
    private Focusable<?>[] addFovFocusable(Focusable<?>[] focusables)
    {
        return ArrayUtils.add(focusables, this.fovField);
    }
}