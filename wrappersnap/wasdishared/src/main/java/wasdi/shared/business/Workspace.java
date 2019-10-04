package wasdi.shared.business;

/**
 * Workspace Entity
 * Created by p.campanella on 25/10/2016.
 */
public class Workspace {

	/**
	 * Workspace ID
	 */
    private String workspaceId;
    /**
     * Name
     */
    private String name;
    /**
     * User Owner
     */
    private String userId;
    /**
     * Creation timestamp
     */
    private Double creationDate;
    /**
     * Last edit timestamp
     */
    private Double lastEditDate;
    /**
     * Code of the WASDI node where the workspace is located
     */
    private String nodeCode = "wasdi";    
    

    public String getNodeCode() {
		return nodeCode;
	}

	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}

	public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Double creationDate) {
        this.creationDate = creationDate;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Double getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(Double lastEditDate) {
        this.lastEditDate = lastEditDate;
    }
}
