package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * The class <b>PluginInfo</b> define some plugin informations.
 */
public class PluginInfo
{
  @XmlAttribute(name = "id", required = true)
  public String id;

  @XmlAttribute(name = "name", required = true)
  public String name;

  @XmlElementWrapper(name = "Types")
  @XmlElement(name = "Type")
  public List<Type> typeList = new ArrayList<>();

  @XmlElementWrapper(name = "ForbiddenTypes")
  @XmlElement(name = "Type")
  public List<Type> forbiddenTypeList = new ArrayList<>();

  @XmlElementWrapper(name = "ForbiddenPlugins")
  @XmlElement(name = "ForbiddenPlugin")
  public List<ForbiddenPlugin> forbiddenPluginList = new ArrayList<>();

  @Override
  public String toString()
  {
    String types = typeList.stream().map(type -> type.name).collect(Collectors.joining(", ", "[", "]"));
    String forbiddenTypes = forbiddenTypeList.stream().map(forbiddenType -> forbiddenType.name).collect(Collectors.joining(", ", "[", "]"));
    String forbiddenPlugins = forbiddenPluginList.stream().map(forbiddenPlugin -> forbiddenPlugin.id).collect(Collectors.joining(", ", "[", "]"));
    return "PluginInfo[id=" + id + ", name=" + name + ", types=" + types + ", forbiddenTypes=" + forbiddenTypes + ", forbiddenPlugins=" + forbiddenPlugins + "]";
  }

  /**
   */
  public boolean containsInformations()
  {
    if (!typeList.isEmpty() || !forbiddenTypeList.isEmpty() || !forbiddenPluginList.isEmpty())
      return true;
    return false;
  }

  /**
   * Duplicate
   */
  public PluginInfo duplicate()
  {
    PluginInfo pluginInfo = new PluginInfo();
    pluginInfo.id = id;
    pluginInfo.name = name;

    pluginInfo.typeList.addAll(typeList);
    pluginInfo.forbiddenTypeList.addAll(forbiddenTypeList);
    pluginInfo.forbiddenPluginList.addAll(forbiddenPluginList);

    return pluginInfo;
  }
}
