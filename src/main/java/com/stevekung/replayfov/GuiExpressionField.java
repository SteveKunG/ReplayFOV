package com.stevekung.replayfov;

import java.math.BigDecimal;
import java.math.MathContext;

import com.replaymod.lib.de.johni0702.minecraft.gui.element.AbstractGuiTextField;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.Consumer;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import com.udojava.evalex.Expression;

public class GuiExpressionField extends AbstractGuiTextField<GuiExpressionField>
{
    private Boolean expressionValid = null;
    private MathContext mathContext = new MathContext(20);

    @Override
    protected void onTextChanged(String from)
    {
        this.verify();
        super.onTextChanged(from);
    }

    private boolean verify()
    {
        try
        {
            this.getExpression().eval();
            this.setTextColor(ReadableColor.WHITE);
            return this.expressionValid = true;
        }
        catch (Expression.ExpressionException | ArithmeticException | NumberFormatException e)
        {
            this.setTextColor(ReadableColor.RED);
            return this.expressionValid = false;
        }
    }

    public boolean isExpressionValid()
    {
        if (this.expressionValid == null)
        {
            this.verify();
        }
        return this.expressionValid;
    }

    public GuiExpressionField setPrecision(int precision)
    {
        this.mathContext = new MathContext(precision);
        return this;
    }

    public Expression getExpression()
    {
        return new Expression(this.getText(), this.mathContext);
    }

    public BigDecimal getBigDecimal() throws Expression.ExpressionException, ArithmeticException, NumberFormatException
    {
        return this.getExpression().eval();
    }

    public long getLong() throws Expression.ExpressionException, ArithmeticException, NumberFormatException
    {
        return this.getBigDecimal().longValue();
    }

    public double getDouble() throws Expression.ExpressionException, ArithmeticException, NumberFormatException
    {
        return this.getBigDecimal().doubleValue();
    }

    public float getFloat() throws Expression.ExpressionException, ArithmeticException, NumberFormatException
    {
        return this.getBigDecimal().floatValue();
    }

    public int getInt() throws Expression.ExpressionException, ArithmeticException, NumberFormatException
    {
        return this.getBigDecimal().intValue();
    }

    @Override
    public GuiExpressionField setSize(int width, int height)
    {
        return super.setSize(width, height);
    }

    @Override
    public GuiExpressionField onTextChanged(Consumer<String> textChanged)
    {
        return super.onTextChanged(textChanged);
    }

    @Override
    protected GuiExpressionField getThis()
    {
        return this;
    }
}