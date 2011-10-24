package edu.ualberta.med.biobank.common.action.check;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.util.HibernateUtil;
import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.common.wrappers.Property;
import edu.ualberta.med.biobank.model.IBiobankModel;
import edu.ualberta.med.biobank.model.User;
import edu.ualberta.med.biobank.server.applicationservice.exceptions.DuplicatePropertySetException;

/**
 * Checks that the {@link Collection} of {@link Property}-s is unique for the
 * model object in the {@link ModelWrapper}, excluding the instance itself (if
 * it is already persisted).
 * 
 * @author delphine
 */
public class UniquePreCheck<T extends IBiobankModel> extends ActionCheck<T> {
    private static final long serialVersionUID = 1L;

    private static final String HQL = "SELECT COUNT(*) FROM {0} o WHERE ({1}) = ({2}) {3}"; //$NON-NLS-1$

    // FIXME what about translation if this message is generated on the server?
    // Can we generated an exception that contain the information to display
    // that can create the appropriate message on the client?
    private static final String EXCEPTION_STRING = "There already exists a {0} with property value(s) ({1}) for ({2}), respectively. These field(s) must be unique."; //$NON-NLS-1$

    protected final Collection<ValueProperty<T>> valueProperties;

    public UniquePreCheck(ValueProperty<T> idProperty, Class<T> modelClass,
        Collection<ValueProperty<T>> valueProperties) {
        super(idProperty, modelClass);
        this.valueProperties = valueProperties;
    }

    @Override
    public boolean isAllowed(User user, Session session) throws ActionException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public T run(User user, Session session) throws ActionException {
        Query query = getQuery(session);
        Long count = HibernateUtil.getCountFromQuery(query);

        if (count > 0) {
            throwException();
        }
        return null;
    }

    private void throwException() throws DuplicatePropertySetException {
        String modelClass = Format.modelClass(getModelClass());
        String values = Format.propertyValues(valueProperties);
        String names = Format.propertyNames(valueProperties);

        String msg = MessageFormat.format(EXCEPTION_STRING, modelClass, values,
            names);

        throw new DuplicatePropertySetException(msg);
    }

    private Query getQuery(Session session) {
        String modelName = getModelClass().getName();
        String propertyNames = StringUtil.join(getPropertyNames(), ", "); //$NON-NLS-1$
        String valueBindings = getValueBindings();
        String notSelfCondition = getNotSelfCondition();

        String hql = MessageFormat.format(HQL, modelName, propertyNames,
            valueBindings, notSelfCondition);

        Query query = session.createQuery(hql);
        setParameters(query);

        return query;
    }

    private String getValueBindings() {
        StringBuilder sb = new StringBuilder();

        for (int i = 1, n = valueProperties.size(); i <= n; i++) {
            sb.append("?"); //$NON-NLS-1$
            if (i < n) {
                sb.append(","); //$NON-NLS-1$
            }
        }

        return sb.toString();
    }

    private void setParameters(Query query) {
        int i = 0;
        for (ValueProperty vp : valueProperties) {
            query.setParameter(i, vp.value);
            i++;
        }
    }

    private String getNotSelfCondition() {
        String idCheck = ""; //$NON-NLS-1$

        Integer id = getModelId();
        if (id != null) {
            String idName = getIdProperty().getName();
            idCheck = " AND " + idName + " <> " + id; //$NON-NLS-1$ //$NON-NLS-2$
        }

        return idCheck;
    }

    private List<String> getPropertyNames() {
        List<String> propertyNames = new ArrayList<String>();

        for (ValueProperty vp : valueProperties) {
            propertyNames.add(vp.property.getName());
        }

        return propertyNames;
    }

}