package cl.plugin.consistency.preferences.pluginInfo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.preferences.TypeElement;
import cl.plugin.consistency.preferences.impl.IData;

/**
 * The class <b>PluginInfoData</b> allows to.<br>
 */
class PluginInfoData implements IData<TypeElement>
{
  final PluginInfo pluginInfo;
  final List<Type> typeList;
  final Set<Type> typeFromPatternSet;
  final boolean isForbiddenPluginTypeList;

  PluginInfoData(PluginInfo pluginInfo, List<Type> typeList, Set<Type> typeFromPatternSet, boolean isForbiddenPluginTypeList)
  {
    this.pluginInfo = pluginInfo;
    this.typeList = typeList;
    this.typeFromPatternSet = typeFromPatternSet;
    this.isForbiddenPluginTypeList = isForbiddenPluginTypeList;
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IData#getElementCount()
   */
  @Override
  public int getElementCount()
  {
    return typeList.size();
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IData#addElement(java.lang.Object)
   */
  @Override
  public void addElement(TypeElement e)
  {
    typeList.add(e.type);
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IData#getElementAt(int)
   */
  @Override
  public TypeElement getElementAt(int index)
  {
    Type type = typeList.get(index);
    return new TypeElement(type, typeFromPatternSet.contains(type));
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IData#removeElementAt(int)
   */
  @Override
  public void removeElementAt(int index)
  {
    typeList.remove(index);
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IData#getElements()
   */
  @Override
  public List<TypeElement> getElements()
  {
    return typeList.stream().map(type -> new TypeElement(type, typeFromPatternSet.contains(type))).collect(Collectors.toList());
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IData#createElement(java.lang.String)
   */
  @Override
  public TypeElement createElement(String name)
  {
    Type type = new Type();
    type.name = name;
    return new TypeElement(type, typeFromPatternSet.contains(type));
  }
}
