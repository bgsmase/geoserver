/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wcs;

import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE;

import java.io.IOException;
import java.util.List;
import net.opengis.wcs20.GetCapabilitiesType;
import org.geoserver.catalog.CoverageInfo;

import org.geoserver.inspire.UniqueResourceIdentifier;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.wcs.WCSInfo;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

public class WCSExtendedCapabilitiesProvider extends
        org.geoserver.wcs2_0.response.WCSExtendedCapabilitiesProvider {

    public static final String COMMON_NAMESPACE = "http://inspire.ec.europa.eu/schemas/common/1.0";
    public static final String DLS_NAMESPACE = "http://inspire.ec.europa.eu/schemas/inspire_dls/1.0";

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[]{COMMON_NAMESPACE,
            "http://inspire.ec.europa.eu/schemas/common/1.0/common.xsd", DLS_NAMESPACE,
            "http://inspire.ec.europa.eu/schemas/inspire_dls/1.0/inspire_dls.xsd"};
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

    Attributes atts(String... atts) {
        AttributesImpl attributes = new AttributesImpl();
        for (int i = 0; i < atts.length; i += 2) {
            attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
        }
        return attributes;
    }

    @Override
    public void encodeExtendedOperations(Translator tx, WCSInfo wcs, GetCapabilitiesType request) throws IOException {
        //INSPIRE has nothing to add to operations section
    }

    @Override
    public void encodeExtendedContents(Translator tx, WCSInfo wcs, List<CoverageInfo> coverages, GetCapabilitiesType request) throws IOException {
        // I need to work out how version is decided with acceptversions param
        // A bit different from WFS as WCS versions have different types
        String version = request.getAcceptVersions().getVersion().get(0);

        if (!existRequiredMetadata(wcs)) return;
        // IGN : INSPIRE SCENARIO 1
        tx.start("ows:ExtendedCapabilities");
        tx.start("inspire_dls:ExtendedCapabilities");

        // Metadata URL
        tx.start("inspire_common:MetadataUrl",
                atts("xsi:type", "inspire_common:resourceLocatorType"));
        String metadataURL = (String) wcs.getMetadata().get(SERVICE_METADATA_URL.key);
        tx.start("inspire_common:URL");
        if (metadataURL != null) {
            tx.chars(metadataURL);
        }
        tx.end("inspire_common:URL");
        tx.start("inspire_common:MediaType");
        String type = (String) wcs.getMetadata().get(SERVICE_METADATA_TYPE.key);
        if (type == null) {
            type = "application/vnd.ogc.csw.GetRecordByIdResponse_xml";
        }
        tx.chars(type);
        tx.end("inspire_common:MediaType");
        tx.end("inspire_common:MetadataUrl");

        // SupportedLanguages
        tx.start("inspire_common:SupportedLanguages",
                atts("xsi:type", "inspire_common:supportedLanguagesType"));
        String language = (String) wcs.getMetadata().get(LANGUAGE.key);
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
        UniqueResourceIdentifiers ids = (UniqueResourceIdentifiers) wcs.getMetadata().get(SPATIAL_DATASET_IDENTIFIER_TYPE.key, UniqueResourceIdentifiers.class);
        if (ids != null) {
            for (UniqueResourceIdentifier id : ids) {
                tx.start("inspire_dls:SpatialDataSetIdentifier");
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
        }

        tx.end("inspire_dls:ExtendedCapabilities");
        tx.end("ows:ExtendedCapabilities");

    }

    private boolean existRequiredMetadata(WCSInfo wcs) {
        if (wcs.getMetadata().isEmpty()) return false; 
        UniqueResourceIdentifiers ids = (UniqueResourceIdentifiers)  wcs.getMetadata().get(SPATIAL_DATASET_IDENTIFIER_TYPE.key, UniqueResourceIdentifiers.class);
        return ids != null && !ids.isEmpty();
    }

}
