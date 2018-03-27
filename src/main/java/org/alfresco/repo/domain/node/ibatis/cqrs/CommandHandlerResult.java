package org.alfresco.repo.domain.node.ibatis.cqrs;

/**
 * Encapsulate the diff object (e.g. ibatis statement)
 *
 * Created by mmuller on 26/03/2018.
 */
public class CommandHandlerResult
{

    /** The command can be rejected or accepted */
    private boolean accepted = false;
    /** Contains the diff object (e.g. ibatis statement) */
    private Object diffObject;

    /**
     *
     * @param diffObject Contains the diff object (e.g. ibatis statement)
     * @param accepted The command can be rejected or accepted
     */
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
        return "Accepted: " + accepted + ", the diffOjbect: " + diffObject.toString();
    }
}
