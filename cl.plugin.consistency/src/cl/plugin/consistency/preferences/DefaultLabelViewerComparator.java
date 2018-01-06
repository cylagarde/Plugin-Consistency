package cl.plugin.consistency.preferences;

import java.util.Comparator;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * The class <b>DefaultLabelViewerComparator</b> allows to.<br>
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
    ColumnLabelProvider columnLabelProvider = (ColumnLabelProvider) ((TableViewer)viewer).getLabelProvider(columnIndex);

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
  public static void configureForSortingColumn(TableViewerColumn tableViewerColumn) {
    TableViewer tableViewer = (TableViewer) tableViewerColumn.getViewer();
    int columnIndex = 0;
    for(; columnIndex < tableViewer.getTable().getColumnCount(); columnIndex++)
    {
      if (tableViewer.getTable().getColumn(columnIndex) == tableViewerColumn.getColumn())
        break;
    }
    tableViewerColumn.getColumn().addSelectionListener(getSelectionAdapter(tableViewer, columnIndex));
  }
}
