package com.stevekung.replayfov.extender;

import com.replaymod.replaystudio.pathing.change.Change;

public interface SPTimelineExtender
{
    void addPositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, float fov, int spectated);

    Change updatePositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, float fov);
}