/*
 * @(#)LocaleElements_el_GR.java	1.13 03/12/19
 */

/*
 * Portions Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.text.resources;

import java.util.ListResourceBundle;

public class LocaleElements_el_GR extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "NumberPatterns",
                new String[] {
                    "#,##0.###;-#,##0.###", // decimal pattern
                    "#,##0.00 \u00A4;-#,##0.00 \u00A4", // currency pattern
                    "#,##0%" // percent pattern
                }
            },
            { "CurrencySymbols",
                new String[][] {
                   {"EUR", "\u20AC"},
                   {"GRD", "\u03B4\u03C1\u03C7"}
                }
            },
            { "DateTimeElements",
                new String[] {
                    "2", // first day of week
                    "4" // min days in first week
                }
            }
        };
    }
}
