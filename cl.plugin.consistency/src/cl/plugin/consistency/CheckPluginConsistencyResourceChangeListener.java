package cl.plugin.consistency;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

import cl.plugin.consistency.model.PluginConsistency;
import cl.plugin.consistency.model.PluginInfo;

/**
 * The class <b>CheckPluginConsistencyResourceChangeListener</b> allows to.<br>
 */
class CheckPluginConsistencyResourceChangeListener implements IResourceChangeListener
{
  ManifestChangedResourceDeltaVisitor manifestChangedResourceDeltaVisitor = new ManifestChangedResourceDeltaVisitor();

  @Override
  public void resourceChanged(IResourceChangeEvent event)
  {
    if (!Activator.getDefault().isPluginConsistencyActivated())
      return;

    //
    switch(event.getType())
    {
      case IResourceChangeEvent.POST_CHANGE:
        try
        {
          event.getDelta().accept(manifestChangedResourceDeltaVisitor);
        }
        catch(CoreException e)
        {
          Activator.logError("Error: "+e, e);
        }
        break;
    }
  }

  /**
   * @param project
   */
  private void launchProjectConsistency(IProject project)
  {
    // check
    try
    {
      PluginConsistency pluginConsistency = Activator.getDefault().getPluginConsistency();
      if (pluginConsistency != null)
      {
        Util.checkProjectConsistency(pluginConsistency, project);
      }
    }
    catch(Exception e)
    {
      Activator.logError("Cannot check consistency on project "+project.getName(), e);
    }
  }

  //  /**
  //   * The class <b>PostBuildResourceDeltaVisitor</b>.<br>
  //   */
  //  class PostBuildResourceDeltaVisitor implements IResourceDeltaVisitor
  //  {
  //    @Override
  //    public boolean visit(IResourceDelta delta)
  //    {
  //      IResource res = delta.getResource();
  //      System.out.println("POST_BUILD " + res + " " + delta.getFlags());
  //
  //      //      if (doTreatment && res.getType() == IResource.PROJECT && res.getProject().getFolder("META-INF").exists())
  //      //      {
  //      ////        System.out.println("res "+res+" "+res.getProject());
  //      //        IProject project = res.getProject();
  //      //        launchProjectConsistency(project);
  //      //        return false;
  //      //      }
  //
  //      return true;
  //    }
  //  }

  /**
   * The class <b>ManifestChangedResourceDeltaVisitor</b>.<br>
   */
  class ManifestChangedResourceDeltaVisitor implements IResourceDeltaVisitor
  {
    Optional<PluginInfo> removedPluginInfoOptional;
    IProject addedProject;

    @Override
    public boolean visit(IResourceDelta delta)
    {
      IResource res = delta.getResource();
      //      System.out.println("POST_CHANGE " + res + " " + delta.getFlags() + "  " + delta.getKind());

      if (res.getType() == IResource.ROOT)
        return true;
      if (res.getType() == IResource.PROJECT)
      {
        checkAndUpdateForRenamingProject(delta);

        IProject project = res.getProject();
        launchProjectConsistency(project);
      }
      return false;

      //      if (res.getType() == IResource.FOLDER)
      //      {
      //        return "META-INF".equals(res.getName());
      //      }
      //      //          System.out.println(res + " " + delta.getKind() + " >" + delta.getFlags());
      //      if ((delta.getFlags() & IResourceDelta.CONTENT) != 0)
      //      {
      //        if ("MANIFEST.MF".equals(res.getName()))
      //        {
      //          IProject project = res.getProject();
      //          launchProjectConsistency(project);
      //        }
      //
      //        return false;
      //      }
      //
      //      return true; // visit the children
    }

    /**
     * @param delta
     * @param res
     */
    private void checkAndUpdateForRenamingProject(IResourceDelta delta)
    {
      IProject project = delta.getResource().getProject();
      if (delta.getKind() == IResourceDelta.REMOVED)
      {
        //        System.out.println("-------------REMOVED " + project.getName());
        PluginConsistency pluginConsistency = Activator.getDefault().getPluginConsistency();
        if (addedProject != null)
        {
          removedPluginInfoOptional = pluginConsistency.pluginInfoList.stream().filter(pluginInfo -> pluginInfo.name.equals(project.getName())).filter(pluginInfo -> pluginInfo.isModified()).findAny();
          if (removedPluginInfoOptional != null && removedPluginInfoOptional.isPresent())
          {
            PluginInfo pluginInfo = removedPluginInfoOptional.get();
            if (pluginInfo.id.equals(Util.getPluginId(addedProject)))
            {
              pluginInfo.name = addedProject.getName();
              File consistencyFile = new File(Activator.getDefault().getConsistencyFilePath());
              Util.savePluginConsistency(pluginConsistency, consistencyFile);

              // check
              launchProjectConsistency(addedProject);
            }
            removedPluginInfoOptional = null;
          }
          addedProject = null;
        }
        else
          removedPluginInfoOptional = pluginConsistency.pluginInfoList.stream().filter(pluginInfo -> pluginInfo.name.equals(project.getName())).filter(pluginInfo -> pluginInfo.isModified()).findAny();
      }
      else if (delta.getKind() == IResourceDelta.ADDED)
      {
        //        System.out.println("+++++++++++++ADDED " + project.getName());
        PluginConsistency pluginConsistency = Activator.getDefault().getPluginConsistency();
        if (removedPluginInfoOptional != null && removedPluginInfoOptional.isPresent())
        {
          PluginInfo pluginInfo = removedPluginInfoOptional.get();
          if (pluginInfo.id.equals(Util.getPluginId(project)))
          {
            pluginInfo.name = project.getName();
            File consistencyFile = new File(Activator.getDefault().getConsistencyFilePath());
            Util.savePluginConsistency(pluginConsistency, consistencyFile);

            // check
            launchProjectConsistency(project);
          }
          removedPluginInfoOptional = null;
        }
        else
          addedProject = project;
      }
    }
  }
}
