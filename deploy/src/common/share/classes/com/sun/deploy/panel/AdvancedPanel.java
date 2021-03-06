/*
 * @(#)AdvancedPanel.java	1.5 04/02/12
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.deploy.panel;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import javax.xml.parsers.*;
import org.xml.sax.helpers.*;
import org.xml.sax.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AdvancedPanel extends JPanel{
    JTree tree;
    
    public AdvancedPanel(){
        initComponents();
    }

    public void initComponents(){
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(new Insets(10, 5, 5, 5)));
        
        // Create panel with JTree on it. 
        JScrollPane treePane = new JScrollPane();

        // Add items to the JTree.
        tree = createJTreeFromXML();
        
        treePane.setViewportView(tree);        

        // add topLevelPanel to the update panel.
        add(treePane, BorderLayout.CENTER);
    }

    
    /** 
     * Create JTree and populate it with content from XML document.
     */
    public JTree createJTreeFromXML()
    {
        try
        {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLDocumentHandler xmlHandler = new XMLDocumentHandler();

            saxParser.parse(this.getClass().getClassLoader().getResourceAsStream("com/sun/deploy/panel/settings.xml") , xmlHandler);
            
            return xmlHandler.getJTree();
        }
        catch (FactoryConfigurationError fce)
        {
            fce.printStackTrace();
        }        
        catch (ParserConfigurationException pce)
        {
            pce.printStackTrace();
        }
        catch (SAXException saxe)
        {
            saxe.printStackTrace();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
}
