package cl.plugin.consistency.preferences;

import java.util.Comparator;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * The class <b>DefaultLabelViewerComparator</b> allows to.<br>
 * <li>for TableViewer</li>
 * <code>
 * tableViewer.setComparator(new DefaultLabelViewerComparator());<br>
 * DefaultLabelViewerComparator.configureForSortingColumn(tableViewerColumn);<br>
 * </code><br>
 * <li>for TreeViewer</li>
 * <code>
 * treeViewer.setComparator(new DefaultLabelViewerComparator());<br>
 * DefaultLabelViewerComparator.configureForSortingColumn(treeViewerColumn);<br>
 * </code>
 */
public class DefaultLabelViewerComparator extends ViewerComparator
{
  private int columnIndex;
  private static final int DESCENDING = 1;
  private int direction = DESCENDING;

  public DefaultLabelViewerComparator()
  {
    this.columnIndex = 0;
  }

  public int getDirection()
  {
    return direction == DESCENDING? SWT.DOWN : SWT.UP;
  }

  /**
   * Set sort column
   * @param column
   */
  public void setColumn(int column)
  {
    if (column == this.columnIndex)
    {
      // Same column as last sort; toggle the direction
      direction = -direction;
    }
    else
    {
      // New column; do an ascending sort
      this.columnIndex = column;
      direction = DESCENDING;
    }
  }

  @Override
  public int compare(Viewer viewer, Object e1, Object e2)
  {
    ILabelProvider columnLabelProvider = (ILabelProvider) ((ColumnViewer) viewer).getLabelProvider(columnIndex);

    String t1 = columnLabelProvider.getText(e1);
    String t2 = columnLabelProvider.getText(e2);
    Comparator<String> comparator = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
    int rc = comparator.compare(t1, t2);

    // If descending order, flip the direction
    rc *= direction;

    return rc;
  }

  /**
   * Return SelectionAdapter for column sorting
   * @param tableViewer
   * @param columnIndex
   */
  private static SelectionAdapter getSelectionAdapter(TableViewer tableViewer, int columnIndex)
  {
    SelectionAdapter selectionAdapter = new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        DefaultLabelViewerComparator comparator = (DefaultLabelViewerComparator) tableViewer.getComparator();
        comparator.setColumn(columnIndex);
        int dir = comparator.getDirection();
        tableViewer.getTable().setSortDirection(dir);
        tableViewer.getTable().setSortColumn(tableViewer.getTable().getColumn(columnIndex));
        tableViewer.refresh();
      }
    };
    return selectionAdapter;
  }

  /**
   * Configure TableViewerColumn for sorting
   * @param tableViewerColumn
   */
  public static void configureForSortingColumn(TableViewerColumn tableViewerColumn)
  {
    TableViewer tableViewer = (TableViewer) tableViewerColumn.getViewer();
    int columnIndex = 0;
    for(; columnIndex < tableViewer.getTable().getColumnCount(); columnIndex++)
    {
      if (tableViewer.getTable().getColumn(columnIndex) == tableViewerColumn.getColumn())
        break;
    }
    tableViewerColumn.getColumn().addSelectionListener(getSelectionAdapter(tableViewer, columnIndex));
  }

  /**
   * Return SelectionAdapter for column sorting
   * @param treeViewer
   * @param columnIndex
   */
  private static SelectionAdapter getSelectionAdapter(TreeViewer treeViewer, int columnIndex)
  {
    SelectionAdapter selectionAdapter = new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        DefaultLabelViewerComparator comparator = (DefaultLabelViewerComparator) treeViewer.getComparator();
        comparator.setColumn(columnIndex);
        int dir = comparator.getDirection();
        treeViewer.getTree().setSortDirection(dir);
        treeViewer.getTree().setSortColumn(treeViewer.getTree().getColumn(columnIndex));
        treeViewer.refresh();
      }
    };
    return selectionAdapter;
  }

  /**
   * Configure TreeViewerColumn for sorting
   * @param treeViewerColumn
   */
  public static void configureForSortingColumn(TreeViewerColumn treeViewerColumn)
  {
    TreeViewer treeViewer = (TreeViewer) treeViewerColumn.getViewer();
    int columnIndex = 0;
    for(; columnIndex < treeViewer.getTree().getColumnCount(); columnIndex++)
    {
      if (treeViewer.getTree().getColumn(columnIndex) == treeViewerColumn.getColumn())
        break;
    }
    treeViewerColumn.getColumn().addSelectionListener(getSelectionAdapter(treeViewer, columnIndex));
  }
}
