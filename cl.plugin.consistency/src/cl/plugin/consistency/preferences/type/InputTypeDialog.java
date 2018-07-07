package cl.plugin.consistency.preferences.type;

import java.util.function.BiFunction;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The class <b>InputTypeDialog</b> allows to.<br>
 */
class InputTypeDialog extends Dialog
{
  /**
   * The title of the dialog.
   */
  private final String title;

  /**
   * The message to display, or <code>null</code> if none.
   */
  private final String newNameMessage;
  private final String newDescriptionMessage;

  /**
   * The input value; the empty string by default.
   */
  private String newName;
  private String newDescription;

  /**
   * The input validator, or <code>null</code> if none.
   */
  private final BiFunction<String, String, String> typeValidator;

  private Button okButton;

  private Text newNameText;
  private Text newDescriptionText;

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
     * @param initialNewName
     *            the initial input value, or <code>null</code> if none
     *            (equivalent to the empty string)
     * @param typeValidator
     *            an input validator, or <code>null</code> if none
     */
  InputTypeDialog(Shell parentShell, String dialogTitle, String newNameMessage, String initialNewName, String newDescriptionMessage, String initialNewDescription, BiFunction<String, String, String> typeValidator)
  {
    super(parentShell);
    this.title = dialogTitle;
    this.newNameMessage = newNameMessage;
    this.newDescriptionMessage = newDescriptionMessage;
    newName = initialNewName == null? "" : initialNewName;
    newDescription = initialNewDescription == null? "" : initialNewDescription;
    this.typeValidator = typeValidator;
  }

  @Override
  protected void buttonPressed(int buttonId)
  {
    newName = buttonId == IDialogConstants.OK_ID? newNameText.getText() : null;
    newDescription = buttonId == IDialogConstants.OK_ID? newDescriptionText.getText() : null;
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

    newNameText.setFocus();
    if (newName != null)
    {
      newNameText.setText(newName);
      newNameText.selectAll();
    }
    if (newDescription != null)
    {
      newDescriptionText.setText(newDescription);
      if (newName == null)
        newDescriptionText.selectAll();
    }
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    // create composite
    Composite composite = (Composite) super.createDialogArea(parent);

    // create message
    if (newNameMessage != null)
    {
      Label label = new Label(composite, SWT.WRAP);
      label.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
      label.setText(newNameMessage);
    }

    //
    newNameText = new Text(composite, getInputTextStyle());
    newNameText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    newNameText.addModifyListener(e -> validateInput());

    new Label(composite, SWT.NONE);

    // create message
    if (newDescriptionMessage != null)
    {
      Label label = new Label(composite, SWT.WRAP);
      label.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
      label.setText(newDescriptionMessage);
    }

    //
    newDescriptionText = new Text(composite, getInputTextStyle());
    newDescriptionText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    newDescriptionText.addModifyListener(e -> validateInput());

    errorMessageText = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
    errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
    errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    // Set the error message text
    setErrorMessage(errorMessage);

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

  protected Text getNewNameText()
  {
    return newNameText;
  }

  protected Text getNewDescriptionText()
  {
    return newDescriptionText;
  }

  /**
   * Returns the validator.
   *
   * @return the validator
   */
  protected BiFunction<String, String, String> getValidator()
  {
    return typeValidator;
  }

  public String getNewName()
  {
    return newName;
  }

  public String getNewDescription()
  {
    return newDescription;
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
    if (typeValidator != null)
    {
      String containsPatternValue = newNameText.getText();
      String doNotContainsPatternValue = newDescriptionText.getText();
      errorMessage = typeValidator.apply(containsPatternValue, doNotContainsPatternValue);
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
