package cl.plugin.consistency.preferences.pattern;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import cl.plugin.consistency.Cache;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.preferences.BundlesLabelProvider;

/**
 * The class <b>InputPatternDialog</b> allows to.<br>
 */
class InputPatternDialog extends Dialog
{
  /**
   * The title of the dialog.
   */
  private final String title;

  /**
   * The message to display, or <code>null</code> if none.
   */
  private final String acceptPatternMessage;
  private final String doNotAcceptPatternMessage;

  private final Cache cache;

  /**
   * The input value; the empty string by default.
   */
  private String description;
  private String acceptPattern;
  private String doNotAcceptPattern;

  /**
   * The pattern validator, or <code>null</code> if none.
   */
  private final IPatternValidator patternValidator;

  private Button okButton;

  private Text descriptionText;
  private Text acceptPatternText;
  private Text doNotAcceptPatternText;

  private TableViewer pluginAcceptedTableViewer;
  private TableViewer pluginNotAcceptedTableViewer;

  private Text errorMessageText;

  private String errorMessage;

  /**
     * Creates an input dialog with OK and Cancel buttons. Note that the dialog
     * will have no visual representation (no widgets) until it is told to open.
     * <p>
     * Note that the <code>open</code> method blocks for input dialogs.
     * </p>
     *
     * @param parentShell
     *            the parent shell, or <code>null</code> to create a top-level
     *            shell
     * @param dialogTitle
     *            the dialog title, or <code>null</code> if none
     * @param acceptPatternMessage
     *            the dialog message, or <code>null</code> if none
     * @param initialAcceptPattern
     *            the initial input value, or <code>null</code> if none
     *            (equivalent to the empty string)
     * @param patternValidator
     *            an input validator, or <code>null</code> if none
     */
  InputPatternDialog(Shell parentShell, String dialogTitle, String initialDescription, String acceptPatternMessage, String initialAcceptPattern, String doNotAcceptPatternMessage, String initialDoNotAcceptPattern, Cache cache,
    IPatternValidator patternValidator)
  {
    super(parentShell);
    this.title = dialogTitle;
    this.description = initialDescription == null? "" : initialDescription;
    this.acceptPatternMessage = acceptPatternMessage;
    this.doNotAcceptPatternMessage = doNotAcceptPatternMessage;
    acceptPattern = initialAcceptPattern == null? "" : initialAcceptPattern;
    doNotAcceptPattern = initialDoNotAcceptPattern == null? "" : initialDoNotAcceptPattern;
    this.cache = cache;
    this.patternValidator = patternValidator;
  }

  @Override
  protected void buttonPressed(int buttonId)
  {
    description = buttonId == IDialogConstants.OK_ID? descriptionText.getText() : null;
    acceptPattern = buttonId == IDialogConstants.OK_ID? acceptPatternText.getText() : null;
    doNotAcceptPattern = buttonId == IDialogConstants.OK_ID? doNotAcceptPatternText.getText() : null;
    super.buttonPressed(buttonId);
  }

  @Override
  protected void configureShell(Shell shell)
  {
    super.configureShell(shell);
    if (title != null)
      shell.setText(title);

    Point controlSize = getParentShell().getSize();
    Point displayLocation = getParentShell().toDisplay(controlSize.x / 2, controlSize.y / 2);
    Point shellSize = shell.getSize();
    shell.setLocation(displayLocation.x - shellSize.x / 2, displayLocation.y - shellSize.y / 2);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#isResizable()
   */
  @Override
  protected boolean isResizable()
  {
    return true;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent)
  {
    // create OK and Cancel buttons by default
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

    if (description != null)
      descriptionText.setText(description);

    acceptPatternText.setFocus();
    if (acceptPattern != null)
    {
      acceptPatternText.setText(acceptPattern);
      acceptPatternText.selectAll();
    }
    if (doNotAcceptPattern != null)
    {
      doNotAcceptPatternText.setText(doNotAcceptPattern);
      if (acceptPattern == null)
        doNotAcceptPatternText.selectAll();
    }
  }

  @SuppressWarnings("unused")
  @Override
  protected Control createDialogArea(Composite parent)
  {
    // create composite
    Composite composite = (Composite) super.createDialogArea(parent);

    // create message
    Label descriptionLabel = new Label(composite, SWT.WRAP);
    descriptionLabel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    descriptionLabel.setText("Description");

    descriptionText = new Text(composite, getInputTextStyle());
    descriptionText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    descriptionText.addModifyListener(e -> validateInput());

    new Label(composite, SWT.NONE);

    // create message
    if (acceptPatternMessage != null)
    {
      Label label = new Label(composite, SWT.WRAP);
      label.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
      label.setText(acceptPatternMessage);
    }

    //
    acceptPatternText = new Text(composite, getInputTextStyle());
    acceptPatternText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    acceptPatternText.addModifyListener(e -> validateInput());

    new Label(composite, SWT.NONE);

    // create message
    if (doNotAcceptPatternMessage != null)
    {
      Label label = new Label(composite, SWT.WRAP);
      label.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
      label.setText(doNotAcceptPatternMessage);
    }

    //
    doNotAcceptPatternText = new Text(composite, getInputTextStyle());
    doNotAcceptPatternText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    doNotAcceptPatternText.addModifyListener(e -> validateInput());

    errorMessageText = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
    errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
    errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    // Set the error message text
    setErrorMessage(errorMessage);

    //
    Composite listComposite = new Composite(composite, SWT.NONE);
    listComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridLayout layout = new GridLayout(2, true);
    layout.marginWidth = layout.marginHeight = 0;
    listComposite.setLayout(layout);

    Label pluginAcceptedPluginLabel = new Label(listComposite, SWT.NONE);
    pluginAcceptedPluginLabel.setText("Plugin accepted");

    Label pluginNotAcceptedPluginLabel = new Label(listComposite, SWT.NONE);
    pluginNotAcceptedPluginLabel.setText("Plugin not accepted");

    GridData fillBothGridData = new GridData(GridData.FILL_BOTH);
    fillBothGridData.widthHint = 300;
    fillBothGridData.heightHint = 300;

    pluginAcceptedTableViewer = new TableViewer(listComposite, SWT.BORDER);
    pluginAcceptedTableViewer.getTable().setLayoutData(fillBothGridData);
    pluginAcceptedTableViewer.getTable().setLinesVisible(true);
    pluginAcceptedTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    pluginAcceptedTableViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new BundlesLabelProvider(cache)));

    pluginNotAcceptedTableViewer = new TableViewer(listComposite, SWT.BORDER);
    pluginNotAcceptedTableViewer.getTable().setLayoutData(fillBothGridData);
    pluginNotAcceptedTableViewer.getTable().setLinesVisible(true);
    pluginNotAcceptedTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    pluginNotAcceptedTableViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new BundlesLabelProvider(cache)));

    applyDialogFont(composite);
    return composite;
  }

  /**
   * Returns the ok button.
   *
   * @return the ok button
   */
  protected Button getOkButton()
  {
    return okButton;
  }

  protected Text getAcceptPatternText()
  {
    return acceptPatternText;
  }

  protected Text getDoNotAcceptPatternText()
  {
    return doNotAcceptPatternText;
  }

  /**
   * Returns the validator.
   */
  protected IPatternValidator getPatternValidator()
  {
    return patternValidator;
  }

  public String getDescription()
  {
    return description;
  }

  public String getAcceptPattern()
  {
    return acceptPattern;
  }

  public String getDoNotAcceptPattern()
  {
    return doNotAcceptPattern;
  }

  /**
   * Validates the input.
   * <p>
   * The default implementation of this framework method delegates the request
   * to the supplied input validator object; if it finds the input invalid,
   * the error message is displayed in the dialog's message line. This hook
   * method is called whenever the text changes in the input field.
   * </p>
   */
  protected void validateInput()
  {
    String errorMessage = null;
    if (patternValidator != null)
    {
      String descriptionValue = descriptionText.getText();
      String acceptPatternValue = acceptPatternText.getText();
      String doNotAcceptPatternValue = doNotAcceptPatternText.getText();
      errorMessage = patternValidator.getErrorMessage(descriptionValue, acceptPatternValue, doNotAcceptPatternValue);

      PatternInfo patternInfo = new PatternInfo();
      patternInfo.setPattern(acceptPatternValue, doNotAcceptPatternValue);

      Predicate<IProject> predicate = project -> patternInfo.acceptPlugin(cache.getId(project));
      Comparator<Object> pluginIdComparator = cache.getPluginIdComparator();
      IProject[] projects = cache.getValidProjects();
      Map<Boolean, List<IProject>> map = Stream.of(projects)
        .sorted(pluginIdComparator)
        .collect(Collectors.partitioningBy(predicate));
      pluginAcceptedTableViewer.setInput(map.get(Boolean.TRUE));
      pluginNotAcceptedTableViewer.setInput(map.get(Boolean.FALSE));
    }

    setErrorMessage(errorMessage);
  }

  /**
   * Sets or clears the error message.
   * If not <code>null</code>, the OK button is disabled.
   *
   * @param errorMessage
   *            the error message, or <code>null</code> to clear
   * @since 3.0
   */
  public void setErrorMessage(String errorMessage)
  {
    this.errorMessage = errorMessage;
    if (errorMessageText != null && !errorMessageText.isDisposed())
    {
      errorMessageText.setText(errorMessage == null? " \n " : errorMessage); //$NON-NLS-1$
      // Disable the error message text control if there is no error, or
      // no error text (empty or whitespace only).  Hide it also to avoid
      // color change.
      // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130281
      boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
      errorMessageText.setEnabled(hasError);
      errorMessageText.setVisible(hasError);
      errorMessageText.getParent().update();
      // Access the ok button by id, in case clients have overridden button creation.
      // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=113643
      Control button = getButton(IDialogConstants.OK_ID);
      if (button != null)
        button.setEnabled(errorMessage == null);
    }
  }

  /**
   * Returns the style bits that should be used for the input text field.
   * Defaults to a single line entry. Subclasses may override.
   *
   * @return the integer style bits that should be used when creating the
   *         input text
   *
   * @since 3.4
   */
  protected int getInputTextStyle()
  {
    return SWT.SINGLE | SWT.BORDER;
  }

  @FunctionalInterface
  public static interface IPatternValidator
  {
    String getErrorMessage(String description, String acceptPattern, String doNotAcceptPatternMessage);
  }
}
