package com.stevekung.replayfov.mixin;

import java.util.Set;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.replaymod.lib.de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import com.replaymod.replay.gui.overlay.GuiEditMarkerPopup;
import com.replaymod.replay.gui.overlay.GuiMarkerTimeline;
import com.replaymod.replaystudio.data.Marker;

@Mixin(value = GuiMarkerTimeline.class, remap = false)
public abstract class MixinGuiMarkerTimeline extends AbstractGuiTimeline<GuiMarkerTimeline>
{
    @Shadow
    Set<Marker> markers;

    @Shadow
    @Final
    Consumer<Set<Marker>> saveMarkers;

    @Shadow
    abstract Marker getMarkerAt(int mouseX, int mouseY);

    @Redirect(method = "mouseClick", at = @At(value = "INVOKE", target = "com/replaymod/replay/gui/overlay/GuiEditMarkerPopup.open()V"))
    private void newMarkerPopup(GuiEditMarkerPopup popup, ReadablePoint position, int button)
    {
        var marker = this.getMarkerAt(position.getX(), position.getY());

        new GuiEditMarkerPopup(this.getContainer(), marker, updatedMarker ->
        {
            this.markers.remove(marker);
            this.markers.add(updatedMarker);
            this.saveMarkers.accept(this.markers);
        }).open();
    }
}