package com.stevekung.replayfov.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.sugar.Local;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiContainer;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiPanel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiLabel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiNumberField;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiTextField;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.GridLayout;
import com.replaymod.replay.gui.overlay.GuiEditMarkerPopup;
import com.replaymod.replaystudio.data.Marker;
import com.stevekung.replayfov.extender.GuiEditMarkerPopupExtender;
import com.stevekung.replayfov.extender.MarkerExtender;

@Mixin(value = GuiEditMarkerPopup.class, remap = false)
public class MixinGuiEditMarkerPopup implements GuiEditMarkerPopupExtender
{
    @Shadow
    static GuiNumberField newGuiNumberField()
    {
        throw new AssertionError();
    }

    @Shadow
    @Final
    GuiTextField nameField;

    @Shadow
    @Final
    GuiNumberField timeField;

    @Shadow
    @Final
    GuiNumberField xField;

    @Shadow
    @Final
    GuiNumberField yField;

    @Shadow
    @Final
    GuiNumberField zField;

    @Shadow
    @Final
    GuiNumberField yawField;

    @Shadow
    @Final
    GuiNumberField pitchField;

    @Shadow
    @Final
    GuiNumberField rollField;

    @Shadow
    @Final
    @Mutable
    GuiPanel inputs;

    @Unique
    private final GuiNumberField fovField = newGuiNumberField().setPrecision(5);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(GuiContainer<?> container, Marker marker, Consumer<Marker> onSave, CallbackInfo info)
    {
        //@formatter:off
        this.inputs = GuiPanel.builder().layout(new GridLayout().setColumns(2).setSpacingX(7).setSpacingY(3))
                .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.markername"), new GridLayout.Data(0.0, 0.5))
                .with(this.nameField, new GridLayout.Data(1.0, 0.5)).with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.timestamp"), new GridLayout.Data(0.0, 0.5))
                .with(this.timeField, new GridLayout.Data(1.0, 0.5)).with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.xpos"), new GridLayout.Data(0.0, 0.5))
                .with(this.xField, new GridLayout.Data(1.0, 0.5)).with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.ypos"), new GridLayout.Data(0.0, 0.5))
                .with(this.yField, new GridLayout.Data(1.0, 0.5)).with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.zpos"), new GridLayout.Data(0.0, 0.5))
                .with(this.zField, new GridLayout.Data(1.0, 0.5)).with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camyaw"), new GridLayout.Data(0.0, 0.5))
                .with(this.yawField, new GridLayout.Data(1.0, 0.5)).with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.campitch"), new GridLayout.Data(0.0, 0.5))
                .with(this.pitchField, new GridLayout.Data(1.0, 0.5)).with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camroll"), new GridLayout.Data(0.0, 0.5))
                .with(this.rollField, new GridLayout.Data(1.0, 0.5))
                .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.fov"), new GridLayout.Data(0, 0.5))
                .with(this.fovField, new GridLayout.Data(1, 0.5))
                .build();
        //@formatter:on

        this.fovField.setValue(MarkerExtender.class.cast(marker).getFov());
    }

    @Override
    public GuiNumberField getFovField()
    {
        return this.fovField;
    }

    @Mixin(targets = "com.replaymod.replay.gui.overlay.GuiEditMarkerPopup$1", remap = false)
    public static class OnSave
    {
        @Shadow(aliases = "this$0")
        @Final
        GuiEditMarkerPopup this$0;

        @Inject(method = "run", at = @At(value = "INVOKE", target = "java/util/function/Consumer.accept(Ljava/lang/Object;)V"))
        private void run(CallbackInfo info, @Local Marker marker)
        {
            MarkerExtender.class.cast(marker).setFov(((GuiEditMarkerPopupExtender) this.this$0).getFovField().getFloat());
        }
    }
}