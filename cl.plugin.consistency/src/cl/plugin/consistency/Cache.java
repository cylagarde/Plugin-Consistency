package cl.plugin.consistency;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.pde.internal.core.natures.PDE;

import cl.plugin.consistency.custom.NaturalOrderComparator;

/**
 * The class <b>Cache</b> allows to cache some informations.<br>
 */
public class Cache
{
  IProject[] validProjects;

  /**
   * Return
   */
  public IProject[] getValidProjects()
  {
    if (validProjects == null)
    {
      IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
      validProjects = Stream.of(projects).filter(this::isValidProject).toArray(IProject[]::new);
    }
    return validProjects;
  }

  Map<Object, String> elementToIdCacheMap = new HashMap<>();

  /**
   * Get plugin id from cache
   * @param o IProject or Bundle
   */
  public String getId(Object o)
  {
    String id = elementToIdCacheMap.get(o);
    if (id == null)
    {
      id = Util.getId(o);
      if (id != null)
        elementToIdCacheMap.put(o, id);
      else
        PluginConsistencyActivator.logWarning("id is null for '" + o + "'");
    }

    return id;
  }

  Map<IProject, Boolean> isValidPluginWithCacheMap = new HashMap<>();

  /**
   * Return if project is valid (use cache to speed)
   * @param project
   */
  public boolean isValidProject(IProject project)
  {
    return isValidPluginWithCacheMap.computeIfAbsent(project, this::isValidPlugin);
  }

  /**
   * Return true if project is open and has PluginNature
   *
   * @param project
   */
  private boolean isValidPlugin(IProject project)
  {
    try
    {
      if (project.isOpen() && project.hasNature(PDE.PLUGIN_NATURE) && getId(project) != null)
        return true;
    }
    catch(Exception e)
    {
      PluginConsistencyActivator.logError("Error: " + e, e);
    }
    return false;
  }

  /**
   * Return comparator using id
   */
  public Comparator<Object> getPluginIdComparator()
  {
    return Comparator.comparing(this::getId, NaturalOrderComparator.INSTANCE);
  }
}
