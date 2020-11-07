package cl.plugin.consistency.custom;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PaintObjectEvent;
import org.eclipse.swt.custom.PaintObjectListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * The class <b>EhancedStyledText</b> allows to.<br>
 *
 * <pre lang="java">
 * StyledString styledString = new StyledString();
 * styledString.append("text");
 * styledString.append(" (counter)", StyledString.COUNTER_STYLER);
 *
 * // add image
 * styledString.append(" ", new ImageStyler(image));
 *
 * // add control
 * styledString.append(" ", new ControlStyler(parent -> {
 *     Button button = new Button(parent, SWT.PUSH);
 *     button.setText("Push");
 *     return button;
 * }));
 *
 * // add link
 * styledString.append("site", new HyperLinkStyler((mouseEvent, hyperLinkStyler) -> System.out.println("click")));
 *
 * // add gc
 * styledString.append(" ", new GCStyler(0, new Point(16,16), gc -> gc.draw...));
 *
 * //
 * EhancedStyledText ehancedStyledText = ...
 * ehancedStyledText.setStyledString(styledString);
 * </pre>
 */
public class EhancedStyledText extends StyledText
{
  final Set<ImageStyler> imageStylers = new HashSet<>();
  final Set<ControlStyler> controlStylers = new HashSet<>();
  final Set<HyperLinkStyler> hyperLinkStylers = new HashSet<>();
  final Set<GCStyler> gcStylers = new HashSet<>();

  /**
   * Constructor
   *
   * @param parent
   * @param style
   */
  public EhancedStyledText(Composite parent, int style)
  {
    super(parent, style);

    // use a verify listener to keep the offsets up to date
    addVerifyListener(new StyledVerifyListener());
    addPaintObjectListener(new StyledPaintObjectListener());
    addListener(SWT.MouseDown, new StyledLinkMouseListener());
  }

  /**
   * Set the styledString
   * @param styledString
   */
  public void setStyledString(StyledString styledString)
  {
    imageStylers.clear();
    controlStylers.stream().filter(controlStyler -> controlStyler.control != null && !controlStyler.control.isDisposed()).forEach(controlStyler -> controlStyler.control.dispose());
    controlStylers.clear();
    hyperLinkStylers.clear();
    gcStylers.clear();

    setText(styledString.getString());

    StyleRange[] styleRanges = styledString.getStyleRanges();
    setStyleRanges(styleRanges);

    for(StyleRange styleRange : styleRanges)
    {
      if (styleRange.data instanceof ImageStyler)
      {
        if (styleRange.length != 1)
          throw new RuntimeException("Cannot create " + getClass().getSimpleName() + ": text length for image != 1");
        ImageStyler imageStyler = (ImageStyler) styleRange.data;
        imageStyler.start = styleRange.start;
        imageStylers.add(imageStyler);
      }
      else if (styleRange.data instanceof ControlStyler)
      {
        if (styleRange.length != 1)
          throw new RuntimeException("Cannot create " + getClass().getSimpleName() + ": text length for control != 1");
        ControlStyler controlStyler = (ControlStyler) styleRange.data;

        // create control
        controlStyler.control = controlStyler.controlCreator.apply(this);

        // change size
        Point size = controlStyler.control.getSize();
        if (size.x == 0 || size.y == 0)
        {
          controlStyler.control.pack();
          size = controlStyler.control.getSize();
        }
        controlStyler.start = styleRange.start;
        controlStylers.add(controlStyler);

        // change style.metrics
        for(TextStyle style : controlStyler.textStyles)
          style.metrics = new GlyphMetrics(Math.max(0, size.y + controlStyler.y_offset), Math.max(0, -controlStyler.y_offset), size.x);
        controlStyler.textStyles.clear();
      }
      else if (styleRange.data instanceof HyperLinkStyler)
      {
        HyperLinkStyler hyperLinkStyler = (HyperLinkStyler) styleRange.data;
        if (styleRange.length == 0)
          throw new RuntimeException("Cannot create " + getClass().getSimpleName() + ": text length for link == 0");
        hyperLinkStyler.start = styleRange.start;
        hyperLinkStyler.length = styleRange.length;
        hyperLinkStylers.add(hyperLinkStyler);
      }
      else if (styleRange.data instanceof GCStyler)
      {
        if (styleRange.length != 1)
          throw new RuntimeException("Cannot create " + getClass().getSimpleName() + ": text length for gc != 1");
        GCStyler gcStyler = (GCStyler) styleRange.data;
        gcStyler.start = styleRange.start;
        gcStylers.add(gcStyler);
      }
    }

    redraw();
  }

  /**
   * The class <b>StyledLinkMouseListener</b> allows to.<br>
   */
  private class StyledLinkMouseListener implements Listener
  {
    @Override
    public void handleEvent(Event event)
    {
      try
      {
        int offset = getOffsetAtPoint(new Point(event.x, event.y));
        if (offset != -1)
        {
          StyleRange style = getStyleRangeAtOffset(offset);
          if (style != null && style.underline && style.underlineStyle == SWT.UNDERLINE_LINK)
          {
            if (style.data instanceof HyperLinkStyler)
            {
              HyperLinkStyler hyperLinkStyler = (HyperLinkStyler) style.data;
              hyperLinkStyler.consumer.accept(event, hyperLinkStyler);
            }
          }
        }
      }
      catch(Exception e)
      {
      }
    }
  }

  /**
   * The class <b>StyledPaintObjectListener</b> allows to.<br>
   */
  private class StyledPaintObjectListener implements PaintObjectListener
  {
    @Override
    public void paintObject(PaintObjectEvent e)
    {
      paintImage(e);
      paintControl(e);
      paintGC(e);
    }

    private void paintControl(PaintObjectEvent e)
    {
      StyleRange style = e.style;
      int start = style.start;
      int x = e.x;
      int y = e.y + e.ascent;

      for(ControlStyler controlStyler : controlStylers)
      {
        if (start == controlStyler.start)
        {
          if (style.metrics.descent == 0)
            y -= style.metrics.ascent;
          else
            y += style.metrics.descent - controlStyler.control.getSize().y;

          controlStyler.control.setLocation(x, y);

          return;
        }
      }
    }

    private void paintImage(PaintObjectEvent e)
    {
      GC gc = e.gc;
      StyleRange style = e.style;
      int start = style.start;
      int x = e.x;
      int y = e.y + e.ascent;

      for(ImageStyler imageStyler : imageStylers)
      {
        if (start == imageStyler.start)
        {
          if (style.metrics.descent == 0)
            y -= style.metrics.ascent;
          else
            y += style.metrics.descent - imageStyler.image.getBounds().height;

          gc.drawImage(imageStyler.image, x, y);

          return;
        }
      }
    }

    private void paintGC(PaintObjectEvent e)
    {
      GC gc = e.gc;
      StyleRange style = e.style;
      int start = style.start;
      int x = e.x;
      int y = e.y + e.ascent;

      for(GCStyler paintStyler : gcStylers)
      {
        if (start == paintStyler.start)
        {
          if (style.metrics.descent == 0)
            y -= style.metrics.ascent;
          else
            y += style.metrics.descent - paintStyler.size.y;

          Rectangle oldClipping = gc.getClipping();
          Transform oldTransform = new Transform(gc.getDevice());
          gc.getTransform(oldTransform);

          Transform newTransform = new Transform(gc.getDevice());
          newTransform.translate(x, y);
          gc.setTransform(newTransform);
          gc.setClipping(0, 0, paintStyler.size.x, paintStyler.size.y);

          try
          {
            paintStyler.consumer.accept(gc);
          }
          finally
          {
            gc.setTransform(oldTransform);
            gc.setClipping(oldClipping);

            oldTransform.dispose();
            newTransform.dispose();
          }

          return;
        }
      }
    }
  }

  /**
   * The class <b>StyledVerifyListener</b> allows to.<br>
   */
  private class StyledVerifyListener implements VerifyListener
  {
    @Override
    public void verifyText(VerifyEvent e)
    {
      verifyImage(e);
      verifyControl(e);
      verifyLink(e);
      verifyGC(e);
    }

    private void verifyLink(VerifyEvent e)
    {
      int start = e.start;
      int end = e.end;
      int replaceCharCount = e.end - e.start;
      int newCharCount = e.text.length();

      Set<HyperLinkStyler> currentHyperLinkStylers = new HashSet<>(hyperLinkStylers);
      currentHyperLinkStylers.stream().forEach(hyperLinkStyler -> {
        if (start <= hyperLinkStyler.start)
        {
          if (end <= hyperLinkStyler.start)
          {
            hyperLinkStyler.start -= replaceCharCount - newCharCount;
          }
          else if (end < hyperLinkStyler.start + hyperLinkStyler.length)
          {
            hyperLinkStyler.length -= end - hyperLinkStyler.start;
            hyperLinkStyler.start = start + newCharCount;
          }
          else
          {
            hyperLinkStylers.remove(hyperLinkStyler);
          }
        }
        else if (start < hyperLinkStyler.start + hyperLinkStyler.length)
        {
          if (end < hyperLinkStyler.start + hyperLinkStyler.length)
          {
            if (newCharCount > 0)
            {
              HyperLinkStyler newHyperLinkStyler = new HyperLinkStyler(hyperLinkStyler.consumer);
              newHyperLinkStyler.start = end + newCharCount;
              newHyperLinkStyler.length = hyperLinkStyler.length - (start - hyperLinkStyler.start);
              hyperLinkStylers.add(newHyperLinkStyler);

              StyleRange newStyleRange = (StyleRange) getStyleRangeAtOffset(end).clone();
              newStyleRange.data = newHyperLinkStyler;
              newStyleRange.start = end;
              newStyleRange.length = newHyperLinkStyler.length;
              replaceStyleRanges(newHyperLinkStyler.start, newHyperLinkStyler.length, new StyleRange[]{newStyleRange});

              hyperLinkStyler.length = start - hyperLinkStyler.start;
            }
            else
            {
              hyperLinkStyler.length -= replaceCharCount;
            }
          }
          else
          {
            hyperLinkStyler.length = start - hyperLinkStyler.start;
          }
        }
      });
    }

    private void verifyControl(VerifyEvent e)
    {
      int start = e.start;
      int replaceCharCount = e.end - e.start;
      int newCharCount = e.text.length();

      for(Iterator<ControlStyler> iterator = controlStylers.iterator(); iterator.hasNext();)
      {
        ControlStyler controlStyler = iterator.next();
        if (start <= controlStyler.start && controlStyler.start < start + replaceCharCount)
        {
          // this control is being deleted from the text
          if (controlStyler.control != null && !controlStyler.control.isDisposed())
            controlStyler.control.dispose();
          controlStyler.control = null;
          iterator.remove();
          continue;
        }
        if (start <= controlStyler.start)
          controlStyler.start += newCharCount - replaceCharCount;
      }
    }

    private void verifyGC(VerifyEvent e)
    {
      int start = e.start;
      int replaceCharCount = e.end - e.start;
      int newCharCount = e.text.length();

      for(Iterator<GCStyler> iterator = gcStylers.iterator(); iterator.hasNext();)
      {
        GCStyler gcStyler = iterator.next();
        if (start <= gcStyler.start && gcStyler.start < start + replaceCharCount)
        {
          iterator.remove();
          continue;
        }
        if (start <= gcStyler.start)
          gcStyler.start += newCharCount - replaceCharCount;
      }
    }

    private void verifyImage(VerifyEvent e)
    {
      int start = e.start;
      int replaceCharCount = e.end - e.start;
      int newCharCount = e.text.length();

      for(Iterator<ImageStyler> iterator = imageStylers.iterator(); iterator.hasNext();)
      {
        ImageStyler imageStyler = iterator.next();
        if (start <= imageStyler.start && imageStyler.start < start + replaceCharCount)
        {
          // this image is being deleted from the text
          if (imageStyler.image != null && !imageStyler.image.isDisposed())
            imageStyler.image.dispose();
          iterator.remove();
          continue;
        }
        if (start <= imageStyler.start)
          imageStyler.start += newCharCount - replaceCharCount;
      }
    }
  }

  /**
   * The class <b>ImageStyler</b> allows to.<br>
   */
  public static class ImageStyler extends Styler
  {
    final Image image;
    final int y_offset;
    int start;

    public ImageStyler(Image image, int y_offset)
    {
      this.image = image;
      this.y_offset = y_offset;
    }

    public ImageStyler(Image image)
    {
      this(image, 0);
    }

    @Override
    public void applyStyles(TextStyle style)
    {
      style.data = this;
      Rectangle rect = image.getBounds();
      style.metrics = new GlyphMetrics(Math.max(0, rect.height + y_offset), Math.max(0, -y_offset), rect.width);
    }
  }

  /**
   * The class <b>ControlStyler</b> allows to.<br>
   */
  public static class ControlStyler extends Styler
  {
    final Function<EhancedStyledText, Control> controlCreator;
    final int y_offset;
    int start;
    Control control;
    Set<TextStyle> textStyles = new HashSet<>();

    public ControlStyler(Function<EhancedStyledText, Control> controlCreator, int y_offset)
    {
      this.controlCreator = controlCreator;
      this.y_offset = y_offset;
    }

    public ControlStyler(Function<EhancedStyledText, Control> controlCreator)
    {
      this(controlCreator, 0);
    }

    @Override
    public void applyStyles(TextStyle style)
    {
      style.data = this;
      textStyles.add(style);
    }
  }

  /**
   * The class <b>HyperLinkStyler</b> allows to.<br>
   */
  public static class HyperLinkStyler extends Styler
  {
    final BiConsumer<Event, HyperLinkStyler> consumer;
    public int start;
    public int length;

    public HyperLinkStyler(BiConsumer<Event, HyperLinkStyler> consumer)
    {
      this.consumer = consumer;
    }

    @Override
    public void applyStyles(TextStyle style)
    {
      style.data = this;
      style.underline = true;
      style.underlineStyle = SWT.UNDERLINE_LINK;
      if (style instanceof StyleRange)
      {
        StyleRange styleRange = (StyleRange) style;
        start = styleRange.start;
        length = styleRange.length;
      }
    }

    @Override
    public String toString()
    {
      return "HyperLinkStyler[start=" + start + ", length=" + length + "]";
    }
  }

  /**
   * The class <b>GCStyler</b> allows to.<br>
   */
  public static class GCStyler extends Styler
  {
    final Consumer<GC> consumer;
    final int y_offset;
    final Point size;
    int start;

    public GCStyler(int y_offset, Point size, Consumer<GC> consumer)
    {
      this.y_offset = y_offset;
      this.size = size;
      this.consumer = consumer;
    }

    @Override
    public void applyStyles(TextStyle style)
    {
      style.data = this;
      style.metrics = new GlyphMetrics(Math.max(0, size.y + y_offset), Math.max(0, -y_offset), size.x);
    }
  }
}
