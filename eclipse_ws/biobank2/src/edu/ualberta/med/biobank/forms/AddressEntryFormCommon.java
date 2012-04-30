package edu.ualberta.med.biobank.forms;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import edu.ualberta.med.biobank.common.peer.AddressPeer;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.gui.common.forms.FieldInfo;
import edu.ualberta.med.biobank.gui.common.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;

public abstract class AddressEntryFormCommon extends BiobankEntryForm {

    /*
     * Want to preserve insert order so using ListOrderedMap.
     */
    public static final Map<String, FieldInfo> ADDRESS_FIELDS;
    static {
        Map<String, FieldInfo> aMap = new LinkedHashMap<String, FieldInfo>();
        aMap.put(AddressPeer.STREET1.getName(), new FieldInfo(
            "Street 1", BgcBaseText.class,
            SWT.NONE, null, null, null));
        aMap.put(AddressPeer.STREET2.getName(), new FieldInfo(
            "Street 2", BgcBaseText.class,
            SWT.NONE, null, null, null));
        aMap.put(AddressPeer.CITY.getName(), new FieldInfo(
            "City", BgcBaseText.class,
            SWT.NONE, null, NonEmptyStringValidator.class,
            "Enter a city"));
        aMap.put(AddressPeer.PROVINCE.getName(), new FieldInfo(
            "Province/State", BgcBaseText.class,
            SWT.NONE, null, null, null));
        aMap.put(AddressPeer.POSTAL_CODE.getName(), new FieldInfo(
            "Postal/Zip Code",
            BgcBaseText.class, SWT.NONE, null, null, null));
        aMap.put(AddressPeer.COUNTRY.getName(), new FieldInfo(
            "Country", BgcBaseText.class,
            SWT.NONE, null, null, null));
        ADDRESS_FIELDS = Collections.unmodifiableMap(aMap);
    };

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException {
        super.init(site, input);
    }

    protected void createAddressArea(ModelWrapper<?> wrapperObject) {
        Composite client = createSectionWithClient("Address");
        createBoundWidgetsFromMap(ADDRESS_FIELDS, wrapperObject, client);
    }
}
