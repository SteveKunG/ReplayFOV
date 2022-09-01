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
import com.google.common.collect.Lists;
import com.replaymod.core.versions.MCVer;
import com.replaymod.lib.org.apache.commons.lang3.tuple.Triple;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replaystudio.pathing.change.*;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.replaystudio.util.Location;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.SPTimeline;
import com.stevekung.replayfov.FovPositionKeyframe;
import com.stevekung.replayfov.ReplayFov;

@Mixin(value = SPTimeline.class, remap = false)
public abstract class MixinSPTimeline implements FovPositionKeyframe
{
    @Shadow
    @Final
    Path timePath;

    @Shadow
    @Final
    Path positionPath;

    @Shadow
    @Final
    Timeline timeline;

    @Shadow
    EntityPositionTracker entityTracker;

    @Shadow
    abstract Interpolator createDefaultInterpolator();

    @Shadow
    abstract Change updateInterpolators();

    /**
     * @author SteveKunG
     * @reason Use new FOV arg
     */
    @Overwrite
    public void addPositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, int spectated)
    {
        this.addPositionKeyframe(time, posX, posY, posZ, yaw, pitch, roll, MCVer.getMinecraft().options.fov().get(), spectated);
    }

    /**
     * @author SteveKunG
     * @reason Use new FOV arg
     */
    @Overwrite
    public Change updatePositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll)
    {
        return this.updatePositionKeyframe(time, posX, posY, posZ, yaw, pitch, roll, MCVer.getMinecraft().options.fov().get());
    }

    @Override
    public Change updatePositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, int fov)
    {
        ReplayModSimplePathing.LOGGER.debug("Updating position keyframe at {} to pos {}/{}/{} rot {}/{}/{} fov {}", time, posX, posY, posZ, yaw, pitch, roll, fov);
        var keyframe = this.positionPath.getKeyframe(time);
        Preconditions.checkState(keyframe != null, "Keyframe does not exists");
        Preconditions.checkState(!keyframe.getValue(SpectatorProperty.PROPERTY).isPresent(), "Cannot update spectator keyframe");

        var change = UpdateKeyframeProperties.create(this.positionPath, keyframe)
                .setValue(CameraProperties.POSITION, Triple.of(posX, posY, posZ))
                .setValue(CameraProperties.ROTATION, Triple.of(yaw, pitch, roll))
                .setValue(ReplayFov.FOV, Triple.of((float)fov, (float)fov, (float)fov))
                .done();
        change.apply(this.timeline);
        return change;
    }

    @Override
    public void addPositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, int fov, int spectated)
    {
        ReplayModSimplePathing.LOGGER.debug("Adding position keyframe at {} pos {}/{}/{} rot {}/{}/{} fov {} entId {}", time, posX, posY, posZ, yaw, pitch, roll, fov, spectated);
        var path = this.positionPath;
        Preconditions.checkState(this.positionPath.getKeyframe(time) == null, "Keyframe already exists");

        Change change = AddKeyframe.create(path, time);
        change.apply(this.timeline);
        var keyframe = path.getKeyframe(time);
        var builder = UpdateKeyframeProperties.create(path, keyframe);
        builder.setValue(CameraProperties.POSITION, Triple.of(posX, posY, posZ));
        builder.setValue(CameraProperties.ROTATION, Triple.of(yaw, pitch, roll));
        builder.setValue(ReplayFov.FOV, Triple.of((float)(1 / Math.tan(Math.toRadians(fov))), 0f, 0f));

        if (spectated != -1)
        {
            builder.setValue(SpectatorProperty.PROPERTY, spectated);
        }

        var updateChange = builder.done();
        updateChange.apply(this.timeline);
        change = CombinedChange.createFromApplied(change, updateChange);

        // If this new keyframe formed the first segment of the path
        if (path.getSegments().size() == 1)
        {
            // then create an initial interpolator of default type
            var segment = path.getSegments().iterator().next();
            var interpolator = this.createDefaultInterpolator();
            var setInterpolator = SetInterpolator.create(segment, interpolator);
            setInterpolator.apply(this.timeline);
            change = CombinedChange.createFromApplied(change, setInterpolator);
        }

        // Update interpolators for spectator keyframes
        // while this is overkill, it is far simpler than updating differently for every possible case
        change = CombinedChange.createFromApplied(change, this.updateInterpolators());

        var specPosUpdate = this.updateSpectatorPositions();
        specPosUpdate.apply(this.timeline);
        change = CombinedChange.createFromApplied(change, specPosUpdate);

        this.timeline.pushChange(change);
    }

    /**
     * @author SteveKunG
     * @reason Add FOV
     */
    @Overwrite
    private Change updateSpectatorPositions()
    {
        if (this.entityTracker == null)
        {
            return CombinedChange.create();
        }

        var changes = Lists.<Change>newArrayList();
        this.timePath.updateAll();

        for (var keyframe : this.positionPath.getKeyframes())
        {
            var spectator = keyframe.getValue(SpectatorProperty.PROPERTY);

            if (spectator.isPresent())
            {
                var time = this.timePath.getValue(TimestampProperty.PROPERTY, keyframe.getTime());

                if (!time.isPresent())
                {
                    continue; // No time keyframes set at this video time, cannot determine replay time
                }

                var expected = this.entityTracker.getEntityPositionAtTimestamp(spectator.get(), time.get());

                if (expected == null)
                {
                    continue; // We don't have any data on this entity for some reason
                }

                var pos = keyframe.getValue(CameraProperties.POSITION).orElse(Triple.of(0D, 0D, 0D));
                var rot = keyframe.getValue(CameraProperties.ROTATION).orElse(Triple.of(0F, 0F, 0F));
                var actual = new Location(pos.getLeft(), pos.getMiddle(), pos.getRight(), rot.getLeft(), rot.getRight());

                if (!expected.equals(actual))
                {
                    changes.add(UpdateKeyframeProperties.create(this.positionPath, keyframe)
                            .setValue(CameraProperties.POSITION, Triple.of(expected.getX(), expected.getY(), expected.getZ()))
                            .setValue(CameraProperties.ROTATION, Triple.of(expected.getYaw(), expected.getPitch(), 0f))
                            .setValue(ReplayFov.FOV, Triple.of((float)Math.tan(Math.toRadians(MCVer.getMinecraft().options.fov().get())), 0f, 0f)).done());
                }
            }
        }
        return CombinedChange.create(changes.toArray(new Change[changes.size()]));
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