package cl.plugin.consistency.tooltip;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;

/**
 * The class <b>StylerUtilities</b> allows to.<br>
 */
public class StylerUtilities
{
  public final static Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
  public final static Font italicFont = JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);

  public final static Styler boldStyler = createStyler(null, null, boldFont);

  /**
   * Create Styler
   *
   * @param foreground
   * @param background
   * @param font
   */
  public static Styler createStyler(Color foreground, Color background, Font font)
  {
    return new Styler()
    {
      @Override
      public void applyStyles(TextStyle textStyle)
      {
        textStyle.foreground = foreground;
        textStyle.background = background;
        textStyle.font = font;
      }
    };
  }

  /**
   * Create Styler
   *
   * @param foreground
   * @param background
   */
  public static Styler createStyler(Color foreground, Color background)
  {
    return createStyler(foreground, background, null);
  }

  /**
   * Create Styler
   *
   * @param foreground
   */
  public static Styler createStyler(Color foreground)
  {
    return createStyler(foreground, null, null);
  }

  /**
   * Create Styler with bold font
   *
   * @param styler
   */
  public static Styler withBold(Styler styler)
  {
    return new Styler()
    {
      @Override
      public void applyStyles(TextStyle textStyle)
      {
        styler.applyStyles(textStyle);
        textStyle.font = boldFont;
      }
    };
  }

  /**
   * Create Styler with italic font
   *
   * @param styler
   */
  public static Styler withItalic(Styler styler)
  {
    return new Styler()
    {
      @Override
      public void applyStyles(TextStyle textStyle)
      {
        styler.applyStyles(textStyle);
        textStyle.font = italicFont;
      }
    };
  }

  /**
   * Create Styler with underline
   *
   * @param styler
   */
  public static Styler withUnderline(Styler styler)
  {
    return new Styler()
    {
      @Override
      public void applyStyles(TextStyle textStyle)
      {
        styler.applyStyles(textStyle);
        textStyle.underline = true;
      }
    };
  }

  /**
   * Create Styler with underline color
   * 
   * @param styler
   * @param underlineColor
   */
  public static Styler withUnderline(Styler styler, Color underlineColor)
  {
    return new Styler()
    {
      @Override
      public void applyStyles(TextStyle textStyle)
      {
        styler.applyStyles(textStyle);
        textStyle.underline = true;
        textStyle.underlineColor = underlineColor;
      }
    };
  }

}
