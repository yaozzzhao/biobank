package edu.ualberta.med.biobank.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction.CEventInfo;
import edu.ualberta.med.biobank.common.action.collectionEvent.EventAttrInfo;
import edu.ualberta.med.biobank.common.action.eventattr.EventAttrTypeEnum;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenInfo;
import edu.ualberta.med.biobank.common.action.study.StudyEventAttrInfo;
import edu.ualberta.med.biobank.common.action.study.StudyGetEventAttrInfoAction;
import edu.ualberta.med.biobank.common.wrappers.CommentWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenWrapper;
import edu.ualberta.med.biobank.forms.input.FormInput;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.gui.common.widgets.IInfoTableDoubleClickItemListener;
import edu.ualberta.med.biobank.gui.common.widgets.IInfoTableEditItemListener;
import edu.ualberta.med.biobank.gui.common.widgets.InfoTableEvent;
import edu.ualberta.med.biobank.gui.common.widgets.InfoTableSelection;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.AliquotedSpecimen;
import edu.ualberta.med.biobank.model.CollectionEvent;
import edu.ualberta.med.biobank.model.Comment;
import edu.ualberta.med.biobank.model.EventAttrCustom;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.SourceSpecimen;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.Study;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import edu.ualberta.med.biobank.treeview.SpecimenAdapter;
import edu.ualberta.med.biobank.treeview.patient.CollectionEventAdapter;
import edu.ualberta.med.biobank.widgets.infotables.CommentsInfoTable;
import edu.ualberta.med.biobank.widgets.infotables.NewSpecimenInfoTable;
import edu.ualberta.med.biobank.widgets.infotables.NewSpecimenInfoTable.ColumnsShown;

public class CollectionEventViewForm extends BiobankViewForm {
    private static final I18n i18n = I18nFactory
        .getI18n(CollectionEventViewForm.class);

    @SuppressWarnings("nls")
    public static final String ID =
        "edu.ualberta.med.biobank.forms.CollectionEventViewForm";

    private BgcBaseText studyLabel;

    private List<FormPvCustomInfo> pvCustomInfoList;

    private BgcBaseText patientLabel;

    private BgcBaseText visitNumberLabel;

    private NewSpecimenInfoTable sourceSpecimenTable;

    private BgcBaseText activityStatusLabel;

    private NewSpecimenInfoTable aliquotedSpcTable;

    private CEventInfo ceventInfo;

    private CommentsInfoTable commentTable;

    private static class FormPvCustomInfo extends EventAttrCustom {
        BgcBaseText widget;
    }

    @SuppressWarnings("nls")
    @Override
    public void init() throws Exception {
        Assert.isTrue((adapter instanceof CollectionEventAdapter),
            "Invalid editor input: object of type "
                + adapter.getClass().getName());

        updateCEventInfo();

        setPartName(
        // tab name, {0} is the visit number
        i18n.tr("CE {0}", ceventInfo.cevent.getVisitNumber()));
    }

    private void updateCEventInfo() throws Exception {
        ceventInfo =
            SessionManager.getAppService().doAction(
                new CollectionEventGetInfoAction(adapter.getId()));
        SessionManager.logLookup(ceventInfo.cevent);
    }

    @SuppressWarnings("nls")
    @Override
    protected void createFormContent() throws Exception {
        form.setText(
            // form title
            i18n.tr("Collection Event for visit {0}",
                ceventInfo.cevent.getVisitNumber()));
        page.setLayout(new GridLayout(1, false));
        page.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createMainSection();
        createSourceSpecimensSection();
        createAliquotedSpecimensSection();
    }

    private void createMainSection() throws Exception {
        Composite client = toolkit.createComposite(page);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        studyLabel =
            createReadOnlyLabelledField(client, SWT.NONE,
                Study.NAME.format(1).toString());
        patientLabel =
            createReadOnlyLabelledField(client, SWT.NONE,
                Patient.NAME.format(1).toString());
        visitNumberLabel =
            createReadOnlyLabelledField(client, SWT.NONE,
                CollectionEvent.PropertyName.VISIT_NUMBER.toString());
        activityStatusLabel =
            createReadOnlyLabelledField(client, SWT.NONE,
                ActivityStatus.NAME.singular().toString());

        createPvDataSection(client);
        createCommentsSection();

        setCollectionEventValues();
    }

    private void createCommentsSection() {
        Composite client =
            createSectionWithClient(Comment.NAME.format(2).toString());
        commentTable =
            new CommentsInfoTable(client,
                ModelWrapper.wrapModelCollection(
                    SessionManager.getAppService(),
                    ceventInfo.cevent.getComments(),
                    CommentWrapper.class));
        commentTable.adaptToToolkit(toolkit, true);
        toolkit.paintBordersFor(commentTable);
    }

    private void createPvDataSection(Composite client) throws Exception {
        Map<Integer, StudyEventAttrInfo> studyAttrInfos =
            SessionManager.getAppService().doAction(
                new StudyGetEventAttrInfoAction(ceventInfo.cevent.getPatient()
                    .getStudy().getId())).getMap();

        pvCustomInfoList = new ArrayList<FormPvCustomInfo>();

        for (Entry<Integer, StudyEventAttrInfo> entry : studyAttrInfos
            .entrySet()) {
            FormPvCustomInfo combinedPvInfo = new FormPvCustomInfo();
            combinedPvInfo.setLabel(entry.getValue().attr.getGlobalEventAttr()
                .getLabel());
            combinedPvInfo.setType(entry.getValue().type);

            int style = SWT.NONE;
            if (combinedPvInfo.getType() == EventAttrTypeEnum.SELECT_MULTIPLE) {
                style |= SWT.WRAP;
            }

            EventAttrInfo eventAttr = ceventInfo.eventAttrs.get(entry.getKey());
            String value = eventAttr == null ? null : eventAttr.attr.getValue();
            if (combinedPvInfo.getType() == EventAttrTypeEnum.SELECT_MULTIPLE
                && (value != null)) {
                combinedPvInfo.setValue(value.replace(';', '\n'));
            } else {
                combinedPvInfo.setValue(value);
            }

            combinedPvInfo.widget =
                createReadOnlyLabelledField(client, style,
                    entry.getValue().attr.getGlobalEventAttr().getLabel(),
                    combinedPvInfo.getValue());
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            combinedPvInfo.widget.setLayoutData(gd);

            pvCustomInfoList.add(combinedPvInfo);
        }
    }

    private void setCollectionEventValues() {
        setTextValue(studyLabel, ceventInfo.cevent.getPatient().getStudy()
            .getName());
        setTextValue(patientLabel, ceventInfo.cevent.getPatient().getPnumber());
        setTextValue(visitNumberLabel, ceventInfo.cevent.getVisitNumber());
        setTextValue(activityStatusLabel, ceventInfo.cevent.getActivityStatus()
            .getName());
        // assign PvInfo
        for (FormPvCustomInfo combinedPvInfo : pvCustomInfoList) {
            setTextValue(combinedPvInfo.widget, combinedPvInfo.getValue());
        }
    }

    private void createSourceSpecimensSection() {
        Composite client =
            createSectionWithClient(SourceSpecimen.NAME.format(2).toString());
        sourceSpecimenTable =
            new NewSpecimenInfoTable(client, ceventInfo.sourceSpecimenInfos,
                ColumnsShown.CEVENT_SOURCE_SPECIMENS, 10);
        sourceSpecimenTable.adaptToToolkit(toolkit, true);
        sourceSpecimenTable
            .addClickListener(new IInfoTableDoubleClickItemListener<SpecimenInfo>() {
                @Override
                public void doubleClick(InfoTableEvent<SpecimenInfo> event) {
                    Specimen s =
                        ((SpecimenInfo) ((InfoTableSelection) event
                            .getSelection()).getObject()).specimen;
                    AdapterBase.openForm(
                        new FormInput(
                            new SpecimenAdapter(null,
                                new SpecimenWrapper(SessionManager
                                    .getAppService(), s))),
                        SpecimenViewForm.ID);
                }
            });
        sourceSpecimenTable
            .addEditItemListener(new IInfoTableEditItemListener<SpecimenInfo>() {
                @Override
                public void editItem(InfoTableEvent<SpecimenInfo> event) {
                    Specimen s =
                        ((SpecimenInfo) ((InfoTableSelection) event
                            .getSelection()).getObject()).specimen;
                    AdapterBase.openForm(
                        new FormInput(
                            new SpecimenAdapter(null,
                                new SpecimenWrapper(SessionManager
                                    .getAppService(), s))),
                        SpecimenEntryForm.ID);
                }
            });
    }

    private void createAliquotedSpecimensSection() {
        // FIXME should we show that to clinics ?
        Composite client =
            createSectionWithClient(AliquotedSpecimen.NAME.format(2).toString());
        aliquotedSpcTable =
            new NewSpecimenInfoTable(client, ceventInfo.aliquotedSpecimenInfos,
                ColumnsShown.CEVENT_ALIQUOTED_SPECIMENS, 10);
        aliquotedSpcTable.adaptToToolkit(toolkit, true);
        aliquotedSpcTable
            .addClickListener(new IInfoTableDoubleClickItemListener<SpecimenInfo>() {
                @Override
                public void doubleClick(InfoTableEvent<SpecimenInfo> event) {
                    Specimen s =
                        ((SpecimenInfo) ((InfoTableSelection) event
                            .getSelection()).getObject()).specimen;
                    AdapterBase.openForm(
                        new FormInput(
                            new SpecimenAdapter(null,
                                new SpecimenWrapper(SessionManager
                                    .getAppService(), s))),
                        SpecimenViewForm.ID);
                }
            });
        aliquotedSpcTable
            .addEditItemListener(new IInfoTableEditItemListener<SpecimenInfo>() {
                @Override
                public void editItem(InfoTableEvent<SpecimenInfo> event) {
                    Specimen s =
                        ((SpecimenInfo) ((InfoTableSelection) event
                            .getSelection()).getObject()).specimen;
                    AdapterBase.openForm(
                        new FormInput(
                            new SpecimenAdapter(null,
                                new SpecimenWrapper(SessionManager
                                    .getAppService(), s))),
                        SpecimenEntryForm.ID);
                }
            });
    }

    @SuppressWarnings("nls")
    @Override
    public void setValues() throws Exception {
        setPartName(
        // tab name, {0} is visit number
        i18n.tr("CE {0}", ceventInfo.cevent.getVisitNumber()));
        form.setText(
            // form title
            i18n.tr("Collection Event for visit {0}",
                ceventInfo.cevent.getVisitNumber()));
        setCollectionEventValues();
        sourceSpecimenTable.setList(ceventInfo.sourceSpecimenInfos);
        aliquotedSpcTable.setList(ceventInfo.aliquotedSpecimenInfos);
        commentTable.setList(ModelWrapper.wrapModelCollection(
            SessionManager.getAppService(),
            ceventInfo.cevent.getComments(),
            CommentWrapper.class));
    }

}
