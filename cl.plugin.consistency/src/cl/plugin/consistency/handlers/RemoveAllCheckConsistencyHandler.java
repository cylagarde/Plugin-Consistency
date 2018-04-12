package cl.plugin.consistency.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;

import cl.plugin.consistency.Util;

/**
 * The class <b>RemoveAllCheckConsistencyHandler</b> allows to launch check consistency on all projects in the workspace.<br>
 */
public class RemoveAllCheckConsistencyHandler extends AbstractHandler
{
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    Util.removeAllCheckProjectConsistency(new NullProgressMonitor());
    return null;
  }
}
