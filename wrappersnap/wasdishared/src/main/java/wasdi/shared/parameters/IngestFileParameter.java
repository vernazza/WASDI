package wasdi.shared.parameters;

/**
 * Created by s.adamo on 10/10/2016.
 */



public class IngestFileParameter extends BaseParameter{
    
    private String filePath;
    
    private String style;

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

    
    
}
