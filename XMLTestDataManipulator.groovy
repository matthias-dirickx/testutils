import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import groovy.xml.XmlUtil;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;


public class XMLTestDataManipulator {

	//TODO -- make independent of katalon and add to fednot selenium support or fednot test support.
	//TOTO -- make pure java implementation for distributability

	/*XML holder for the object to do the manipulations on.
	 XMLSlurper is a working object. You do the manipulation on a node.
	 Serializing the xml variable will return the updated XML.*/
	private GPathResult xml;

	private Map<String, String> formatLibrary;

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

		dateNodes.each {GPathResult node ->
			node.replaceBody(
					now.plusNanos(
					ChronoUnit.NANOS.between(
					baseLineDt,
					getDateFromNode(node)))
					.format(
					getDateTimeFormatterFromNode(node)));
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
		try {
		    return LocalDateTime.parse(
		        node.toString(),
		    	   getDateTimeFormatterFromNode(node));
		} catch (java.time.DateTimeException e) {
			return LocalDate.parse(
			    node.toString(),
			    getDateTimeFormatterFromNode(node))
			    .atStartOfDay();
		}
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
	 * Helper method -- set the format library (at class level)
	 * @return
	 */
	private Map<String, String> setFormatLibrary() {
		Map<String, String> formatLibrary = new HashMap<>();
		formatLibrary.put(
				/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}$/,
				"yyyy-MM-dd'T'HH:mm:ss.SSS");
		formatLibrary.put(
				/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/,
				"yyyy-MM-dd'T'HH:mm:ss");

		formatLibrary.put(
		     	    /^\d{4}-\d{2}-\d{2}$/,
		     	    "uuuu-MM-dd");
		     	    
		this.formatLibrary = formatLibrary;
	}

	private Map<String, String> getFormatLibrary() {
		if(this.formatLibrary == null) {
			Map<String, String> formatLibrary = new HashMap<>();

			formatLibrary.put(
					/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}$/,
					"yyyy-MM-dd'T'HH:mm:ss.SSS");

			formatLibrary.put(
					/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/,
					"yyyy-MM-dd'T'HH:mm:ss");

		     formatLibrary.put(
		     	    /^\d{4}-\d{2}-\d{2}$/,
		     	    "uuuu-MM-dd");

			this.formatLibrary = formatLibrary;
		}
		return this.formatLibrary;
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

		getFormatLibrary().each {reg, format ->
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
