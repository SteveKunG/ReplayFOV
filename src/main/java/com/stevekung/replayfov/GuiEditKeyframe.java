package com.stevekung.replayfov;

import java.text.DecimalFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiContainer;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiPanel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.*;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import com.replaymod.lib.de.johni0702.minecraft.gui.function.Typeable;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.GridLayout;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.HorizontalLayout;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.VerticalLayout;
import com.replaymod.lib.de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.Colors;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.Consumer;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.Utils;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.change.CombinedChange;
import com.replaymod.replaystudio.pathing.interpolation.CatmullRomSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.simplepathing.InterpolatorType;
import com.replaymod.simplepathing.SPTimeline.SPPath;
import com.replaymod.simplepathing.Setting;
import com.replaymod.simplepathing.gui.GuiPathing;
import com.replaymod.simplepathing.properties.ExplicitInterpolationProperty;
import com.udojava.evalex.Expression;

import net.minecraft.client.resources.language.I18n;

public abstract class GuiEditKeyframe<T extends GuiEditKeyframe<T>> extends AbstractGuiPopup<T> implements Typeable
{
    private static GuiExpressionField newGuiExpressionField()
    {
        return new GuiExpressionField();
    }

    protected final DecimalFormat df = new DecimalFormat("###.#####");

    protected static final Logger logger = LogManager.getLogger();

    protected final GuiPathing guiPathing;

    protected final long time;
    protected final Keyframe keyframe;
    protected final Path path;

    public final GuiLabel title = new GuiLabel();

    public final GuiPanel inputs = new GuiPanel();

    public final GuiExpressionField timeMinField = newGuiExpressionField().setSize(50, 20);
    public final GuiExpressionField timeSecField = newGuiExpressionField().setSize(50, 20);
    public final GuiExpressionField timeMSecField = newGuiExpressionField().setSize(50, 20);

    public final GuiPanel timePanel = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(3))
            .addElements(new HorizontalLayout.Data(0.5), new GuiLabel().setI18nText("replaymod.gui.editkeyframe.timelineposition"), this.timeMinField, new GuiLabel()
                    .setI18nText("replaymod.gui.minutes"), this.timeSecField, new GuiLabel()
                    .setI18nText("replaymod.gui.seconds"), this.timeMSecField, new GuiLabel().setI18nText("replaymod.gui.milliseconds"));

    public final GuiButton saveButton = new GuiButton().setSize(150, 20).setI18nLabel("replaymod.gui.save");

    public final GuiButton cancelButton = new GuiButton()
            .onClick(this::close).setSize(150, 20).setI18nLabel("replaymod.gui.cancel");

    public final GuiPanel buttons = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(7))
            .addElements(new HorizontalLayout.Data(0.5), this.saveButton, this.cancelButton);

    {
        this.setBackgroundColor(Colors.DARK_TRANSPARENT);
        this.popup.setLayout(new VerticalLayout().setSpacing(10))
        .addElements(new VerticalLayout.Data(0.5, false), this.title, this.inputs, this.timePanel, this.buttons);
    }

    protected boolean canSave()
    {
        try
        {
            var timeMin = this.timeMinField.getDouble();
            var timeSec = this.timeSecField.getDouble();
            var timeMSec = this.timeMSecField.getDouble();
            var newTime = (long) ((timeMin * 60 + timeSec) * 1000 + timeMSec);

            if (newTime < 0 || newTime > this.guiPathing.timeline.getLength())
            {
                return false;
            }
            return newTime == this.keyframe.getTime() || this.path.getKeyframe(newTime) == null;
        }
        catch (Expression.ExpressionException | ArithmeticException | NumberFormatException e)
        {
            return false;
        }
    }

    public GuiEditKeyframe(GuiPathing gui, SPPath path, long time, String type)
    {
        super(ReplayModReplay.instance.getReplayHandler().getOverlay());
        this.guiPathing = gui;
        this.time = time;
        this.path = gui.getMod().getCurrentTimeline().getPath(path);
        this.keyframe = this.path.getKeyframe(time);

        Consumer<String> updateSaveButtonState = s -> this.saveButton.setEnabled(this.canSave());
        this.timeMinField.setText(String.valueOf(time / 1000 / 60)).onTextChanged(updateSaveButtonState);
        this.timeSecField.setText(String.valueOf(time / 1000 % 60)).onTextChanged(updateSaveButtonState);
        this.timeMSecField.setText(String.valueOf(time % 1000)).onTextChanged(updateSaveButtonState);

        this.title.setI18nText("replaymod.gui.editkeyframe.title." + type);
        this.saveButton.onClick(() ->
        {
            var timeMin = this.timeMinField.getDouble();
            var timeSec = this.timeSecField.getDouble();
            var timeMSec = this.timeMSecField.getDouble();
            var change = this.save();
            var newTime = (long) ((timeMin * 60 + timeSec) * 1000 + timeMSec);

            if (newTime != time)
            {
                change = CombinedChange.createFromApplied(change, gui.getMod().getCurrentTimeline().moveKeyframe(path, time, newTime));

                if (gui.getMod().getSelectedPath() == path && gui.getMod().getSelectedTime() == time)
                {
                    gui.getMod().setSelected(path, newTime);
                }
            }
            gui.getMod().getCurrentTimeline().getTimeline().pushChange(change);
            this.close();
        });
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown)
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            this.cancelButton.onClick();
            return true;
        }
        return false;
    }

    @Override
    public void open()
    {
        super.open();
    }

    protected abstract Change save();

    public static class Spectator extends GuiEditKeyframe<Spectator>
    {
        public Spectator(GuiPathing gui, SPPath path, long keyframe)
        {
            super(gui, path, keyframe, "spec");
            Utils.link(this.timeMinField, this.timeSecField, this.timeMSecField);
            this.popup.invokeAll(IGuiLabel.class, e -> e.setColor(ReadableColor.BLACK));
        }

        @Override
        protected Change save()
        {
            return CombinedChange.createFromApplied();
        }

        @Override
        protected Spectator getThis()
        {
            return this;
        }
    }

    public static class Time extends GuiEditKeyframe<Time>
    {
        public final GuiExpressionField timestampMinField = newGuiExpressionField().setSize(50, 20);
        public final GuiExpressionField timestampSecField = newGuiExpressionField().setSize(50, 20);
        public final GuiExpressionField timestampMSecField = newGuiExpressionField().setSize(50, 20);

        {
            this.inputs.setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(3))
            .addElements(new HorizontalLayout.Data(0.5), new GuiLabel()
                    .setI18nText("replaymod.gui.editkeyframe.timestamp"), this.timestampMinField, new GuiLabel()
                    .setI18nText("replaymod.gui.minutes"), this.timestampSecField, new GuiLabel()
                    .setI18nText("replaymod.gui.seconds"), this.timestampMSecField, new GuiLabel()
                    .setI18nText("replaymod.gui.milliseconds"));
        }

        public Time(GuiPathing gui, SPPath path, long keyframe)
        {
            super(gui, path, keyframe, "time");

            this.keyframe.getValue(TimestampProperty.PROPERTY).ifPresent(time ->
            {
                this.timestampMinField.setText(String.valueOf(time / 1000 / 60));
                this.timestampSecField.setText(String.valueOf(time / 1000 % 60));
                this.timestampMSecField.setText(String.valueOf(time % 1000));
            });

            Consumer<String> updateSaveButtonState = s -> this.saveButton.setEnabled(this.canSave());

            this.timestampMinField.onTextChanged(updateSaveButtonState);
            this.timestampMSecField.onTextChanged(updateSaveButtonState);
            this.timestampMSecField.onTextChanged(updateSaveButtonState);

            Utils.link(this.timestampMinField, this.timestampSecField, this.timestampMSecField, this.timeMinField, this.timeSecField, this.timeMSecField);

            this.popup.invokeAll(IGuiLabel.class, e -> e.setColor(ReadableColor.BLACK));
        }

        @Override
        protected Change save() throws Expression.ExpressionException, ArithmeticException, NumberFormatException
        {
            var timeMin = this.timestampMinField.getDouble();
            var timeSec = this.timestampSecField.getDouble();
            var timeMSec = this.timestampMSecField.getDouble();
            var time = (int) ((timeMin * 60 + timeSec) * 1000 + timeMSec);
            return this.guiPathing.getMod().getCurrentTimeline().updateTimeKeyframe(this.keyframe.getTime(), time);
        }

        @Override
        protected boolean canSave()
        {
            try
            {
                var timeMin = this.timestampMinField.getDouble();
                var timeSec = this.timestampSecField.getDouble();
                var timeMSec = this.timestampMSecField.getDouble();
                var time = (long) ((timeMin * 60 + timeSec) * 1000 + timeMSec);

                if (time < 0)
                { // TODO add check to make sure time isn't longer than the replay
                    return false;
                }
                else
                {
                    return super.canSave();
                }
            }
            catch (Expression.ExpressionException | ArithmeticException | NumberFormatException e)
            {
                return false;
            }
        }

        @Override
        protected Time getThis()
        {
            return this;
        }
    }

    public static class Position extends GuiEditKeyframe<Position>
    {
        public final GuiExpressionField xField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField yField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField zField = newGuiExpressionField().setSize(90, 20);

        public final GuiExpressionField yawField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField pitchField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField rollField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField fovField = newGuiExpressionField().setSize(90, 20);

        public final InterpolationPanel interpolationPanel = new InterpolationPanel();

        {
            var positionInputs = new GuiPanel()
                    .setLayout(new GridLayout().setCellsEqualSize(false).setColumns(4).setSpacingX(3).setSpacingY(5))
                    .addElements(new GridLayout.Data(1, 0.5), new GuiLabel().setI18nText("replaymod.gui.editkeyframe.xpos"), this.xField, new GuiLabel()
                            .setI18nText("replaymod.gui.editkeyframe.camyaw"), this.yawField, new GuiLabel()
                            .setI18nText("replaymod.gui.editkeyframe.ypos"), this.yField, new GuiLabel()
                            .setI18nText("replaymod.gui.editkeyframe.campitch"), this.pitchField, new GuiLabel()
                            .setI18nText("replaymod.gui.editkeyframe.zpos"), this.zField, new GuiLabel()
                            .setI18nText("replaymod.gui.editkeyframe.camroll"), this.rollField, new GuiLabel()
                            .setI18nText("replaymod.gui.editkeyframe.fov"), this.fovField);

            this.inputs.setLayout(new VerticalLayout().setSpacing(10)).addElements(new VerticalLayout.Data(0.5, false), positionInputs, this.interpolationPanel);
        }

        public Position(GuiPathing gui, SPPath path, long keyframe)
        {
            super(gui, path, keyframe, "pos");
            Consumer<String> updateSaveButtonState = s -> this.saveButton.setEnabled(this.canSave());

            this.keyframe.getValue(CameraProperties.POSITION).ifPresent(pos ->
            {
                this.xField.setText(this.df.format(pos.getLeft()));
                this.yField.setText(this.df.format(pos.getMiddle()));
                this.zField.setText(this.df.format(pos.getRight()));
            });
            this.keyframe.getValue(CameraProperties.ROTATION).ifPresent(rot ->
            {
                this.yawField.setText(this.df.format(rot.getLeft()));
                this.pitchField.setText(this.df.format(rot.getMiddle()));
                this.rollField.setText(this.df.format(rot.getRight()));
            });

            this.keyframe.getValue(ReplayFov.FOV).ifPresent(val ->
            {
                double fov;

                if (val.getLeft() < 0)
                {
                    fov = Math.toDegrees(Math.atan(1 / val.getLeft()) + Math.PI);
                }
                else
                {
                    fov = Math.toDegrees(Math.atan(1 / val.getLeft()));
                }

                this.fovField.setText(this.df.format(fov));
            });

            this.xField.onTextChanged(updateSaveButtonState);
            this.yField.onTextChanged(updateSaveButtonState);
            this.zField.onTextChanged(updateSaveButtonState);
            this.yawField.onTextChanged(updateSaveButtonState);
            this.pitchField.onTextChanged(updateSaveButtonState);
            this.rollField.onTextChanged(updateSaveButtonState);
            this.fovField.onTextChanged(updateSaveButtonState);

            Utils.link(this.xField, this.yField, this.zField, this.yawField, this.pitchField, this.rollField, this.timeMinField, this.timeSecField, this.timeMSecField);

            this.popup.invokeAll(IGuiLabel.class, e -> e.setColor(ReadableColor.BLACK));
        }

        @Override
        protected boolean canSave()
        {
            if (this.xField.setPrecision(14).isExpressionValid() && this.yField.setPrecision(14).isExpressionValid() && this.zField.setPrecision(14)
                    .isExpressionValid() && this.yawField.setPrecision(11).isExpressionValid() && this.pitchField.setPrecision(11)
                    .isExpressionValid() && this.rollField.setPrecision(11).isExpressionValid() && this.fovField.setPrecision(11).isExpressionValid())
            {
                return super.canSave();
            }
            else
            {
                return false;
            }
        }

        @Override
        protected Change save() throws Expression.ExpressionException, ArithmeticException, NumberFormatException
        {
            var x = this.xField.setPrecision(14).getDouble();
            var y = this.yField.setPrecision(14).getDouble();
            var z = this.zField.setPrecision(14).getDouble();
            var yaw = this.yawField.setPrecision(11).getFloat();
            var pitch = this.pitchField.setPrecision(11).getFloat();
            var roll = this.rollField.setPrecision(11).getFloat();
            var fov = this.fovField.setPrecision(11).getFloat();
            var timeline = this.guiPathing.getMod().getCurrentTimeline();
            var positionChange = ((FovPositionKeyframe) timeline).updatePositionKeyframe(this.time, x, y, z, yaw, pitch, roll, (float) (1 / Math.tan(Math.toRadians(fov))));

            if (this.interpolationPanel.getSettingsPanel() == null)
            {
                // The last keyframe doesn't have interpolator settings because there is no segment following it
                return positionChange;
            }

            var interpolator = this.interpolationPanel.getSettingsPanel().createInterpolator();

            if (this.interpolationPanel.getInterpolatorType() == InterpolatorType.DEFAULT)
            {
                return CombinedChange.createFromApplied(positionChange, timeline.setInterpolatorToDefault(this.time), timeline.setDefaultInterpolator(interpolator));
            }
            else
            {
                return CombinedChange.createFromApplied(positionChange, timeline.setInterpolator(this.time, interpolator));
            }
        }

        @Override
        protected Position getThis()
        {
            return this;
        }

        public class InterpolationPanel extends AbstractGuiContainer<InterpolationPanel>
        {
            @SuppressWarnings("rawtypes")
            private SettingsPanel settingsPanel;
            private GuiDropdownMenu<InterpolatorType> dropdown = new GuiDropdownMenu<>();

            @SuppressWarnings("unchecked")
            public InterpolationPanel()
            {
                this.setLayout(new VerticalLayout());

                this.dropdown = new GuiDropdownMenu<InterpolatorType>()
                        .setToString(s -> I18n.get(s.getI18nName()))
                        .setValues(InterpolatorType.values()).setHeight(20)
                        .onSelection(i -> this.setSettingsPanel(this.dropdown.getSelectedValue()));

                // set hover tooltips
                for (var e : this.dropdown.getDropdownEntries().entrySet())
                {
                    e.getValue().setTooltip(new GuiTooltip().setI18nText(e.getKey().getI18nDescription()));
                }

                var dropdownPanel = new GuiPanel()
                        .setLayout(new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(3).setSpacingY(5))
                        .addElements(new GridLayout.Data(1, 0.5), new GuiLabel().setI18nText("replaymod.gui.editkeyframe.interpolator"), this.dropdown);

                this.addElements(new VerticalLayout.Data(0.5, false), dropdownPanel);

                var segment = Position.this.path.getSegments().stream()
                        .filter(s -> s.getStartKeyframe() == Position.this.keyframe).findFirst();
                if (segment.isPresent())
                {
                    var interpolator = segment.get().getInterpolator();
                    var type = InterpolatorType.fromClass(interpolator.getClass());
                    if (Position.this.keyframe.getValue(ExplicitInterpolationProperty.PROPERTY).isPresent())
                    {
                        this.dropdown.setSelected(type); // trigger the callback once to display settings panel
                    }
                    else
                    {
                        this.setSettingsPanel(InterpolatorType.DEFAULT);
                        type = InterpolatorType.DEFAULT;
                    }
                    if (this.getInterpolatorTypeNoDefault(type).getInterpolatorClass().isInstance(interpolator))
                    {
                        // noinspection unchecked
                        this.settingsPanel.loadSettings(interpolator);
                    }
                }
                else
                {
                    // Disable dropdown if this is the last keyframe
                    this.dropdown.setDisabled();
                }
            }

            @SuppressWarnings("rawtypes")
            public SettingsPanel getSettingsPanel()
            {
                return this.settingsPanel;
            }

            public void setSettingsPanel(InterpolatorType type)
            {
                this.removeElement(this.settingsPanel);

                switch (this.getInterpolatorTypeNoDefault(type))
                {
                    default:
                    case CATMULL_ROM:
                        this.settingsPanel = new CatmullRomSettingsPanel();
                        break;
                    case CUBIC:
                        this.settingsPanel = new CubicSettingsPanel();
                        break;
                    case LINEAR:
                        this.settingsPanel = new LinearSettingsPanel();
                        break;
                }

                this.addElements(new GridLayout.Data(0.5, 0.5), this.settingsPanel);
            }

            protected InterpolatorType getInterpolatorTypeNoDefault(InterpolatorType interpolatorType)
            {
                if (interpolatorType == InterpolatorType.DEFAULT || interpolatorType == null)
                {
                    var defaultType = InterpolatorType
                            .fromString(Position.this.guiPathing.getMod().getCore().getSettingsRegistry().get(Setting.DEFAULT_INTERPOLATION));
                    return defaultType;
                }
                return interpolatorType;
            }

            public InterpolatorType getInterpolatorType()
            {
                return this.dropdown.getSelectedValue();
            }

            @Override
            protected InterpolationPanel getThis()
            {
                return this;
            }

            public abstract class SettingsPanel<I extends Interpolator, T extends SettingsPanel<I, T>> extends AbstractGuiContainer<T>
            {
                public abstract void loadSettings(I interpolator);

                public abstract I createInterpolator();
            }

            public class CatmullRomSettingsPanel extends SettingsPanel<CatmullRomSplineInterpolator, CatmullRomSettingsPanel>
            {
                public final GuiLabel alphaLabel = new GuiLabel().setColor(Colors.BLACK)
                        .setI18nText("replaymod.gui.editkeyframe.interpolator.catmullrom.alpha");
                public final GuiNumberField alphaField = new GuiNumberField().setSize(100, 20).setPrecision(5)
                        .setMinValue(0).setValidateOnFocusChange(true);

                {
                    this.setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER));
                    this.addElements(new HorizontalLayout.Data(0.5), this.alphaLabel, this.alphaField);
                }

                @Override
                public void loadSettings(CatmullRomSplineInterpolator interpolator)
                {
                    this.alphaField.setValue(interpolator.getAlpha());
                }

                @Override
                public CatmullRomSplineInterpolator createInterpolator()
                {
                    return new CatmullRomSplineInterpolator(this.alphaField.getDouble());
                }

                @Override
                protected CatmullRomSettingsPanel getThis()
                {
                    return this;
                }
            }

            public class CubicSettingsPanel extends SettingsPanel<CubicSplineInterpolator, CubicSettingsPanel>
            {
                @Override
                public void loadSettings(CubicSplineInterpolator interpolator)
                {
                }

                @Override
                public CubicSplineInterpolator createInterpolator()
                {
                    return new CubicSplineInterpolator();
                }

                @Override
                protected CubicSettingsPanel getThis()
                {
                    return this;
                }
            }

            public class LinearSettingsPanel extends SettingsPanel<LinearInterpolator, LinearSettingsPanel>
            {
                @Override
                public void loadSettings(LinearInterpolator interpolator)
                {
                }

                @Override
                public LinearInterpolator createInterpolator()
                {
                    return new LinearInterpolator();
                }

                @Override
                protected LinearSettingsPanel getThis()
                {
                    return this;
                }
            }
        }
    }
}