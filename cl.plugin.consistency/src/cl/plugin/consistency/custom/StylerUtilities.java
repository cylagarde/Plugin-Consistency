package cl.plugin.consistency.custom;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.TextStyle;

/**
 * The class <b>StylerUtilities</b> allows to.<br>
 */
public class StylerUtilities
{
  public final static Font BOLD_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
  public final static Font ITALIC_FONT = JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);

  public final static Styler BOLD_STYLER = createStyler(null, null, BOLD_FONT);

  /**
   * Create Styler
   *
   * @param foreground
   * @param background
   * @param font
   */
  public static Styler createStyler(Color foreground, Color background, Font font)
  {
    return new Styler() {
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
    return new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle)
      {
        if (styler != null)
          styler.applyStyles(textStyle);
        if (textStyle.font == null)
          textStyle.font = BOLD_FONT;
        else
        {
          FontData fontData = textStyle.font.getFontData()[0];
          if (fontData.getStyle() != SWT.BOLD)
          {
            String symbolicName = fontData.getName() + " " + fontData.getHeight() + " " + fontData.getStyle() + " BOLD";
            if (!JFaceResources.getFontRegistry().hasValueFor(symbolicName))
            {
              FontData[] fontDatas = textStyle.font.getFontData();
              for(FontData element : fontDatas)
                element.setStyle(element.getStyle() | SWT.BOLD);
              JFaceResources.getFontRegistry().put(symbolicName, fontDatas);
            }
            Font font = JFaceResources.getFontRegistry().get(symbolicName);
            textStyle.font = font;
          }
        }
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
    return new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle)
      {
        if (styler != null)
          styler.applyStyles(textStyle);
        if (textStyle.font == null)
          textStyle.font = ITALIC_FONT;
        else
        {
          FontData fontData = textStyle.font.getFontData()[0];
          if (fontData.getStyle() != SWT.ITALIC)
          {
            String symbolicName = fontData.getName() + " " + fontData.getHeight() + " " + fontData.getStyle() + " ITALIC";
            if (!JFaceResources.getFontRegistry().hasValueFor(symbolicName))
            {
              FontData[] fontDatas = textStyle.font.getFontData();
              for(FontData element : fontDatas)
                element.setStyle(element.getStyle() | SWT.ITALIC);
              JFaceResources.getFontRegistry().put(symbolicName, fontDatas);
            }
            Font font = JFaceResources.getFontRegistry().get(symbolicName);
            textStyle.font = font;
          }
        }
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
    return withUnderline(styler, null, SWT.UNDERLINE_SINGLE);
  }

  /**
   * Create Styler with underline color
   *
   * @param styler
   * @param underlineColor
   */
  public static Styler withUnderline(Styler styler, Color underlineColor)
  {
    return withUnderline(styler, underlineColor, SWT.UNDERLINE_SINGLE);
  }

  /**
   * Create Styler with underline color
   *
   * @param styler
   * @param underlineColor
   * @param underlineStyle
   */
  public static Styler withUnderline(Styler styler, Color underlineColor, int underlineStyle)
  {
    return new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle)
      {
        if (styler != null)
          styler.applyStyles(textStyle);
        textStyle.underline = true;
        textStyle.underlineColor = underlineColor;
        textStyle.underlineStyle = underlineStyle;
      }
    };
  }

}
