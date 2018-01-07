package cl.plugin.consistency.preferences.impl;

import java.util.Collection;

/**
 * The class <b>ITypeManagerDataModel</b> allows to.<br>
 */
public interface IElementManagerDataModel<E, T>
{

  /**
   * @param data
   */
  void refreshData(T data);

  Collection<E> getElements();

  /**
   * @return
   */
  String getSectionTitle();

  /**
   * @return
   */
  String getAddElementToolTipText();

}
