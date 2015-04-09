/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wfs;

import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_SCHEMA;

import java.io.IOException;

import org.geoserver.inspire.UniqueResourceIdentifier;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.wfs.GetCapabilities;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

public class WFSExtendedCapabilitiesProvider implements
        org.geoserver.wfs.WFSExtendedCapabilitiesProvider {

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[]{DLS_NAMESPACE, DLS_SCHEMA};
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        namespaces.declarePrefix("gml", "http://schemas.opengis.net/gml");
        namespaces
                .declarePrefix("gmd", "http://schemas.opengis.net/iso/19139/20060504/gmd/gmd.xsd");
        namespaces
                .declarePrefix("gco", "http://schemas.opengis.net/iso/19139/20060504/gco/gco.xsd");
        namespaces
                .declarePrefix("srv", "http://schemas.opengis.net/iso/19139/20060504/srv/srv.xsd");
        // IGN : We add another xmlns for inspire_common
        namespaces.declarePrefix("inspire_common", COMMON_NAMESPACE);
        // IGN : We add another xmlns for inspire_dls
        namespaces.declarePrefix("inspire_dls", DLS_NAMESPACE);
    }

    @Override
    public void encode(Translator tx, WFSInfo wfs, GetCapabilitiesRequest request)
            throws IOException {
        String version = GetCapabilities.version(request);
        // can't add to a pre 1.1.0 version
        if ("1.0.0".equals(version)) {
            return;
        }
        String metadataURL = (String) wfs.getMetadata().get(SERVICE_METADATA_URL.key);
        String mediaType = (String) wfs.getMetadata().get(SERVICE_METADATA_TYPE.key);
        String language = (String) wfs.getMetadata().get(LANGUAGE.key);
        UniqueResourceIdentifiers ids = (UniqueResourceIdentifiers) wfs.getMetadata().get(SPATIAL_DATASET_IDENTIFIER_TYPE.key, UniqueResourceIdentifiers.class);
        //Don't create extended capabilities element if mandatory content not present
        if (metadataURL == null) {
            return;
        }
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // IGN : INSPIRE SCENARIO 1
        tx.start("ows:ExtendedCapabilities");
        tx.start("inspire_dls:ExtendedCapabilities");

        // Metadata URL
        tx.start("inspire_common:MetadataUrl",
                atts("xsi:type", "inspire_common:resourceLocatorType"));
        tx.start("inspire_common:URL");
        tx.chars(metadataURL);
        tx.end("inspire_common:URL");
        if (mediaType != null) {
            tx.start("inspire_common:MediaType");
            tx.chars(mediaType);
            tx.end("inspire_common:MediaType");
        }
        tx.end("inspire_common:MetadataUrl");

        // SupportedLanguages
        tx.start("inspire_common:SupportedLanguages",
                atts("xsi:type", "inspire_common:supportedLanguagesType"));
        language = language != null ? language : "eng";
        tx.start("inspire_common:DefaultLanguage");
        tx.start("inspire_common:Language");
        tx.chars(language);
        tx.end("inspire_common:Language");
        tx.end("inspire_common:DefaultLanguage");
        // TODO when more than one language
        // tx.start("inspire_common:SupportedLanguage");
        // tx.start("inspire_common:Language");
        // tx.chars(language);
        // tx.end("inspire_common:Language");
        // tx.end("inspire_common:SupportedLanguage");
        tx.end("inspire_common:SupportedLanguages");

        // ResponseLanguage
        tx.start("inspire_common:ResponseLanguage");
        tx.start("inspire_common:Language");
        tx.chars(language);
        tx.end("inspire_common:Language");
        tx.end("inspire_common:ResponseLanguage");

        // unique spatial dataset identifiers
        for (UniqueResourceIdentifier id : ids) {
            if (id.getMetadataURL() != null) {
                tx.start("inspire_dls:SpatialDataSetIdentifier", atts("metadataURL", id.getMetadataURL()));
            } else {
                tx.start("inspire_dls:SpatialDataSetIdentifier");
            }
            tx.start("inspire_common:Code");
            tx.chars(id.getCode());
            tx.end("inspire_common:Code");
            if (id.getNamespace() != null) {
                tx.start("inspire_common:Namespace");
                tx.chars(id.getNamespace());
                tx.end("inspire_common:Namespace");
            }
            tx.end("inspire_dls:SpatialDataSetIdentifier");
        }
        tx.end("inspire_dls:ExtendedCapabilities");
        tx.end("ows:ExtendedCapabilities");

    }

    Attributes atts(String... atts) {
        AttributesImpl attributes = new AttributesImpl();
        for (int i = 0; i < atts.length; i += 2) {
            attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
        }
        return attributes;
    }

}
