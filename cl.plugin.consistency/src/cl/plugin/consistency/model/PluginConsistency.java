package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

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
   * @param pluginConsistency
   */
  public PluginConsistency compact()
  {
    PluginConsistency pluginConsistency = new PluginConsistency();

    // add all availables types
    pluginConsistency.typeList.addAll(typeList);

    // add all availables patterns
    pluginConsistency.patternList.addAll(patternList);

    //
    for(PluginInfo pluginInfo : pluginInfoList)
    {
      if (pluginInfo.containsInformations())
      {
        PluginInfo newPluginInfo = pluginInfo.duplicate();

        // remove types from pattern
        Set<Type> typeFromPatternInfoSet = patternList.stream().filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id)).flatMap(patternInfo -> patternInfo.typeList.stream()).collect(Collectors.toSet());
        Set<Type> forbiddenTypeFromPatternInfoSet = patternList.stream().filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id)).flatMap(patternInfo -> patternInfo.forbiddenTypeList.stream()).collect(Collectors.toSet());

        newPluginInfo.typeList.removeIf(typeFromPatternInfoSet::contains);
        newPluginInfo.forbiddenTypeList.removeIf(forbiddenTypeFromPatternInfoSet::contains);

        if (newPluginInfo.containsInformations())
          pluginConsistency.pluginInfoList.add(newPluginInfo);
      }
    }
    return pluginConsistency;
  }

}
