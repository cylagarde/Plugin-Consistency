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

  @XmlElementWrapper(name = "DeclaredPluginTypes")
  @XmlElement(name = "Type")
  public List<Type> declaredPluginTypeList = new ArrayList<>();

  @XmlElementWrapper(name = "ForbiddenPluginTypes")
  @XmlElement(name = "Type")
  public List<Type> forbiddenPluginTypeList = new ArrayList<>();

  @XmlElementWrapper(name = "ForbiddenPlugins")
  @XmlElement(name = "ForbiddenPlugin")
  public List<ForbiddenPlugin> forbiddenPluginList = new ArrayList<>();

  @Override
  public String toString()
  {
    String declaredPluginTypes = declaredPluginTypeList.stream().map(type -> type.name).collect(Collectors.joining(", ", "[", "]"));
    String forbiddenPluginTypes = forbiddenPluginTypeList.stream().map(type -> type.name).collect(Collectors.joining(", ", "[", "]"));
    String forbiddenPlugins = forbiddenPluginList.stream().map(forbiddenPlugin -> forbiddenPlugin.id).collect(Collectors.joining(", ", "[", "]"));
    return "PluginInfo[id=" + id + ", name=" + name + ", declaredPluginTypes=" + declaredPluginTypes + ", forbiddenPluginTypes=" + forbiddenPluginTypes + ", forbiddenPlugins=" + forbiddenPlugins + "]";
  }

  /**
   */
  public boolean containsInformations()
  {
    if (!declaredPluginTypeList.isEmpty() || !forbiddenPluginTypeList.isEmpty() || !forbiddenPluginList.isEmpty())
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

    pluginInfo.declaredPluginTypeList.addAll(declaredPluginTypeList);
    pluginInfo.forbiddenPluginTypeList.addAll(forbiddenPluginTypeList);
    pluginInfo.forbiddenPluginList.addAll(forbiddenPluginList);

    return pluginInfo;
  }
}
