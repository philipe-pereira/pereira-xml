package br.com.pereiraeng.xml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XMLutils {

	public static boolean pingXML(String str) {
		boolean out = false;
		try {
			URL url = new URI(str).toURL();
			XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			InputStream is = url.openStream();
			reader.parse(new InputSource(is));
			is.close();
			out = true;
		} catch (SAXException | ParserConfigurationException | IOException | URISyntaxException e) {
			System.out.println(e.getMessage());
		}
		return out;
	}

	/**
	 * Função que carrega o arquivo xml com {@link Properties propriedades}
	 * 
	 * @param filename caminho do arquivo com termição xml
	 * @param defaults <code>null</code> ou os valores default (se o arquivo
	 *                 indicado não for encontrado, ele será criado com esses
	 *                 valores
	 * @return {@link Properties propriedades}
	 */
	public static Properties loadProperties(String filename, Hashtable<String, String> defaults) {
		Properties props = new Properties();
		try {
			props.loadFromXML(new FileInputStream(filename));
		} catch (FileNotFoundException e1) {
			if (defaults != null) {
				for (Entry<String, String> e : defaults.entrySet())
					props.setProperty(e.getKey(), e.getValue());
				try {
					props.storeToXML(new FileOutputStream(filename), "Default configuration at: " + filename);
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		return props;
	}
}
