package cl.plugin.consistency;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.natures.PDE;

import cl.plugin.consistency.custom.NaturalOrderComparator;

/**
 * The class <b>Cache</b> allows to cache some informations.<br>
 */
public class Cache
{
  final Map<Object, String> elementToIdCacheMap = new HashMap<>();
  final Map<IProject, Boolean> isValidPluginWithCacheMap = new HashMap<>();
  IProject[] validProjects;
  Map<String, IPluginModelBase> idToPluginModelBases;

  /**
   * Return
   */
  public Stream<IProject> getValidProjects()
  {
    if (validProjects == null)
    {
      IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
      validProjects = Stream.of(projects).filter(this::isValidProject).toArray(IProject[]::new);
    }
    return Stream.of(validProjects);
  }

  /**
   * Return all plugin model bases
   */
  public Map<String, IPluginModelBase> getPluginModelBases()
  {
    if (idToPluginModelBases == null)
    {
      idToPluginModelBases = new TreeMap<>();
      for(IPluginModelBase pluginModelBase : PluginRegistry.getActiveModels(false))
        idToPluginModelBases.put(getId(pluginModelBase), pluginModelBase);
      idToPluginModelBases = Collections.unmodifiableMap(idToPluginModelBases);
    }

    return idToPluginModelBases;
  }

  /**
   * Get plugin id from cache
   * @param o IProject or Bundle
   */
  public String getId(Object o)
  {
    String id = elementToIdCacheMap.computeIfAbsent(o, Util::getId);
    if (id == null)
      PluginConsistencyActivator.logWarning("id is null for '" + o + "'");

    return id;
  }

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
