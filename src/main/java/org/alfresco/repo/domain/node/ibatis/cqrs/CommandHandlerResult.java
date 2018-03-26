package org.alfresco.repo.domain.node.ibatis.cqrs;

/**
 * Created by mmuller on 26/03/2018.
 */
public class CommandHandlerResult
{

    private boolean accepted = false;
    private Object diffObject;

    public CommandHandlerResult(Object diffObject, boolean accepted)
    {
        this.diffObject = diffObject;
        this.accepted = accepted;
    }

    public boolean isAccepted()
    {
        return accepted;
    }

    public Object getDiffObject()
    {
        return diffObject;
    }

    @Override
    public String toString()
    {
        return "Accepted: " + accepted + " the diffOjbect: " + diffObject.toString();
    }
}
