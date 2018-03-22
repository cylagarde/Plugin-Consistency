package cl.plugin.consistency.preferences.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import cl.plugin.consistency.Util;
import cl.plugin.consistency.preferences.SectionPane;

/**
 * The class <b>ElementManagerComposite</b> allows to.<br>
 */
public class ElementManagerComposite<E extends IElement, T extends IData<E>>
{
  final IElementManagerDataModel<E, T> elementManagerDataModel;
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

    SectionPane sectionPane = new SectionPane(parent, SWT.NONE);
    sectionPane.getHeaderSection().setText(elementManagerDataModel.getSectionTitle());
    sectionPane.getHeaderSection().setImage(elementManagerDataModel.getSectionImage());

    // Add toolbar to section
    final ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    sectionPane.createToolBar(toolBarManager);

    addElementAction = new AddElementAction();
    toolBarManager.add(addElementAction);
    toolBarManager.update(true);

    //
    scrolledComposite = new ScrolledComposite(sectionPane, SWT.H_SCROLL | SWT.V_SCROLL);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);
    scrolledComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).indent(6, 0).create());

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
    List<String> elements = elementManagerDataModel.getElements().stream().map(E::getName).distinct().collect(Collectors.toList());
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

    // enable add action if elements are available
    Collection<E> elements = elementManagerDataModel.getElements();
    if (data != null)
    {
      // remove all elements present in data
      List<E> dataElements = data.getElements();
      elements.removeAll(dataElements);
    }
    addElementAction.setEnabled(!elements.isEmpty());

    // remove all controls
    Stream.of(elementListComposite.getChildren()).forEach(Control::dispose);

    // create all combos
    if (data != null)
    {
      for(E element : data.getElements())
      {
        ElementBiConsumer elementBiConsumer = new ElementBiConsumer();
        List<String> notUsedElements = getNotUsedElements();
        notUsedElements.add(element.getName());
        Collections.sort(notUsedElements);
        ComboViewer elementComboViewer = Util.createCombo(elementListComposite, notUsedElements, element.getName(), elementBiConsumer);
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
      boolean enabled = combo == null || (combo.getSelectionIndex() != 0 && getNotUsedElements().size() != 1);
      addElementAction.setEnabled(enabled);

      // reconstruct items for all combos
      try
      {
        fireEvent = false;
        for(Control control : elementListComposite.getChildren())
        {
          if (control instanceof Combo)
          {
            Combo child = (Combo) control;
            List<String> notUsedElements = getNotUsedElements();
            String selection = child.getText();
            notUsedElements.add(selection);
            Collections.sort(notUsedElements);
            ComboViewer comboViewer = (ComboViewer) child.getData("ComboViewer");
            comboViewer.setInput(notUsedElements);
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
      List<String> notUsedElements = getNotUsedElements();
      Collections.sort(notUsedElements);
      ComboViewer elementComboViewer = Util.createCombo(elementListComposite, notUsedElements, "", elementBiConsumer);
      elementComboViewer.getControl().setFocus();
      elementBiConsumer.elementComboViewer = elementComboViewer;

      elementComboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

      setEnabled(false);
      elementListComposite.layout();
      scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
  }
}
