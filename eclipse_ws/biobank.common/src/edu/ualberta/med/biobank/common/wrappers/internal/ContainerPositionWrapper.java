package edu.ualberta.med.biobank.common.wrappers.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.ContainerPosition;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class ContainerPositionWrapper extends
    AbstractPositionWrapper<ContainerPosition> {

    private ContainerWrapper parent;
    private ContainerWrapper container;

    public ContainerPositionWrapper(WritableApplicationService appService,
        ContainerPosition wrappedObject) {
        super(appService, wrappedObject);
    }

    public ContainerPositionWrapper(WritableApplicationService appService) {
        super(appService);
    }

    @Override
    protected String[] getPropertyChangeNames() {
        List<String> properties = new ArrayList<String>(Arrays.asList(super
            .getPropertyChangeNames()));
        properties.add("parentContainer");
        properties.add("container");
        return properties.toArray(new String[properties.size()]);
    }

    @Override
    public Class<ContainerPosition> getWrappedClass() {
        return ContainerPosition.class;
    }

    private void setParentContainer(ContainerWrapper parentContainer) {
        this.parent = parentContainer;
        Container oldParent = wrappedObject.getParentContainer();
        Container newParent = null;
        if (parentContainer != null) {
            newParent = parentContainer.getWrappedObject();
        }
        wrappedObject.setParentContainer(newParent);
        propertyChangeSupport.firePropertyChange("parentContainer", oldParent,
            newParent);
    }

    private ContainerWrapper getParentContainer() {
        if (parent == null) {
            Container c = wrappedObject.getParentContainer();
            if (c == null)
                return null;
            parent = new ContainerWrapper(appService, c);
        }
        return parent;
    }

    public ContainerWrapper getContainer() {
        if (container == null) {
            Container c = wrappedObject.getContainer();
            if (c == null)
                return null;
            container = new ContainerWrapper(appService, c);
        }
        return container;
    }

    public void setContainer(ContainerWrapper container) {
        this.container = container;
        Container oldContainer = wrappedObject.getContainer();
        Container newContainer = null;
        if (container != null) {
            newContainer = container.getWrappedObject();
        }
        wrappedObject.setContainer(newContainer);
        propertyChangeSupport.firePropertyChange("container", oldContainer,
            newContainer);
    }

    @Override
    protected void deleteChecks() throws BiobankCheckException,
        ApplicationException {
    }

    @Override
    public int compareTo(ModelWrapper<ContainerPosition> modelWrapper) {
        if (modelWrapper instanceof ContainerPositionWrapper) {
            return getContainer().compareTo(
                ((ContainerPositionWrapper) modelWrapper).getContainer());
        }
        return 0;
    }

    @Override
    public String toString() {
        return "[" + getRow() + ", " + getCol() + "] "
            + getContainer().toString();
    }

    @Override
    public ContainerWrapper getParent() {
        return getParentContainer();
    }

    @Override
    public void setParent(ContainerWrapper parent) {
        setParentContainer(parent);
    }

    @Override
    protected void checkObjectAtPosition() throws ApplicationException,
        BiobankCheckException {
        ContainerWrapper parent = getParent();
        if (parent != null) {
            // do a hql query because parent might need a reload - but if we are
            // in the middle of parent.persist, don't want to do that !
            HQLCriteria criteria = new HQLCriteria(
                "from " + ContainerPosition.class.getName()
                    + " where parentContainer.id=? and row=? and col=?",
                Arrays.asList(new Object[] { parent.getId(), getRow(), getCol() }));
            List<ContainerPosition> positions = appService.query(criteria);
            if (positions.size() == 0) {
                return;
            }
            ContainerPositionWrapper childPosition = new ContainerPositionWrapper(
                appService, positions.get(0));
            if (!childPosition.getContainer().equals(getContainer())) {
                throw new BiobankCheckException("Position "
                    + childPosition.getContainer().getLabel() + " (" + getRow()
                    + ":" + getCol() + ") in container "
                    + getParent().toString()
                    + " is not available in container "
                    + parent.getFullInfoLabel());
            }
        }
    }

    @Override
    public void reload() throws Exception {
        super.reload();
        parent = null;
        container = null;
    }
}
