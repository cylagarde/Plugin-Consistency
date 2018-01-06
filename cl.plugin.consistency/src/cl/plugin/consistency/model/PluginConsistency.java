package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.List;

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
      if (pluginInfo.isModified())
        pluginConsistency.pluginInfoList.add(pluginInfo);
    }
    return pluginConsistency;
  }

}
