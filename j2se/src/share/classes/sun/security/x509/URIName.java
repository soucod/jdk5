/*
 * @(#)URIName.java	1.16 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.x509;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import sun.security.util.*;

/**
 * This class implements the URIName as required by the GeneralNames
 * ASN.1 object.
 * <p>
 * [RFC3280] When the subjectAltName extension contains a URI, the name MUST be
 * stored in the uniformResourceIdentifier (an IA5String). The name MUST
 * be a non-relative URL, and MUST follow the URL syntax and encoding
 * rules specified in [RFC 1738].  The name must include both a scheme
 * (e.g., "http" or "ftp") and a scheme-specific-part.  The scheme-
 * specific-part must include a fully qualified domain name or IP
 * address as the host.
 * <p>
 * As specified in [RFC 1738], the scheme name is not case-sensitive
 * (e.g., "http" is equivalent to "HTTP").  The host part is also not
 * case-sensitive, but other components of the scheme-specific-part may
 * be case-sensitive. When comparing URIs, conforming implementations
 * MUST compare the scheme and host without regard to case, but assume
 * the remainder of the scheme-specific-part is case sensitive.
 * <p>
 * [RFC1738] In general, URLs are written as follows:
 * <pre>
 * <scheme>:<scheme-specific-part>
 * </pre>
 * A URL contains the name of the scheme being used (<scheme>) followed
 * by a colon and then a string (the <scheme-specific-part>) whose
 * interpretation depends on the scheme.
 * <p>
 * While the syntax for the rest of the URL may vary depending on the
 * particular scheme selected, URL schemes that involve the direct use
 * of an IP-based protocol to a specified host on the Internet use a
 * common syntax for the scheme-specific data:
 * <pre>
 * //<user>:<password>@<host>:<port>/<url-path>
 * </pre>
 * [RFC2732] specifies that an IPv6 address contained inside a URL
 * must be enclosed in square brackets (to allow distinguishing the
 * colons that separate IPv6 components from the colons that separate
 * scheme-specific data.
 * <p>
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @author Sean Mullan
 * @author Steve Hanna
 * @version 1.16
 * @see GeneralName
 * @see GeneralNames
 * @see GeneralNameInterface
 */
public class URIName implements GeneralNameInterface {

    // private attributes
    private URI uri;
    private String host;
    private DNSName hostDNS;
    private IPAddressName hostIP;

    /**
     * Create the URIName object from the passed encoded Der value.
     *
     * @param derValue the encoded DER URIName.
     * @exception IOException on error.
     */
    public URIName(DerValue derValue) throws IOException {
	this(derValue.getIA5String());
    }

    /**
     * Create the URIName object with the specified name.
     *
     * @param name the URIName.
     * @throws IOException if name is not a proper URIName
     */
    public URIName(String name) throws IOException {
	try {
	    uri = new URI(name);
	} catch (URISyntaxException use) {
	    throw (IOException) new IOException(use.toString()).initCause(use);
	}
	if (uri.getScheme() == null) {
	    // for now, assume this is a NameConstraintsExtension URIName
	    // @@@ This should be fixed so that we know whether we are parsing
	    // @@@ an AlternativeName or NameConstraints extension.
	    host = uri.getSchemeSpecificPart();
	    try {
	        if (host.charAt(0) == '.') {
	            hostDNS = new DNSName(host.substring(1));
	        } else {
 	            hostDNS = new DNSName(host);
	        }
	    } catch (IOException ioe) {
		// assume this was a badly formed AlternativeName
		throw (IOException) new IOException
		    ("URI name must include scheme").initCause(ioe);
	    }
	} else {
	    if (!uri.isAbsolute()) {
	        throw new IOException("URI name must not be relative");
	    }
	    host = uri.getHost();
	    if (host == null) {
		throw new IOException("URI name must include host");
	    }
            if (host.charAt(0) == '[') {
		// Verify host is a valid IPv6 address name
		String ipV6Host = host.substring(1, host.length()-1);
		try {
                    hostIP = new IPAddressName(ipV6Host);
		} catch (IOException ioe) {
                    throw new IOException
                        ("Host portion is not a valid IPv6 address: "
                        + ioe.getMessage());
		}
            } else {
		try {
                    hostDNS = new DNSName(host);
		} catch (IOException ioe) {
                    // Not a valid DNS Name; see if it is a valid IPv4
                    // IPAddressName
                    try {
                        hostIP = new IPAddressName(host);
                    } catch (Exception ioe2) {
                        throw new IOException("Host portion is not a valid "
                                + "DNS name, IPv4 address, or IPv6 address");
                    }
		}
            }
	}
    }

    /**
     * Return the type of the GeneralName.
     */
    public int getType() {
        return GeneralNameInterface.NAME_URI;
    }

    /**
     * Encode the URI name into the DerOutputStream.
     *
     * @param out the DER stream to encode the URIName to.
     * @exception IOException on encoding errors.
     */
    public void encode(DerOutputStream out) throws IOException {
        out.putIA5String(uri.toASCIIString());
    }

    /**
     * Convert the name into user readable string.
     */
    public String toString() {
        return "URIName: " + uri.toString();
    }

    /**
     * Compares this name with another, for equality.
     *
     * @return true iff the names are equivalent according to RFC2459.
     */
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}

	if (!(obj instanceof URIName)) {
	    return false;
	}

	URIName other = (URIName) obj;

	return uri.equals(other.getURI());
    }

    /**
     * Returns the URIName as a java.net.URI object
     */
    public URI getURI() {
	return uri;
    }

    /**
     * Returns this URI name.
     */
    public String getName() {
        return uri.toString();
    }

    /**
     * Return the scheme name portion of a URIName
     *
     * @returns scheme portion of full name
     */
    public String getScheme() {
	return uri.getScheme();
    }

    /**
     * Return the host name or IP address portion of the URIName
     *
     * @returns host name or IP address portion of full name
     */
    public String getHost() {
	return host;
    }

    /**
     * Return the host object type; if host name is a
     * DNSName, then this host object does not include any
     * initial "." on the name.
     *
     * @returns host name as DNSName or IPAddressName
     */
    public Object getHostObject() {
	if (hostIP != null) {
	    return hostIP;
	} else {
	    return hostDNS;
	}
    }

    /**
     * Returns the hash code value for this object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode() {
	return uri.hashCode();
    }

    /**
     * Return type of constraint inputName places on this name:<ul>
     *   <li>NAME_DIFF_TYPE = -1: input name is different type from name 
     *       (i.e. does not constrain).
     *   <li>NAME_MATCH = 0: input name matches name.
     *   <li>NAME_NARROWS = 1: input name narrows name (is lower in the naming 
     *       subtree)
     *   <li>NAME_WIDENS = 2: input name widens name (is higher in the naming 
     *       subtree)
     *   <li>NAME_SAME_TYPE = 3: input name does not match or narrow name, but 
     *       is same type.
     * </ul>.
     * These results are used in checking NameConstraints during
     * certification path verification.
     * <p>
     * RFC3280: For URIs, the constraint applies to the host part of the name. 
     * The constraint may specify a host or a domain.  Examples would be
     * "foo.bar.com";  and ".xyz.com".  When the the constraint begins with
     * a period, it may be expanded with one or more subdomains.  That is,
     * the constraint ".xyz.com" is satisfied by both abc.xyz.com and
     * abc.def.xyz.com.  However, the constraint ".xyz.com" is not satisfied
     * by "xyz.com".  When the constraint does not begin with a period, it
     * specifies a host.
     * <p>
     * @param inputName to be checked for being constrained
     * @returns constraint type above
     * @throws UnsupportedOperationException if name is not exact match, but 
     *  narrowing and widening are not supported for this name type.
     */
    public int constrains(GeneralNameInterface inputName) 
	throws UnsupportedOperationException {
	int constraintType;
	if (inputName == null) {
	    constraintType = NAME_DIFF_TYPE;
	} else if (inputName.getType() != NAME_URI) {
	    constraintType = NAME_DIFF_TYPE;
	} else {
 	    // Assuming from here on that one or both of these is
 	    // actually a URI name constraint (not a URI), so we
 	    // only need to compare the host portion of the name

	    String otherHost = ((URIName)inputName).getHost();

 	    // Quick check for equality
 	    if (otherHost.equalsIgnoreCase(host)) {
		constraintType = NAME_MATCH;
 	    } else {
 		Object otherHostObject = ((URIName)inputName).getHostObject();
 
 		if ((hostDNS == null) ||
 		    !(otherHostObject instanceof DNSName)) {
 		    // If one (or both) is an IP address, only same type
 		    constraintType = NAME_SAME_TYPE;
 		} else {
 		    // Both host portions are DNS names. Are they domains?
 		    boolean thisDomain = (host.charAt(0) == '.');
 		    boolean otherDomain = (otherHost.charAt(0) == '.');
 		    DNSName otherDNS = (DNSName) otherHostObject;
 
 		    // Run DNSName.constrains.
 		    constraintType = hostDNS.constrains(otherDNS);
 		    // If neither one is a domain, then they can't
 		    // widen or narrow. That's just SAME_TYPE.
 		    if ((!thisDomain && !otherDomain) &&
 			((constraintType == NAME_WIDENS) ||
 			 (constraintType == NAME_NARROWS))) {
			constraintType = NAME_SAME_TYPE;
		    }

 		    // If one is a domain and the other isn't,
 		    // then they can't match. The one that's a
 		    // domain doesn't include the one that's
 		    // not a domain.
 		    if ((thisDomain != otherDomain) &&
 			(constraintType == NAME_MATCH)) {
 			if (thisDomain) {
 			    constraintType = NAME_WIDENS;
 			} else {
 			    constraintType = NAME_NARROWS;
			}
		    }
		}
	    }
	}
	return constraintType;
    }

    /**
     * Return subtree depth of this name for purposes of determining
     * NameConstraints minimum and maximum bounds and for calculating
     * path lengths in name subtrees.
     *
     * @returns distance of name from root
     * @throws UnsupportedOperationException if not supported for this name type
     */
    public int subtreeDepth() throws UnsupportedOperationException {
	DNSName dnsName = null;
	try {
	    dnsName = new DNSName(host);
	} catch (IOException ioe) {
	    throw new UnsupportedOperationException(ioe.getMessage());
	}
	return dnsName.subtreeDepth();
    }
}
