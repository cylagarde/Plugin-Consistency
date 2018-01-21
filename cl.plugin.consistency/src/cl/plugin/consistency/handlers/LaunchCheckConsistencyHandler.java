package cl.plugin.consistency.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PluginConsistency;

/**
 * The class <b>LaunchCheckConsistencyHandler</b> allows to.<br>
 */
public class LaunchCheckConsistencyHandler extends AbstractHandler
{
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    PluginConsistency pluginConsistency = PluginConsistencyActivator.getDefault().getPluginConsistency();
    Util.launchConsistencyCheck(pluginConsistency);

    return null;
  }
}
