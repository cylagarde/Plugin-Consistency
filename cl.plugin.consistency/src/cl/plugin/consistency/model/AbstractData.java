package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * The class <b>AbstractData</b> allows to.<br>
 */
public class AbstractData
{
  @XmlElementWrapper(name = "DeclaredPluginTypes")
  @XmlElement(name = "Type")
  public List<Type> declaredPluginTypeList = new ArrayList<>();

  @XmlElementWrapper(name = "ForbiddenPluginTypes")
  @XmlElement(name = "Type")
  public List<Type> forbiddenPluginTypeList = new ArrayList<>();

  @XmlElementWrapper(name = "ForbiddenPlugins")
  @XmlElement(name = "ForbiddenPlugin")
  public List<ForbiddenPlugin> forbiddenPluginList = new ArrayList<>();
}
