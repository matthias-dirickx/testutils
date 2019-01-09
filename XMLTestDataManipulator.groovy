/*
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/


import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import groovy.xml.XmlUtil;

import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern
import java.util.concurrent.TimeUnit

import com.kms.katalon.core.logging.KeywordLogger;

public class XMLTestDataManipulator {
	
	//TODO Make this file compatible with both groovy and java.
	//especially each statements should be replaced and XmlSlurper lib should be verified within java context.

	//Logger -- Katalon, so not usable outside Katalon context.
	KeywordLogger log = new KeywordLogger();

	/*XML holder for the object to do the manipulations on.
	XMLSlurper is a working object. You do the manipulation on a node.
	Serializing the xml variable will return the updated XML.*/
	private GPathResult xml;

	
	/**
	 * Creates an instance of XMLTestDataManipulator.
	 * The xml object (type is GPathResult) is set with the file content.
	 * 
	 * The file needs to be xml. If not, this will fail.
	 * There is no error handling with regards to file type.
	 * 
	 * @param xmlFile -- File object
	 */
	public XMLTestDataManipulator(File xmlFile) {
		xml = new XmlSlurper().parse(xmlFile);
	}

	
	/**
	 * Creates an instance of XMLTestDataManipulator.
	 * The xml object (type is GPathResult) is set with the string.
	 * 
	 * The string needs to be valid xml. If not, this will fail.
	 * There is no error handling with regards to xml validation.
	 * 
	 * @param xmlString -- String object
	 */
	public XMLTestDataManipulator(String xmlString) {
		this.xml = new XmlSlurper().parseText(xmlString);
	}

	
	/**
	 * Weak implementation! Read code before you implement.
	 * 
	 * We guess the creation date:
	 *     Currently:
	 *        - the tag containing either 'date' or 'time'
	 *        - that also contains 'creat' (to cover 'creation' and 'created' and 'create')
	 *        - that appears at the highest level (closest in level to the root element)
	 *        - will serve as the creation date.
	 *        
	 * Then there is a diff in nanoseconds between a calculated 'now' and the baseline guessed creation date.
	 * 
	 * This difference is then added to all dates.
	 * All dates are guessed by taking every tag that contains 'date'.
	 * 
	 * Time has been put out of scope now.
	 * 
	 * This will update the guessed creation date to 'now', and update all other dates relative to that.
	 * 
	 * @return this (fluid api)
	 */
	public XMLTestDataManipulator updateAllDatesRelativeToDetectedCreationDate() {
		List<GPathResult> dateNodes = getDateNodes();
		GPathResult createdDateNode = getCreatedDateNode();

		//Baseline date-time (what is the estimated created date)
		LocalDateTime baseLineDt = getDateFromNode(createdDateNode);

		//new created date time
		LocalDateTime now = LocalDateTime.now();

		//Milliseconds between detected creation and now date
		Long diff = ChronoUnit.NANOS.between(baseLineDt, now)

		dateNodes.each {GPathResult node ->
			LocalDateTime dt = getDateFromNode(node);
			DateTimeFormatter dtf = getDateTimeFormatterFromNode(node);
			
			LocalDateTime newdt = dt.plusNanos(diff);
			
			node.replaceBody(newdt.format(dtf));
		}
		return this;
	}

	
	/**
	 * Get the XML serialized as string.
	 * 
	 * Example use:
	 *    - do manipulations
	 *    - getXmlAsString() to put it in a message body.
	 * 
	 * @return String -- serialized string object
	 */
	public String getXmlAsString() {
		return XmlUtil.serialize(this.xml);
	}

	
	/**
	 * Get the guessed creation date based on a regex.
	 * 
	 * Find all, then take [0] of the list.
	 * 
	 * @return GPathResult -- Guessed creation date node
	 */
	private GPathResult getCreatedDateNode() {
		//sequences that contain either 'date' or 'time' that also contain 'creat'.
		//creat because create, created, creation
		final String createdDateTimeTagFinderRegex =
				/(?i)((?:(?=.*date.*)|(?=.*time.*)))(.*creat.*)/;

		def creationCandidates = xml.breadthFirst()
				.findAll(
				{GPathResult node ->
					node.name() =~ createdDateTimeTagFinderRegex})
				.collect(
				{GPathResult node -> node});
		return creationCandidates[0];
	}

	
	/**
	 * Get the node body as date.
	 * 
	 * If a date is multiple fields, then this won't work with the current implementation.
	 * 
	 * In that case you need to look to the child nodes. This assumed a date field with one text input.
	 * 
	 * @param node -- GPathResult
	 * @return LocalDateTime -- A LocalDateTime object derived from the node.
	 *     --> If format not defined, IllegalFormatException thrown by getDateTimeFormatterFromNode()
	 */
	private LocalDateTime getDateFromNode(GPathResult node) {
		LocalDateTime dateTime = LocalDateTime.parse(
				node.toString(),
				getDateTimeFormatterFromNode(node));
		return dateTime;
	}
	
	
	/**
	 * 
	 * @param node -- GPathResult
	 * @return DateTimeFormatter
	 */
	private DateTimeFormatter getDateTimeFormatterFromNode(GPathResult node) {
		return DateTimeFormatter.ofPattern(dateFormatDetector(node.toString()));
	}

	
	/**
	 * Get the guessed date format by regex comparison.
	 * 
	 * @param dateString -- String (date as string)
	 * @return String -- format in string object
	 * @throws IllegalFormatException -- When format not matchable
	 */
	private String dateFormatDetector(String dateString) throws IllegalFormatException {
		String formatToUse;
		boolean found;
		found = false;

		Map<String, String> formatLibrary = new HashMap<>();
		formatLibrary.put(
			/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}$/,
			"yyyy-MM-dd'T'HH:mm:ss.SSS");
		formatLibrary.put(
			/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/,
			"yyyy-MM-dd'T'HH:mm:ss");

		formatLibrary.each {reg, format ->
			boolean m = (dateString =~ reg);
			if(m) {
				formatToUse = format;
				found = true;
			}
		}

		if (found) {
			return formatToUse;
		} else {
			String message = "Date format for $dateString not found."+
					"\nCheck if:"+
					"\n- the value is a date"+
					"\n- that the format has a definition."
			throw new IllegalFormatException();
		}
	}

	
	/**
	 * Get the date tags.
	 * 
	 * @return List<GPathResult> -- a list of nodes that contain 'date' in the tag name.
	 */
	private List<GPathResult> getDateNodes() {
		final String dateFinderRegex =
				/(?i)(.*date.*)/;

		List<GPathResult> dateNodes = xml.breadthFirst()
				.findAll(
				{GPathResult node ->
					node.name() =~ dateFinderRegex})
				.collect(
				{GPathResult node -> node});
		return dateNodes;
	}
}
