package edu.ualberta.med.biobank.common.action.researchGroup;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.IdResult;
import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.action.util.SessionUtil;
import edu.ualberta.med.biobank.common.permission.researchGroup.SubmitRequestPermission;
import edu.ualberta.med.biobank.common.util.RequestSpecimenState;
import edu.ualberta.med.biobank.model.Request;
import edu.ualberta.med.biobank.model.RequestSpecimen;
import edu.ualberta.med.biobank.model.ResearchGroup;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.User;

public class SubmitRequestAction implements Action<IdResult> {
    /**
     * 
     */
    private static final long serialVersionUID = -448974534976230815L;
    private Integer rgId;
    private List<String> specs;

    public SubmitRequestAction(Integer rgId, List<String> specs) {
        this.rgId = rgId;
        this.specs = specs;
    }

    @Override
    public boolean isAllowed(User user, Session session) throws ActionException {
        return new SubmitRequestPermission(rgId).isAllowed(user,
            session);
    }

    @Override
    public IdResult run(User user, Session session) throws ActionException {
        Request request = new Request();
        for (String id : specs) {
            Query q = session.createQuery("from "
                + Specimen.class.getName() + " where inventoryId=?");
            q.setParameter(0, id);
            Specimen spec = (Specimen) q.list().get(0);
            if (spec == null)
                continue;
            RequestSpecimen r =
                new RequestSpecimen();
            r.setRequest(request);
            r.setState(RequestSpecimenState.AVAILABLE_STATE.getId());
            r.setSpecimen(spec);
        }

        request.setResearchGroup(new SessionUtil(session).get(
            ResearchGroup.class,
            rgId));
        request.setCreated(new Date());
        request.setSubmitted(new Date());
        request.setAddress(new SessionUtil(session).get(ResearchGroup.class,
            rgId).getAddress());

        session.saveOrUpdate(request);
        session.flush();

        return new IdResult(request.getId());
    }
}