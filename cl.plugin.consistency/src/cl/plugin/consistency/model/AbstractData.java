package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * The class <b>AbstractData</b> allows to.<br>
 */
public abstract class AbstractData
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

  //  void beforeMarshal(Marshaller u)
  //  {
  //    // do not save empty list
  //    if (declaredPluginTypeList != null && declaredPluginTypeList.isEmpty())
  //      declaredPluginTypeList = null;
  //
  //    if (forbiddenPluginTypeList != null && forbiddenPluginTypeList.isEmpty())
  //      forbiddenPluginTypeList = null;
  //
  //    if (forbiddenPluginList != null && forbiddenPluginList.isEmpty())
  //      forbiddenPluginList = null;
  //  }
}
