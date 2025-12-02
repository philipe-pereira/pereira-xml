package br.com.pereiraeng.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * <p>
 * Classe abstrata dos leitores de arquivos XML.
 * </p>
 * 
 * <p>
 * A necessidade de se criar essa classe intermediária é:
 * </p>
 * 
 * <ul>
 * <li>As interfaces {@link ContentHandler} e {@link ErrorHandler} exigem que
 * suas classes tenham um grandeza número de funções, a maior parte delas não
 * tem interesse prático;</i>
 * <li>As funções que são utilizadas com frequência (
 * {@link #startElement(String, String, String, Attributes)} e
 * {@link #endElement(String, String, String)}) recebem um grande número de
 * argumentos, sendo que alguns não tem utilidade;</i>
 * <li>A função {@link #characters(char[], int, int)} costuma cortar em duas ou
 * mais partes os blocos internos (sendo isto previsto na especificação da
 * classe), o que atrapalha a leitura dos arquivos;</i>
 * <li>Além disso, adicionou-se a opção {@link #notParse(String) stock}, quando
 * se deseja guardar uma parte do código XML original, ou seja, sem quebrá-lo
 * entre seus campos e atributos.</i>
 * </ul>
 * 
 * 
 * 
 * @author Philipe PEREIRA
 *
 */
public abstract class XMLadapter implements ContentHandler, ErrorHandler {

	private XMLReader reader;

	private final boolean stockable;

	/**
	 * Construtor do objeto leitor de arquivos XML
	 * 
	 */
	public XMLadapter() {
		this(false);
	}

	/**
	 * Construtor do objeto leitor de arquivos XML
	 * 
	 * @param stockable <code>true</code> para se habilitar a opção
	 *                  {@link #notParse(String) stock}, quando se deseja guardar
	 *                  uma parte do código XML sem quebrá-lo
	 */
	public XMLadapter(boolean stockable) {
		this.stockable = stockable;
		try {
			reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			// Specify the error handler and the content handler.
			reader.setContentHandler(this);
			reader.setErrorHandler(this);
		} catch (SAXException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	// =============================== PARSE ===============================

	public void parse(File file) {
		try {
			parse(file.toURI().toURL().toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void parse(String fileName) {
		parse(new InputSource(fileName));
	}

	public void parse(InputStream inputStream) {
		parse(new InputSource(inputStream));
	}

	public void parse(InputSource inputStream) {
		try {
			reader.parse(inputStream);
		} catch (SAXException | IOException event) {
			System.err.println(event.getMessage());
		}
	}

	// =============================== STOCK ===============================

	private transient String qName;

	private transient StringBuilder stockpile;

	private boolean open = false;

	/**
	 * <p>
	 * Função que é chamada quando se quer guardar uma parte do código XML, sem
	 * quebrá-lo entre seus campos e atributos. As funções
	 * {@link #startElement(String, Attributes)}, {@link #characters(String)} e
	 * {@link #endElement(String)} não serão invocadas enquanto o leitor estiver
	 * acumulando os códigos XML a serem guardados.
	 * </p>
	 * 
	 * <p>
	 * Quando a leitura chegar no fim do bloco onde houve início o processo de
	 * estocagem, a função {@link #endElement(String)} será invocada, e o que foi
	 * guardado pode ser recuperado pela função {@link #getStock()}.
	 * </p>
	 * 
	 * <p>
	 * Para que esta função possa ser invocada e tenha efeito, a
	 * {@link XMLadapter#XMLadapter(boolean) variável de estoque} deve ser
	 * estabelecida.
	 * </p>
	 * 
	 * @param qName nome do bloco XML cujo interior será guardado sem ser quebrado.
	 */
	public void notParse(String qName) {
		this.qName = qName;
		stockpile = new StringBuilder();
	}

	/**
	 * Função que encerra o processo de estocagem (iniciado em
	 * {@link #notParse(String)}) e retorna o código XML que foi guardado sem ser
	 * quebrado.
	 * 
	 * @return código XML que foi guardado
	 */
	public String getStock() {
		qName = null;
		String out = stockpile.toString();
		stockpile = null;
		return out;
	}

	// =============================== LEITURA ===============================

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		boolean stock = stockable;
		if (stock)
			stock = this.qName != null;

		if (stock) {
			// se está guardando o código
			if (open)
				stockpile.append(">\n");
			stockpile.append("<" + qName);
			int size = atts.getLength();
			for (int i = 0; i < size; i++)
				stockpile.append(String.format(" %s=\"%s\"", atts.getQName(i), atts.getValue(i)));
			this.open = true;
		} else
			startElement(qName, atts);
	}

	public abstract void startElement(String qName, Attributes atts);

	private transient String content;

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (content == null)
			content = "";
		String str = new String(ch, start, length);
		if (!str.trim().isEmpty())
			content += str;
	}

	public abstract void characters(String s);

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		boolean stock = stockable;
		if (stock)
			stock = this.qName != null && !qName.equals(this.qName);

		if (stock) {
			if (hasContent()) // se há conteúdo
				stockpile.append(String.format(">%s</%s>\n", content, qName));
			else {
				// se não há conteúdo...
				if (this.open) // ... e acabou de abrir
					stockpile.append("/>\n");
				else // já abriu antes (logo foi fechado na linha 170)
					stockpile.append(String.format("</%s>\n", qName));
			}
			this.open = false;
		} else {
			if (hasContent())
				characters(content.trim());
			endElement(qName);
		}
		content = null;
	}

	private boolean hasContent() {
		return content != null ? !content.trim().isEmpty() : false;
	}

	public abstract void endElement(String qName);

	// ========================================================================

	// unuseful methods

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	// error methods
	@Override
	public void error(SAXParseException e) throws SAXException {
		System.err.println("Error");
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		System.err.println("Fatal error");
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		System.err.println("Warning");
	}
}
