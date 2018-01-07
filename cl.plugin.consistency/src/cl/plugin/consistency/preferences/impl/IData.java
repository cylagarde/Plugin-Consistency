package cl.plugin.consistency.preferences.impl;

import java.util.List;

/**
 * The class <b>IData</b> allows to.<br>
 */
public interface IData<E>
{
  int getElementCount();

  void addElement(E e);

  E getElementAt(int index);

  void removeElementAt(int index);

  List<E> getElements();

  E createElement(String name);
}
