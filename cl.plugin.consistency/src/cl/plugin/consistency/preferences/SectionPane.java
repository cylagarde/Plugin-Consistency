package cl.plugin.consistency.preferences;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.forms.widgets.FormFonts;

/**
 * The class <b>SectionPane</b> allows to.<br>
 */
public class SectionPane extends Composite
{
  private final TitledSection headerSection;

  private ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
  // private ColorRegistry colorRegistry = new ColorRegistry();

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

  /**
   * @param toolBarManager
   */
  public ToolBar createToolBar(ToolBarManager toolBarManager)
  {
    ToolBar toolbar = toolBarManager.createControl(headerSection);
    headerSection.setTextClient(toolbar);
    return toolbar;
  }

  /**
   * Return header section
   */
  public TitledSection getHeaderSection()
  {
    return headerSection;
  }

  /**
   * The class <b>TitledSection</b> allows to.<br>
   */
  public class TitledSection extends Section
  {
    Composite headerComposite;
    Label imageLabel;
    Label spaceLabel;
    Label titleLabel;

    private TitledSection(Composite parent)
    {
      super(parent, ExpandableComposite.TITLE_BAR);
      setMenu(parent.getMenu());

      //
      if (textLabel != null)
        textLabel.dispose();

      //
      headerComposite = new Composite(this, SWT.NONE) {
        @Override
        public void setForeground(Color color)
        {
          titleLabel.setForeground(color);
        }

        @Override
        public void setBackground(Color color)
        {
          titleLabel.setBackground(color);
        }

        @Override
        public void setFont(Font font)
        {
          titleLabel.setFont(font);
        }
      };
      // headerComposite.setBackground(null);
      headerComposite.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).margins(0, 0).numColumns(50).extendedMargins(0, 0, 2, 0).create());
      textLabel = headerComposite;

      imageLabel = new Label(headerComposite, SWT.NONE);
      spaceLabel = new Label(headerComposite, SWT.NONE);
      spaceLabel.setLayoutData(GridDataFactory.swtDefaults().hint(5, 0).exclude(true).create());
      titleLabel = new Label(headerComposite, SWT.WRAP);

      IThemeEngine themeEngine = PlatformUI.getWorkbench().getService(IThemeEngine.class);
      if (themeEngine != null && themeEngine.getActiveTheme() != null && themeEngine.getActiveTheme().getLabel().contains("Dark"))
      {
        String darkTitleBarBackgroundKey = "darkTitleBarBackground";
        if (!colorRegistry.hasValueFor(darkTitleBarBackgroundKey))
          colorRegistry.put(darkTitleBarBackgroundKey, new RGB(181, 186, 188));
        setTitleBarBackground(colorRegistry.get(darkTitleBarBackgroundKey));

        String darkTitleBarBorderColorKey = "darkTitleBarBorderColor";
        if (!colorRegistry.hasValueFor(darkTitleBarBorderColorKey))
          colorRegistry.put(darkTitleBarBorderColorKey, new RGB(81 + 50, 86 + 50, 88 + 50));
        setTitleBarBorderColor(colorRegistry.get(darkTitleBarBorderColorKey));

        String darkBackgroundKey = "darkBackground";
        if (!colorRegistry.hasValueFor(darkBackgroundKey))
          colorRegistry.put(darkBackgroundKey, new RGB(81, 86, 88));
        setBackground(colorRegistry.get(darkBackgroundKey));
        getParent().setBackground(colorRegistry.get(darkBackgroundKey));
      }
      else
      {
        FormToolkit formToolkit = new FormToolkit(parent.getDisplay());
        formToolkit.adapt(this, true, true);
        formToolkit.adapt(parent);
        FormColors colors = formToolkit.getColors();
        setFont(FormFonts.getInstance().getBoldFont(colors.getDisplay(), parent.getFont()));
        colors.initializeSectionToolBarColors();
        setTitleBarBackground(colors.getColor(IFormColors.TB_BG));
        setTitleBarBorderColor(colors.getColor(IFormColors.TB_BORDER));
        setTitleBarForeground(colors.getColor(IFormColors.TB_TOGGLE));

        setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        getParent().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
      }
    }

    /**
     * Set image
     *
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
     *
     * @param text
     */
    @Override
    public void setText(String text)
    {
      titleLabel.setText(text);
      layout();
    }
  }
}
