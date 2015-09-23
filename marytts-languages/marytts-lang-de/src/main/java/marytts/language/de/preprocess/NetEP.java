/**
 * Copyright 2002 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.language.de.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for internet (URI or email) patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class NetEP extends ExpansionPattern {
	private final String[] _knownTypes = { "net", "net:email", "net:uri" };
	/**
	 * Every subclass has its own list knownTypes, an internal string representation of known types. These are possible values of
	 * the <code>type</code> attribute to the <code>say-as</code> element, as defined in MaryXML.dtd. If there is more than one
	 * known type, the first type (<code>knownTypes[0]</code>) is expected to be the most general one, of which the others are
	 * specializations.
	 */
	private final List<String> knownTypes = Arrays.asList(_knownTypes);

	public List<String> knownTypes() {
		return knownTypes;
	}

	// Domain-specific primitives:
	/*
	 * Email syntax is specified in http://www.faqs.org/rfcs/rfc2822.html (see end of this file for excerpt)
	 */
	protected final String aText = "[A-Za-z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+";
	protected final String dotAtomText = "(?:" + aText + "(?:\\." + aText + ")*)";
	protected final String sNetEmail = "(?:(" + dotAtomText + ")\\@(" + dotAtomText + "))";
	/*
	 * For the URI regular expression, see the excerpt from RFC2396 as found at http://www.ietf.org/rfc/rfc2396.txt at the bottom
	 * of this file.
	 */
	// protected final String domainSuffix =
	// "(?:ad|ae|af|ag|ai|al|am|an|ao|aq|ar|arpa|as|at|au|aw|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cf|cg|ch|ci|ck|cl|cm|cn|co|com|cr|cs|cu|cv|cx|cy|cz|de|dj|dk|dm|do|dz|ec|edu|ee|eg|eh|es|et|fi|fj|fk|fm|fo|fr|fx|ga|gb|gd|ge|gf|gh|gi|gl|gm|gn|gov|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|in|int|io|iq|ir|is|it|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|mg|mh|mil|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nato|nc|ne|net|nf|ng|ni|nl|no|np|nr|nt|nu|nz|om|org|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|pt|pw|py|qa|re|ro|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|st|su|sv|sy|sz|tc|td|tf|tg|th|tj|tk|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|um|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zr|zw)";
	protected final String domainSuffix = "(?:com|edu|net|info|biz|org|de|eu|uk|ie|fr|au|jp|at|ch|ws|tv|cc)";
	protected final String domain = "(?:(?:[A-Za-z0-9\\-]+\\.)+" + domainSuffix + ")";
	protected final String path = "(?:(?:/~?[A-Za-z0-9\\-\\.\\_]+)+/?)";
	protected final String sNetUri = "(?:(?:(?:http|ftp)://)?(" + domain + ")(" + path + ")?)";
	// protected final String sNetUriSubstructure = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
	// in this, $4 is www.xy.com and $5 is /pub/data/myfile.html

	// We don't use sMatchingChars here, but override isCandidate().

	protected final Pattern reNetEmail = Pattern.compile(sNetEmail);
	protected final Pattern reNetUri = Pattern.compile(sNetUri);
	// protected final Pattern reNetUriSubstructure = Pattern.compile(sNetUriSubstructure);

	private final Pattern reMatchingChars = null;

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	/**
	 * Every subclass has its own logger. The important point is that if several threads are accessing the variable at the same
	 * time, the logger needs to be thread-safe or it will produce rubbish.
	 */
	private Logger logger = MaryUtils.getLogger("NetEP");

	public NetEP() {
		super();
	}

	protected boolean isCandidate(Element t) {
		String s = MaryDomUtils.tokenText(t);
		return (s.indexOf('@') != -1 || s.indexOf('.') != -1 || s.indexOf('/') != -1 || s.indexOf(':') != -1 || s.equals("http")
				|| s.equals("ftp") || s.equals("mailto"));
	}

	/**
	 * Inform whether this module performs a full expansion of the input, or whether other patterns should be applied after this
	 * one.
	 * 
	 * @return false
	 */
	protected boolean doesFullExpansion() {
		return false;
	}

	protected int canDealWith(String s, int type) {
		return match(s, type);
	}

	protected int match(String s, int type) {
		switch (type) {
		case 0:
			if (matchNetEmail(s))
				return 1;
			if (matchNetUri(s))
				return 2;
			break;
		case 1:
			if (matchNetEmail(s))
				return 1;
			break;
		case 2:
			if (matchNetUri(s))
				return 2;
			break;
		}
		return -1;
	}

	protected List<Element> expand(List<Element> tokens, String s, int type) {
		if (tokens == null)
			throw new NullPointerException("Received null argument");
		if (tokens.isEmpty())
			throw new IllegalArgumentException("Received empty list");
		Element firstOld = (Element) tokens.get(0);
		Document doc = firstOld.getOwnerDocument();
		// we expect type to be one of the return values of match():
		List<Element> expanded = null;
		switch (type) {
		case 1:
			expanded = expandNetEmail(doc, s);
			break;
		case 2:
			expanded = expandNetUri(doc, s);
			break;
		}
		replaceTokens(tokens, expanded);
		// Slow down the new part,
		// so the spelled out form will be understandable.
		// slowDown((Element)expanded.get(0),
		// (Element)expanded.get(expanded.size()-1));
		return expanded;
	}

	private boolean matchNetEmail(String s) {
		return reNetEmail.matcher(s).matches();
	}

	private boolean matchNetUri(String s) {
		return reNetUri.matcher(s).matches();
	}

	protected List<Element> expandNetEmail(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reNetEmail.matcher(s);
		if (!reMatcher.find())
			return null;
		String localPart = reMatcher.group(1);
		String localPartExpanded = abbrev.ruleExpandAbbrev(localPart, true); // true = sayPuncutation
		exp.addAll(makeNewTokens(doc, localPartExpanded, true, // create mtu
				localPart, true)); // force accents
		exp.add(MaryDomUtils.createBoundary(doc));
		exp.addAll(makeNewTokens(doc, "at['?{t]", true, // create mtu
				"@", true)); // force accents
		String domain = reMatcher.group(2);
		exp.addAll(expandDomain(doc, domain));
		return exp;
	}

	protected List<Element> expandNetUri(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reNetUri.matcher(s);
		if (!reMatcher.find())
			return null;
		String domain = reMatcher.group(1);
		exp.addAll(expandDomain(doc, domain));
		String path = reMatcher.group(2);
		if (path != null && path.length() > 0) {
			String pathExpanded = abbrev.ruleExpandAbbrev(path, true); // true = sayPunctuation
			exp.addAll(makeNewTokens(doc, pathExpanded, true, // create mtu
					path, true)); // force accents
			exp.add(MaryDomUtils.createBoundary(doc));
		}
		return exp;
	}

	private List<Element> expandDomain(Document doc, String domain) {
		logger.debug("Expanding domain `" + domain + "'");
		ArrayList<Element> exp = new ArrayList<Element>();
		String domainSuffix = null;
		String toExpand;
		if (domain.endsWith(".com")) {
			toExpand = domain.substring(0, domain.length() - 4);
			domainSuffix = " dot['dOt] com['kOm]";
		} else if (domain.lastIndexOf(".") == domain.length() - 3) {
			// Domains consisting of two characters (de, ch, uk, ...)
			// are to be spelled out
			toExpand = domain.substring(0, domain.length() - 3);
			logger.debug("toExpand = `" + toExpand + "'");
			domainSuffix = " " + domain.substring(domain.length() - 2, domain.length() - 1) + " "
					+ domain.substring(domain.length() - 1);
			logger.debug("domainSuffix = `" + domainSuffix + "'");
		} else {
			toExpand = domain;
		}
		String domainExpanded = toExpand.replaceAll("\\.", " ");
		logger.debug("domainExpanded = `" + domainExpanded + "'");
		if (domainSuffix != null)
			domainExpanded += domainSuffix;
		logger.debug("domainExpanded with suffix = `" + domainExpanded + "'");
		exp.addAll(makeNewTokens(doc, domainExpanded, true, // create mtu
				domain, true)); // force accents
		// System.err.println("Expanded `" + s + "' as `" + domainExpanded + "'");
		exp.add(MaryDomUtils.createBoundary(doc));
		return exp;
	}

}

/*
 * Request for Comments: 2822 3. Syntax
 * 
 * 3.2.4. Atom
 * 
 * Several productions in structured header field bodies are simply strings of certain basic characters. Such productions are
 * called atoms.
 * 
 * Some of the structured header field bodies also allow the period character (".", ASCII value 46) within runs of atext. An
 * additional "dot-atom" token is defined for those purposes.
 * 
 * atext = ALPHA / DIGIT / ; Any character except controls, "!" / "#" / ; SP, and specials. "$" / "%" / ; Used for atoms "&" / "'"
 * / "*" / "+" / "-" / "/" / "=" / "?" / "^" / "_" / "`" / "{" / "|" / "}" / "~"
 * 
 * atom = [CFWS] 1*atext [CFWS]
 * 
 * dot-atom = [CFWS] dot-atom-text [CFWS]
 * 
 * dot-atom-text = 1*atext *("." 1*atext)
 * 
 * Both atom and dot-atom are interpreted as a single unit, comprised of the string of characters that make it up. Semantically,
 * the optional comments and FWS surrounding the rest of the characters are not part of the atom; the atom is only the run of
 * atext characters in an atom, or the atext and "." characters in a dot-atom.
 * 
 * 3.4. Address Specification
 * 
 * Addresses occur in several message header fields to indicate senders and recipients of messages. An address may either be an
 * individual mailbox, or a group of mailboxes.
 * 
 * address = mailbox / group
 * 
 * mailbox = name-addr / addr-spec
 * 
 * name-addr = [display-name] angle-addr
 * 
 * angle-addr = [CFWS] "<" addr-spec ">" [CFWS] / obs-angle-addr
 * 
 * group = display-name ":" [mailbox-list / CFWS] ";" [CFWS]
 * 
 * display-name = phrase
 * 
 * mailbox-list = (mailbox *("," mailbox)) / obs-mbox-list
 * 
 * address-list = (address *("," address)) / obs-addr-list
 * 
 * A mailbox receives mail. It is a conceptual entity which does not necessarily pertain to file storage. For example, some sites
 * may choose to print mail on a printer and deliver the output to the addressee's desk. Normally, a mailbox is comprised of two
 * parts: (1) an optional display name that indicates the name of the recipient (which could be a person or a system) that could
 * be displayed to the user of a mail application, and (2) an addr-spec address enclosed in angle brackets ("<" and ">"). There is
 * also an alternate simple form of a mailbox where the addr-spec address appears alone, without the recipient's name or the angle
 * brackets. The Internet addr-spec address is described in section 3.4.1.
 * 
 * Note: Some legacy implementations used the simple form where the addr-spec appears without the angle brackets, but included the
 * name of the recipient in parentheses as a comment following the addr-spec. Since the meaning of the information in a comment is
 * unspecified, implementations SHOULD use the full name-addr form of the mailbox, instead of the legacy form, to specify the
 * display name associated with a mailbox. Also, because some legacy implementations interpret the comment, comments generally
 * SHOULD NOT be used in address fields to avoid confusing such implementations.
 * 
 * When it is desirable to treat several mailboxes as a single unit (i.e., in a distribution list), the group construct can be
 * used. The group construct allows the sender to indicate a named group of recipients. This is done by giving a display name for
 * the group, followed by a colon, followed by a comma separated list of any number of mailboxes (including zero and one), and
 * ending with a semicolon. Because the list of mailboxes can be empty, using the group construct is also a simple way to
 * communicate to recipients that the message was sent to one or more named sets of recipients, without actually providing the
 * individual mailbox address for each of those recipients.
 * 
 * 3.4.1. Addr-spec specification
 * 
 * An addr-spec is a specific Internet identifier that contains a locally interpreted string followed by the at-sign character
 * ("@", ASCII value 64) followed by an Internet domain. The locally interpreted string is either a quoted-string or a dot-atom.
 * If the string can be represented as a dot-atom (that is, it contains no characters other than atext characters or "."
 * surrounded by atext
 * 
 * characters), then the dot-atom form SHOULD be used and the quoted-string form SHOULD NOT be used. Comments and folding white
 * space SHOULD NOT be used around the "@" in the addr-spec.
 * 
 * addr-spec = local-part "@" domain
 * 
 * local-part = dot-atom / quoted-string / obs-local-part
 * 
 * domain = dot-atom / domain-literal / obs-domain
 * 
 * domain-literal = [CFWS] "[" *([FWS] dcontent) [FWS] "]" [CFWS]
 * 
 * dcontent = dtext / quoted-pair
 * 
 * dtext = NO-WS-CTL / ; Non white space controls
 * 
 * %d33-90 / ; The rest of the US-ASCII %d94-126 ; characters not including "[", ; "]", or "\"
 * 
 * The domain portion identifies the point to which the mail is delivered. In the dot-atom form, this is interpreted as an
 * Internet domain name (either a host name or a mail exchanger name) as described in [STD3, STD13, STD14]. In the domain-literal
 * form, the domain is interpreted as the literal Internet address of the particular host. In both cases, how addressing is used
 * and how messages are transported to a particular host is covered in the mail transport document [RFC2821]. These mechanisms are
 * outside of the scope of this document.
 * 
 * The local-part portion is a domain dependent string. In addresses, it is simply interpreted on the particular host as a name of
 * a particular mailbox.
 */

/*
 * RFC 2396 URI Generic Syntax August 1998
 * 
 * 
 * B. Parsing a URI Reference with a Regular Expression
 * 
 * As described in Section 4.3, the generic URI syntax is not sufficient to disambiguate the components of some forms of URI.
 * Since the "greedy algorithm" described in that section is identical to the disambiguation method used by POSIX regular
 * expressions, it is natural and commonplace to use a regular expression for parsing the potential four components and fragment
 * identifier of a URI reference.
 * 
 * The following line is the regular expression for breaking-down a URI reference into its components.
 * 
 * ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))? 12 3 4 5 6 7 8 9
 * 
 * The numbers in the second line above are only to assist readability; they indicate the reference points for each subexpression
 * (i.e., each paired parenthesis). We refer to the value matched for subexpression <n> as $<n>. For example, matching the above
 * expression to
 * 
 * http://www.ics.uci.edu/pub/ietf/uri/#Related
 * 
 * results in the following subexpression matches:
 * 
 * $1 = http: $2 = http $3 = //www.ics.uci.edu $4 = www.ics.uci.edu $5 = /pub/ietf/uri/ $6 = <undefined> $7 = <undefined> $8 =
 * #Related $9 = Related
 * 
 * where <undefined> indicates that the component is not present, as is the case for the query component in the above example.
 * Therefore, we can determine the value of the four components and fragment as
 * 
 * scheme = $2 authority = $4 path = $5 query = $7 fragment = $9
 * 
 * and, going in the opposite direction, we can recreate a URI reference from its components using the algorithm in step 7 of
 * Section 5.2.
 * 
 * 
 * 
 * 
 * 
 * Berners-Lee, et. al. Standards Track [Page 29]
 */
