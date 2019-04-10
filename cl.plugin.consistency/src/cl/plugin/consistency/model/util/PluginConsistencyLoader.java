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
  static final String SCHEMA_XSD = PluginConsistency.class.getPackage().getName().replace('.', '/') + "/pluginConsistency.xsd";
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
      final URL schemaURL = PluginConsistency.class.getClassLoader().getResource(SCHEMA_XSD);
      pluginConsistencySchema = schemaFactory.newSchema(schemaURL);
    }
    return pluginConsistencySchema;
  }

  /**
   * Load plugin consistency file
   *
   * @throws Exception
   */
  public static PluginConsistency loadPluginConsistencyFile(File pluginConsistencyFile) throws Exception
  {
    PluginConsistency pluginConsistency = JaxbLoaderUtil.load(pluginConsistencyFile, PluginConsistency.class, getValidationSchema());

    // remove duplicate types
    pluginConsistency.typeList = new ArrayList<>(new LinkedHashSet<>(pluginConsistency.typeList));
    for(PluginInfo pluginInfo : pluginConsistency.pluginInfoList)
    {
      pluginInfo.authorizedPluginTypeList = new ArrayList<>(new LinkedHashSet<>(pluginInfo.authorizedPluginTypeList));
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
    JaxbLoaderUtil.save(pluginConsistency, pluginConsistencyFile);
  }
}
