package com.stevekung.replayfov;

import java.text.DecimalFormat;
import java.util.function.Consumer;

import com.google.common.base.Strings;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiContainer;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiPanel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiButton;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiLabel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiTextField;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.IGuiLabel;
import com.replaymod.lib.de.johni0702.minecraft.gui.function.Typeable;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.GridLayout;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.HorizontalLayout;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.VerticalLayout;
import com.replaymod.lib.de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.Colors;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import com.replaymod.replaystudio.data.Marker;

public class GuiEditMarkerPopup extends AbstractGuiPopup<GuiEditMarkerPopup> implements Typeable
{
    private final Consumer<Marker> onSave;
    private final DecimalFormat df = new DecimalFormat("###.#####");

    public GuiEditMarkerPopup(GuiContainer<?> container, Marker marker, Consumer<Marker> onSave)
    {
        super(container);
        this.onSave = onSave;
        this.setBackgroundColor(Colors.DARK_TRANSPARENT);
        this.popup.setLayout(new VerticalLayout().setSpacing(5)).addElements(new VerticalLayout.Data(0.5), this.title, this.inputs, this.buttons);
        this.popup.invokeAll(IGuiLabel.class, label -> label.setColor(ReadableColor.BLACK));
        this.nameField.setText(Strings.nullToEmpty(marker.getName()));
        this.timeField.setText(String.valueOf(marker.getTime()));
        this.xField.setText(this.df.format(marker.getX()));
        this.yField.setText(this.df.format(marker.getY()));
        this.zField.setText(this.df.format(marker.getZ()));
        this.yawField.setText(this.df.format(marker.getYaw()));
        this.pitchField.setText(this.df.format(marker.getPitch()));
        this.rollField.setText(this.df.format(marker.getRoll()));
    }

    private static GuiExpressionField newGuiExpressionField()
    {
        return new GuiExpressionField().setSize(150, 20);
    }

    public final GuiButton saveButton = new GuiButton().onClick(new Runnable()
    {
        @Override
        public void run()
        {
            var marker = new Marker();
            marker.setName(Strings.emptyToNull(GuiEditMarkerPopup.this.nameField.getText()));
            marker.setTime(GuiEditMarkerPopup.this.timeField.getInt());
            marker.setX(GuiEditMarkerPopup.this.xField.getDouble());
            marker.setY(GuiEditMarkerPopup.this.yField.getDouble());
            marker.setZ(GuiEditMarkerPopup.this.zField.getDouble());
            marker.setYaw(GuiEditMarkerPopup.this.yawField.getFloat());
            marker.setPitch(GuiEditMarkerPopup.this.pitchField.getFloat());
            marker.setRoll(GuiEditMarkerPopup.this.rollField.getFloat());
            GuiEditMarkerPopup.this.onSave.accept(marker);
            GuiEditMarkerPopup.this.close();
        }
    }).setSize(150, 20).setI18nLabel("replaymod.gui.save");

    public final GuiLabel title = new GuiLabel().setI18nText("replaymod.gui.editkeyframe.title.marker");

    public final GuiTextField nameField = new GuiTextField().setSize(150, 20);
    // TODO: Replace with a min/sec/msec field
    public final GuiExpressionField timeField = newGuiExpressionField();

    com.replaymod.lib.de.johni0702.minecraft.gui.utils.Consumer<String> updateSaveButtonState = s -> this.saveButton.setEnabled(this.canSave());

    public final GuiExpressionField xField = newGuiExpressionField().onTextChanged(this.updateSaveButtonState);
    public final GuiExpressionField yField = newGuiExpressionField().onTextChanged(this.updateSaveButtonState);
    public final GuiExpressionField zField = newGuiExpressionField().onTextChanged(this.updateSaveButtonState);

    public final GuiExpressionField yawField = newGuiExpressionField().onTextChanged(this.updateSaveButtonState);
    public final GuiExpressionField pitchField = newGuiExpressionField().onTextChanged(this.updateSaveButtonState);
    public final GuiExpressionField rollField = newGuiExpressionField().onTextChanged(this.updateSaveButtonState);

    public final GuiPanel inputs = GuiPanel.builder()
            .layout(new GridLayout().setColumns(2).setSpacingX(7).setSpacingY(3))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.markername"), new GridLayout.Data(0, 0.5))
            .with(this.nameField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.timestamp"), new GridLayout.Data(0, 0.5))
            .with(this.timeField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.xpos"), new GridLayout.Data(0, 0.5))
            .with(this.xField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.ypos"), new GridLayout.Data(0, 0.5))
            .with(this.yField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.zpos"), new GridLayout.Data(0, 0.5))
            .with(this.zField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camyaw"), new GridLayout.Data(0, 0.5))
            .with(this.yawField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.campitch"), new GridLayout.Data(0, 0.5))
            .with(this.pitchField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camroll"), new GridLayout.Data(0, 0.5))
            .with(this.rollField, new GridLayout.Data(1, 0.5))
            .build();

    public final GuiButton cancelButton = new GuiButton().onClick(this::close).setSize(150, 20).setI18nLabel("replaymod.gui.cancel");

    public final GuiPanel buttons = new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(7)).addElements(new HorizontalLayout.Data(0.5), this.saveButton, this.cancelButton);

    private boolean canSave()
    {
        return this.timeField.isExpressionValid() && this.xField.isExpressionValid() && this.yField.isExpressionValid() && this.zField.isExpressionValid() && this.yawField.isExpressionValid() && this.pitchField.isExpressionValid() && this.rollField.isExpressionValid();
    }

    @Override
    protected GuiEditMarkerPopup getThis()
    {
        return this;
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
}