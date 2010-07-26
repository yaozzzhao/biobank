package edu.ualberta.med.biobank.forms;

import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.wrappers.AliquotWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.logs.BiobankLogger;
import edu.ualberta.med.biobank.treeview.AliquotAdapter;
import edu.ualberta.med.biobank.util.RowColPos;
import edu.ualberta.med.biobank.widgets.BiobankText;
import edu.ualberta.med.biobank.widgets.grids.ContainerDisplayWidget;

public class AliquotViewForm extends BiobankViewForm {

    private static BiobankLogger logger = BiobankLogger
        .getLogger(AliquotViewForm.class.getName());

    public static final String ID = "edu.ualberta.med.biobank.forms.AliquotViewForm";

    private AliquotAdapter aliquotAdapter;

    private AliquotWrapper aliquot;

    private BiobankText sampleTypeLabel;

    private BiobankText linkDateLabel;

    private BiobankText volumeLabel;

    private BiobankText shipmentWaybillLabel;

    private BiobankText patientLabel;

    private BiobankText dateProcessedLabel;

    private BiobankText dateDrawnLabel;

    private BiobankText activityStatusLabel;

    private BiobankText commentLabel;

    private BiobankText positionLabel;

    @Override
    public void init() {
        Assert.isTrue((adapter instanceof AliquotAdapter),
            "Invalid editor input: object of type "
                + adapter.getClass().getName());

        aliquotAdapter = (AliquotAdapter) adapter;
        aliquot = aliquotAdapter.getAliquot();
        retrieveAliquot();
        aliquot.logLookup(SessionManager.getInstance().getCurrentSite()
            .getNameShort());
        setPartName("Aliquot: " + aliquot.getInventoryId());
    }

    private void retrieveAliquot() {
        try {
            aliquot.reload();
        } catch (Exception e) {
            logger.error("Can't reload aliquot with id " + aliquot.getId());
        }
    }

    @Override
    protected void createFormContent() throws Exception {
        form.setText("Aliquot " + aliquot.getInventoryId());
        GridLayout layout = new GridLayout(1, false);
        form.getBody().setLayout(layout);
        form.getBody().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createInformationSection();
        createContainersSection();
        setValues();
    }

    private void createInformationSection() {
        Composite client = toolkit.createComposite(form.getBody());
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        sampleTypeLabel = createReadOnlyLabelledField(client, SWT.NONE, "Type");
        linkDateLabel = createReadOnlyLabelledField(client, SWT.NONE,
            "Link Date");
        volumeLabel = createReadOnlyLabelledField(client, SWT.NONE,
            "Volume (ml)");
        shipmentWaybillLabel = createReadOnlyLabelledField(client, SWT.NONE,
            "Shipment Waybill");
        patientLabel = createReadOnlyLabelledField(client, SWT.NONE, "Patient");
        dateProcessedLabel = createReadOnlyLabelledField(client, SWT.NONE,
            "Date Processed");
        dateDrawnLabel = createReadOnlyLabelledField(client, SWT.NONE,
            "Date Drawn");
        activityStatusLabel = createReadOnlyLabelledField(client, SWT.NONE,
            "Activity Status");
        commentLabel = createReadOnlyLabelledField(client,
            SWT.WRAP | SWT.MULTI, "Comment");
        positionLabel = createReadOnlyLabelledField(client, SWT.WRAP,
            "Position");
    }

    private void createContainersSection() {
        if (aliquot.getParent() != null) {
            Section section = createSection("Container Visualization");
            Composite containersComposite = toolkit.createComposite(section);
            section.setClient(containersComposite);
            section.setExpanded(false);
            containersComposite.setLayout(new GridLayout(1, false));
            toolkit.paintBordersFor(containersComposite);

            Stack<ContainerWrapper> parents = new Stack<ContainerWrapper>();
            ContainerWrapper container = aliquot.getParent();
            while (container != null) {
                parents.push(container);
                container = container.getParent();
            }
            while (!parents.isEmpty()) {
                container = parents.pop();
                RowColPos position;
                if (parents.isEmpty()) {
                    position = aliquot.getPosition();
                } else {
                    position = parents.peek().getPosition();
                }
                Composite containerComposite = toolkit
                    .createComposite(containersComposite);
                GridLayout layout = new GridLayout(1, false);
                layout.horizontalSpacing = 0;
                layout.marginWidth = 0;
                layout.verticalSpacing = 0;
                containerComposite.setLayout(layout);
                toolkit
                    .createLabel(containerComposite, container.getLabel()
                        + " (" + container.getContainerType().getNameShort()
                        + ") ");
                ContainerDisplayWidget containerWidget = new ContainerDisplayWidget(
                    containerComposite);
                containerWidget.setContainer(container);
                containerWidget.setSelection(position);
                toolkit.adapt(containerWidget);
            }
        }
    }

    private void setValues() {
        setTextValue(sampleTypeLabel, aliquot.getSampleType().getName());
        setTextValue(linkDateLabel, aliquot.getFormattedLinkDate());
        setTextValue(volumeLabel, aliquot.getQuantity() == null ? null
            : aliquot.getQuantity().toString());
        setTextValue(shipmentWaybillLabel, aliquot.getPatientVisit()
            .getShipment().getWaybill());
        setTextValue(patientLabel, aliquot.getPatientVisit().getPatient()
            .getPnumber());
        setTextValue(dateProcessedLabel, aliquot.getPatientVisit()
            .getFormattedDateProcessed());
        setTextValue(dateDrawnLabel, aliquot.getPatientVisit()
            .getFormattedDateDrawn());
        setTextValue(activityStatusLabel, aliquot.getActivityStatus());
        setTextValue(commentLabel, aliquot.getComment());
        setTextValue(positionLabel, aliquot.getPositionString(true, false));
    }

    @Override
    protected void reload() {
        retrieveAliquot();
        setValues();
        setPartName("Aliquot: " + aliquot.getInventoryId());
        form.setText("Aliquot: " + aliquot.getInventoryId());
    }

}
