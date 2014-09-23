/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * A link to underlying data represented by resource.
 * 
 * @author Marcus Sen, British Geological Survey
 * TODO: check if id and about needed
 * TODO: where to @uml.property names come from?
 * TODO: check if it is GeoServer style to re-declare methods of super-interface
 * 
 */
public interface DataLinkInfo extends Info {

    /**
     * Identifier.
     */
    String getId();

    /**
     * @uml.property name="about"
     */
    String getAbout();

    /**
     * @uml.property name="about"
     */
    void setAbout(String about);

    /**
     * @uml.property name="type"
     */
    String getType();

    /**
     * @uml.property name="type"
     */
    void setType(String type);

    /**
     * @uml.property name="content"
     */
    String getContent();

    /**
     * @uml.property name="content"
     */
    void setContent(String content);
}
