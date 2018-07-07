package cl.plugin.consistency.handlers;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PluginConsistency;

/**
 * The class <b>LaunchCheckConsistencyHandler</b> allows to launch check consistency on all projects in the workspace.<br>
 */
public class LaunchCheckConsistencyHandler extends AbstractHandler
{
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    Shell shell = HandlerUtil.getActiveShell(event);
    PluginConsistency pluginConsistency = PluginConsistencyActivator.getDefault().getPluginConsistency();
    Util.updatePluginConsistency(pluginConsistency);
    launchConsistencyCheck(shell, pluginConsistency);
    return null;
  }

  /**
   * Launch consistency check
   * @param shell
   * @param pluginConsistency
   */
  public static void launchConsistencyCheck(Shell shell, PluginConsistency pluginConsistency)
  {
    Consumer<List<IMarker>> markerConsumer = markers -> {
      Display.getDefault().asyncExec(() -> {
        if (markers.isEmpty())
          MessageDialog.openInformation(shell, "Project consistency", "No problem found");
        else
          MessageDialog.openWarning(shell, "Project consistency", markers.size() + (markers.size() == 1? " problem was" : " problems were") + " found.\nSee 'Problems' view for details.");
      });
    };
    Util.launchConsistencyCheck(pluginConsistency, markerConsumer);
  }
}
