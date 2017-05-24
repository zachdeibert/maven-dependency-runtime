package com.github.zachdeibert.mavendependencyruntime;

import java.text.ParseException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

abstract class AbstractXmlParser {
	protected static String find(String name, Element node, String def) throws ParseException {
		NodeList list = node.getElementsByTagName(name);
		if ( list.getLength() > 0 ) {
			return list.item(0).getTextContent();
		} else if ( def == null ) {
			throw new ParseException(String.format("Unable to find required tag '%s' in node", name), -1);
		} else {
			return def;
		}
	}
}
