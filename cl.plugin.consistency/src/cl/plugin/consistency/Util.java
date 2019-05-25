package cl.plugin.consistency;

import java.io.File;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.UnmarshalException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import cl.plugin.consistency.model.ForbiddenPlugin;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.model.PluginConsistency;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.model.util.PluginConsistencyLoader;

/**
 * The class <b>Util</b> allows to.<br>
 */
public class Util
{
  public static PatternInfo duplicatePatternInfo(PatternInfo patternInfo)
  {
    PatternInfo duplicatePatternInfo = new PatternInfo();
    duplicatePatternInfo.pattern = patternInfo.pattern;
    patternInfo.declaredPluginTypeList.stream().map(Util::duplicateType).forEach(duplicatePatternInfo.declaredPluginTypeList::add);
    patternInfo.forbiddenPluginTypeList.stream().map(Util::duplicateType).forEach(duplicatePatternInfo.forbiddenPluginTypeList::add);
    return duplicatePatternInfo;
  }

  public static Type duplicateType(Type type)
  {
    Type duplicateType = new Type();
    duplicateType.name = type.name;
    return duplicateType;
  }

  /**
   * Recursive all controls
   * @param control
   * @param consumer
   */
  public static void recursive(Control control, Consumer<Control> consumer)
  {
    consumer.accept(control);
    if (control instanceof Composite)
    {
      Composite composite = (Composite) control;
      for(Control child : composite.getChildren())
        recursive(child, consumer);
    }
  }


  /**
   * Create combo
   */
  public static ComboViewer createCombo(Composite parent, List<?> items, Object firstSelection, BiConsumer<IStructuredSelection, IStructuredSelection> selectionConsumer)
  {
    ComboViewer comboViewer = new ComboViewer(parent);
    comboViewer.setContentProvider(ArrayContentProvider.getInstance());
    comboViewer.setInput(items);
    IStructuredSelection[] oldStructuredSelection = new IStructuredSelection[]{new StructuredSelection(firstSelection)};
    comboViewer.setSelection(oldStructuredSelection[0]);

    if (selectionConsumer != null)
    {
      comboViewer.addSelectionChangedListener(event -> {
        IStructuredSelection selection = (IStructuredSelection) comboViewer.getSelection();
        selectionConsumer.accept(oldStructuredSelection[0], selection);
        oldStructuredSelection[0] = selection;
        parent.layout();
      });
    }

    return comboViewer;
  }

  /**
   * @param consistency_file_path
   */
  public static File getConsistencyFile(String consistency_file_path)
  {
    File pluginConsistencyFile = new File(consistency_file_path);
    if (!pluginConsistencyFile.exists())
    {
      IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(consistency_file_path);
      if (resource != null)
        pluginConsistencyFile = resource.getRawLocation().toFile();
      else
      {
        // check if project exists
        Path path = new Path(consistency_file_path);
        IProject project = getWorkspaceProject(consistency_file_path);
        if (project != null)
          pluginConsistencyFile = new File(project.getLocation().toFile(), path.removeFirstSegments(1).toString());
      }
    }
    return pluginConsistencyFile;
  }

  /**
   * @param fullPath
   */
  public static IProject getWorkspaceProject(String fullPath)
  {
    // check if project exists
    String projectName = new Path(fullPath).segment(0);
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    if (project == null || ! project.exists())
      project = null;
    return project;
  }

  /**
   * Return true if file is a consistency file
   */
  public static String canLoadConsistencyFile(File consistencyFile)
  {
    try
    {
      PluginConsistencyLoader.loadPluginConsistencyFile(consistencyFile);
      return null;
    }
    catch(UnmarshalException e)
    {
      return "UnmarshalException:" + e.getCause().getLocalizedMessage();
    }
    catch(Exception e)
    {
      return e.getLocalizedMessage();
    }
  }

  /**
   * Load and update plugin consistency file
   * @param consistencyFile
   */
  public static PluginConsistency loadAndUpdateConsistencyFile(File consistencyFile)
  {
    PluginConsistency pluginConsistency = null;

    try
    {
      if (consistencyFile != null && consistencyFile.exists())
        pluginConsistency = PluginConsistencyLoader.loadPluginConsistencyFile(consistencyFile);
    }
    catch(Exception e)
    {
      PluginConsistencyActivator.logError("Cannot load consistency file", e);
    }

    if (pluginConsistency == null)
      pluginConsistency = new PluginConsistency();

    // init with default
    if (pluginConsistency.typeList.isEmpty())
    {
      Arrays.asList("API", "IHM", "IMPLEMENTATION", "TEST").stream().map(name -> {
        Type type = new Type();
        type.name = name;
        return type;
      }).forEach(pluginConsistency.typeList::add);
    }

    updatePluginConsistency(pluginConsistency);

    return pluginConsistency;
  }

  /**
   * Update plugin consistency, adding new workspace projects, updating name, id, type, etc...
   * @param pluginConsistency
   */
  public static void updatePluginConsistency(PluginConsistency pluginConsistency)
  {
    Cache cache = new Cache();

    // add new project to pluginConsistency
    IProject[] validPluginProjects = cache.getValidProjects();
    for(IProject pluginProject : validPluginProjects)
    {
      String id = cache.getId(pluginProject);
      if (id != null)
      {
        // find PluginInfo with 'id' and update 'name'
        Optional<PluginInfo> pluginInfoWithIdOptional = pluginConsistency.pluginInfoList.stream().filter(pluginInfo -> id.equals(pluginInfo.id)).findAny();
        if (pluginInfoWithIdOptional.isPresent())
        {
          PluginInfo pluginInfo = pluginInfoWithIdOptional.get();
          pluginInfo.name = pluginProject.getName();

          //
          updatePluginInfoWithPattern(pluginConsistency, pluginInfo);

          continue;
        }
      }

      // find PluginInfo with 'name' and update 'id'
      String name = pluginProject.getName();
      if (name != null)
      {
        Optional<PluginInfo> pluginInfoWithNameOptional = pluginConsistency.pluginInfoList.stream().filter(pluginInfo -> name.equals(pluginInfo.name)).findAny();
        if (pluginInfoWithNameOptional.isPresent())
        {
          PluginInfo pluginInfo = pluginInfoWithNameOptional.get();
          pluginInfo.id = cache.getId(pluginProject);

          //
          updatePluginInfoWithPattern(pluginConsistency, pluginInfo);

          continue;
        }
      }

      if (id == null)
      {
        PluginConsistencyActivator.logError("id null for '" + pluginProject + "'");
        continue;
      }
      if (name == null)
      {
        PluginConsistencyActivator.logError("name null for '" + pluginProject + "'");
        continue;
      }

      // add
      PluginInfo pluginInfo = new PluginInfo();
      pluginInfo.id = id;
      pluginInfo.name = name;

      //
      updatePluginInfoWithPattern(pluginConsistency, pluginInfo);

      //
      pluginConsistency.pluginInfoList.add(pluginInfo);
    }
  }

  /**
   * Update pluginInfo with Pattern information
   * @param pluginConsistency
   * @param pluginInfo
   */
  public static void updatePluginInfoWithPattern(PluginConsistency pluginConsistency, PluginInfo pluginInfo)
  {
    updatePluginInfoWithPattern(pluginConsistency, pluginInfo, true, true);
  }

  /**
   * Update pluginInfo with Pattern information
   * @param pluginConsistency
   * @param pluginInfo
   */
  public static void updatePluginInfoWithPattern(PluginConsistency pluginConsistency, PluginInfo pluginInfo, boolean updateType, boolean updateForbiddenPluginType)
  {
    Set<String> availableTypeSet = pluginConsistency.typeList.stream().map(type -> type.name).collect(Collectors.toSet());
    Set<String> declaredPluginTypeSet = pluginInfo.declaredPluginTypeList.stream().map(type -> type.name).collect(Collectors.toSet());
    Set<String> forbiddenPluginTypeSet = pluginInfo.forbiddenPluginTypeList.stream().map(type -> type.name).collect(Collectors.toSet());

    for(PatternInfo patternInfo : pluginConsistency.patternList)
    {
      if (patternInfo.acceptPlugin(pluginInfo.id))
      {
        // add type
        if (updateType)
        {
          for(Type type : patternInfo.declaredPluginTypeList)
          {
            String typeName = type.name;
            if (availableTypeSet.contains(typeName) && !declaredPluginTypeSet.contains(typeName))
            {
              Type newType = new Type();
              newType.name = typeName;
              declaredPluginTypeSet.add(typeName);
              pluginInfo.declaredPluginTypeList.add(newType);
            }
          }
        }

        // add forbidden type
        if (updateForbiddenPluginType)
        {
          for(Type type : patternInfo.forbiddenPluginTypeList)
          {
            String typeName = type.name;
            if (availableTypeSet.contains(typeName) && !forbiddenPluginTypeSet.contains(typeName))
            {
              Type newType = new Type();
              newType.name = typeName;
              forbiddenPluginTypeSet.add(typeName);
              pluginInfo.forbiddenPluginTypeList.add(newType);
            }
          }
        }
      }
    }
  }

  /**
   * Update all pluginInfos with Pattern information
   * @param pluginConsistency
   */
  public static void updatePluginInfoWithPattern(PluginConsistency pluginConsistency)
  {
    for(PluginInfo pluginInfo : pluginConsistency.pluginInfoList)
      updatePluginInfoWithPattern(pluginConsistency, pluginInfo);
  }

  /**
   * Remove pattern in pluginInfo
   * @param pluginConsistency
   * @param pluginInfo
   */
  public static void removePatternInPluginInfo(PluginConsistency pluginConsistency, PluginInfo pluginInfo)
  {
    Set<String> declaredPluginTypeSet = pluginInfo.declaredPluginTypeList.stream().map(type -> type.name).collect(Collectors.toSet());
    Set<String> forbiddenPluginTypeSet = pluginInfo.forbiddenPluginTypeList.stream().map(type -> type.name).collect(Collectors.toSet());

    for(PatternInfo patternInfo : pluginConsistency.patternList)
    {
      if (patternInfo.acceptPlugin(pluginInfo.id))
      {
        // remove type
        for(Type type : patternInfo.declaredPluginTypeList)
        {
          String typeName = type.name;
          if (declaredPluginTypeSet.contains(typeName))
            pluginInfo.declaredPluginTypeList.removeIf(type_ -> type_.name.equals(typeName));
        }

        // remove forbidden type
        for(Type type : patternInfo.forbiddenPluginTypeList)
        {
          String typeName = type.name;
          if (forbiddenPluginTypeSet.contains(typeName))
            pluginInfo.forbiddenPluginTypeList.removeIf(type_ -> type_.name.equals(typeName));
        }
      }
    }
  }

  /**
   * Remove pattern in all pluginInfos
   * @param pluginConsistency
   */
  public static void removePatternInAllPluginInfos(PluginConsistency pluginConsistency)
  {
    for(PluginInfo pluginInfo : pluginConsistency.pluginInfoList)
      removePatternInPluginInfo(pluginConsistency, pluginInfo);
  }

  /**
   * Reset types in all pluginInfos
   * @param pluginConsistency
   */
  public static void resetTypesInAllPluginInfos(PluginConsistency pluginConsistency)
  {
    for(PluginInfo pluginInfo : pluginConsistency.pluginInfoList)
      resetTypesInPluginInfo(pluginInfo);
  }

  /**
   * Reset types in pluginInfo
   * @param pluginInfo
   */
  public static void resetTypesInPluginInfo(PluginInfo pluginInfo)
  {
    pluginInfo.declaredPluginTypeList.clear();
    pluginInfo.forbiddenPluginTypeList.clear();
    pluginInfo.forbiddenPluginList.clear();
  }

  private static final String CL_PLUGIN_CONSISTENCY_MARKER = "cl.plugin.consistency.marker";
  private static final String PLUGIN_CONSISTENCY_MARKER_REQUIRE_BUNDLE_ATTRIBUTE = "pluginConsistencyMarkerRequireBundleAttribute";

  /**
   * Check project consistency
   * @param pluginConsistency
   * @param project
   * @param markerConsumer
   */
  public static WorkspaceJob checkProjectConsistency(PluginConsistency pluginConsistency, IProject project, Consumer<List<IMarker>> markerConsumer) throws Exception
  {
    Cache cache = new Cache();
    if (!cache.isValidProject(project))
      return null;
    long time = System.currentTimeMillis();

    IFile manifest = PDEProject.getManifest(project);
    IMarker[] pbMarkers = manifest.findMarkers(CL_PLUGIN_CONSISTENCY_MARKER, false, 0);
    List<IMarker> newMarkerList = new ArrayList<>();
    List<Runnable> runnableList = new ArrayList<>();

    Optional<PluginInfo> optionalPluginInfo = pluginConsistency.pluginInfoList.stream().filter(pInfo -> pInfo.name.equals(project.getName()) || pInfo.id.equals(cache.getId(project))).findAny();

    // create new PluginInfo
    if (!optionalPluginInfo.isPresent())
    {
      PluginInfo pluginInfo = new PluginInfo();
      pluginInfo.id = cache.getId(project);
      pluginInfo.name = project.getName();

      //
      updatePluginInfoWithPattern(pluginConsistency, pluginInfo);

      //
      pluginConsistency.pluginInfoList.add(pluginInfo);

      optionalPluginInfo = Optional.of(pluginInfo);
    }

    PluginInfo pluginInfo = optionalPluginInfo.get();

    // get IBundleProjectDescription
    IBundleProjectService bundleProjectService = PluginConsistencyActivator.getBundleProjectService();
    IBundleProjectDescription bundleProjectDescription = bundleProjectService.getDescription(project);
    IRequiredBundleDescription[] requiredBundles = bundleProjectDescription.getRequiredBundles();

    //
    if (requiredBundles != null)
    {
      Set<String> forbiddenPluginTypeSet = pluginInfo.forbiddenPluginTypeList.stream().map(type -> type.name).collect(Collectors.toSet());

      for(IRequiredBundleDescription requiredBundle : requiredBundles)
      {
        // find PluginInfo for requiredBundle
        Optional<PluginInfo> optional = pluginConsistency.pluginInfoList.stream().filter(pInfo -> pInfo.id.equals(requiredBundle.getName())).findAny();
        if (optional.isPresent())
        {
          PluginInfo requirePluginInfo = optional.get();
          for(Type type : requirePluginInfo.declaredPluginTypeList)
          {
            String typeName = type.name;
            // check forbidden type
            if (forbiddenPluginTypeSet.contains(typeName))
            {
              String requireBundle = requirePluginInfo.id;

              // if not same
              String pluginId = cache.getId(project);
              if (!pluginInfo.id.equals(pluginId))
              {
                pluginInfo.id = pluginId;

                // save
                File consistencyFile = Util.getConsistencyFile(PluginConsistencyActivator.getDefault().getConsistencyFilePath());
                savePluginConsistency(pluginConsistency, consistencyFile);
              }

              //
              String pluginName = pluginInfo.name;
              if (!pluginName.equals(pluginInfo.id))
                pluginName += " (id=" + pluginInfo.id + ")";
              String message = "The plugin '" + pluginName + "' uses bundle '" + requirePluginInfo.id + "' which has a forbidden type '" + typeName + "'";
              Runnable runnable = () -> {
                try
                {
                  IMarker marker = createManifestProblemMarker(manifest, requireBundle, message);
                  newMarkerList.add(marker);
                }
                catch(Exception e)
                {
                  PluginConsistencyActivator.logError("Cannot create problem marker for '" + project.getName() + "'", e);
                }
              };
              runnableList.add(runnable);
            }
          }
        }

        // check forbidden plugin
        Optional<ForbiddenPlugin> optional2 = pluginInfo.forbiddenPluginList.stream().filter(forbiddenPluginInfo -> forbiddenPluginInfo.id.equals(requiredBundle.getName())).findAny();
        if (optional2.isPresent())
        {
          ForbiddenPlugin forbiddenPluginInfo = optional2.get();
          String requireBundle = forbiddenPluginInfo.id;

          // if not same
          String pluginId = cache.getId(project);
          if (!pluginInfo.id.equals(pluginId))
          {
            pluginInfo.id = pluginId;

            // save
            File consistencyFile = Util.getConsistencyFile(PluginConsistencyActivator.getDefault().getConsistencyFilePath());
            savePluginConsistency(pluginConsistency, consistencyFile);
          }

          //
          String pluginName = pluginInfo.name;
          if (!pluginName.equals(pluginInfo.id))
            pluginName += " (id=" + pluginInfo.id + ")";
          String message = "The plugin '" + pluginName + "' uses forbidden bundle '" + requireBundle + "'";
          Runnable runnable = () -> {
            try
            {
              IMarker marker = createManifestProblemMarker(manifest, requireBundle, message);
              newMarkerList.add(marker);
            }
            catch(Exception e)
            {
              PluginConsistencyActivator.logError("Cannot create problem marker for '" + project.getName() + "'", e);
            }
          };
          runnableList.add(runnable);
        }
      }
    }

    WorkspaceJob workspaceJob = null;

    // launch job
    if (!runnableList.isEmpty() || pbMarkers.length != 0)
    {
      workspaceJob = new WorkspaceJob("Check consistency '" + project.getName() + "'")
      {
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
        {
          for(Runnable runnable : runnableList)
            runnable.run();

          if (markerConsumer != null)
            markerConsumer.accept(newMarkerList);

          // remove old markers
          for(IMarker pbMarker : pbMarkers)
          {
            if (pbMarker.exists() && pbMarker.getAttribute(PLUGIN_CONSISTENCY_MARKER_REQUIRE_BUNDLE_ATTRIBUTE) == null)
              continue;
            if (!newMarkerList.contains(pbMarker))
              pbMarker.delete();
          }

          return Status.OK_STATUS;
        }
      };
      workspaceJob.schedule();
    }

    PluginConsistencyActivator.logInfo("Check consistency on project " + project + " TIME=" + (System.currentTimeMillis() - time));

    return workspaceJob;
  }

  /**
   * Create Manifest Problem Marker
   * @param manifest
   * @param requireBundle
   * @param message
   * @throws Exception
   */
  private static IMarker createManifestProblemMarker(IFile manifest, String requireBundle, String message) throws Exception
  {
    IMarker[] pbMarkers = manifest.findMarkers(CL_PLUGIN_CONSISTENCY_MARKER, false, 0);
    Optional<IMarker> optional = Stream.of(pbMarkers).filter(marker -> {
      try
      {
        return requireBundle.equals(marker.getAttribute(PLUGIN_CONSISTENCY_MARKER_REQUIRE_BUNDLE_ATTRIBUTE)) && message.equals(marker.getAttribute(IMarker.MESSAGE));
      }
      catch(Exception e)
      {
        return false;
      }
    }).filter(marker -> marker.exists()).findAny();

    //
    IMarker marker = optional.isPresent()? optional.get() : null;
    if (marker == null)
    {
      marker = manifest.createMarker(CL_PLUGIN_CONSISTENCY_MARKER);
      System.err.println("Error : " + message);
      marker.setAttribute(IMarker.MESSAGE, message);
      marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
      marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
      //      marker.setAttribute(IMarker.USER_EDITABLE, true);
      marker.setAttribute(PLUGIN_CONSISTENCY_MARKER_REQUIRE_BUNDLE_ATTRIBUTE, requireBundle);
    }

    // try to find line
    int require_bundle_line_number = -1;
    StringBuilder buffer = new StringBuilder(1024);

    File manifestFile = manifest.getLocation().toFile();
    String content = new String(Files.readAllBytes(manifestFile.toPath()));
    String lf = content.indexOf("\r\n") != -1? "\r\n" : "\n";
    int sourceOffset = 0;
    String requireTag = "Require-Bundle:";

    try (LineNumberReader br = new LineNumberReader(new InputStreamReader(manifest.getContents())))
    {
      String line = null;
      while((line = br.readLine()) != null)
      {
        buffer.append(line).append(lf);
        if (line.startsWith(requireTag))
        {
          require_bundle_line_number = br.getLineNumber();
          line = line.substring(requireTag.length());
          sourceOffset = buffer.length() - line.length() - lf.length();
        }

        if (require_bundle_line_number != -1)
        {
          // other tag
          if (line.contains(": "))
          {
            buffer.delete(buffer.length() - line.length() - lf.length(), buffer.length());
            break;
          }

          // find
          String target = requireBundle + ",";
          int[] indices = lastIndexOf(buffer.toString().toCharArray(), sourceOffset, buffer.toString().length(), target.toCharArray(), 0, target.length(), buffer.length());
          if (indices != null && buffer.charAt(indices[0] - 1) == ' ')
          {
            updateMarker(marker, indices[0], indices[1] - 1, br.getLineNumber() - indices[2]);
            break;
          }

          // find
          target = requireBundle + ";";
          indices = lastIndexOf(buffer.toString().toCharArray(), sourceOffset, buffer.toString().length(), target.toCharArray(), 0, target.length(), buffer.length());
          if (indices != null && buffer.charAt(indices[0] - 1) == ' ')
          {
            updateMarker(marker, indices[0], indices[1] - 1, br.getLineNumber() - indices[2]);
            break;
          }

          // find
          target = requireBundle;
          indices = lastIndexOf(buffer.toString().toCharArray(), sourceOffset, buffer.toString().length(), target.toCharArray(), 0, target.length(), buffer.length());
          if (indices != null && buffer.charAt(indices[0] - 1) == ' ' && (buffer.charAt(indices[1]) == ' ' || buffer.charAt(indices[1]) == '\r' || buffer.charAt(indices[1]) == '\n'))
          {
            updateMarker(marker, indices[0], indices[1], br.getLineNumber() - indices[2]);
            break;
          }
        }
      }
    }

    return marker;
  }

  /**
   * Update marker
   * @param marker
   * @param char_start
   * @param char_end
   * @param line_number
   */
  private static void updateMarker(IMarker marker, Integer char_start, Integer char_end, Integer line_number) throws CoreException
  {
    if (!char_start.equals(marker.getAttribute(IMarker.CHAR_START)))
      marker.setAttribute(IMarker.CHAR_START, char_start);
    if (!char_end.equals(marker.getAttribute(IMarker.CHAR_END)))
      marker.setAttribute(IMarker.CHAR_END, char_end);
    if (!line_number.equals(marker.getAttribute(IMarker.LINE_NUMBER)))
      marker.setAttribute(IMarker.LINE_NUMBER, line_number);
  }

  static int[] lastIndexOf(char[] source, int sourceOffset, int sourceCount, char[] target, int targetOffset, int targetCount, int fromIndex)
  {
    /*
     * Check arguments; return immediately where possible. For consistency, don't check for null str or empty string.
     */
    if (fromIndex < 0 || targetCount == 0)
      return null;
    int rightIndex = sourceCount - targetCount;
    int fromIndexModified = fromIndex;
    if (fromIndexModified > rightIndex)
      fromIndexModified = rightIndex;

    int strLastIndex = targetOffset + targetCount - 1;
    char strLastChar = target[strLastIndex];
    int min = sourceOffset + targetCount - 1;
    int i = min * 0 + fromIndex - 1;

    startSearchForLastChar:
    while(true)
    {
      while(i >= min && source[i] != strLastChar)
        i--;
      //      System.out.println(source[i] +" - "+ strLastChar);
      if (i < min)
        return null;
      int last = i + 1;
      int j = i - 1;
      int k = strLastIndex - 1;

      int endline = 0;
      while(k > -1)
      {
        //        System.out.println(source[j] +" - "+ target[k]);
        if (source[j] == '\n')
          endline++;
        boolean foundSpace = source[j] == ' ' || source[j] == '\r' || source[j] == '\n';
        if (source[j] != target[k] && !foundSpace)
        {
          j--;
          k--;
          i--;
          continue startSearchForLastChar;
        }
        j--;
        if (!foundSpace)
          k--;
      }
      return new int[]{j + 1, last, endline};
    }
  }

  /**
   * Remove all consistency markers
   * @param project
   */
  public static void removeCheckProjectConsistency(IProject project) throws Exception
  {
    IFile manifest = PDEProject.getManifest(project);
    manifest.deleteMarkers(CL_PLUGIN_CONSISTENCY_MARKER, false, 0);
  }

  /**
   * Remove all consistency markers for all projects
   */
  public static void removeAllCheckProjectConsistency(IProgressMonitor monitor)
  {
    IProject[] validProjects = new Cache().getValidProjects();
    monitor.beginTask("Removing consistencies check ...", validProjects.length);
    for(IProject project : validProjects)
    {
      if (monitor.isCanceled())
        break;

      try
      {
        monitor.subTask("Removing consistency check for project " + project.getName());
        Util.removeCheckProjectConsistency(project);
        monitor.worked(1);
      }
      catch(Exception e)
      {
        PluginConsistencyActivator.logError("Error when removing consistency check on project " + project.getName(), e);
      }
    }
  }

  /**
   * Save PluginConsistency
   * @param pluginConsistency
   */
  public static void savePluginConsistency(PluginConsistency pluginConsistency, File consistencyFile)
  {
    // remove useless pluginInfo
    PluginConsistency compactPluginConsistency = pluginConsistency.compact();

    try
    {
      PluginConsistencyLoader.savePluginConsistency(compactPluginConsistency, consistencyFile);
    }
    catch(Exception e)
    {
      String message = "Exception when saving plugin consistency informations : " + e.getLocalizedMessage();
      PluginConsistencyActivator.logError(message, e);
    }
  }

  /**
   * Return the project
   * @param pluginInfo
   */
  public static IProject getProject(PluginInfo pluginInfo)
  {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(pluginInfo.name);
  }

  /**
   * Launch job for checking projects
   * @param pluginConsistency
   * @param markerConsumer
   */
  public static void launchConsistencyCheck(PluginConsistency pluginConsistency, Consumer<List<IMarker>> markerConsumer)
  {
    // launch project checking
    WorkspaceJob job = new WorkspaceJob("Check all projects consistency")
    {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
      {
        List<IMarker> markerList = new CopyOnWriteArrayList<>();
        Consumer<List<IMarker>> addMarkerConsumer = markerList::addAll;

        IProject[] validProjects = new Cache().getValidProjects();
        monitor.beginTask("Checking projects consistency ...", validProjects.length);

        List<WorkspaceJob> workspaceJobList = new ArrayList<>();
        for(IProject project : validProjects)
        {
          if (monitor.isCanceled())
            break;
          try
          {
            monitor.subTask("Checking project " + project.getName());
            WorkspaceJob workspaceJob = checkProjectConsistency(pluginConsistency, project, addMarkerConsumer);
            if (workspaceJob != null)
              workspaceJobList.add(workspaceJob);
            monitor.worked(1);
          }
          catch(Exception e)
          {
            PluginConsistencyActivator.logError("Error when checking onsistency on project " + project.getDefaultCharset(), e);
          }
        }
        workspaceJobList.forEach(Util::join);
        monitor.done();

        if (markerConsumer != null)
          markerConsumer.accept(markerList);

        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  /**
   * Join WorkspaceJob
   * @param workspaceJob
   */
  private static void join(WorkspaceJob workspaceJob)
  {
    try
    {
      workspaceJob.join();
    }
    catch(Exception e)
    {
      PluginConsistencyActivator.logError("Error when joining WorkspaceJob", e);
    }
  }
}
