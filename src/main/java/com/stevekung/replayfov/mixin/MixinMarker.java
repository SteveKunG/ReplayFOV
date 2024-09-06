package com.stevekung.replayfov.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.replaymod.replaystudio.data.Marker;
import com.stevekung.replayfov.extender.MarkerExtender;

@Mixin(value = Marker.class, remap = false)
public class MixinMarker implements MarkerExtender
{
    @Shadow
    String name;

    @Shadow
    int time;

    @Shadow
    double x;

    @Shadow
    double y;

    @Shadow
    double z;

    @Shadow
    float yaw;

    @Shadow
    float pitch;

    @Shadow
    float roll;

    @Unique
    private float fov;

    @Override
    public float getFov()
    {
        return this.fov;
    }

    @Override
    public void setFov(float fov)
    {
        this.fov = fov;
    }

    //@formatter:off
    @Override
    @SuppressWarnings("all")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Marker)) return false;

        Marker marker = (Marker) o;

        if (time != marker.getTime()) return false;
        if (Double.compare(marker.getX(), x) != 0) return false;
        if (Double.compare(marker.getY(), y) != 0) return false;
        if (Double.compare(marker.getZ(), z) != 0) return false;
        if (Float.compare(marker.getYaw(), yaw) != 0) return false;
        if (Float.compare(marker.getPitch(), pitch) != 0) return false;
        if (Float.compare(marker.getRoll(), roll) != 0) return false;
        if (Float.compare(MarkerExtender.class.cast(marker).getFov(), this.fov) != 0) return false;
        return !(name != null ? !name.equals(marker.getName()) : marker.getName() != null);
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        result = 31 * result + time;
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (yaw != +0.0f ? Float.floatToIntBits(yaw) : 0);
        result = 31 * result + (pitch != +0.0f ? Float.floatToIntBits(pitch) : 0);
        result = 31 * result + (roll != +0.0f ? Float.floatToIntBits(roll) : 0);
        result = 31 * result + (this.fov != +0.0f ? Float.floatToIntBits(this.fov) : 0);
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "Marker{" +
                "name='" + name + '\'' +
                ", time=" + time +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", roll=" + roll +
                ", fov=" + this.fov +
                '}';
    }
    //@formatter:on
}