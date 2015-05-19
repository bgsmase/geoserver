package org.geoserver.inspire.web;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.Converters;
import org.junit.Before;
import org.junit.Test;

public class UniqueResourceIdentifiersEditorTest extends GeoServerWicketTestSupport {

    private UniqueResourceIdentifiers identifiers;

    @Before
    public void setupPanel() {
        identifiers = Converters.convert(
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two",
                UniqueResourceIdentifiers.class);
        tester.startPage(new FormTestPage(new ComponentBuilder() {

            public Component buildComponent(String id) {
                return new UniqueResourceIdentifiersEditor(id, new Model(identifiers));
            }
        }));

    }

    @Test
    public void testContents() {
        // print(tester.getLastRenderedPage(), true, true);
        tester.assertComponent("form", Form.class);
        
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:1:itemProperties:0:component:border:txt", "one");
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:1:itemProperties:1:component:border:txt", "http://www.geoserver.org/one");
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:1:itemProperties:2:component:border:txt", null);
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:2:itemProperties:0:component:border:txt", "two");
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:2:itemProperties:1:component:border:txt", "http://www.geoserver.org/two");
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:2:itemProperties:2:component:border:txt", "http://metadata.geoserver.org/id?two");
    }
    
    @Test
    public void testRemoveLinks() {
        // print(tester.getLastRenderedPage(), true, true);
        tester.assertComponent("form", Form.class);
        
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:1:itemProperties:0:component:border:txt", "one");
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:2:itemProperties:0:component:border:txt", "two");

        // remove the first identifier
        tester.executeAjaxEvent("form:panel:container:identifiers:listContainer:items:1:itemProperties:3:component:remove", "onclick");
        assertNull(tester.getLastRenderedPage().get("form:panel:container:identifiers:listContainer:items:1:itemProperties:0:component:border:txt"));
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:2:itemProperties:0:component:border:txt", "two");
        
        // remove the second as well
        tester.executeAjaxEvent("form:panel:container:identifiers:listContainer:items:2:itemProperties:3:component:remove", "onclick");
        assertNull(tester.getLastRenderedPage().get("form:panel:container:identifiers:listContainer:items:1:itemProperties:0:component:border:txt"));
        assertNull(tester.getLastRenderedPage().get("form:panel:container:identifiers:listContainer:items:2:itemProperties:0:component:border:txt"));
        
        // print(tester.getLastRenderedPage(), true, true);
        
        // now trigger the validation, we cannot be without spatial data identifiers
        tester.submitForm("form");
        
        String error = new ParamResourceModel("UniqueResourceIdentifiersEditor.noSpatialDatasetIdentifiers", null).getString();
        tester.assertErrorMessages(new String[] {error});
    }
    
    @Test
    public void testAddIdentifiers() {
        tester.executeAjaxEvent("form:panel:addIdentifier", "onclick");
        
        // new empty line
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:3:itemProperties:0:component:border:txt", null);
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:3:itemProperties:1:component:border:txt", null);
        
        // try to submit, should complain about invalid code
        FormTester ft = tester.newFormTester("form");
        ft.submit();
        List<Serializable> messages = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, messages.size());
        assertTrue(((ValidationErrorFeedback) messages.get(0)).getMessage().contains("Code"));
        
        // print(tester.getLastRenderedPage(), true, true);
        
        // submit with just code, that is fine
        ft = tester.newFormTester("form");
        ft.setValue("panel:container:identifiers:listContainer:items:3:itemProperties:0:component:border:txt", "code");
        ft.submit();
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:3:itemProperties:0:component:border:txt", "code");
        
        // now provide an invalid namespace (not a valid URI)
        ft = tester.newFormTester("form");
        ft.setValue("panel:container:identifiers:listContainer:items:3:itemProperties:1:component:border:txt", "invalid uri");
        ft.submit();
        messages = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, messages.size());
        
        // finally, set a valid namespace
        ft = tester.newFormTester("form");
        ft.setValue("panel:container:identifiers:listContainer:items:3:itemProperties:1:component:border:txt", "http://www.geoserver.org/meta");
        ft.submit();
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:3:itemProperties:1:component:border:txt", "http://www.geoserver.org/meta");

        // now provide an invalid metadataURL (not a valid URI)
        ft = tester.newFormTester("form");
        ft.setValue("panel:container:identifiers:listContainer:items:3:itemProperties:2:component:border:txt", "invalid uri");
        ft.submit();
        messages = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, messages.size());
        
        // finally, set a valid metadataURL
        ft = tester.newFormTester("form");
        ft.setValue("panel:container:identifiers:listContainer:items:3:itemProperties:2:component:border:txt", "http://www.geoserver.org/meta");
        ft.submit();
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:panel:container:identifiers:listContainer:items:3:itemProperties:2:component:border:txt", "http://www.geoserver.org/meta");
}


}
