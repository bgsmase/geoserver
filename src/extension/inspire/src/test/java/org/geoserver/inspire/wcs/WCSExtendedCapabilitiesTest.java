/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wcs;

import static org.geoserver.inspire.wfs.WFSExtendedCapabilitiesProvider.DLS_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.inspire.InspireMetadata;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.WCSInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WCSExtendedCapabilitiesTest extends GeoServerSystemTestSupport {
    private static final String WCS_2_0_0_GETCAPREQUEST = "wcs?request=GetCapabilities&service=WCS&acceptVersions=2.0.0";
    
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
    @Ignore
    public void testExtendedCaps200() throws Exception {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wcs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wcs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one");
        getGeoServer().save(wcs);

        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&acceptVersions=2.0.0");
        
        assertEquals(DLS_NAMESPACE, dom.getDocumentElement().getAttribute("xmlns:inspire_dls"));

        final Element extendedCaps = getFirstElementByTagName(dom,
                "inspire_dls:ExtendedCapabilities");
        assertNotNull(extendedCaps);

        final Element mdUrl = getFirstElementByTagName(extendedCaps, "inspire_common:MetadataUrl");
        assertNotNull(mdUrl);

        final Element url = getFirstElementByTagName(mdUrl, "inspire_common:URL");
        assertNotNull(url);
        assertEquals("http://foo.com?bar=baz", url.getFirstChild().getNodeValue());

        final Element suppLangs = getFirstElementByTagName(extendedCaps,
                "inspire_common:SupportedLanguages");
        assertNotNull(suppLangs);
        final Element defLang = getFirstElementByTagName(suppLangs,
                "inspire_common:DefaultLanguage");
        assertNotNull(defLang);
        final Element defLangVal = getFirstElementByTagName(defLang, "inspire_common:Language");
        assertEquals("fre", defLangVal.getFirstChild().getNodeValue());

        final Element respLang = getFirstElementByTagName(extendedCaps,
                "inspire_common:ResponseLanguage");
        assertNotNull(respLang);
        final Element respLangVal = getFirstElementByTagName(defLang, "inspire_common:Language");
        assertEquals("fre", respLangVal.getFirstChild().getNodeValue());
        
        final Element sdi = getFirstElementByTagName(extendedCaps, "inspire_dls:SpatialDataSetIdentifier");
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
