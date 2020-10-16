package cl.plugin.consistency.custom;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * The class <b>StyledToolTip</b> use {@link EhancedStyledText} to give style to tooltip.
 *
 * <pre>
 * new StyledToolTip(control, event -> {
 *   if (condition_not_to_show_tooltip)
 *     return null;
 *   StyledString styledString = new StyledString();
 *   styledString.append("text");
 *   styledString.append(" (counter)", StyledString.COUNTER_STYLER);
 *   ...
 *   // add image
 *   styledString.append(" ", new ImageStyler(image));
 *   return styledString;
 * });
 * </pre>
 */
public class StyledToolTip extends ToolTip
{
  private final Function<Event, StyledString> styledStringFunction;

  /**
   * Constructor
   *
   * @param control
   * @param styledStringFunction Give the text for tooltip from the event
   */
  public StyledToolTip(Control control, Function<Event, StyledString> styledStringFunction)
  {
    super(control);
    this.styledStringFunction = styledStringFunction;

    setShift(new Point(0, 24));
  }

  /**
   * Constructor
   *
   * @param control
   * @param styledString
   */
  public StyledToolTip(Control control, StyledString styledString)
  {
    this(control, e -> styledString);
  }

  /**
   * Constructor
   *
   * @param table
   * @param styledStringFunction Give the styledString for tooltip from row and column
   */
  public StyledToolTip(Table table, BiFunction<Integer, Integer, StyledString> styledStringFunction)
  {
    this(table, getFunction(table, styledStringFunction));
  }

  private static Function<Event, StyledString> getFunction(Table table, BiFunction<Integer, Integer, StyledString> styledStringFunction)
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
            StyledString text = styledStringFunction.apply(row, column);
            if (text != null)
              return text;
          }
        }
      }
      return styledStringFunction.apply(-1, -1);
    };
  }

  /**
   * Constructor
   *
   * @param tree
   * @param styledStringFunction Give the styledString for tooltip from row and column
   */
  public StyledToolTip(Tree tree, BiFunction<Integer, Integer, StyledString> styledStringFunction)
  {
    this(tree, getFunction(tree, styledStringFunction));
  }

  private static Function<Event, StyledString> getFunction(Tree tree, BiFunction<Integer, Integer, StyledString> styledStringFunction)
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
            StyledString text = styledStringFunction.apply(row, column);
            if (text != null)
              return text;
          }
        }
      }
      return styledStringFunction.apply(-1, -1);
    };
  }

  @Override
  protected StyledText createToolTipContentArea(Event event, Composite parent)
  {
    EhancedStyledText styledText = new EhancedStyledText(parent, SWT.READ_ONLY | SWT.WRAP);
    styledText.setEditable(false);
    styledText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

    StyledString styledString = styledStringFunction.apply(event);
    styledText.setStyledString(styledString);

    return styledText;
  }

  @Override
  protected boolean shouldCreateToolTip(Event event)
  {
    return styledStringFunction.apply(event) != null;
  }
}