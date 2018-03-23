package cl.plugin.consistency.preferences.pattern;

import java.util.List;
import java.util.stream.Collectors;

import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.preferences.TypeElement;
import cl.plugin.consistency.preferences.impl.IData;

/**
 * The class <b>PatternInfoData</b> allows to.<br>
 */
class PatternInfoData implements IData<TypeElement>
{
  final PatternInfo patternInfo;
  final List<Type> typeList;

  PatternInfoData(PatternInfo patternInfo, List<Type> typeList)
  {
    this.patternInfo = patternInfo;
    this.typeList = typeList;
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
    return new TypeElement(typeList.get(index));
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
    return typeList.stream().map(TypeElement::new).collect(Collectors.toList());
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IData#createElement(java.lang.String)
   */
  @Override
  public TypeElement createElement(String name)
  {
    Type type = new Type();
    type.name = name;
    return new TypeElement(type);
  }

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    String types = typeList.stream().map(type -> type.name).collect(Collectors.joining(", "));
    return "PatternInfoData[types=" + types + "]";
  }
}
