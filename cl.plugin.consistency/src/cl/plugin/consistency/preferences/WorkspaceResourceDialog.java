package cl.plugin.consistency.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.NewFolderDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

import cl.plugin.consistency.PluginConsistencyActivator;

/**
 */
public class WorkspaceResourceDialog extends ElementTreeSelectionDialog implements ISelectionStatusValidator
{
  protected boolean showNewFolderControl = false;
  protected boolean showFileControl = false;
  protected boolean showFiles = true;

  protected Button newFolderButton;
  protected Text fileText;
  protected String fileTextContent = "";

  protected IContainer selectedContainer;

  protected boolean fAllowMultiple = true;

  public WorkspaceResourceDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider)
  {
    super(parent, labelProvider, contentProvider);
    setComparator(new ResourceComparator(ResourceComparator.NAME));
    setValidator(this);
  }

  public WorkspaceResourceDialog(Shell parent)
  {
    this(parent, new WorkbenchLabelProvider(), new WorkbenchContentProvider());
  }

  public void loadContents()
  {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    setInput(root);
  }

  public ViewerFilter createDefaultViewerFilter(boolean showFiles)
  {
    this.showFiles = showFiles;
    return new ViewerFilter()
    {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element)
      {
        if (element instanceof IResource)
        {
          IResource workspaceResource = (IResource) element;
          return workspaceResource.isAccessible() && (WorkspaceResourceDialog.this.showFiles || workspaceResource.getType() != IResource.FILE);
        }
        return false;
      }
    };
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);

    if (isShowNewFolderControl())
      createNewFolderControl(composite);
    if (isShowFileControl())
      createFileControl(composite);

    applyDialogFont(composite);
    return composite;
  }

  protected void createNewFolderControl(Composite parent)
  {
    newFolderButton = new Button(parent, SWT.PUSH);
    newFolderButton.setText("&New Folder...");
    newFolderButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent event)
      {
        newFolderButtonPressed();
      }
    });
    newFolderButton.setFont(parent.getFont());
    updateNewFolderButtonState();
  }

  protected void updateNewFolderButtonState()
  {
    IStructuredSelection selection = (IStructuredSelection) getTreeViewer().getSelection();
    selectedContainer = null;
    if (selection.size() == 1)
    {
      Object first = selection.getFirstElement();
      if (first instanceof IContainer)
        selectedContainer = (IContainer) first;
    }
    newFolderButton.setEnabled(selectedContainer != null);
  }

  protected void newFolderButtonPressed()
  {
    NewFolderDialog dialog = new NewFolderDialog(getShell(), selectedContainer);
    if (dialog.open() == Window.OK)
    {
      TreeViewer treeViewer = getTreeViewer();
      treeViewer.refresh(selectedContainer);
      Object createdFolder = dialog.getResult()[0];
      treeViewer.reveal(createdFolder);
      treeViewer.setSelection(new StructuredSelection(createdFolder));
    }
  }

  protected void createFileControl(Composite parent)
  {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    {
      GridLayout layout = new GridLayout(2, false);
      layout.marginLeft = -5;
      layout.marginRight = -5;
      layout.marginTop = -5;
      layout.marginBottom = -5;
      composite.setLayout(layout);
    }

    Label fileLabel = new Label(composite, SWT.NONE);
    fileLabel.setText("&File Name:");

    fileText = new Text(composite, SWT.BORDER);
    fileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fileText.addModifyListener(e -> fileTextModified(fileText.getText()));
    fileText.setEnabled(!fAllowMultiple);

    if (fileTextContent != null)
      fileText.setText(fileTextContent);
  }

  /**
   * Sets the initial selection.
   * Convenience method.
   * @param selection the initial selection.
   */
  @Override
  public void setInitialSelection(Object selection)
  {
    IFile selectFile = null;
    if (selection instanceof IFile)
    {
      selectFile = (IFile) selection;
      IResource resource = selectFile;
      while(resource != null && !resource.exists())
        resource = resource.getParent();
      if (resource != null)
        selection = resource;
    }

    super.setInitialSelection(selection);

    if (selectFile != null)
      setFileText(selectFile.getName());
  }

  protected void fileTextModified(String text)
  {
    fileTextContent = text;

    if (!fAllowMultiple && showFiles && getResult().length == 1)
    {
      super.setResult(Collections.singletonList(getFile()));
      updateOKStatus();
    }
  }

  /*
   * @see org.eclipse.ui.dialogs.ElementTreeSelectionDialog#access$setResult(java.util.List)
   */
  @SuppressWarnings("rawtypes")
  @Override
  protected void access$setResult(List result)
  {
    super.access$setResult(result);

    if (!fAllowMultiple && showFiles && result.size() == 1)
    {
      if (result.get(0) instanceof IFile)
        setFileText(((IFile) result.get(0)).getName());
      else
      {
        result = Collections.singletonList(getFile());
        super.access$setResult(result);
      }
    }
  }

  /*
   * @see org.eclipse.ui.dialogs.SelectionDialog#setResult(java.util.List)
   */
  @SuppressWarnings("rawtypes")
  @Override
  protected void setResult(List newResult)
  {
    if (!fAllowMultiple && showFiles && newResult != null && newResult.size() == 1 && newResult.get(0) instanceof IFile)
      super.setResult(Collections.singletonList(getFile()));
    else
      super.setResult(newResult);
  }

  /**
   * Specifies if multiple selection is allowed.
   * @param allowMultiple
   */
  @Override
  public void setAllowMultiple(boolean allowMultiple)
  {
    fAllowMultiple = allowMultiple;
    super.setAllowMultiple(allowMultiple);
    if (fileText != null && !fileText.isDisposed())
      fileText.setEnabled(!allowMultiple);
  }

  @Override
  public IStatus validate(Object[] selectedElements)
  {
    if (isShowNewFolderControl())
      updateNewFolderButtonState();

    String message = null;
    for(int i = 0; i < selectedElements.length; i++)
    {
      if (selectedElements[i] instanceof IFile)
        message = acceptFilename((((IFile) selectedElements[i])).getName());
      else
        message = !showFiles? null : !isShowFileControl()? null : acceptFilename(fileText.getText());
      if (message != null)
        break;
    }
    return new Status(message == null? IStatus.OK : IStatus.ERROR, PluginConsistencyActivator.PLUGIN_ID, 0, message, null);
  }

  protected String acceptFilename(String filename)
  {
    return filename.trim().length() > 0? null : "File name is empty";
  }

  public IContainer[] getSelectedContainers()
  {
    List<IContainer> containers = new ArrayList<>();
    Object[] result = getResult();
    for(int i = 0; i < result.length; i++)
    {
      if (result[i] instanceof IContainer)
        containers.add((IContainer) result[i]);
    }
    return containers.toArray(new IContainer[containers.size()]);
  }

  public IFile[] getSelectedFiles()
  {
    List<IFile> files = new ArrayList<>();
    Object[] result = getResult();
    for(int i = 0; i < result.length; i++)
    {
      if (result[i] instanceof IFile)
        files.add((IFile) result[i]);
    }
    return files.toArray(new IFile[files.size()]);
  }

  public IFile getFile()
  {
    String file = getFileText();
    if (file.length() != 0)
    {
      Object[] result = getResult();
      if (result.length == 1)
      {
        if (result[0] instanceof IFile)
          result[0] = ((IFile) result[0]).getParent();
        if (result[0] instanceof IContainer)
        {
          IContainer container = (IContainer) result[0];
          return container.getFile(new Path(file));
        }
      }
    }
    return null;
  }

  public void setFileText(String text)
  {
    if (text == null)
      text = "";

    if (fileText != null && !fileText.isDisposed())
      fileText.setText(text);
    else
      fileTextContent = text;
    updateOKStatus();
  }

  public String getFileText()
  {
    return fileText != null && !fileText.isDisposed()? fileText.getText() : fileTextContent;
  }

  public boolean isShowNewFolderControl()
  {
    return showNewFolderControl;
  }

  public void setShowNewFolderControl(boolean showNewFolderControl)
  {
    this.showNewFolderControl = showNewFolderControl;
  }

  public boolean isShowFileControl()
  {
    return showFileControl;
  }

  public void setShowFileControl(boolean showFileControl)
  {
    this.showFileControl = showFileControl;
  }
}
