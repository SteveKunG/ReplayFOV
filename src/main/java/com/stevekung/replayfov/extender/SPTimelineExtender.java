package com.stevekung.replayfov.extender;

import com.replaymod.replaystudio.pathing.change.Change;

public interface SPTimelineExtender
{
    void replayfov$addPositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, float fov, int spectated);

    Change replayfov$updatePositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, float fov);
}