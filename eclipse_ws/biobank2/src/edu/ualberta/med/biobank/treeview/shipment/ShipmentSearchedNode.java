package edu.ualberta.med.biobank.treeview.shipment;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import edu.ualberta.med.biobank.common.wrappers.ClinicWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.common.wrappers.ShipmentWrapper;
import edu.ualberta.med.biobank.treeview.AbstractSearchedNode;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import edu.ualberta.med.biobank.treeview.admin.ClinicAdapter;
import edu.ualberta.med.biobank.views.ShipmentAdministrationView;

public class ShipmentSearchedNode extends AbstractSearchedNode {

    public ShipmentSearchedNode(AdapterBase parent, int id) {
        super(parent, id, false);
    }

    @Override
    protected AdapterBase createChildNode(ModelWrapper<?> child) {
        Assert.isTrue(child instanceof ClinicWrapper);
        return new ClinicAdapter(this, (ClinicWrapper) child);
    }

    @Override
    protected AdapterBase createChildNode() {
        return new ClinicAdapter(this, null);
    }

    @Override
    protected boolean isParentTo(ModelWrapper<?> parent, ModelWrapper<?> child) {
        if (child instanceof ShipmentWrapper) {
            return parent.equals(((ShipmentWrapper) child).getClinic());
        }
        return false;
    }

    @Override
    public List<AdapterBase> search(Object searchedObject) {
        if (searchedObject instanceof Date) {
            Date date = (Date) searchedObject;
            return Arrays.asList(getChild((int) date.getTime()));
        } else
            return findChildFromClass(searchedObject, ClinicWrapper.class);
    }

    @Override
    protected void addNode(ModelWrapper<?> wrapper) {
        ShipmentAdministrationView.getCurrent().addToNode(this, wrapper);
    }
}
