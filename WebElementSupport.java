/**
Test Utils WebElementSupport
Copyright (C) 2019
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
**/

import org.openqa.selenium.WebElement;

import groovy.util.XmlSlurper;
import groovy.xml.XmlUtil;
import groovy.util.slurpersupport.GPathResult;

public class WebElementSupport {

	/**
	 * Get only the root element text.
	 * This means that if there is for instance:
	 * <div>
	 *   Some text here
	 *   <div> some more text here
	 	     <div>and some more text here</div>
	 *   </div>
	 * </div>
	 * 
	 * Then with getText() you'd get 'Some text here some more text here and some more text here'.
	 * With this function you'll get 'Some text here'
	 * 
	 * There is no implementation for visualizing or translating breaks or any other markup.
	 * We just return text.
	 * 
	 * The boolean 'strict' tells the function wheather to actually include markup tags.
	 * <ul>
	 *    <li>True --> Really <i>ONLY</i> the root tag will be taken</li>
	 *    <li>False -> Markup tags within the root will also be taken into account</li>
	 * </ul>
	 * 
	 * @param el - WebElement
	 * @param strict - boolean 
	 * @return String - The result
	 */
	public static String getTextFromRootElementOnly(WebElement el, boolean strict) {

		String[] whitelist = [
			'p',
			'b',
			'i',
			'strong',
			'em',
			'mark',
			'small',
			'del',
			'ins',
			'sub',
			'sup'
		];

		if(strict) {
			whitelist = [];
		}

		//Get the element HTML, including outer tags
		String xml_element = el.getAttribute("outerHTML");

		//Put it as a root node
		GPathResult rootNode = new XmlSlurper().parseText(xml_element);

		/*Delete all child nodes.
		 * The XmlSlurper object is tolerant.
		 */
		rootNode.children().each {
			if(!(it.name() in whitelist)) {
				it.replaceNode{}
			}
		}

		/*
		 * Serialize the GPathResult to get the manipulated XML.
		 * If not, you just get the old result.
		 * Mandatory detour.
		 * text result is trimmed.
		 */
		return new XmlSlurper().parseText(XmlUtil.serialize(rootNode))
				.text()
				.trim();
	}
}
