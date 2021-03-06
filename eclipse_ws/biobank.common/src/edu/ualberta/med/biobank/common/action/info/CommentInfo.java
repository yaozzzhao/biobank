package edu.ualberta.med.biobank.common.action.info;

import java.util.Collection;
import java.util.Date;

import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.action.ActionResult;
import edu.ualberta.med.biobank.model.Comment;
import edu.ualberta.med.biobank.model.User;

public class CommentInfo implements ActionResult {

    private static final long serialVersionUID = -7537167935539051938L;

    public Integer id;
    public String message;
    public Date createdAt;
    public Integer userId;

    public CommentInfo(String message, Date createdAt, Integer userId) {
        this.message = message;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Comment getCommentModel(ActionContext actionContext) {
        Comment dbComment;
        if (id == null)
            dbComment = new Comment();
        else
            dbComment = actionContext.load(Comment.class, id);
        dbComment.setMessage(message);
        User user = actionContext.load(User.class, userId);
        dbComment.setUser(user);
        return dbComment;
    }

    public static void setCommentModelCollection(ActionContext actionContext,
        Collection<Comment> modelCommentList, Collection<CommentInfo> newList) {
        if (newList != null) {
            for (CommentInfo info : newList) {
                Comment commentModel = info.getCommentModel(actionContext);
                modelCommentList.add(commentModel);
                // FIXME add a hibernate cascade?
                actionContext.getSession().saveOrUpdate(commentModel);
            }
        }
    }

    public static CommentInfo createFromModel(Comment c) {
        CommentInfo ci =
            new CommentInfo(c.getMessage(), c.getCreatedAt(), c.getUser()
                .getId());
        ci.id = c.getId();
        return ci;
    }
}
