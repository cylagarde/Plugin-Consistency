package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.model.util.PluginConsistencyLoader;

/**
 * The class <b>PluginConsistency</b> define some plugin consistency informations
 */
@XmlRootElement(name = "PluginConsistency")
public class PluginConsistency
{
  @XmlElementWrapper(name = "Types")
  @XmlElement(name = "Type")
  public List<Type> typeList = new ArrayList<>();

  @XmlElementWrapper(name = "PatternInfos")
  @XmlElement(name = "PatternInfo")
  public List<PatternInfo> patternList = new ArrayList<>();

  @XmlElementWrapper(name = "PluginInfos")
  @XmlElement(name = "PluginInfo")
  public List<PluginInfo> pluginInfoList = new ArrayList<>();

  /**
   * Remove useless pluginInfo
   */
  public PluginConsistency compactForSaving()
  {
    try
    {
      byte[] pluginConsistencyContent = PluginConsistencyLoader.savePluginConsistency(this);
      PluginConsistency pluginConsistency = PluginConsistencyLoader.loadPluginConsistency(pluginConsistencyContent);

      // remove from pattern
      Set<PluginInfo> pluginInfoToRemoveSet = new HashSet<>();
      for(PluginInfo pluginInfo : pluginConsistency.pluginInfoList)
      {
        if (pluginInfo.containsInformations())
        {
          Set<PatternInfo> acceptedPatternInfos = pluginConsistency.patternList.stream()
            .filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id))
            .collect(Collectors.toSet());

          // remove types from pattern
          Set<Type> declaredPluginTypeFromPatternInfoSet = acceptedPatternInfos.stream().flatMap(patternInfo -> patternInfo.declaredPluginTypeList.stream()).collect(Collectors.toSet());
          Set<Type> forbiddenPluginTypeFromPatternInfoSet = acceptedPatternInfos.stream().flatMap(patternInfo -> patternInfo.forbiddenPluginTypeList.stream()).collect(Collectors.toSet());
          Set<ForbiddenPlugin> forbiddenPluginFromPatternInfoSet = acceptedPatternInfos.stream().flatMap(patternInfo -> patternInfo.forbiddenPluginList.stream()).collect(Collectors.toSet());

          pluginInfo.declaredPluginTypeList.removeIf(declaredPluginTypeFromPatternInfoSet::contains);
          pluginInfo.forbiddenPluginTypeList.removeIf(forbiddenPluginTypeFromPatternInfoSet::contains);
          pluginInfo.forbiddenPluginList.removeIf(forbiddenPluginFromPatternInfoSet::contains);

          if (pluginInfo.declaredPluginTypeList.isEmpty())
            pluginInfo.declaredPluginTypeList = null;
          if (pluginInfo.forbiddenPluginTypeList.isEmpty())
            pluginInfo.forbiddenPluginTypeList = null;
          if (pluginInfo.forbiddenPluginList.isEmpty())
            pluginInfo.forbiddenPluginList = null;

          if (pluginInfo.declaredPluginTypeList == null && pluginInfo.forbiddenPluginTypeList == null && pluginInfo.forbiddenPluginList == null)
            pluginInfoToRemoveSet.add(pluginInfo);
        }
      }
      pluginConsistency.pluginInfoList.removeAll(pluginInfoToRemoveSet);

      //
      for(PatternInfo patternInfo : pluginConsistency.patternList)
      {
        if (patternInfo.declaredPluginTypeList.isEmpty())
          patternInfo.declaredPluginTypeList = null;
        if (patternInfo.forbiddenPluginTypeList.isEmpty())
          patternInfo.forbiddenPluginTypeList = null;
        if (patternInfo.forbiddenPluginList.isEmpty())
          patternInfo.forbiddenPluginList = null;
      }

      return pluginConsistency;
    }
    catch(Exception e)
    {
      PluginConsistencyActivator.logError("Cannot compact PluginConsistency content", e);
      return this;
    }
  }
}
