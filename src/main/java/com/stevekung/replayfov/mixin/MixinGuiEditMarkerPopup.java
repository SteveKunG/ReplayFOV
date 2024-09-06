package com.stevekung.replayfov.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiContainer;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiPanel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiLabel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiNumberField;
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

    @Unique
    private final GuiNumberField fovField = newGuiNumberField().setPrecision(5);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(GuiContainer<?> container, Marker marker, Consumer<Marker> onSave, CallbackInfo info)
    {
        this.fovField.setValue(MarkerExtender.class.cast(marker).getFov());
    }

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "com/replaymod/lib/de/johni0702/minecraft/gui/container/GuiPanel$GuiPanelBuilder.build()Lcom/replaymod/lib/de/johni0702/minecraft/gui/container/GuiPanel;"))
    private GuiPanel input(GuiPanel.GuiPanelBuilder builder)
    {
        //@formatter:off
        return builder
                .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.fov"), new GridLayout.Data(0, 0.5))
                .with(this.fovField, new GridLayout.Data(1, 0.5))
                .build();
        //@formatter:on
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