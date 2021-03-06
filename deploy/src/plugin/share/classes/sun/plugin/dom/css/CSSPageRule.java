/*
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package sun.plugin.dom.css;

import org.w3c.dom.DOMException;
import sun.plugin.dom.DOMObject;
import sun.plugin.dom.DOMObjectHelper;
import sun.plugin.dom.DOMObjectFactory;

/**
 *  The <code>CSSPageRule</code> interface represents a @page rule within a 
 * CSS style sheet. The <code>@page</code> rule is used to specify the 
 * dimensions, orientation, margins, etc. of a page box for paged media. 
 * <p>See also the <a href='http://www.w3.org/TR/2000/REC-DOM-Level-2-Style-20001113'>Document Object Model (DOM) Level 2 Style Specification</a>.
 * @since DOM Level 2
 */
public final class CSSPageRule extends CSSRule 
			       implements org.w3c.dom.css.CSSPageRule {

    public CSSPageRule(DOMObject obj, 
		       org.w3c.dom.Document document,
		       org.w3c.dom.Node ownerNode,
		       org.w3c.dom.css.CSSStyleSheet parentStyleSheet, 
		       org.w3c.dom.css.CSSRule parentRule) {
	super(obj, document, ownerNode, parentStyleSheet, parentRule);
    }

    /**
     *  The parsable textual representation of the page selector for the rule. 
     * @exception DOMException
     *   SYNTAX_ERR: Raised if the specified CSS string value has a syntax 
     *   error and is unparsable.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this rule is readonly.
     */
    public String getSelectorText() {
	return DOMObjectHelper.getStringMemberNoEx(obj, CSSConstants.ATTR_SELECTOR_TEXT);
    }

    public void setSelectorText(String selectorText)
                           throws DOMException {
	DOMObjectHelper.setStringMemberNoEx(obj, CSSConstants.ATTR_SELECTOR_TEXT, selectorText);
    }

    /**
     *  The declaration-block of this rule. 
     */
    public org.w3c.dom.css.CSSStyleDeclaration getStyle() {
	Object result = obj.getMember(CSSConstants.ATTR_STYLE);
	if(result != null && result instanceof DOMObject) {
	    Object ret = DOMObjectFactory.createCSSObject((DOMObject)result,
			document, ownerNode, parentStyleSheet, parentRule);
	    if(ret != null && ret instanceof org.w3c.dom.css.CSSStyleDeclaration)
		return (org.w3c.dom.css.CSSStyleDeclaration)ret;
	}
	return null;
    }

}
