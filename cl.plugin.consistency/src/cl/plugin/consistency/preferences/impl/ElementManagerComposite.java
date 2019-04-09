package cl.plugin.consistency.preferences.impl;

import java.util.ArrayList;
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
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
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

import cl.plugin.consistency.Util;
import cl.plugin.consistency.custom.NaturalOrderComparator;
import cl.plugin.consistency.preferences.SectionPane;

/**
 * The class <b>ElementManagerComposite</b> allows to.<br>
 */
public class ElementManagerComposite<E extends IElement, T extends IData<E>>
{
  final IElementManagerDataModel<E, T> elementManagerDataModel;
  final ToolBar toolBar;
  final Composite elementListComposite;
  final ScrolledComposite scrolledComposite;
  T data;
  IAction addElementAction;
  boolean fireEvent = true;
  private final static String COMBO_VIEWER_TAG = "ComboViewer";
  private final static String IS_PATTERN_TYPE_TAG = "isPatternType";

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
    toolBar = sectionPane.createToolBar(toolBarManager);

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

  public void setEnabled(boolean enabled)
  {
    toolBar.setEnabled(enabled);
    Util.recursive(elementListComposite, control -> control.setEnabled(enabled));
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
        boolean isPatternType = element.isPatternType();
        ElementBiConsumer elementBiConsumer = null;
        List<String> items = new ArrayList<>();
        items.add(element.getName());
        if (!isPatternType)
        {
          elementBiConsumer = new ElementBiConsumer();
          items.addAll(getNotUsedElements());
          Collections.sort(items, NaturalOrderComparator.INSTANCE);
        }

        ComboViewer elementComboViewer = Util.createCombo(elementListComposite, items, element.getName(), elementBiConsumer);
        elementComboViewer.getControl().setData(IS_PATTERN_TYPE_TAG, isPatternType);
        elementComboViewer.getControl().setData(COMBO_VIEWER_TAG, elementComboViewer);
        if (isPatternType)
          elementComboViewer.getControl().setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.COUNTER_COLOR));

        if (elementBiConsumer != null)
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

      // enable if find not used elements
      boolean enabled = getNotUsedElements().size() != 1;
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
            List<String> items = new ArrayList<>();
            String selection = child.getText();
            items.add(selection);
            boolean isPatternType = (boolean) child.getData(IS_PATTERN_TYPE_TAG);
            if (!isPatternType)
            {
              items.addAll(getNotUsedElements());
              Collections.sort(items, NaturalOrderComparator.INSTANCE);
            }

            ComboViewer comboViewer = (ComboViewer) child.getData(COMBO_VIEWER_TAG);
            comboViewer.setInput(items);
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
      Collections.sort(notUsedElements, NaturalOrderComparator.INSTANCE);
      String firstSelection = notUsedElements.stream().filter(item -> !item.equals("")).findFirst().orElse("");
      ComboViewer elementComboViewer = Util.createCombo(elementListComposite, notUsedElements, firstSelection, elementBiConsumer);
      elementComboViewer.getControl().setFocus();
      elementBiConsumer.elementComboViewer = elementComboViewer;
      elementComboViewer.getControl().setData(IS_PATTERN_TYPE_TAG, false);
      elementComboViewer.getControl().setData(COMBO_VIEWER_TAG, elementComboViewer);

      elementComboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

      setEnabled(!firstSelection.equals(""));

      IStructuredSelection[] oldStructuredSelection = new IStructuredSelection[]{new StructuredSelection(firstSelection)};
      elementComboViewer.setSelection(oldStructuredSelection[0]);

      elementListComposite.layout();
      scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
  }
}
