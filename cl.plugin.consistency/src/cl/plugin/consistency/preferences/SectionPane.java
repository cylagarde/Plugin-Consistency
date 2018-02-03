package cl.plugin.consistency.preferences;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.forms.widgets.FormFonts;

/**
 * The class <b>SectionPane</b> allows to.<br>
 */
public class SectionPane extends Composite
{
  private final TitledSection headerSection;

  /**
   * Constructor
   *
   * @param parent
   * @param style
   */
  public SectionPane(Composite parent, int style)
  {
    super(parent, style);
    setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());

    // create section header
    headerSection = new TitledSection(this);
    headerSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
  }

  //  /**
  //   * Set title
  //   * @param title
  //   */
  //  public void setTitle(String title)
  //  {
  //    headerSection.setText(title);
  //  }
  //
  //  /**
  //   * Set title image
  //   * @param image
  //   */
  //  public void setTitleImage(Image image)
  //  {
  //    headerSection.setImage(image);
  //  }

  /**
   * @param toolBarManager
   */
  public ToolBar createToolBar(ToolBarManager toolBarManager)
  {
    ToolBar toolbar = toolBarManager.createControl(headerSection);
    headerSection.setTextClient(toolbar);
    return toolbar;
  }

  public TitledSection getHeaderSection()
  {
    return headerSection;
  }

  /**
   * The class <b>TitledSection</b> allows to.<br>
   */
  public static class TitledSection extends Section
  {
    Composite headerComposite;
    Label imageLabel;
    Label spaceLabel;
    Label titleLabel;

    private TitledSection(Composite parent)
    {
      super(parent, Section.TITLE_BAR);
      setMenu(parent.getMenu());

      //
      if (textLabel != null)
      {
        textLabel.dispose();
        textLabel = null;
      }

      //
      headerComposite = new Composite(this, SWT.NONE)
      {
        @Override
        public void setForeground(Color color)
        {
          titleLabel.setForeground(color);
        }

        @Override
        public void setFont(Font font)
        {
          titleLabel.setFont(font);
        }
      };
      //      headerComposite.setBackground(null);
      headerComposite.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).margins(0, 0).numColumns(50).extendedMargins(0, 0, 2, 0).create());
      textLabel = headerComposite;

      imageLabel = new Label(headerComposite, SWT.NONE);
      spaceLabel = new Label(headerComposite, SWT.NONE);
      spaceLabel.setLayoutData(GridDataFactory.swtDefaults().hint(5, 0).exclude(true).create());
      //      imageLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
      //
      titleLabel = new Label(headerComposite, SWT.WRAP);
      //      titleLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));

      FormToolkit formToolkit = new FormToolkit(parent.getDisplay());
      formToolkit.adapt(this, true, true);
      formToolkit.adapt(parent);
      FormColors colors = formToolkit.getColors();
      setFont(FormFonts.getInstance().getBoldFont(colors.getDisplay(), parent.getFont()));
      colors.initializeSectionToolBarColors();
      setTitleBarBackground(colors.getColor(IFormColors.TB_BG));
      setTitleBarBorderColor(colors.getColor(IFormColors.TB_BORDER));
      setTitleBarForeground(colors.getColor(IFormColors.TB_TOGGLE));
      //formToolkit.dispose();
    }

    /**
     * Set image
     * @param image
     */
    public void setImage(Image image)
    {
      ((GridData) spaceLabel.getLayoutData()).exclude = image == null;
      imageLabel.setImage(image);
      layout();
    }

    /**
     * Set text
     * @param text
     */
    @Override
    public void setText(String title)
    {
      titleLabel.setText(title);
      layout();
    }
  }
}
