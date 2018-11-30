package cl.plugin.consistency.custom;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

// TODO gerer le add/delete char in link
/**
 * The class <b>EhancedStyledText</b> allows to.<br>
 *
 * <pre lang="java">
 * EhancedStyledText ehancedStyledText = ...
 * StyledString styledString = new StyledString();
 * styledString.append("text");
 * styledString.append(" (counter)", StyledString.COUNTER_STYLER);
 * // add image
 * styledString.append(" ", new ImageStyler(image));
 * // add control
 * styledString.append(" ", new ControlStyler(parent -> {
 *     Button button = new Button(parent, SWT.PUSH);
 *     button.setText("Push");
 *     return button;
 * }));
 * // add link
 * styledString.append("site", new HyperLinkStyler(mouseEvent -> true, () -> System.out.println("click")));
 * </pre>
 */
public class EhancedStyledText extends StyledText
{
  final Set<ImageStyler> imageStylers = new HashSet<>();
  final Set<ControlStyler> controlStylers = new HashSet<>();
  final Set<HyperLinkStyler> hyperLinkStylers = new HashSet<>();

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

  public void setStyledString(StyledString styledString)
  {
    imageStylers.clear();
    controlStylers.clear();
    hyperLinkStylers.clear();

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
    }

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
        int offset = getOffsetAtLocation(new Point(event.x, event.y));
        if (offset != -1)
        {
          StyleRange style = getStyleRangeAtOffset(offset);
          if (style != null && style.underline && style.underlineStyle == SWT.UNDERLINE_LINK)
          {
            if (style.data instanceof HyperLinkStyler)
            {
              HyperLinkStyler hyperLinkStyler = (HyperLinkStyler) style.data;
              if (hyperLinkStyler.clickOnEvent.test(event))
                hyperLinkStyler.runnable.run();
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
    }

    private void verifyLink(VerifyEvent e)
    {
      int start = e.start;
      int replaceCharCount = e.end - e.start;
      int newCharCount = e.text.length();

      for(Iterator<HyperLinkStyler> iterator = hyperLinkStylers.iterator(); iterator.hasNext();)
      {
        HyperLinkStyler hyperLinkStyler = iterator.next();

        if (start <= hyperLinkStyler.start && start + replaceCharCount >= hyperLinkStyler.start + hyperLinkStyler.length)
        {
          iterator.remove();
          continue;
        }
        if (start + replaceCharCount < hyperLinkStyler.start + hyperLinkStyler.length)
        {
          int newLength = hyperLinkStyler.start + hyperLinkStyler.length - start - replaceCharCount;
          hyperLinkStyler.length = newLength;
        }
        if (start <= hyperLinkStyler.start)
        {
          int newStart = start + newCharCount;
          hyperLinkStyler.start = newStart;
        }
      }
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
    final Function<Composite, Control> controlCreator;
    final int y_offset;
    int start;
    Control control;
    Set<TextStyle> textStyles = new HashSet<>();

    public ControlStyler(Function<Composite, Control> controlCreator, int y_offset)
    {
      this.controlCreator = controlCreator;
      this.y_offset = y_offset;
    }

    public ControlStyler(Function<Composite, Control> controlCreator)
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
    final Predicate<Event> clickOnEvent;
    final Runnable runnable;
    public int start;
    public int length;

    public HyperLinkStyler(Predicate<Event> clickOnEvent, Runnable runnable)
    {
      this.clickOnEvent = clickOnEvent;
      this.runnable = runnable;
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
  }
}
