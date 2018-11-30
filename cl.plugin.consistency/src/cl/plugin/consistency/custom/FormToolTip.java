package cl.plugin.consistency.custom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * A tooltip for a {@link Control} which shows a text that can be marked up using Eclipse UI Forms {@link FormText} tags.
 * See forum post <a href="http://www.eclipse.org/forums/index.php?t=msg&goto=551364&S=15e3e271d91f521d26beb7b2ba809148#msg_551364"
 */
public class FormToolTip extends ToolTip {
  private final Map<String, RGB> tagColorMap = new HashMap<>();
  private final Map<String, Font> tagFontMap = new HashMap<>();

  private final Function<Event, String> textFunction;

  /**
   * Constructor
   *
   * @param control
   * @param textFunction Give the text for tooltip from the event
   */
  public FormToolTip(Control control, Function<Event, String> textFunction)
  {
    super(control);
    this.textFunction = textFunction;

    addDefaultTagColors();
    setShift(new Point(0, 24));
  }

  /**
   * Constructor
   *
   * @param control
   * @param text
   */
  public FormToolTip(Control control, String text)
  {
    this(control, e -> text);
  }

  /**
   * Constructor
   *
   * @param table
   * @param textFunction Give the text for tooltip from row and column
   */
  public FormToolTip(Table table, BiFunction<Integer, Integer, String> textFunction)
  {
    this(table, getFunction(table, textFunction));
  }

  private static Function<Event, String> getFunction(Table table, BiFunction<Integer, Integer, String> textFunction)
  {
    return event -> {
      TableItem item = table.getItem(new Point(event.x, event.y));
      if (item != null)
      {
        int columnCount = table.getColumnCount();
        for(int column = 0; column < columnCount; column++)
        {
          Rectangle rect = item.getBounds(column);
          if (rect.contains(event.x, event.y))
          {
            int row = table.indexOf(item);
            String text = textFunction.apply(row, column);
            if (text != null)
              return text;
          }
        }
      }
      return textFunction.apply(-1, -1);
    };
  }

  /**
   * Constructor
   *
   * @param tree
   * @param textFunction Give the text for tooltip from row and column
   */
  public FormToolTip(Tree tree, BiFunction<Integer, Integer, String> textFunction)
  {
    this(tree, getFunction(tree, textFunction));
  }

  private static Function<Event, String> getFunction(Tree tree, BiFunction<Integer, Integer, String> textFunction)
  {
    return event -> {
      TreeItem item = tree.getItem(new Point(event.x, event.y));
      if (item != null)
      {
        int columnCount = tree.getColumnCount();
        for(int column = 0; column < columnCount; column++)
        {
          Rectangle rect = item.getBounds(column);
          if (rect.contains(event.x, event.y))
          {
            int row = tree.indexOf(item);
            String text = textFunction.apply(row, column);
            if (text != null)
              return text;
          }
        }
      }
      return textFunction.apply(-1, -1);
    };
  }

  private void addDefaultTagColors()
  {
    addTagColor("red", new RGB(255, 0, 0));
    addTagColor("green", new RGB(0, 255, 0));
    addTagColor("blue", new RGB(0, 0, 255));
  }

  /**
   * Add tag color.
   * for example <span color=\"red\">rouge</span>.
   * addTagColor("red", new RGB(255, 0, 0));
   *
   * @param tag
   * @param color
   */
  public void addTagColor(String tag, RGB color)
  {
    tagColorMap.put(tag, color);
  }

  /**
   * Add tag font.
   * for example <span font=\"red\">rouge</span>.
   * addTagFont("red", JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
   *
   * @param tag
   * @param font
   */
  public void addTagFont(String tag, Font font)
  {
    tagFontMap.put(tag, font);
  }

	@Override
  protected FormText createToolTipContentArea(Event event, Composite parent)
  {
    FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    Form form = toolkit.createForm(parent);
    form.getBody().setLayout(new FillLayout());

    // Parse tags but do not expand URLs because they don't make much sense in a tooltip.
    FormText formText = toolkit.createFormText(form.getBody(), true);
    formText.setWhitespaceNormalized(false);

    formText.setText(textFunction.apply(event), true, false);

    formText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

    FormColors colors = toolkit.getColors();
    addColors(formText, colors);
    addFonts(formText);

    return formText;
	}

  @Override
  protected boolean shouldCreateToolTip(Event event)
  {
    return textFunction.apply(event) != null;
  }

  private void addColors(FormText formText, FormColors colors)
  {
    tagColorMap.forEach((tag, rgb) -> {
      colors.createColor(tag, rgb);
      formText.setColor(tag, colors.getColor(tag));
    });
  }

  private void addFonts(FormText formText)
  {
    tagFontMap.forEach((tag, font) -> {
      formText.setFont(tag, font);
    });
  }
}