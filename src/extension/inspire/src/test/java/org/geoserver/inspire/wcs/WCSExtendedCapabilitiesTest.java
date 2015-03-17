/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wcs;

import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.inspire.InspireMetadata;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.WCSInfo;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Existence of ExtendedCapabilities element", 1, nodeList.getLength());

        final Element extendedCapabilities = (Element) nodeList.item(0);

        nodeList = extendedCapabilities.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Existence of MetadataURL element", 1, nodeList.getLength());

        final Element metadataUrl = (Element) nodeList.item(0);

        nodeList = metadataUrl.getElementsByTagNameNS(COMMON_NAMESPACE, "URL");
        assertEquals("Existence of URL element", 1, nodeList.getLength());

        final Element url = (Element) nodeList.item(0);
        
        assertEquals("http://foo.com?bar=baz", url.getFirstChild().getNodeValue());

        nodeList = extendedCapabilities.getElementsByTagNameNS(COMMON_NAMESPACE, "SupportedLanguages");
        assertEquals("Existence of SupportedLanguages element", 1, nodeList.getLength());

        final Element supportedLanguages = (Element) nodeList.item(0);
        
        nodeList = supportedLanguages.getElementsByTagNameNS(COMMON_NAMESPACE, "DefaultLanguage");
        assertEquals("Existence of DefaultLanguage element", 1, nodeList.getLength());
        
        final Element defaultLanguage = (Element) nodeList.item(0);

        nodeList = defaultLanguage.getElementsByTagNameNS(COMMON_NAMESPACE, "Language");
        assertEquals("Existence of Language element", 1, nodeList.getLength());
        
        final Element dlLanguage = (Element) nodeList.item(0);
        
        assertEquals("fre", dlLanguage.getFirstChild().getNodeValue());

        // It is not necessary to repeat the DefaultLanguage as a SupportedLanguage
        // so won't test SupportedLanguage although implementation does repeat
        // it at time of writing 2015-03-17.
        
        nodeList = extendedCapabilities.getElementsByTagNameNS(COMMON_NAMESPACE, "ResponseLanguage");
        assertEquals("Existence of ResponseLanguage element", 1, nodeList.getLength());
        
        final Element responseLanguage = (Element) nodeList.item(0);

        nodeList = responseLanguage.getElementsByTagNameNS(COMMON_NAMESPACE, "Language");
        assertEquals("Existence of Language element", 1, nodeList.getLength());
        
        final Element rlLanguage = (Element) nodeList.item(0);
        
        assertEquals("fre", rlLanguage.getFirstChild().getNodeValue());

        final Element sdi = getFirstElementByTagName(extendedCapabilities, "inspire_dls:SpatialDataSetIdentifier");
        final Element code = getFirstElementByTagName(sdi, "inspire_common:Code");
        assertEquals("one", code.getFirstChild().getNodeValue());
        final Element ns = getFirstElementByTagName(sdi, "inspire_common:Namespace");
        assertEquals("http://www.geoserver.org/inspire/one", ns.getFirstChild().getNodeValue());

    }

    @Test
    @Ignore
    public void testChangeMediaType() throws Exception {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wcs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
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
