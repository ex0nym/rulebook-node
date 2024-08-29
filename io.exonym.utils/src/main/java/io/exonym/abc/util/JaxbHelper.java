/*
 * Copyright (c) 2023. All Rights Reserved. Exonym GmbH
 */

package io.exonym.abc.util;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import eu.abc4trust.smartcard.Base64;
import eu.abc4trust.xml.PresentationPolicy;
import io.exonym.lite.couchdb.Base64TypeAdapter;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JaxbHelper {

	public static Gson gson;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());
		gson = builder.setPrettyPrinting().create();

	}


	public static String serializeToXml(Object o, Class<?> clazz) throws Exception{
		JAXBContext context = JAXBContext.newInstance(clazz);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new AbcNamespace());
		m.setProperty("com.sun.xml.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		ByteOutputStream bos = new ByteOutputStream();
		m.marshal(o, bos);
		String xml = bos.toString();
		if (xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")){
			return xml;

		} else {
			return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + bos.toString();

		}
	}

	public static XMLStreamReader serializeToXmlStream(Object o, Class<?> clazz) throws Exception{
		String s = serializeToXml(o, clazz);
		Reader reader = new StringReader(s);
		XMLInputFactory f = XMLInputFactory.newInstance();
		return f.createXMLStreamReader(reader);
		
	}
	
	public static XMLStreamReader xmlToXmlStream(String xmlString) throws Exception{
		Reader reader = new StringReader(xmlString);
		XMLInputFactory f = XMLInputFactory.newInstance();
		return f.createXMLStreamReader(reader);
		
	}

	public static String b64XmlToString(String b64Bytes) throws Exception{
		return new String(Base64.decode(b64Bytes), StandardCharsets.UTF_8);

	}


	public static <T> T b64XmlToClass(String b64Bytes, Class<T> clazz) throws Exception{
		String xml = b64XmlToString(b64Bytes);
		return xmlToClass(xml, clazz);

	}

	public static <T> T xmlToClass(byte[] bytes, Class<T> clazz) throws Exception{
		return xmlToClass(new String(bytes, "UTF8"), clazz);

	}

	public static <T> T xmlToClass(String string, Class<T> clazz) throws Exception{
		XMLStreamReader reader = xmlToXmlStream(string);
		return xmlToClass(reader, clazz);
		
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T xmlToClass(XMLStreamReader stream, Class<T> clazz) throws Exception{
		try {
			JAXBContext context = JAXBContext.newInstance(clazz);
			Unmarshaller u = context.createUnmarshaller();
			return (T) u.unmarshal(stream);
			
		} catch (Exception e) {
			throw e; 
			
		}
	}
	
	public static <T> T xmlFileToClass(Path file, Class<T> clazz) throws Exception{
		byte[] xml = Files.readAllBytes(file);
		return (T) JaxbHelper.xmlToClass(xml, clazz);

	}

	public static String serializeToJson(Object o, Class<?> clazz) throws Exception {
		if (o==null){
			throw new NullPointerException();
		}
		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		AnnotationIntrospector introspection = new JaxbAnnotationIntrospector(mapper.getTypeFactory());
		mapper.setAnnotationIntrospector(introspection);
		return mapper.writeValueAsString(o);

	}

	public static <T> T jsonToClass(String json, Class<T> clazz) throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		return (T) mapper.readValue(json, clazz);

	}

	public static <T> T jsonFileToClass(Path path, Class<T> clazz) throws Exception{
		if (Files.isRegularFile(path)){
			byte[] json = Files.readAllBytes(path);
			return (T) JaxbHelper.jsonToClass(new String(json, StandardCharsets.UTF_8), clazz);

		} else {
			throw new UxException(ErrorMessages.FILE_NOT_FOUND);

		}
	}

	public static Path fileToPath(File file){
		return Path.of(file.toURI());

	}

}
