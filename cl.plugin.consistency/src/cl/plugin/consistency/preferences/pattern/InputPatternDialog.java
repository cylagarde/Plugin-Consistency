package cl.plugin.consistency.preferences.pattern;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
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
  private final String containsPatternMessage;
  private final String doNotContainsPatternMessage;

  private final Cache cache;

  /**
   * The input value; the empty string by default.
   */
  private String containsPattern;
  private String doNotContainsPattern;

  /**
   * The input validator, or <code>null</code> if none.
   */
  private final BiFunction<String, String, String> patternValidator;

  private Button okButton;

  private Text containsPatternText;
  private Text doNotContainsPatternText;

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
     * @param dialogMessage
     *            the dialog message, or <code>null</code> if none
     * @param initialContainsPattern
     *            the initial input value, or <code>null</code> if none
     *            (equivalent to the empty string)
     * @param patternValidator
     *            an input validator, or <code>null</code> if none
     */
  InputPatternDialog(Shell parentShell, String dialogTitle, String containsPatternMessage, String initialContainsPattern, String doNotContainsPatternMessage, String initialDoNotContainsPattern, Cache cache, BiFunction<String, String, String> patternValidator)
  {
    super(parentShell);
    this.title = dialogTitle;
    this.containsPatternMessage = containsPatternMessage;
    this.doNotContainsPatternMessage = doNotContainsPatternMessage;
    containsPattern = initialContainsPattern == null? "" : initialContainsPattern;
    doNotContainsPattern = initialDoNotContainsPattern == null? "" : initialDoNotContainsPattern;
    this.cache = cache;
    this.patternValidator = patternValidator;
  }

  @Override
  protected void buttonPressed(int buttonId)
  {
    containsPattern = buttonId == IDialogConstants.OK_ID? containsPatternText.getText() : null;
    doNotContainsPattern = buttonId == IDialogConstants.OK_ID? doNotContainsPatternText.getText() : null;
    super.buttonPressed(buttonId);
  }

  @Override
  protected void configureShell(Shell shell)
  {
    super.configureShell(shell);
    if (title != null)
      shell.setText(title);
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

    containsPatternText.setFocus();
    if (containsPattern != null)
    {
      containsPatternText.setText(containsPattern);
      containsPatternText.selectAll();
    }
    if (doNotContainsPattern != null)
    {
      doNotContainsPatternText.setText(doNotContainsPattern);
      if (containsPattern == null)
        doNotContainsPatternText.selectAll();
    }
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    // create composite
    Composite composite = (Composite) super.createDialogArea(parent);

    // create message
    if (containsPatternMessage != null)
    {
      Label label = new Label(composite, SWT.WRAP);
      label.setText(containsPatternMessage);
    }

    //
    containsPatternText = new Text(composite, getInputTextStyle());
    containsPatternText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    containsPatternText.addModifyListener(e -> validateInput());

    new Label(composite, SWT.NONE);

    // create message
    if (doNotContainsPatternMessage != null)
    {
      Label label = new Label(composite, SWT.WRAP);
      label.setText(doNotContainsPatternMessage);
    }

    //
    doNotContainsPatternText = new Text(composite, getInputTextStyle());
    doNotContainsPatternText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    doNotContainsPatternText.addModifyListener(e -> validateInput());

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
    pluginAcceptedTableViewer.setLabelProvider(new BundlesLabelProvider(cache));

    pluginNotAcceptedTableViewer = new TableViewer(listComposite, SWT.BORDER);
    pluginNotAcceptedTableViewer.getTable().setLayoutData(fillBothGridData);
    pluginNotAcceptedTableViewer.getTable().setLinesVisible(true);
    pluginNotAcceptedTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    pluginNotAcceptedTableViewer.setLabelProvider(new BundlesLabelProvider(cache));

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

  protected Text getContainsPatternText()
  {
    return containsPatternText;
  }

  protected Text getDoNotContainsPatternText()
  {
    return doNotContainsPatternText;
  }

  /**
   * Returns the validator.
   *
   * @return the validator
   */
  protected BiFunction<String, String, String> getValidator()
  {
    return patternValidator;
  }

  public String getContainsPattern()
  {
    return containsPattern;
  }

  public String getDoNotContainsPattern()
  {
    return doNotContainsPattern;
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
      String containsPatternValue = containsPatternText.getText();
      String doNotContainsPatternValue = doNotContainsPatternText.getText();
      errorMessage = patternValidator.apply(containsPatternValue, doNotContainsPatternValue);

      PatternInfo patternInfo = new PatternInfo();
      patternInfo.setPattern(containsPatternValue, doNotContainsPatternValue);

      Predicate<IProject> predicate = project -> patternInfo.acceptPlugin(cache.getId(project));
      Comparator<IProject> projectComparator = Comparator.comparing(IProject::getName);
      IProject[] projects = cache.getValidProjects();
      Map<Boolean, List<IProject>> map = Stream.of(projects).sorted(projectComparator).collect(Collectors.partitioningBy(predicate));
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
}
