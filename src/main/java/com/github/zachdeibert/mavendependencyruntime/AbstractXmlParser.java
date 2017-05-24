package com.github.zachdeibert.mavendependencyruntime;

import java.text.ParseException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Base class for any class that needs to do XML parsing
 * 
 * @author Zach Deibert
 * @since 1.0.0
 */
abstract class AbstractXmlParser {
	/**
	 * Searches for a node and returns the text inside of it
	 * 
	 * @param name
	 *            The name of the node to search for
	 * @param node
	 *            The node to search inside of
	 * @param def
	 *            The default value, or <code>null</code> if the value is
	 *            required
	 * @return The text content of the node it found, or <code>def</code> if the
	 *         node is not found
	 * @since 1.0.0
	 * @throws ParseException
	 *             If the node cannot be found and there is no default value
	 */
	protected static String find(String name, Element node, String def) throws ParseException {
		NodeList list = node.getElementsByTagName(name);
		if (list.getLength() > 0) {
			return list.item(0).getTextContent();
		} else if (def == null) {
			throw new ParseException(String.format("Unable to find required tag '%s' in node", name), -1);
		} else {
			return def;
		}
	}
}
