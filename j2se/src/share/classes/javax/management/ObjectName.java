/*
 * @(#)ObjectName.java	1.67 04/04/19
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.management;


// java import
import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import com.sun.jmx.mbeanserver.GetPropertyAction;

/**
 * <p>Represents the object name of an MBean, or a pattern that can
 * match the names of several MBeans.  Instances of this class are
 * immutable.</p>
 *
 * <p>An instance of this class can 
 * be used to represent: 
 * <ul>
 * <li> An object name 
 * <li> An object name pattern, within the context of a query
 * </ul></p>
 *
 * <p>An object name consists of two parts, the domain and the key
 * properties.</p>
 *
 * <p>The <em>domain</em> is a string of characters not including
 * the character colon (<code>:</code>).</p>
 *
 * <p>If the domain includes at least one occurrence of the wildcard
 * characters asterisk (<code>*</code>) or question mark
 * (<code>?</code>), then the object name is a pattern.  The asterisk
 * matches any sequence of zero or more characters, while the question
 * mark matches any single character.</p>
 *
 * <p>If the domain is empty, it will be replaced in certain contexts
 * by the <em>default domain</em> of the MBean server in which the
 * ObjectName is used.</p>
 *
 * <p>The <em>key properties</em> are an unordered set of keys and
 * associated values.</p>
 *
 * <p>Each <em>key</em> is a nonempty string of characters which may
 * not contain any of the characters comma (<code>,</code>), equals
 * (<code>=</code>), colon, asterisk, or question mark.  The same key
 * may not occur twice in a given ObjectName.</p>
 *
 * <p>Each <em>value</em> associated with a key is a string of
 * characters that is either unquoted or quoted.</p>
 *
 * <p>An <em>unquoted value</em> is a possibly empty string of
 * characters which may not contain any of the characters comma,
 * equals, colon, quote, asterisk, or question mark.</p>
 *
 * <p>A <em>quoted value</em> consists of a quote (<code>"</code>),
 * followed by a possibly empty string of characters, followed by
 * another quote.  Within the string of characters, the backslash
 * (<code>\</code>) has a special meaning.  It must be followed by
 * one of the following characters:</p>
 *
 * <ul>
 * <li>Another backslash.  The second backslash has no special
 * meaning and the two characters represent a single backslash.
 *
 * <li>The character 'n'.  The two characters represent a newline
 * ('\n' in Java).
 *
 * <li>A quote.  The two characters represent a quote, and that quote
 * is not considered to terminate the quoted value. An ending closing
 * quote must be present for the quoted value to be valid.
 *
 * <li>A question mark (?) or star (*).  The two characters represent
 * a question mark or star respectively.
 * </ul>
 *
 * <p>A quote, question mark, or star may not appear inside a quoted
 * value except immediately after an odd number of consecutive
 * backslashes.</p>
 *
 * <p>The quotes surrounding a quoted value, and any backslashes
 * within that value, are considered to be part of the value.</p>
 *
 * <p>An ObjectName may be a <em>property pattern</em>.  In this case
 * it may have zero or more keys and associated values.  It matches a
 * nonpattern ObjectName whose domain matches and that contains the
 * same keys and associated values, as well as possibly other keys and
 * values.</p>
 *
 * <p>An ObjectName is a pattern if its domain contains a wildcard or
 * if the ObjectName is a property pattern.</p>
 *
 * <p>If an ObjectName is not a pattern, it must contain at least one
 * key with its associated value.</p>
 *
 * <p>An ObjectName can be written as a String with the following
 * elements in order:</p>
 *
 * <ul>
 * <li>The domain.
 * <li>A colon (<code>:</code>).
 * <li>A key property list as defined below.
 * </ul>
 *
 * <p>A key property list written as a String is a comma-separated
 * list of elements.  Each element is either an asterisk or a key
 * property.  A key property consists of a key, an equals
 * (<code>=</code>), and the associated value.</p>
 *
 * <p>At most one element of a key property list may be an asterisk.
 * If the key property list contains an asterisk element, the
 * ObjectName is a property pattern.</p>
 *
 * <p>Spaces have no special significance in a String representing an
 * ObjectName.  For example, the String:
 * <pre>
 * domain: key1 = value1 , key2 = value2
 * </pre>
 * represents an ObjectName with two keys.  The name of each key
 * contains six characters, of which the first and last are spaces.
 * The value associated with the key <code>"&nbsp;key1&nbsp;"</code>
 * also begins and ends with a space.</p>
 *
 * <p>In addition to the restrictions on characters spelt out above,
 * no part of an ObjectName may contain a newline character
 * (<code>'\n'</code>), whether the domain, a key, or a value, whether
 * quoted or unquoted.  The newline character can be represented in a
 * quoted value with the sequence <code>\n</code>.
 *
 * <p>The rules on special characters and quoting apply regardless of
 * which constructor is used to make an ObjectName.</p>
 *
 * <p>To avoid collisions between MBeans supplied by different
 * vendors, a useful convention is to begin the domain name with the
 * reverse DNS name of the organization that specifies the MBeans,
 * followed by a period and a string whose interpretation is
 * determined by that organization.  For example, MBeans specified by
 * Sun Microsystems Inc., DNS name <code>sun.com</code>, would have
 * domains such as <code>com.sun.MyDomain</code>.  This is essentially
 * the same convention as for Java-language package names.</p>
 *
 * @since 1.5
 */
public class ObjectName implements QueryExp, Serializable { 

    /**
     * A structure recording property structure and
     * proposing minimal services
     */
    private final static class Property {

        int _key_index;
        int _key_length;
        int _value_length;

        /**
         * Constructor.
         */
        Property(int key_index, int key_length, int value_length) {
            _key_index = key_index;
            _key_length = key_length;
            _value_length = value_length;
        }

        /**
         * Assigns the key index of property
         */
        void setKeyIndex(int key_index) {
            _key_index = key_index;
        }

        /**
         * Returns a key string for receiver key
         */
        String getKeyString(String name) {
            return name.substring(_key_index, _key_index + _key_length);
        }

        /**
         * Returns a value string for receiver key
         */
        String getValueString(String name) {
            int in_begin = _key_index + _key_length + 1;
            int out_end = in_begin + _value_length;
            return name.substring(in_begin, out_end);
        }
    }
    // Inner classes <========================================



    // Private fields ---------------------------------------->


    // Serialization compatibility stuff -------------------->

    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form 
    private static final long oldSerialVersionUID = -5467795090068647408L;
    //
    // Serial version for new serial form 
    private static final long newSerialVersionUID = 1081892073854801359L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields = 
    {
	new ObjectStreamField("domain", String.class),
	new ObjectStreamField("propertyList", Hashtable.class),
	new ObjectStreamField("propertyListString", String.class),
	new ObjectStreamField("canonicalName", String.class),
	new ObjectStreamField("pattern", Boolean.TYPE),
	new ObjectStreamField("propertyPattern", Boolean.TYPE)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields = { };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    private static final ObjectStreamField[] serialPersistentFields;
    private static boolean compat = false;  
    static {
	try {
	    PrivilegedAction act = new GetPropertyAction("jmx.serial.form");
	    String form = (String) AccessController.doPrivileged(act);
	    compat = (form != null && form.equals("1.0"));
	} catch (Exception e) {
	    // OK: exception means no compat with 1.0, too bad
	}
	if (compat) {
	    serialPersistentFields = oldSerialPersistentFields;
	    serialVersionUID = oldSerialVersionUID;
	} else {
	    serialPersistentFields = newSerialPersistentFields;
	    serialVersionUID = newSerialVersionUID;
	}
    }

    //
    // Serialization compatibility stuff <==============================

    // Class private fields ----------------------------------->

    /**
     * a shared empty array for empty property lists
     */
    static final private Property[] _Empty_property_array = new Property[0];

    /**
     * a shared empty hashtable for empty property lists
     */
    static final private Hashtable _EmptyPropertyList = new Hashtable(1);

    
    // Class private fields <==============================

    // Instance private fields ----------------------------------->

    /**
     * a String containing the canonical name
     */
    private transient String _canonicalName;

    
    /**
     * An array of properties in the same seq order as time creation
     */
    private transient Property[] _kp_array;

    /**
     * An array of properties in the same seq order as canonical order
     */
    private transient Property[] _ca_array;


    /**
     * The length of the domain part of built objectname
     */
    private transient int _domain_length = 0;


    /**
     * The propertyList of built object name. Initialized lazily.
     * Table that contains all the pairs (key,value) for this ObjectName.
     */
    private transient Hashtable _propertyList;

    /**
     * boolean that declares if this ObjectName domain part is a pattern
     */
    private transient boolean _domain_pattern = false;

    /**
     * boolean that declares if this ObjectName contains a pattern on the 
     * key property list
     */
    private transient boolean _property_pattern = false;

    // Instance private fields <=======================================

    // Private fields <========================================


    //  Private methods ----------------------------------------> 

    // Category : Instance construction -------------------------> 

    /**
     * Initializes this {@link ObjectName} from the given string
     * representation.
     *
     * @param name A string representation of the {@link ObjectName}
     *
     * @exception MalformedObjectNameException The string passed as a
     * parameter does not have the right format.
     * @exception NullPointerException The <code>name</code> parameter
     * is null.
     */
    private void construct(String name)
	throws MalformedObjectNameException, NullPointerException {

	// The name cannot be null
	if (name == null)
	    throw new NullPointerException("name cannot be null");

	// Test if the name is empty
	if (name.length() == 0) {
            // this is equivalent to the whole word query object name.
            _canonicalName = "*:*";
            _kp_array = _Empty_property_array;
            _ca_array = _Empty_property_array;
            _domain_length = 1;
            _propertyList = null;
            _domain_pattern = true;
            _property_pattern = true;
            return;
        }

        // initialize parsing of the string
        char[] name_chars = name.toCharArray();
        int len = name_chars.length;
        char[] canonical_chars = new char[len]; // canonical form will be same
                                                // length at most
        int cname_index = 0;
	int index = 0;
        char c, c1;

        // parses domain part
    domain_parsing:
        while (index < len) {
            switch (c = name_chars[index]) {
                case ':' :
                    _domain_length = index++;
                    break domain_parsing;
                case '=' :
                    int i = ++index;
                    while ((i < len) && (name_chars[i++] != ':'))
                    if (i == len)
			throw new MalformedObjectNameException(
					   "Domain part must be specified");
		    break;
                case '\n' :
                    throw new MalformedObjectNameException(
			      "Invalid character '\\n' in domain name");
                case '*' :
                case '?' :
                    _domain_pattern = true;
                default :
                    index++;
            }
        }

        // check for non-empty properties
        if (index == len)
	    throw new MalformedObjectNameException(
					 "Key properties cannot be empty");

        // we have got the domain part, begins building of _canonicalName
        System.arraycopy(name_chars, 0, canonical_chars, 0, _domain_length);
        canonical_chars[_domain_length] = ':';
        cname_index = _domain_length + 1;

        // parses property list
        Property prop;
        HashMap keys_map = new HashMap();
        String[] keys;
        String key_name;
        boolean quoted_value;
        int property_index = 0;
        int in_index;
        int key_index, key_length, value_index, value_length;

        keys = new String[10];
        _kp_array = new Property[10];
        _property_pattern = false;

        while (index < len) {
            c = name_chars[index];

            // case of pattern properties
            if (c == '*') {
                if (_property_pattern)
		    throw new MalformedObjectNameException(
			      "Cannot have several '*' characters in pattern " +
			      "properties");
                else {
                    _property_pattern = true;
                    if ((++index < len ) && (name_chars[index] != ','))
			throw new MalformedObjectNameException(
			          "Invalid character found after '*': end of " +
				  "name or ',' expected");
                    else if (index == len) {
                        if (property_index == 0) {
                            // empty properties case
                            _kp_array = _Empty_property_array;
                            _ca_array = _Empty_property_array;
                            _propertyList = _EmptyPropertyList;
                        }
                        break;
                    }
                    else {
                        // correct pattern spec in props, continue
                        index++;
                        continue;
                    }
                }
            }

            // standard property case, key part
            in_index = index;
            key_index = in_index;
            while ((in_index < len) && ((c1 = name_chars[in_index++]) != '='))
                switch (c1) {
                    // '=' considered to introduce value part
                    case  '*' :
                    case  '?' :
                    case  ',' :
                    case  ':' :
                    case  '\n' :
			final String ichar = ((c1=='\n')?"\\n":""+c1);
                        throw new MalformedObjectNameException(
				  "Invalid character '" + ichar +
				  "' in key part of property");
                    default: ;
                }
            if (in_index == len)
		throw new MalformedObjectNameException(
					     "Unterminated key property part");
            if (in_index == index)
		throw new MalformedObjectNameException("Invalid key (empty)");
            value_index = in_index; // in_index pointing after '=' char
            key_length = value_index - key_index - 1; // found end of key

            // standard property case, value part
            if (name_chars[in_index] == '\"') {
                quoted_value = true;
                // the case of quoted value part
            quoted_value_parsing:
                while ((++in_index < len) &&
		       ((c1 = name_chars[in_index]) != '\"')) {
                    // the case of an escaped character
                    if (c1 == '\\') {
                        if (++in_index == len)
			    throw new MalformedObjectNameException(
					       "Unterminated quoted value");
                        switch (c1 = name_chars[in_index]) {
                            case '\\' :
                            case '\"' :
                            case '?' :
                            case '*' :
                            case 'n' :
                                break; // valid character
                            default :
                                throw new MalformedObjectNameException(
					  "Invalid escape sequence '\\" +
					  c1 + "' in quoted value");
                        }
		    } else if (c1 == '\n') {
			throw new MalformedObjectNameException(
						     "Newline in quoted value");
		    } else {
                        switch (c1) {
                            case '?' :
                            case '*' :
                                throw new MalformedObjectNameException(
				      "Invalid unescaped reserved character '" +
				      c1 + "' in quoted value");
                            default:
                                break;
                        }
                    }
		}
                if (in_index == len)
		    throw new MalformedObjectNameException(
						 "Unterminated quoted value");
                else value_length = ++in_index - value_index;
            }
            else {
                // the case of standard value part
                quoted_value = false;
                while ((in_index < len) && ((c1 = name_chars[in_index]) != ','))
                switch (c1) {
                    // ',' considered to be the value separator
                    case '*' :
                    case '?' :
                    case '=' :
                    case ':' :
                    case '"' :
                    case '\n' :
			final String ichar = ((c1=='\n')?"\\n":""+c1);
                        throw new MalformedObjectNameException(
						 "Invalid character '" + c1 +
						 "' in value part of property");
                    default : in_index++;
                }
                value_length = in_index - value_index;
            }

            // Parsed property, checks the end of name
            if (in_index == len - 1) {
                if (quoted_value)
		    throw new MalformedObjectNameException(
					     "Invalid ending character `" +
					     name_chars[in_index] + "'");
                else throw new MalformedObjectNameException(
						  "Invalid ending comma");
            }
            else in_index++;

            // we got the key and value part, prepare a property for this
            prop = new Property(key_index, key_length, value_length);
            key_name = name.substring(key_index, key_index + key_length);

            if (property_index == keys.length) {
                String[] tmp_string_array = new String[property_index + 10];
                System.arraycopy(keys, 0, tmp_string_array, 0, property_index);
                keys = tmp_string_array;
            }
            keys[property_index] = key_name;

            addProperty(prop, property_index, keys_map, key_name);
            property_index++;
            index = in_index;
        }

        // computes and set canonical name
        setCanonicalName(name_chars, canonical_chars, keys,
			 keys_map, cname_index, property_index);
    }

    /**
     * Construct an ObjectName from a domain and a Hashtable.
     *
     * @param domain Domain of the ObjectName.
     * @param props  Hashtable containing couples <i>key</i> -> <i>value</i>.
     *
     * @exception MalformedObjectNameException The <code>domain</code>
     * contains an illegal character, or one of the keys or values in
     * <code>table</code> contains an illegal character, or one of the
     * values in <code>table</code> does not follow the rules for quoting.
     * @exception NullPointerException One of the parameters is null.
     */
    private void construct(String domain, Hashtable props)
	throws MalformedObjectNameException, NullPointerException {

	// The domain cannot be null
	if (domain == null)
	    throw new NullPointerException("domain cannot be null");

	// The key property list cannot be null
	if (props == null)
	    throw new NullPointerException("key property list cannot be null");

	// The key property list cannot be empty
	if (props.isEmpty())
	    throw new MalformedObjectNameException(
					 "key property list cannot be empty");

        // checks domain validity
	if (!isDomain(domain))
            throw new MalformedObjectNameException("Invalid domain: " + domain);

        // init canonicalname
	final StringBuffer sb = new StringBuffer();
        sb.append(domain).append(':');
        _domain_length = domain.length();

        // allocates the property array
        int nb_props = props.size();
        _kp_array = new Property[nb_props];

        String[] keys = new String[nb_props];
	final Enumeration e = props.keys();
        final HashMap keys_map = new HashMap();
        Property prop;
        int key_index;
	for (int i = 0; e.hasMoreElements(); i++ ) {
            if (i > 0) sb.append(",");
	    String key = "";
	    try {
		key = (String)e.nextElement();
	    } catch (Exception x) {
		throw new MalformedObjectNameException("Invalid key `" +
						       key + "'");
	    }
	    String value = "";
	    try {
		value = (String)props.get(key);
	    } catch (Exception x) {
		throw new MalformedObjectNameException("Invalid value `" +
						       value + "'");
	    }
	    key_index = sb.length();
            checkKey(key);
            sb.append(key);
            keys[i] = key;
            sb.append("=");
	    checkValue(value);
            sb.append(value);
            prop = new Property(key_index, key.length(), value.length());
	    addProperty(prop, i, keys_map, key);
	}

        // initialise canonical name and data structure
        int len = sb.length();
        char[] initial_chars = new char[len];
        sb.getChars(0, len, initial_chars, 0);
        char[] canonical_chars = new char[len];
        System.arraycopy(initial_chars, 0, canonical_chars, 0,
			 _domain_length + 1);
        setCanonicalName(initial_chars, canonical_chars, keys, keys_map,
			 _domain_length + 1, _kp_array.length);
    }
    // Category : Instance construction <==============================

    // Category : Internal utilities ------------------------------>

    /**
     * Add passed property to the list at the given index
     * for the passed key name
     */
    private void addProperty(Property prop, int index,
			     HashMap keys_map, String key_name)
	throws MalformedObjectNameException {

        if (keys_map.containsKey(key_name)) throw new
                MalformedObjectNameException("key `" + 
					 key_name +"' already defined");
            
        // if no more space for property arrays, have to increase it 
        if (index == _kp_array.length) {
            Property[] tmp_prop_array = new Property[index + 10];
            System.arraycopy(_kp_array, 0, tmp_prop_array, 0, index);
            _kp_array = tmp_prop_array;
        }
        _kp_array[index] = prop;
        keys_map.put(key_name, prop);
    }
        
    /**
     * Sets the canonical name of receiver from input 'specified_chars'
     * array, by filling 'canonical_chars' array with found 'nb-props'
     * properties starting at position 'prop_index'.
     */
    private void setCanonicalName(char[] specified_chars,
                                  char[] canonical_chars,
                                  String[] keys, HashMap keys_map,
                                  int prop_index, int nb_props) {

        // Sort the list of found properties
        if (_kp_array != _Empty_property_array) {
            String[] tmp_keys = new String[nb_props];
            Property[] tmp_props = new Property[nb_props];

            System.arraycopy(keys, 0, tmp_keys, 0, nb_props);
            Arrays.sort(tmp_keys);
            keys = tmp_keys;
            System.arraycopy(_kp_array, 0, tmp_props, 0 , nb_props);
            _kp_array = tmp_props;
            _ca_array = new Property[nb_props];

            // now assigns _ca_array to the sorted list of keys
            // (there cannot be two identical keys in an objectname.
            for (int i = 0; i < nb_props; i++)
                _ca_array[i] = (Property) keys_map.get(keys[i]);

            // now we build the canonical name and set begin indexes of
            // properties to reflect canonical form
            int last_index = nb_props - 1;
            int prop_len; 
            Property prop;
            for (int i = 0; i <= last_index; i++) {
                prop = _ca_array[i];
		// length of prop including '=' char
                prop_len = prop._key_length + prop._value_length + 1;
                System.arraycopy(specified_chars, prop._key_index,
				 canonical_chars, prop_index, prop_len);
                prop.setKeyIndex(prop_index);
                prop_index += prop_len;
                if (i != last_index) {
                    canonical_chars[prop_index] = ',';
                    prop_index++;
                }
            }
        }
        
        // terminate canonicalname with '*' in case of pattern
        if (_property_pattern) {
            if (_kp_array != _Empty_property_array)
		canonical_chars[prop_index++] = ',';
            canonical_chars[prop_index++] = '*';
        }

        // we now build the canonicalname string
        _canonicalName = (new String(canonical_chars, 0, prop_index)).intern();
    }

    /**
     * Parse a key.
     * <pre>final int endKey=parseKey(s,startKey);</pre>
     * <p>key starts at startKey (included), and ends at endKey (excluded).
     * If (startKey == endKey), then the key is empty.
     *
     * @param s The char array of the original string.
     * @param startKey index at which to begin parsing.
     * @return The index following the last character of the key.
     **/
    private final static int parseKey(final char[] s, final int startKey) 
	throws MalformedObjectNameException {
	int next   = startKey;
	int endKey = startKey;
	final int len = s.length;
	while (next < len) {
	    final char k = s[next++];
	    switch (k) {
	    case '*':
	    case '?':
	    case ',':
	    case ':':
	    case '\n':
		final String ichar = ((k=='\n')?"\\n":""+k);
		throw new 
		    MalformedObjectNameException("Invalid character in key: `"
						 + ichar + "'");
	    case '=':
		// we got the key.
		endKey = next-1;
		break;
	    default:
		if (next < len) continue;
		else endKey=next;
	    }
	    break;
	}
	return endKey;
    }

    /**
     * Parse a value.
     * <pre>final int endVal=parseValue(s,startVal);</pre>
     * <p>value starts at startVal (included), and ends at endVal (excluded).
     * If (startVal == endVal), then the key is empty.
     *
     * @param s The char array of the original string.
     * @param startValue index at which to begin parsing.
     * @return The index following the last character of the value.
     **/
    private final static int parseValue(final char[] s, final int startValue) 
	throws MalformedObjectNameException {

	int next   = startValue;
	int endValue = startValue;

	final int len = s.length;
	final char q=s[startValue];

	if (q == '"') {
	    // quoted value
	    if (++next == len) throw new 
		MalformedObjectNameException("Invalid quote");
	    while (next < len) {
		char last = s[next];
                if (last == '\\') {
                    if (++next == len) throw new 
                        MalformedObjectNameException(
			   "Invalid unterminated quoted character sequence");
                    last = s[next];
                    switch (last) {
                        case '\\' :
                        case '?' :
                        case '*' :
			case 'n' :
                            break;
                        case '\"' :
			    // We have an escaped quote. If this escaped
			    // quote is the last character, it does not
			    // qualify as a valid termination quote.
			    //
			    if (next+1 == len) throw new
				MalformedObjectNameException(
						 "Missing termination quote");
			    break;
                        default:
                            throw new 
                                MalformedObjectNameException(
				"Invalid quoted character sequence '\\" + 
				last + "'");
                    }
                } else if (last == '\n') {
		    throw new MalformedObjectNameException(
						 "Newline in quoted value");
                } else if (last == '\"') {
                    next++;
                    break;
                } else {
                    switch (last) {
                        case '?' :
                        case '*' :
                            throw new MalformedObjectNameException(
				      "Invalid unescaped reserved character '" +
				      last + "' in quoted value");
                        default:
                            break;
                    }
                }
                next++;

		// Check that last character is a termination quote.
		// We have already handled the case were the last
		// character is an escaped quote earlier.
		//
		if ((next >= len) && (last != '\"')) throw new 
		    MalformedObjectNameException("Missing termination quote");
	    }
	    endValue = next;
	    if (next < len) {
		if (s[next++] != ',') throw new 
		    MalformedObjectNameException("Invalid quote");
	    }
	}
        else {
	    // Non quoted value.
	    while (next < len) {
		final char v=s[next++];
		switch(v) {
                    case '*':
                    case '?':
                    case '=':
                    case ':':
                    case '\n' :
			final String ichar = ((v=='\n')?"\\n":""+v);
                        throw new 
			MalformedObjectNameException("Invalid character `" + 
						     ichar + "' in value");
		    case ',':
		        endValue = next-1;
		    break;
		default:
		    if (next < len) continue;
		    else endValue=next;
		}
		break;
	    }
	}
	return endValue;
    }

    /**
     * Check if the value given in parameter in the first constructor is a 
     * valid value
     */
    private  String checkValue(String val) 
	throws MalformedObjectNameException {

	if (val == null) throw new
	    MalformedObjectNameException("Invalid value (null)");

	final int len  = val.length();
	if (len == 0) throw new
	    MalformedObjectNameException("Invalid value (empty)");

	final char[] s = val.toCharArray();
	final int endValue = parseValue(s,0);
	if (endValue < len) throw new 
	    MalformedObjectNameException("Invalid character in value: `" + 
					 s[endValue] + "'");
	return val;
    }
   
    /**
     * Check if the key given in parameter in the first constructor is a 
     * valid key.
     */
    private String checkKey(String key) 
	throws MalformedObjectNameException {

	if (key == null) throw new
	    MalformedObjectNameException("Invalid key (null)");
	
	final int len = key.length();
	if (len == 0) throw new
	    MalformedObjectNameException("Invalid key (empty)");
	final char[] k=key.toCharArray();
	final int endKey = parseKey(k,0);
	if (endKey < len) throw new 
	    MalformedObjectNameException("Invalid character in value: `" + 
					 k[endKey] + "'");
	return key;
    }

    /*
     * Tests whether string s is matched by pattern p.
     * Supports "?", "*" each of which may be escaped with "\";
     * Not yet supported: internationalization; "\" inside brackets.<P>
     * Wildcard matching routine by Karl Heuer.  Public Domain.<P>
     */  
    private static boolean wildmatch(char[] s, char[] p, int si, int pi) {
        char c;
        final int slen = s.length;
        final int plen = p.length;

        while (pi < plen) {            // While still string
            c = p[pi++];
            if (c == '?') {
                if (++si > slen) return false;
            } else if (c == '*') {        // Wildcard
                if (pi >= plen) return true;
                do {
                    if (wildmatch(s,p,si,pi)) return true;
                } while (++si < slen);
                return false;
            } else {
                if (si >= slen || c != s[si++]) return false;
            }
        }
        return (si == slen);
    }

    // Category : Internal utilities <==============================

    // Category : Internal accessors ------------------------------>

    /**
     * Check if domain is a valid domain
     */
    private boolean isDomain(String domain) {
	if (domain == null) return true;
	final char[] d=domain.toCharArray();
	final int len = d.length;
	int next = 0;
	while (next < len) {
	    final char c = d[next++];
	    switch (c) {
                case ':' :
                case '\n' :
                    return false;
                case '*' :
                case '?' :
                    _domain_pattern = true;
                default:
                    continue;
	    }
	}
	return true;
    }

    // Category : Internal accessors <==============================
    
    // Category : Serialization ----------------------------------->

    /**
     * Deserializes an {@link ObjectName} from an {@link ObjectInputStream}.
     * @serialData <ul>
     *               <li>In the current serial form (value of property
     *                   <code>jmx.serial.form</code> differs from
     *                   <code>1.0</code>): the string
     *                   &quot;&lt;domain&gt;:&lt;properties&gt;&lt;wild&gt;&quot;,
     *                   where: <ul>
     *                            <li>&lt;domain&gt; represents the domain part
     *                                of the {@link ObjectName}</li>
     *                            <li>&lt;properties&gt; represents the list of
     *                                properties, as returned by 
     *                                {@link #getKeyPropertyListString}
     *                            <li>&lt;wild&gt; is empty if not
     *                                <code>isPropertyPattern</code>, or
     *                                is the character "<code>*</code>" if
     *                                <code>isPropertyPattern</code>
     *                                and &lt;properties&gt; is empty, or
     *                                is "<code>,*</code>" if
     *                                <code>isPropertyPattern</code> and
     *                                &lt;properties&gt; is not empty.
     *                            </li>
     *                          </ul>
     *                   The intent is that this string could be supplied
     *                   to the {@link #ObjectName(String)} constructor to
     *                   produce an equivalent {@link ObjectName}.
     *               </li>
     *               <li>In the old serial form (value of property
     *                   <code>jmx.serial.form</code> is
     *                   <code>1.0</code>): &lt;domain&gt; &lt;propertyList&gt;
     *                   &lt;propertyListString&gt; &lt;canonicalName&gt;
     *                   &lt;pattern&gt; &lt;propertyPattern&gt;,
     *                   where: <ul>
     *                            <li>&lt;domain&gt; represents the domain part
     *                                of the {@link ObjectName}</li>
     *                            <li>&lt;propertyList&gt; is the 
     *                                {@link Hashtable} that contains all the
     *                                pairs (key,value) for this
     *                                {@link ObjectName}</li>
     *                            <li>&lt;propertyListString&gt; is the
     *                                {@link String} representation of the
     *                                list of properties in any order (not
     *                                mandatorily a canonical representation)
     *                                </li>
     *                            <li>&lt;canonicalName&gt; is the
     *                                {@link String} containing this
     *                                {@link ObjectName}'s canonical name</li>
     *                            <li>&lt;pattern&gt; is a boolean which is
     *                                <code>true</code> if this
     *                                {@link ObjectName} contains a pattern</li>
     *                            <li>&lt;propertyPattern&gt; is a boolean which
     *                                is <code>true</code> if this
     *                                {@link ObjectName} contains a pattern in
     *                                the list of properties</li>
     *                          </ul>
     *               </li>
     *             </ul>
     */
    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
	if (compat) {
	    // Read an object serialized in the old serial form
	    //
	    in.defaultReadObject();
	} else {
	    // Read an object serialized in the new serial form
	    //
	    in.defaultReadObject();
	    String s = (String)in.readObject();
	    try {
		construct(s);
	    } catch (NullPointerException e) {
		throw new InvalidObjectException(e.toString());
	    } catch (MalformedObjectNameException e) {
		throw new InvalidObjectException(e.toString());
	    }
	}
    }


    /**
     * Serializes an {@link ObjectName} to an {@link ObjectOutputStream}.
     * @serialData <ul>
     *               <li>In the current serial form (value of property
     *                   <code>jmx.serial.form</code> differs from
     *                   <code>1.0</code>): the string
     *                   &quot;&lt;domain&gt;:&lt;properties&gt;&lt;wild&gt;&quot;,
     *                   where: <ul>
     *                            <li>&lt;domain&gt; represents the domain part
     *                                of the {@link ObjectName}</li>
     *                            <li>&lt;properties&gt; represents the list of
     *                                properties, as returned by 
     *                                {@link #getKeyPropertyListString}
     *                            <li>&lt;wild&gt; is empty if not
     *                                <code>isPropertyPattern</code>, or
     *                                is the character "<code>*</code>" if
     *                                this <code>isPropertyPattern</code>
     *                                and &lt;properties&gt; is empty, or
     *                                is "<code>,*</code>" if
     *                                <code>isPropertyPattern</code> and
     *                                &lt;properties&gt; is not empty.
     *                            </li>
     *                          </ul>
     *                   The intent is that this string could be supplied
     *                   to the {@link #ObjectName(String)} constructor to
     *                   produce an equivalent {@link ObjectName}.
     *               </li>
     *               <li>In the old serial form (value of property
     *                   <code>jmx.serial.form</code> is
     *                   <code>1.0</code>): &lt;domain&gt; &lt;propertyList&gt;
     *                   &lt;propertyListString&gt; &lt;canonicalName&gt;
     *                   &lt;pattern&gt; &lt;propertyPattern&gt;,
     *                   where: <ul>
     *                            <li>&lt;domain&gt; represents the domain part
     *                                of the {@link ObjectName}</li>
     *                            <li>&lt;propertyList&gt; is the 
     *                                {@link Hashtable} that contains all the
     *                                pairs (key,value) for this
     *                                {@link ObjectName}</li>
     *                            <li>&lt;propertyListString&gt; is the
     *                                {@link String} representation of the
     *                                list of properties in any order (not
     *                                mandatorily a canonical representation)
     *                                </li>
     *                            <li>&lt;canonicalName&gt; is the
     *                                {@link String} containing this
     *                                {@link ObjectName}'s canonical name</li>
     *                            <li>&lt;pattern&gt; is a boolean which is
     *                                <code>true</code> if this
     *                                {@link ObjectName} contains a pattern</li>
     *                            <li>&lt;propertyPattern&gt; is a boolean which
     *                                is <code>true</code> if this
     *                                {@link ObjectName} contains a pattern in
     *                                the list of properties</li>
     *                          </ul>
     *               </li>
     *             </ul>
     */
    private void writeObject(ObjectOutputStream out)
	    throws IOException {
      if (compat)
      {
        // Serializes this instance in the old serial form
        //
        ObjectOutputStream.PutField fields = out.putFields();
	fields.put("domain", _canonicalName.substring(0, _domain_length));
	fields.put("propertyList", getKeyPropertyList());
	fields.put("propertyListString", getKeyPropertyListString());
	fields.put("canonicalName", _canonicalName);
	fields.put("pattern", (_domain_pattern || _property_pattern));
	fields.put("propertyPattern", _property_pattern);
	out.writeFields();
      }
      else
      {
        // Serializes this instance in the new serial form
        //
        out.defaultWriteObject();
        out.writeObject(getSerializedNameString());
      }
    }

    //  Category : Serialization <===================================
    
    // Private methods <========================================

    // Public methods ----------------------------------------> 
    
    // Category : ObjectName Construction ------------------------------>

    /**
     * <p>Return an instance of ObjectName that can be used anywhere
     * an object obtained with {@link #ObjectName(String) new
     * ObjectName(name)} can be used.  The returned object may be of
     * a subclass of ObjectName.  Calling this method twice with the
     * same parameters may return the same object or two equal but
     * not identical objects.</p>
     *
     * @param name  A string representation of the object name.
     *
     * @return an ObjectName corresponding to the given String.
     *
     * @exception MalformedObjectNameException The string passed as a 
     * parameter does not have the right format.
     * @exception NullPointerException The <code>name</code> parameter
     * is null.
     *
     * @since.unbundled JMX 1.2
     */
    public static ObjectName getInstance(String name)
	    throws MalformedObjectNameException, NullPointerException {
        return new ObjectName(name);
    }

    /**
     * <p>Return an instance of ObjectName that can be used anywhere
     * an object obtained with {@link #ObjectName(String, String,
     * String) new ObjectName(domain, key, value)} can be used.  The
     * returned object may be of a subclass of ObjectName.  Calling
     * this method twice with the same parameters may return the same
     * object or two equal but not identical objects.</p>
     *
     * @param domain  The domain part of the object name.     
     * @param key  The attribute in the key property of the object name.     
     * @param value The value in the key property of the object name.
     *
     * @return an ObjectName corresponding to the given domain,
     * key, and value.
     *
     * @exception MalformedObjectNameException The
     * <code>domain</code>, <code>key</code>, or <code>value</code>
     * contains an illegal character, or <code>value</code> does not
     * follow the rules for quoting.
     * @exception NullPointerException One of the parameters is null.
     *
     * @since.unbundled JMX 1.2
     */
    public static ObjectName getInstance(String domain, String key,
					 String value) 
	    throws MalformedObjectNameException, NullPointerException {
        return new ObjectName(domain, key, value);
    }

    /**
     * <p>Return an instance of ObjectName that can be used anywhere
     * an object obtained with {@link #ObjectName(String, Hashtable)
     * new ObjectName(domain, table)} can be used.  The returned
     * object may be of a subclass of ObjectName.  Calling this method
     * twice with the same parameters may return the same object or
     * two equal but not identical objects.</p>
     *
     * @param domain  The domain part of the object name.
     * @param table A hash table containing one or more key
     * properties.  The key of each entry in the table is the key of a
     * key property in the object name.  The associated value in the
     * table is the associated value in the object name.
     *
     * @return an ObjectName corresponding to the given domain and
     * key mappings.
     *
     * @exception MalformedObjectNameException The <code>domain</code>
     * contains an illegal character, or one of the keys or values in
     * <code>table</code> contains an illegal character, or one of the
     * values in <code>table</code> does not follow the rules for
     * quoting.
     * @exception NullPointerException One of the parameters is null.
     *
     * @since.unbundled JMX 1.2
     */
    public static ObjectName getInstance(String domain, Hashtable table) 
	throws MalformedObjectNameException, NullPointerException {
        return new ObjectName(domain, table);
    }

    /**
     * <p>Return an instance of ObjectName that can be used anywhere
     * the given object can be used.  The returned object may be of a
     * subclass of ObjectName.  If <code>name</code> is of a subclass
     * of ObjectName, it is not guaranteed that the returned object
     * will be of the same class.</p>
     *
     * <p>The returned value may or may not be identical to
     * <code>name</code>.  Calling this method twice with the same
     * parameters may return the same object or two equal but not
     * identical objects.</p>
     *
     * <p>Since ObjectName is immutable, it is not usually useful to
     * make a copy of an ObjectName.  The principal use of this method
     * is to guard against a malicious caller who might pass an
     * instance of a subclass with surprising behavior to sensitive
     * code.  Such code can call this method to obtain an ObjectName
     * that is known not to have surprising behavior.</p>
     *
     * @param name an instance of the ObjectName class or of a subclass
     * 
     * @return an instance of ObjectName or a subclass that is known to
     * have the same semantics.  If <code>name</code> respects the
     * semantics of ObjectName, then the returned object is equal
     * (though not necessarily identical) to <code>name</code>.
     *
     * @exception NullPointerException The <code>name</code> is null.
     *
     * @since.unbundled JMX 1.2
     */
    public static ObjectName getInstance(ObjectName name)
	    throws NullPointerException {
	if (name.getClass().equals(ObjectName.class))
	    return name;
	try {
	    return new ObjectName(name.getSerializedNameString());
	} catch (MalformedObjectNameException e) {
	    throw new IllegalArgumentException("Unexpected: " + e);
	    // can't happen
	}
    }

    /**
     * Construct an object name from the given string.
     *
     * @param name  A string representation of the object name.
     *
     * @exception MalformedObjectNameException The string passed as a 
     * parameter does not have the right format.
     * @exception NullPointerException The <code>name</code> parameter
     * is null.
     */
    public ObjectName(String name)
	throws MalformedObjectNameException, NullPointerException {
	construct(name);
    }

    /**
     * Construct an object name with exactly one key property.
     *
     * @param domain  The domain part of the object name.     
     * @param key  The attribute in the key property of the object name.     
     * @param value The value in the key property of the object name.
     *
     * @exception MalformedObjectNameException The
     * <code>domain</code>, <code>key</code>, or <code>value</code>
     * contains an illegal character, or <code>value</code> does not
     * follow the rules for quoting.
     * @exception NullPointerException One of the parameters is null.
     */
    public ObjectName(String domain, String key, String value)
	throws MalformedObjectNameException, NullPointerException {
	// If key or value are null a NullPointerException
	// will be thrown by the put method in Hashtable.
	//
	Hashtable table = new Hashtable(1);
	table.put(key, value);
	construct(domain, table);
    }

    /**
     * Construct an object name with several key properties from a Hashtable.
     *
     * @param domain  The domain part of the object name.
     * @param table A hash table containing one or more key
     * properties.  The key of each entry in the table is the key of a
     * key property in the object name.  The associated value in the
     * table is the associated value in the object name.
     *
     * @exception MalformedObjectNameException The <code>domain</code>
     * contains an illegal character, or one of the keys or values in
     * <code>table</code> contains an illegal character, or one of the
     * values in <code>table</code> does not follow the rules for
     * quoting.
     * @exception NullPointerException One of the parameters is null.
     */
    public ObjectName(String domain, Hashtable table)
	throws MalformedObjectNameException, NullPointerException {
        construct(domain, table);
    }

    // Category : ObjectName Construction <==============================


    // Category : Getter methods ------------------------------>

    /**
     * Checks whether the object name is a pattern.  An object name is
     * a pattern if its domain contains a wildcard or if the object
     * name is a property pattern.
     *
     * @return  True if the name is a pattern, otherwise false.
     */
    public boolean isPattern() {    
        return (_domain_pattern || _property_pattern);
    }
 
    /**
     * Checks whether the object name is a pattern on the domain part.
     *
     * @return  True if the name is a domain pattern, otherwise false.
     *
     * @since.unbundled JMX 1.2
     */
    public boolean isDomainPattern() {
        return _domain_pattern;
    }

    /**
     * Checks whether the object name is a pattern on the key properties.
     *
     * @return  True if the name is a pattern, otherwise false.
     */
    public boolean isPropertyPattern() {    
        return _property_pattern;
    }

    /**
     * <p>Returns the canonical form of the name; that is, a string
     * representation where the properties are sorted in lexical
     * order.</p>
     *
     * <p>More precisely, the canonical form of the name is a String
     * consisting of the <em>domain part</em>, a colon
     * (<code>:</code>), the <em>canonical key property list</em>, and
     * a <em>pattern indication</em>.</p>
     *
     * <p>The <em>canonical key property list</em> is the same string
     * as described for {@link #getCanonicalKeyPropertyListString()}.</p>
     *
     * <p>The <em>pattern indication</em> is:
     * <ul>
     * <li>empty for an ObjectName
     * that is not a property pattern;
     * <li>an asterisk for an ObjectName
     * that is a property pattern with no keys; or
     * <li>a comma and an
     * asterisk (<code>,*</code>) for an ObjectName that is a property
     * pattern with at least one key.
     * </ul></p>
     *
     * @return The canonical form of the name.
     */
    public String getCanonicalName()  {    
        return _canonicalName;
    } 

    /**
     * Returns the domain part.
     *
     * @return the domain.
     */    
    public String getDomain()  {       
        return _canonicalName.substring(0, _domain_length);
    } 
   
    /**
    * Obtains the value associated with a key in a key property.
    *
    * @param property The property whose value is to be obtained.
    *
    * @return The value of the property, or null if there is no such
    * property in this ObjectName.
    *
    * @exception NullPointerException If <code>property</code> is null.
    */
    public String getKeyProperty(String property) throws NullPointerException {
        return (String) _getKeyPropertyList().get(property);
    }

    /**
     * <p>Returns the key properties as a Hashtable.  The returned
     * value is a Hashtable in which each key is a key in the
     * ObjectName's key property list and each value is the associated
     * value.</p>
     *
     * <p>The returned value must not be modidied.</p>
     *
     * @return The table of key properties.
     */
    private final Hashtable _getKeyPropertyList()  {
        synchronized (this) {
            if (_propertyList == null) {
                // build (lazy eval) the property list from the canonical
		// properties array
                _propertyList = new Hashtable();
                int len = _ca_array.length;
                Property prop;
                for (int i = len - 1; i >= 0; i--) {
                    prop = _ca_array[i];
                    _propertyList.put(prop.getKeyString(_canonicalName),
                                      prop.getValueString(_canonicalName));
                }
            }
        }
        return _propertyList;
    }     

    /**
     * <p>Returns the key properties as a Hashtable.  The returned
     * value is a Hashtable in which each key is a key in the
     * ObjectName's key property list and each value is the associated
     * value.</p>
     *
     * <p>The returned value may be unmodifiable.  If it is
     * modifiable, changing it has no effect on this ObjectName.</p>
     *
     * @return The table of key properties.
     */
    public Hashtable getKeyPropertyList()  {
        return (Hashtable)_getKeyPropertyList().clone();
    }     

    /**
     * <p>Returns a string representation of the list of key
     * properties specified at creation time.  If this ObjectName was
     * constructed with the constructor {@link #ObjectName(String)},
     * the key properties in the returned String will be in the same
     * order as in the argument to the constructor.</p>
     *
     * @return The key property list string.  This string is
     * independent of whether the ObjectName is a pattern.
     */
    public String getKeyPropertyListString()  { 
        // BEWARE : we rebuild the propertyliststring at each call !!
        if (_kp_array.length == 0) return "";

        // the size of the string is the canonical one minus domain 
	// part and pattern part
        final int total_size = _canonicalName.length() - _domain_length - 1
	    - (_property_pattern?2:0);

        final char[] dest_chars = new char[total_size];
        final char[] value = _canonicalName.toCharArray();
	writeKeyPropertyListString(value,dest_chars,0);
        return new String(dest_chars);
    }

    /**
     * <p>Returns the serialized string of the ObjectName.
     * properties specified at creation time.  If this ObjectName was
     * constructed with the constructor {@link #ObjectName(String)},
     * the key properties in the returned String will be in the same
     * order as in the argument to the constructor.</p>
     *
     * @return The key property list string.  This string is
     * independent of whether the ObjectName is a pattern.
     */
    private String getSerializedNameString()  { 

        // the size of the string is the canonical one 
        final int total_size = _canonicalName.length();
        final char[] dest_chars = new char[total_size];
	final char[] value = _canonicalName.toCharArray();
	final int offset = _domain_length+1;

	// copy "domain:" into dest_chars
	//
	System.arraycopy(value, 0, dest_chars, 0, offset);

	// Add property list string
	final int end = writeKeyPropertyListString(value,dest_chars,offset);

	// Add ",*" if necessary
        if (_property_pattern) {
	    if (end == offset)  {
		// Property list string is empty.
		dest_chars[end] = '*';
	    } else {
		// Property list string is not empty.
		dest_chars[end]   = ',';
		dest_chars[end+1] = '*';
	    }
	}

        return new String(dest_chars);
    }

    /**
     * <p>Write a string representation of the list of key
     * properties specified at creation time in the given array, starting
     * at the specified offset.  If this ObjectName was
     * constructed with the constructor {@link #ObjectName(String)},
     * the key properties in the returned String will be in the same
     * order as in the argument to the constructor.</p>
     *
     * @return offset + #of chars written
     */
    private int writeKeyPropertyListString(char[] canonicalChars, 
					   char[] data, int offset)  { 
        if (_kp_array.length == 0) return offset;

        final char[] dest_chars = data;
        final char[] value = _canonicalName.toCharArray();

        int index = offset;
        final int len = _kp_array.length;
	final int last = len - 1;
        for (int i = 0; i < len; i++) {
            final Property prop = _kp_array[i];
            final int prop_len = prop._key_length + prop._value_length + 1;
            System.arraycopy(value, prop._key_index, dest_chars, index, 
			     prop_len);
            index += prop_len;
            if (i < last ) dest_chars[index++] = ',';
        }
	return index;
    }



    /**
     * Returns a string representation of the list of key properties,
     * in which the key properties are sorted in lexical order. This
     * is used in lexicographic comparisons performed in order to
     * select MBeans based on their key property list.  Lexical order
     * is the order implied by {@link String#compareTo(String)
     * String.compareTo(String)}.
     *
     * @return The canonical key property list string.  This string is
     * independent of whether the ObjectName is a pattern.
     */
    public String getCanonicalKeyPropertyListString()  {
        if (_ca_array.length == 0) return "";

        int len = _canonicalName.length();
        if (_property_pattern) len -= 2;
        return _canonicalName.substring(_domain_length +1, len);
    }
    // Category : Getter methods <===================================
    
    // Category : Utilities ---------------------------------------->

    /**
     * <p>Returns a string representation of the object name.  The
     * format of this string is not specified, but users can expect
     * that two ObjectNames return the same string if and only if they
     * are equal.</p>
     *
     * @return a string representation of this object name.
     */
    public String toString()  {
	return getSerializedNameString();
    }

    /**
     * Compares the current object name with another object name.  Two
     * ObjectName instances are equal if and only if their canonical
     * forms are equal.  The canonical form is the string described
     * for {@link #getCanonicalName()}.
     *
     * @param object  The object name that the current object name is to be 
     *        compared with.
     *
     * @return True if <code>object</code> is an ObjectName whose
     * canonical form is equal to that of this ObjectName.
     */
    public boolean equals(Object object)  {

        // same object case
        if (this == object) return true;

        // object is not an object name case
        if (!(object instanceof ObjectName)) return false;

        // equality when canonical names are the same
        // (because usage of intern())
        ObjectName on = (ObjectName) object;
        String on_string = on._canonicalName;
        if (_canonicalName == on_string) return true;

        // Because we are sharing canonical form between object names,
        // we have finished the comparison at this stage ==> unequal
        return false;
   }

    /**
     * Returns a hash code for this object name.
     *
     */   
    public int hashCode() {
        return _canonicalName.hashCode();
    }

    /**
     * <p>Returns a quoted form of the given String, suitable for
     * inclusion in an ObjectName.  The returned value can be used as
     * the value associated with a key in an ObjectName.  The String
     * <code>s</code> may contain any character.  Appropriate quoting
     * ensures that the returned value is legal in an ObjectName.</p>
     *
     * <p>The returned value consists of a quote ('"'), a sequence of
     * characters corresponding to the characters of <code>s</code>,
     * and another quote.  Characters in <code>s</code> appear
     * unchanged within the returned value except:</p>
     *
     * <ul>
     * <li>A quote ('"')is replaced by a backslash (\) followed by a quote.
     * <li>A star ('*') is replaced by a backslash (\) followed by a star.
     * <li>A question mark ('?') is replaced by a backslash (\) followed by
     * a question mark.
     * <li>A backslash ('\') is replaced by two backslashes.
     * <li>A newline character (the character '\n' in Java) is replaced
     * by a backslash followed by the character '\n'.
     * </ul>
     *
     * @param s the String to be quoted.
     *
     * @return the quoted String.
     *
     * @exception NullPointerException if <code>s</code> is null.
     *
     * @since.unbundled JMX 1.2
     */
    public static String quote(String s)
	    throws NullPointerException {
	final StringBuffer buf = new StringBuffer("\"");
	final int len = s.length();
	for (int i = 0; i < len; i++) {
	    char c = s.charAt(i);
	    switch (c) {
	    case '\n':
		c = 'n';
		// fall in...
	    case '\\':
	    case '\"':
	    case '*':
	    case '?':
		buf.append('\\');
		break;
	    }
	    buf.append(c);
	}
	buf.append('"');
	return buf.toString();
    }

    /**
     * <p>Returns an unquoted form of the given String.  If
     * <code>q</code> is a String returned by {@link #quote quote(s)},
     * then <code>unquote(q).equals(s)</code>.  If there is no String
     * <code>s</code> for which <code>quote(s).equals(q)</code>, then
     * unquote(q) throws an IllegalArgumentException.</p>
     *
     * <p>These rules imply that there is a one-to-one mapping between
     * quoted and unquoted forms.</p>
     *
     * @param q the String to be unquoted.
     *
     * @return the unquoted String.
     *
     * @exception IllegalArgumentException if <code>q</code> could not
     * have been returned by the {@link #quote} method, for instance
     * if it does not begin and end with a quote (").
     *
     * @exception NullPointerException if <code>q</code> is null.
     *
     * @since.unbundled JMX 1.2
     */
    public static String unquote(String q)
	    throws IllegalArgumentException, NullPointerException {
	final StringBuffer buf = new StringBuffer();
	final int len = q.length();
	if (len < 2 || q.charAt(0) != '"' || q.charAt(len - 1) != '"')
	    throw new IllegalArgumentException("Argument not quoted");
	for (int i = 1; i < len - 1; i++) {
	    char c = q.charAt(i);
	    if (c == '\\') {
		if (i == len - 2)
		    throw new IllegalArgumentException("Trailing backslash");
		c = q.charAt(++i);
		switch (c) {
		case 'n':
		    c = '\n';
		    break;
		case '\\':
		case '\"':
		case '*':
		case '?':
		    break;
                default: 
                  throw new IllegalArgumentException(
				   "Bad character '" + c + "' after backslash");
		}
	    }
            else {
                switch (c) {
                    case '*' :
                    case '?' :
                    case '\"':    
                    case '\n':
                         throw new IllegalArgumentException(
					  "Invalid unescaped character '" + c +
					  "' in the string to unquote");
                    default : ;
                }
            }
	    buf.append(c);
	}
	return buf.toString();
    }

    // Category : Utilities <===================================

    // Category : QueryExp Interface ---------------------------------------->


    /**
     * <p>Test whether this ObjectName, which may be a pattern,
     * matches another ObjectName.  If <code>name</code> is a pattern,
     * the result is false.  If this ObjectName is a pattern, the
     * result is true if and only if <code>name</code> matches the
     * pattern.  If neither this ObjectName nor <code>name</code> is
     * a pattern, the result is true if and only if the two
     * ObjectNames are equal as described for the {@link
     * #equals(Object)} method.</p>
     *
     * @param name The name of the MBean to compare to.
     *
     * @return True if <code>name</code> matches this ObjectName.
     *
     * @exception NullPointerException if <code>name</code> is null.
     *
     * @since.unbundled JMX 1.2
     */
    public boolean apply(ObjectName name) throws NullPointerException {

        if (name == null) throw new NullPointerException();
        
	if (name._domain_pattern || name._property_pattern)
	    return false;

	// No pattern
	if (!_domain_pattern && !_property_pattern)
	    return _canonicalName.equals(name._canonicalName);

	return matchDomains(name) && matchKeys(name);
    }

    private final boolean matchDomains(ObjectName name) {
	if (_domain_pattern) {
	    // wildmatch domains
	    final char[] dom_pattern = getDomain().toCharArray();
	    final char[] dom_string  = name.getDomain().toCharArray();
	    return wildmatch(dom_string,dom_pattern,0,0);
	} 
	return getDomain().equals(name.getDomain());
    }

    private final boolean matchKeys(ObjectName name) {
	if (_property_pattern) {
	    // Every property inside pattern should exist in name
	    final Hashtable  nameProps = name._getKeyPropertyList();
	    final Property[] props=_ca_array;
	    final String     cn=_canonicalName;
	    for (int i= props.length -1; i >= 0 ; i--) {
		
		// find value in given object name for key at current 
		// index in receiver
		
		final Property p = props[i];
		final String   k = p.getKeyString(cn);
		final String   v = (String)nameProps.get(k);

		// did we find a value for this key ?
		if (v == null) return false; 
                    
		// if this property is ok (same key, same value), 
		// go to next
		if (v.equals(p.getValueString(cn))) continue; 
		return false;
	    }
	    return true;
	} 
	final String p1 = name.getCanonicalKeyPropertyListString();
	final String p2 = getCanonicalKeyPropertyListString();
	return (p1.equals(p2));
    }

    /* Method inherited from QueryExp, no implementation needed here
       because ObjectName is not relative to an MBeanServer and does
       not contain a subquery.
    */
    public void setMBeanServer(MBeanServer mbs) { }

    // Category : QueryExp Interface <=========================

    // Public methods <========================================

}
