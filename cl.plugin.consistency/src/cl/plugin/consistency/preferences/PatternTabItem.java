package cl.plugin.consistency.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabItem;

/**
 *
 */
public class PatternTabItem
{
  final PluginTabFolder pluginTabFolder;

  /**
   * Constructor
   */
  public PatternTabItem(PluginTabFolder pluginTabFolder)
  {
    this.pluginTabFolder = pluginTabFolder;

    //
    TabItem patternTabItem = new TabItem(pluginTabFolder.tabFolder, SWT.NONE);
    patternTabItem.setText("Patterns");

    //
    Composite pluginTabComposite = new Composite(pluginTabFolder.tabFolder, SWT.NONE);
    patternTabItem.setControl(pluginTabComposite);

    GridLayout pluginTabCompositeLayout = new GridLayout(1, false);
    pluginTabCompositeLayout.marginWidth = pluginTabCompositeLayout.marginHeight = 0;
    pluginTabCompositeLayout.verticalSpacing = 10;
    pluginTabComposite.setLayout(pluginTabCompositeLayout);

    //    configureProjectSashForm(pluginTabComposite);
  }

  /**
   *
   */
  void refresh()
  {
    System.err.println("TODO refresh PatternTabItem");
  }
}
