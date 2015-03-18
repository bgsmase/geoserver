/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_NAMESPACE;
import org.geoserver.inspire.InspireMetadata;
import org.geoserver.wcs.WCSInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.HashMap;

public class WCSExtendedCapabilitiesTest extends GeoServerSystemTestSupport {

    private static final String WCS_1_1_1_GETCAPREQUEST = "wcs?request=GetCapabilities&service=WCS&version=1.1.1";
    private static final String WCS_2_0_0_GETCAPREQUEST = "wcs?request=GetCapabilities&service=WCS&acceptVersions=2.0.0";
    private static final String WCS_2_0_1_GETCAPREQUEST = "wcs?request=GetCapabilities&service=WCS&acceptVersions=2.0.1";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

    @Before
    public void clearMetadata() {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().clear();
        getGeoServer().save(wcs);
    }

    @Test
    public void testNoInspireElementWhenNoMetadata() throws Exception {
        final Document dom = getAsDOM(WCS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertTrue(nodeList.getLength() == 0);
    }

    @Test
    public void testNoInspireElement111() throws Exception {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wcs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wcs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one");
        getGeoServer().save(wcs);

        final Document dom = getAsDOM(WCS_1_1_1_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertTrue(nodeList.getLength() == 0);
    }

    @Test
    public void testExtendedCaps200() throws Exception {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wcs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wcs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one");
        getGeoServer().save(wcs);

        final Document dom = getAsDOM(WCS_2_0_0_GETCAPREQUEST);

        HashMap namespaces = new HashMap();
        namespaces.put("inspire_common", COMMON_NAMESPACE);
        namespaces.put("inspire_dls", DLS_NAMESPACE);
        NamespaceContext nsCtx = new SimpleNamespaceContext(namespaces);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        xpath.setNamespaceContext(nsCtx);

        assertEquals("Existence of ExtendedCapabilities element", "1",
                xpath.evaluate("count(//inspire_dls:ExtendedCapabilities)", dom));

        assertEquals("Expected MetadataURL URL",
                "http://foo.com?bar=baz",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:MetadataUrl/inspire_common:URL", dom));

        assertEquals("Expected default language",
                "fre",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:SupportedLanguages/inspire_common:DefaultLanguage/inspire_common:Language", dom));

        // Can decide to repeat default language in list of supported languages
        // but it isn't required by INSPIRE so won't test for it
        
        assertEquals("Expected response language",
                "fre",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:ResponseLanguage/inspire_common:Language", dom));
        
        assertEquals("Expected response spatial dataset identifier code",
                "one",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_dls:SpatialDataSetIdentifier/inspire_common:Code", dom));
        
        assertEquals("Expected spatial dataset identifier namespace",
                "http://www.geoserver.org/inspire/one",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_dls:SpatialDataSetIdentifier/inspire_common:Namespace", dom));

    }
    
    @Test
    public void testChangeMediaType() throws Exception {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wcs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wcs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one");
        getGeoServer().save(wcs);

        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&acceptVersions=2.0.0");
        // print(dom);
        assertEquals(DLS_NAMESPACE, dom.getDocumentElement().getAttribute("xmlns:inspire_dls"));
        assertMetadataUrlAndMediaType(dom, "http://foo.com?bar=baz", "application/vnd.ogc.csw.GetRecordByIdResponse_xml");

        wcs.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/xml");
        getGeoServer().save(wcs);

        dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&acceptVersions=2.0.0");
        assertEquals(DLS_NAMESPACE, dom.getDocumentElement().getAttribute("xmlns:inspire_dls"));
        assertMetadataUrlAndMediaType(dom, "http://foo.com?bar=baz", "application/xml");
    }

    void assertMetadataUrlAndMediaType(Document dom, String metadataUrl, String metadataMediaType) {
        final Element extendedCaps = getFirstElementByTagName(dom,
                "inspire_dls:ExtendedCapabilities");
        assertNotNull(extendedCaps);

        final Element mdUrl = getFirstElementByTagName(extendedCaps, "inspire_common:MetadataUrl");
        assertNotNull(mdUrl);

        final Element url = getFirstElementByTagName(mdUrl, "inspire_common:URL");
        assertNotNull(url);
        assertEquals(metadataUrl, url.getFirstChild().getNodeValue());

        final Element mediaType = getFirstElementByTagName(mdUrl, "inspire_common:MediaType");
        assertNotNull(mediaType);
        assertEquals(metadataMediaType, mediaType.getFirstChild().getNodeValue());

    }
}
