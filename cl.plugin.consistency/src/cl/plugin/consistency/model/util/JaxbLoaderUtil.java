package cl.plugin.consistency.model.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;

/**
 * The class JaxbLoaderUtil purposes some utilities methods for Jaxb
 */
public final class JaxbLoaderUtil {
    private JaxbLoaderUtil() {}

    /**
     * Create Unmarshaller
     *
     * @param clazz The instance class
     * @param schema The schema instance
     * @return The Unmarshaller instance
     * @throws JAXBException The JAXBException
     */
    private static Unmarshaller createUnmarshaller(Class<?> clazz, Schema schema) throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        jaxbUnmarshaller.setSchema(schema);
        return jaxbUnmarshaller;
    }

    /**
     * Create Marshaller
     * @param clazz The class
     * @return The Marshaller instance
     * @throws JAXBException The JAXBException
     * @throws PropertyException The PropertyException
     */
    private static Marshaller createMarshaller(Class<?> clazz) throws JAXBException, PropertyException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");
        jaxbMarshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final String /* MarshallerImpl. */ INDENT_STRING = "com.sun.xml.internal.bind.indentString";
        jaxbMarshaller.setProperty(INDENT_STRING, "  ");
        return jaxbMarshaller;
    }

    /**
     * Load file
     *
     * @param file The file
     * @param clazz The instance class
     * @param <O> The type
     * @return The instance
     * @throws Exception The exception
     */
    public static <O> O load(File file, Class<O> clazz) throws Exception {
        return load(file, clazz, null);
    }

    /**
     * Load file with validation schema
     *
     * @param file The file
     * @param clazz The instance class
     * @param schema The Schema instance
     * @param <O> The type
     * @return The instance
     * @throws Exception The exception
     */
    public static <O> O load(File file, Class<O> clazz, Schema schema) throws Exception {
        final Unmarshaller jaxbUnmarshaller = createUnmarshaller(clazz, schema);
        final O o = clazz.cast(jaxbUnmarshaller.unmarshal(file));
        return o;
    }

    /**
     * Load from inputStream
     *
     * @param inputStream The inputStream
     * @param clazz The instance class
     * @param <O> The type
     * @return The instance
     * @throws Exception The exception
     */
    public static <O> O load(InputStream inputStream, Class<O> clazz) throws Exception {
        return load(inputStream, clazz, null);
    }

    /**
     * Load from inputStream with validation
     *
     * @param inputStream The inputStream
     * @param clazz The instance class
     * @param schema The Schema instance
     * @param <O> The type
     * @return The instance
     * @throws Exception The exception
     */
    public static <O> O load(InputStream inputStream, Class<O> clazz, Schema schema) throws Exception {
        final Unmarshaller jaxbUnmarshaller = createUnmarshaller(clazz, schema);
        final O o = clazz.cast(jaxbUnmarshaller.unmarshal(inputStream));
        return o;
    }

    /**
     * Load from url with validation
     *
     * @param url The url
     * @param clazz The instance class
     * @param schema The Schema instance
     * @param <O> The type
     * @return The instance
     * @throws Exception The exception
     */
    public static <O> O load(URL url, Class<O> clazz, Schema schema) throws Exception {
        final Unmarshaller jaxbUnmarshaller = createUnmarshaller(clazz, schema);
        final O o = clazz.cast(jaxbUnmarshaller.unmarshal(url));
        return o;
    }

    /**
     * Save instance to file
     *
     * @param o The instance to save
     * @param file The file to save the instance
     * @throws Exception The exception
     */
    public static void save(Object o, File file) throws Exception {
        final Marshaller jaxbMarshaller = createMarshaller(o.getClass());
        jaxbMarshaller.marshal(o, file);
    }

    /**
     * Save instance to outputStream
     *
     * @param o The instance to save
     * @param outputStream The outputStream to save the instance
     * @throws Exception The exception
     */
    public static void save(Object o, OutputStream outputStream) throws Exception {
        final Marshaller jaxbMarshaller = createMarshaller(o.getClass());
        jaxbMarshaller.marshal(o, outputStream);
    }
}
