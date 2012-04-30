package edu.ualberta.med.biobank.treeview.dispatch;

import java.util.List;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.dispatch.DispatchRetrievalAction;
import edu.ualberta.med.biobank.common.util.DispatchState;
import edu.ualberta.med.biobank.common.wrappers.DispatchWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.treeview.AdapterBase;

public class ReceivingNoErrorsDispatchGroup extends AbstractDispatchGroup {

    public ReceivingNoErrorsDispatchGroup(AdapterBase parent, int id) {
        super(parent, id,
            Messages.ReceivingNoErrorsDispatchGroup_receiving_node_label);
    }

    @Override
    protected List<? extends ModelWrapper<?>> getWrapperChildren()
        throws Exception {
        return ModelWrapper.wrapModelCollection(SessionManager.getAppService(),
            SessionManager.getAppService().doAction(
                new DispatchRetrievalAction(DispatchState.RECEIVED,
                    SessionManager.getUser().getCurrentWorkingCenter().getId(),
                    false, true)).getList(), DispatchWrapper.class);
    }

}
