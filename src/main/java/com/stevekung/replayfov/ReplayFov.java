package com.stevekung.replayfov;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.lib.org.apache.commons.lang3.tuple.Triple;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.property.AbstractProperty;
import com.replaymod.replaystudio.pathing.property.PropertyPart;
import com.replaymod.replaystudio.pathing.property.PropertyParts;

import net.minecraft.util.Mth;

public class ReplayFov
{
    public static final Fov FOV = new Fov();

    public static class Fov extends AbstractProperty<Triple<Float, Float, Float>>
    {
        private final PropertyPart<Triple<Float, Float, Float>> FOV = new PropertyParts.ForFloatTriple(this, true, PropertyParts.TripleElement.LEFT);
        private final PropertyPart<Triple<Float, Float, Float>> A = new PropertyParts.ForFloatTriple(this, true, PropertyParts.TripleElement.MIDDLE);
        private final PropertyPart<Triple<Float, Float, Float>> B = new PropertyParts.ForFloatTriple(this, true, PropertyParts.TripleElement.RIGHT);

        private Fov()
        {
            super("fov", "replaymod.gui.fov", CameraProperties.GROUP, Triple.of(0f, 0f, 0f));
        }

        @Override
        public Collection<PropertyPart<Triple<Float, Float, Float>>> getParts()
        {
            return Arrays.asList(this.FOV, this.A, this.B);
        }

        @Override
        public void applyToGame(Triple<Float, Float, Float> value, @Nonnull Object replayHandler)
        {
            var handler = (ReplayHandler)replayHandler;
            handler.spectateCamera();
            var cameraEntity = handler.getCameraEntity();

            if (cameraEntity != null)
            {
                double fov;

                if (value.getLeft() < 0)
                {
                    fov = Math.toDegrees(Math.atan(1 / value.getLeft()) + Math.PI);
                }
                else
                {
                    fov = Math.toDegrees(Math.atan(1 / value.getLeft()));
                }
                ((FovCamera)cameraEntity).setFov(Mth.floor(fov));
            }
        }

        @Override
        public void toJson(JsonWriter writer, Triple<Float, Float, Float> value) throws IOException
        {
            writer.beginArray().value(value.getLeft()).value(value.getMiddle()).value(value.getRight()).endArray();
        }

        @Override
        public Triple<Float, Float, Float> fromJson(JsonReader reader) throws IOException
        {
            reader.beginArray();

            try
            {
                return Triple.of((float)reader.nextDouble(), (float)reader.nextDouble(), (float)reader.nextDouble());
            }
            finally
            {
                reader.endArray();
            }
        }
    }
}