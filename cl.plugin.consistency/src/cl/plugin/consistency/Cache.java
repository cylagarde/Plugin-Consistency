package cl.plugin.consistency;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

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
      validProjects = Util.getValidProjects();
    return validProjects;
  }

  Map<Object, String> elementToIdCacheMap = new HashMap<>();

  /**
   * Get plugin id from cache
   * @param o IProject or Bundle
   */
  public String getIdInCache(Object o)
  {
    String id = elementToIdCacheMap.get(o);
    if (id == null)
    {
      if (o instanceof IProject)
        id = Util.getPluginId((IProject) o);
      else
        id = ((Bundle) o).getSymbolicName();

      elementToIdCacheMap.put(o, id);
    }

    return id;
  }

  Map<IProject, Boolean> isValidPluginWithCacheMap = new HashMap<>();

  /**
   * Return if project is valid (use cache to speed)
   * @param project
   */
  public boolean isValidProjectWithCache(IProject project)
  {
    return isValidPluginWithCacheMap.computeIfAbsent(project, Util::isValidPlugin);
  }
}
