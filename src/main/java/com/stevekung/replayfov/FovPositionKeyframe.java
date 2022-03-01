package com.stevekung.replayfov;

import com.replaymod.replaystudio.pathing.change.Change;

public interface FovPositionKeyframe
{
    void addPositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, float fov, int spectated);

    Change updatePositionKeyframe(long time, double posX, double posY, double posZ, float yaw, float pitch, float roll, float fov);
}