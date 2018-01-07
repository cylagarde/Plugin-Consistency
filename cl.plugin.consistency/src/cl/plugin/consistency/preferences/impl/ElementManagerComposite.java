package cl.plugin.consistency.preferences.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import cl.plugin.consistency.Util;

/**
 * The class <b>ElementManagerComposite</b> allows to.<br>
 */
public class ElementManagerComposite<E extends IElement, T extends IData<E>>
{
  final IElementManagerDataModel<E, T> elementManagerDataModel;
  public final Section section;
  final Composite elementListComposite;
  final ScrolledComposite scrolledComposite;
  T data;
  IAction addElementAction;
  boolean fireEvent = true;

  /**
   * Constructor
   * @param parent
   * @param style
   */
  public ElementManagerComposite(IElementManagerDataModel<E, T> elementManagerDataModel, Composite parent, int style)
  {
    this.elementManagerDataModel = elementManagerDataModel;

    FormToolkit formToolkit = new FormToolkit(parent.getDisplay());

    section = formToolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
    section.setText(elementManagerDataModel.getSectionTitle());
    section.setLayout(new GridLayout());

    // Add toolbar to section
    final ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    final ToolBar toolbar = toolBarManager.createControl(section);
    addElementAction = new AddElementAction();

    toolBarManager.add(addElementAction);
    toolBarManager.update(true);
    section.setTextClient(toolbar);

    //
    scrolledComposite = new ScrolledComposite(section, SWT.H_SCROLL | SWT.V_SCROLL);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);
    scrolledComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

    section.setClient(scrolledComposite);

    //
    elementListComposite = formToolkit.createComposite(scrolledComposite);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = gridLayout.marginHeight = 0;
    gridLayout.marginTop = gridLayout.marginLeft = gridLayout.marginRight = gridLayout.marginBottom = 0;
    elementListComposite.setLayout(gridLayout);
    scrolledComposite.setContent(elementListComposite);

    //
    scrolledComposite.addControlListener(new ControlAdapter()
    {
      @Override
      public void controlResized(ControlEvent ce)
      {
        Rectangle rect = scrolledComposite.getClientArea();
        scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(rect.width, SWT.DEFAULT));
      }
    });
  }

  /**
   * Return the not used types
   */
  private List<String> getNotUsedElements()
  {
    List<String> elements = elementManagerDataModel.getElements().stream().map(E::getName).sorted().distinct().collect(Collectors.toList());
    elements.add(0, "");
    List<String> currentElements = data.getElements().stream().map(E::getName).collect(Collectors.toList());
    elements.removeAll(currentElements);
    return elements;
  }

  /**
   * @param data
   */
  public void setData(T data)
  {
    this.data = data;
    //    Util.setEnabled(section, pluginInfo != null);

    Stream.of(elementListComposite.getChildren()).forEach(Control::dispose);
    if (data != null)
    {
      for(E element : data.getElements())
      {
        ElementBiConsumer elementBiConsumer = new ElementBiConsumer();
        List<String> elements = getNotUsedElements();
        elements.add(element.getName());
        Collections.sort(elements);
        ComboViewer elementComboViewer = Util.createCombo(elementListComposite, elements, element.getName(), elementBiConsumer);
        elementBiConsumer.elementComboViewer = elementComboViewer;

        elementComboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      }
    }
    elementListComposite.layout();
    scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  /**
   * The class <b>ElementBiConsumer</b> allows to.<br>
   */
  class ElementBiConsumer implements BiConsumer<IStructuredSelection, IStructuredSelection>
  {
    ComboViewer elementComboViewer;

    @Override
    public void accept(IStructuredSelection oldStructuredSelection, IStructuredSelection newStructuredSelection)
    {
      if (!fireEvent)
        return;
      int elementIndex = Arrays.asList(elementListComposite.getChildren()).indexOf(elementComboViewer.getControl());

      String selectedElementName = (String) newStructuredSelection.getFirstElement();
      if (selectedElementName == null || selectedElementName.isEmpty())
      {
        if (elementIndex < data.getElementCount())
          data.removeElementAt(elementIndex);

        // remove ElementComboViewer
        elementComboViewer.getControl().dispose();
        elementListComposite.layout();
        scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
      }
      else
      {
        if (elementIndex < data.getElementCount())
        {
          // same -> no change
          E element = data.getElementAt(elementIndex);
          if (element.getName().equals(selectedElementName))
            return;
          element.setName(selectedElementName);
        }
        else
        {
          E element = data.createElement(selectedElementName);
          data.addElement(element);
        }
      }

      // enable if last combo dont use empty selection
      Control[] children = elementListComposite.getChildren();
      Combo combo = children.length == 0? null : (Combo) children[children.length - 1];
      addElementAction.setEnabled(combo == null || (combo.getSelectionIndex() != 0 && getNotUsedElements().size() != 1));

      // reconstruct items for all combos
      try
      {
        fireEvent = false;
        for(Control control : elementListComposite.getChildren())
        {
          if (control instanceof Combo)
          {
            Combo child = (Combo) control;
            List<String> elements = getNotUsedElements();
            String selection = child.getText();
            elements.add(selection);
            ComboViewer comboViewer = (ComboViewer) child.getData("ComboViewer");
            comboViewer.setInput(elements);
            comboViewer.setSelection(new StructuredSelection(selection));
          }
        }
      }
      finally
      {
        fireEvent = true;
      }

      elementManagerDataModel.refreshData(data);
    }
  }

  /**
   * The class <b>AddElementAction</b> allows to.<br>
   */
  class AddElementAction extends Action
  {
    {
      setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(org.eclipse.ui.ISharedImages.IMG_OBJ_ADD));
      setToolTipText(elementManagerDataModel.getAddElementToolTipText());
    }

    @Override
    public void run()
    {
      ElementBiConsumer elementBiConsumer = new ElementBiConsumer();
      ComboViewer elementComboViewer = Util.createCombo(elementListComposite, getNotUsedElements(), "", elementBiConsumer);
      elementBiConsumer.elementComboViewer = elementComboViewer;

      elementComboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

      setEnabled(false);
      elementListComposite.layout();
      scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
  }
}
