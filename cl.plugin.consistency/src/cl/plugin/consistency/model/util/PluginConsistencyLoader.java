package cl.plugin.consistency.model.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import cl.plugin.consistency.model.PluginConsistency;
import cl.plugin.consistency.model.PluginInfo;

/**
 * The class <b>PluginConsistencyLoader</b> allows to load or save a plugin consistency file.<br>
 */
public class PluginConsistencyLoader
{
  static final String SCHEMA_XSD = "pluginConsistency.xsd";
  static final URL SCHEMA_URL = PluginConsistency.class.getResource(SCHEMA_XSD);
  static Schema pluginConsistencySchema;

  /**
   * Return the validation schema for PluginConsistency
   *
   * @return The schema
   * @throws SAXException The SAXException
   */
  private static Schema getValidationSchema() throws SAXException
  {
    if (pluginConsistencySchema == null)
    {
      final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      pluginConsistencySchema = schemaFactory.newSchema(SCHEMA_URL);
    }
    return pluginConsistencySchema;
  }

  /**
   * Load plugin consistency file
   *
   * @throws Exception
   */
  public static PluginConsistency loadPluginConsistency(File pluginConsistencyFile) throws Exception
  {
    PluginConsistency pluginConsistency = JaxbLoaderUtil.load(pluginConsistencyFile, PluginConsistency.class, getValidationSchema());

    // remove duplicate types
    pluginConsistency.typeList = new ArrayList<>(new LinkedHashSet<>(pluginConsistency.typeList));
    for(PluginInfo pluginInfo : pluginConsistency.pluginInfoList)
    {
      pluginInfo.declaredPluginTypeList = new ArrayList<>(new LinkedHashSet<>(pluginInfo.declaredPluginTypeList));
      pluginInfo.forbiddenPluginTypeList = new ArrayList<>(new LinkedHashSet<>(pluginInfo.forbiddenPluginTypeList));
      pluginInfo.forbiddenPluginList = new ArrayList<>(new LinkedHashSet<>(pluginInfo.forbiddenPluginList));
    }

    return pluginConsistency;
  }

  /**
   * Save plugin consistency
   *
   * @throws Exception
   */
  public static void savePluginConsistency(PluginConsistency pluginConsistency, File pluginConsistencyFile) throws Exception
  {
    String schemaLocation = SCHEMA_URL.getPath();
    if (schemaLocation.startsWith("/"))
      schemaLocation = schemaLocation.substring(1);
    JaxbLoaderUtil.save(pluginConsistency, pluginConsistencyFile, schemaLocation);
  }
}
