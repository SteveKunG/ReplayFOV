package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.google.common.base.Preconditions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.replaymod.core.versions.MCVer;
import com.replaymod.lib.org.apache.commons.lang3.tuple.Triple;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.replaystudio.pathing.change.*;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.SPTimeline;
import com.stevekung.replayfov.ReplayFov;
import com.stevekung.replayfov.extender.SPTimelineExtender;

@Mixin(value = SPTimeline.class, remap = false)
public abstract class MixinSPTimeline implements SPTimelineExtender
{
    @Shadow
    @Final
    Path positionPath;

    @Shadow
    @Final
    Timeline timeline;

    @Shadow
    abstract Interpolator createDefaultInterpolator();

    @Shadow
    abstract Change updateInterpolators();

    @Shadow
    abstract Change updateSpectatorPositions();

    /**
     * @author SteveKunG
     * @reason Use new FOV arg
     */
    @Overwrite
    public void addPositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, int spectated)
    {
        this.addPositionKeyframe(time, posX, posY, posZ, yaw, pitch, roll, (float) MCVer.getMinecraft().options.fov, spectated);
    }

    /**
     * @author SteveKunG
     * @reason Use new FOV arg
     */
    @Overwrite
    public Change updatePositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll)
    {
        return this.updatePositionKeyframe(time, posX, posY, posZ, yaw, pitch, roll, (float) MCVer.getMinecraft().options.fov);
    }

    @Override
    @SuppressWarnings("all")
    public Change updatePositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, float fov)
    {
        //@formatter:off
        ReplayModSimplePathing.LOGGER.debug("Updating position keyframe at {} to pos {}/{}/{} rot {}/{}/{} fov {}",
                time, posX, posY, posZ, yaw, pitch, roll, fov);

        Keyframe keyframe = positionPath.getKeyframe(time);

        Preconditions.checkState(keyframe != null, "Keyframe does not exists");
        Preconditions.checkState(!keyframe.getValue(SpectatorProperty.PROPERTY).isPresent(), "Cannot update spectator keyframe");

        Change change = UpdateKeyframeProperties.create(positionPath, keyframe)
                .setValue(CameraProperties.POSITION, Triple.of(posX, posY, posZ))
                .setValue(CameraProperties.ROTATION, Triple.of(yaw, pitch, roll))
                .setValue(ReplayFov.FOV, Triple.of(fov, fov, fov))
                .done();
        change.apply(timeline);
        return change;
        //@formatter:on
    }

    @Override
    @SuppressWarnings("all")
    public void addPositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, float fov, int spectated)
    {
        //@formatter:off
        ReplayModSimplePathing.LOGGER.debug("Adding position keyframe at {} pos {}/{}/{} rot {}/{}/{} fov {} entId {}",
                time, posX, posY, posZ, yaw, pitch, roll, fov, spectated);

        Path path = positionPath;

        Preconditions.checkState(positionPath.getKeyframe(time) == null, "Keyframe already exists");

        Change change = AddKeyframe.create(path, time);
        change.apply(timeline);
        Keyframe keyframe = path.getKeyframe(time);

        UpdateKeyframeProperties.Builder builder = UpdateKeyframeProperties.create(path, keyframe);
        builder.setValue(CameraProperties.POSITION, Triple.of(posX, posY, posZ));
        builder.setValue(CameraProperties.ROTATION, Triple.of(yaw, pitch, roll));
        // Add FOV
        builder.setValue(ReplayFov.FOV, Triple.of((float) (1 / Math.tan(Math.toRadians(fov))), 0f, 0f));
        if (spectated != -1) {
            builder.setValue(SpectatorProperty.PROPERTY, spectated);
        }
        UpdateKeyframeProperties updateChange = builder.done();
        updateChange.apply(timeline);
        change = CombinedChange.createFromApplied(change, updateChange);

        // If this new keyframe formed the first segment of the path
        if (path.getSegments().size() == 1) {
            // then create an initial interpolator of default type
            PathSegment segment = path.getSegments().iterator().next();
            Interpolator interpolator = createDefaultInterpolator();
            SetInterpolator setInterpolator = SetInterpolator.create(segment, interpolator);
            setInterpolator.apply(timeline);
            change = CombinedChange.createFromApplied(change, setInterpolator);
        }

        // Update interpolators for spectator keyframes
        // while this is overkill, it is far simpler than updating differently for every possible case
        change = CombinedChange.createFromApplied(change, updateInterpolators());

        Change specPosUpdate = updateSpectatorPositions();
        specPosUpdate.apply(timeline);
        change = CombinedChange.createFromApplied(change, specPosUpdate);

        timeline.pushChange(change);
        //@formatter:on
    }

    @ModifyExpressionValue(method = "updateSpectatorPositions", at = @At(value = "INVOKE", target = "com/replaymod/replaystudio/pathing/change/UpdateKeyframeProperties$Builder.done()Lcom/replaymod/replaystudio/pathing/change/UpdateKeyframeProperties;"))
    private UpdateKeyframeProperties addFov(UpdateKeyframeProperties.Builder builder)
    {
        //@formatter:off
        return builder
                .setValue(ReplayFov.FOV, Triple.of((float) Math.tan(Math.toRadians(MCVer.getMinecraft().options.fov)), 0f, 0f))
                .done();
        //@formatter:on
    }

    @Inject(method = "registerPositionInterpolatorProperties", at = @At("TAIL"))
    private void registerPositionInterpolatorProperties(Interpolator interpolator, CallbackInfoReturnable<Interpolator> info)
    {
        interpolator.registerProperty(ReplayFov.FOV);
    }

    @Inject(method = "createTimelineStatic", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void createTimelineStatic(CallbackInfoReturnable<Timeline> info, Timeline timeline)
    {
        timeline.registerProperty(ReplayFov.FOV);
    }
}