package cl.plugin.consistency;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import cl.plugin.consistency.model.PluginConsistency;
import cl.plugin.consistency.model.PluginInfo;

/**
 * The class <b>CheckPluginConsistencyResourceChangeListener</b> allows to listen resource change and update consistency file, etc...<br>
 */
class CheckPluginConsistencyResourceChangeListener implements IResourceChangeListener
{
  @Override
  public void resourceChanged(IResourceChangeEvent event)
  {
    //    System.out.println("------------------------resourceChanged");

    //
    switch(event.getType())
    {
      case IResourceChangeEvent.POST_CHANGE:
        try
        {
          event.getDelta().accept(new UpdatePluginConsistencyFileResourceDeltaVisitor());
        }
        catch(Exception e)
        {
          PluginConsistencyActivator.logError("Error: " + e, e);
        }
        break;
    }
  }

  @SuppressWarnings("unused")
  private static String getKind(IResourceDelta delta)
  {
    switch(delta.getKind())
    {
      case IResourceDelta.REMOVED:
        return "IResourceDelta.REMOVED";
      case IResourceDelta.ADDED:
        return "IResourceDelta.ADDED";
      case IResourceDelta.CHANGED:
        return "IResourceDelta.CHANGED";
    }
    return "unknown " + delta.getKind();
  }

  /**
   * The class <b>UpdatePluginConsistencyFileResourceDeltaVisitor</b>.<br>
   */
  class UpdatePluginConsistencyFileResourceDeltaVisitor implements IResourceDeltaVisitor
  {
    Cache cache = new Cache();

    @Override
    public boolean visit(IResourceDelta delta)
    {
      IResource res = delta.getResource();

      if (res.getType() == IResource.ROOT)
        return true;

      //      System.out.println();
      //      System.out.println("POST_CHANGE " + res + " " + delta.getFlags() + "  " + getKind(delta));
      //      System.out.println("moved = " + delta.getMovedFromPath() + " " + delta.getMovedToPath());
      //      System.out.println("getAffectedChildren = " + delta.getAffectedChildren().length);
      //      System.out.println("getProjectRelativePath = " + delta.getProjectRelativePath());

      if (res.getType() == IResource.PROJECT)
        return visitProject(delta);

      if (res.getType() == IResource.FOLDER)
        return visitFolder(delta);

      if (res.getType() == IResource.FILE)
        return visitFile(delta);

      return false;
    }

    /**
     * @param delta
     */
    private boolean visitProject(IResourceDelta delta)
    {
      IProject project = (IProject) delta.getResource();

      if (delta.getKind() == IResourceDelta.REMOVED)
      {
        if (delta.getMovedToPath() != null)
        {
          String consistency_file_path = PluginConsistencyActivator.getDefault().getConsistencyFilePath();
          String prefix = project.getFullPath().toString() + "/";

          // check if consistency path starts with project name
          if (consistency_file_path != null && consistency_file_path.startsWith(prefix))
          {
            String consistency_file_path_without_prefix = consistency_file_path.substring(prefix.length());
            prefix = delta.getMovedToPath().toString() + "/";
            String newConsistencyFilePath = prefix + consistency_file_path_without_prefix;
            PluginConsistencyActivator.getDefault().setConsistencyFilePath(newConsistencyFilePath.toString());
          }

          // update PluginConsistency
          PluginConsistency pluginConsistency = PluginConsistencyActivator.getDefault().getPluginConsistency();
          if (pluginConsistency != null)
          {
            // find PluginInfo with id
            IProject newProject = project.getWorkspace().getRoot().findMember(delta.getMovedToPath()).getProject();
            String projectId = cache.getId(newProject);
            Optional<PluginInfo> pluginInfoOptional = pluginConsistency.pluginInfoList.stream().filter(pluginInfo -> pluginInfo.id.equals(projectId)).findAny();
            if (pluginInfoOptional.isPresent())
            {
              PluginInfo pluginInfo = pluginInfoOptional.get();

              // set new name
              pluginInfo.name = newProject.getName();

              File consistencyFile = Util.getConsistencyFile(PluginConsistencyActivator.getDefault().getConsistencyFilePath());
              if (consistencyFile != null)
              {
                savePluginConsistency(pluginConsistency, consistencyFile);

                // check
                if (pluginInfo.containsInformations())
                  launchProjectConsistency(project);
              }
            }
          }
        }
      }
      else if (delta.getKind() == IResourceDelta.CHANGED)
      {
        // visit children
        return true;
      }

      return false;
    }

    /**
     * @param delta
     */
    private boolean visitFolder(IResourceDelta delta)
    {
      IFolder folder = (IFolder) delta.getResource();

      if ("META-INF".equals(folder.getName()))
        return true;

      // check if consistency path starts with folder name
      if (delta.getKind() == IResourceDelta.REMOVED)
      {
        if (delta.getMovedToPath() != null)
        {
          String consistency_file_path = PluginConsistencyActivator.getDefault().getConsistencyFilePath();
          String prefix = folder.getFullPath().toString() + "/";

          // check if consistency path starts with folder full name
          if (consistency_file_path != null && consistency_file_path.startsWith(prefix))
          {
            String consistency_file_path_without_prefix = consistency_file_path.substring(prefix.length());
            prefix = delta.getMovedToPath().toString() + "/";
            String newConsistencyFilePath = prefix + consistency_file_path_without_prefix;
            PluginConsistencyActivator.getDefault().setConsistencyFilePath(newConsistencyFilePath.toString());
          }
        }
      }
      else if (delta.getKind() == IResourceDelta.CHANGED)
      {
        String consistency_file_path = PluginConsistencyActivator.getDefault().getConsistencyFilePath();
        String prefix = folder.getFullPath().toString() + "/";

        // check if consistency path starts with folder name
        if (consistency_file_path != null && consistency_file_path.startsWith(prefix))
          return true;
      }

      return true;
    }

    /**
     * @param delta
     */
    private boolean visitFile(IResourceDelta delta)
    {
      IFile file = (IFile) delta.getResource();

      if ("MANIFEST.MF".equals(file.getName()))
      {
        if (delta.getKind() == IResourceDelta.CHANGED)
        {
          // update old pluginInfo.id with addedProject id
          PluginConsistency pluginConsistency = PluginConsistencyActivator.getDefault().getPluginConsistency();
          if (pluginConsistency != null)
          {
            String projectName = file.getProject().getName();

            // find PluginInfo with name
            Optional<PluginInfo> pluginInfoOptional = pluginConsistency.pluginInfoList.stream().filter(pluginInfo -> pluginInfo.name.equals(projectName)).findAny();
            if (pluginInfoOptional.isPresent())
            {
              PluginInfo pluginInfo = pluginInfoOptional.get();
              String oldId = pluginInfo.id;
              String newId = cache.getId(file.getProject());
              if (oldId == null || !oldId.equals(newId))
              {
                pluginInfo.id = newId;

                // save
                File consistencyFile = Util.getConsistencyFile(PluginConsistencyActivator.getDefault().getConsistencyFilePath());
                if (consistencyFile != null)
                  savePluginConsistency(pluginConsistency, consistencyFile);
              }

              // check
              if (pluginInfo.containsInformations())
                launchProjectConsistency(file.getProject());
            }
          }
        }
      }
      else if (delta.getKind() == IResourceDelta.REMOVED)
      {
        // check if it's consistency file
        String consistencyFilePath = PluginConsistencyActivator.getDefault().getConsistencyFilePath();
        if (file.getFullPath().toString().equals(consistencyFilePath) && delta.getMovedToPath() != null)
          PluginConsistencyActivator.getDefault().setConsistencyFilePath(delta.getMovedToPath().toString());
      }

      return false;
    }
  }

  /**
   * Save PluginConsistency
   * @param pluginConsistency
   */
  public static void savePluginConsistency(PluginConsistency pluginConsistency, File consistencyFile)
  {
    Util.savePluginConsistency(pluginConsistency, consistencyFile);

    String consistency_file_path = PluginConsistencyActivator.getDefault().getConsistencyFilePath();
    IProject project = Util.getWorkspaceProject(consistency_file_path);
    if (project != null && project.isOpen())
    {
      WorkspaceJob job = new WorkspaceJob("Refresh project " + project.getName()) {
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
        {
          project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
          return Status.OK_STATUS;
        }
      };
      job.schedule();
    }
  }

  /**
   * @param project
   */
  private void launchProjectConsistency(IProject project)
  {
    if (!PluginConsistencyActivator.getDefault().isPluginConsistencyActivated())
      return;

    // check
    try
    {
      PluginConsistency pluginConsistency = PluginConsistencyActivator.getDefault().getPluginConsistency();
      if (pluginConsistency != null)
        Util.checkProjectConsistency(pluginConsistency, project, null);
    }
    catch(Exception e)
    {
      PluginConsistencyActivator.logError("Cannot check consistency on project " + project.getName(), e);
    }
  }
}
