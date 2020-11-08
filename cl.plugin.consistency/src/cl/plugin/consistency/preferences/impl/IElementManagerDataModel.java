package cl.plugin.consistency.preferences.impl;

import java.util.Collection;

import org.eclipse.swt.graphics.Image;

/**
 * The class <b>IElementManagerDataModel</b> allows to.<br>
 */
public interface IElementManagerDataModel<E, T>
{
  /**
   * Refresh data
   * @param data
   */
  void refreshData(T data);

  Collection<E> getElements();

  /**
   * Return section title
   */
  String getSectionTitle();

  /**
   * Return section image
   */
  Image getSectionImage();

  /**
   * Return add element tooltip text
   */
  String getAddElementToolTipText();

}
