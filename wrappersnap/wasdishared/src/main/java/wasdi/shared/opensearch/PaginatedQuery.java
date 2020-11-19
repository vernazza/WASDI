/**
 * Created by Cristiano Nattero on 2019-02-07
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

/**
 * @author c.nattero
 *
 */
public class PaginatedQuery {
	String m_sQuery;
	String m_sOffset;
	String m_sLimit;
	String m_sSortedBy;
	String m_sOrder;
	String m_sOriginalLimit;
	
	public PaginatedQuery(String sQuery, String sOffset, String sLimit, String sSortedBy, String sOrder) {
		this.internalInit(sQuery, sOffset, sLimit, sSortedBy, sOrder, sLimit);
	}
	
	public PaginatedQuery(String sQuery, String sOffset, String sLimit, String sSortedBy, String sOrder, String sOriginalLimit) {
		this.internalInit(sQuery, sOffset, sLimit, sSortedBy, sOrder, sOriginalLimit);
	}
	
	private void internalInit(String sQuery, String sOffset, String sLimit, String sSortedBy, String sOrder, String sOriginalLimit) {
		if (sSortedBy == null) {
			sSortedBy = "ingestiondate";
		}
		if (sOrder == null) {
			sOrder = "asc";
		}
		this.m_sQuery = sQuery;
		this.m_sOffset = sOffset;
		this.m_sLimit = sLimit;
		this.m_sSortedBy = sSortedBy;
		this.m_sOrder = sOrder;
		this.m_sOriginalLimit = sOriginalLimit;
	}
	
	public String getQuery() {
		return m_sQuery;
	}
	public void setQuery(String sQuery) {
		this.m_sQuery = sQuery;
	}
	public String getOffset() {
		return m_sOffset;
	}
	public void setOffset(String sOffset) {
		this.m_sOffset = sOffset;
	}
	public String getLimit() {
		return m_sLimit;
	}
	public void setLimit(String sLimit) {
		this.m_sLimit = sLimit;
	}
	public String getSortedBy() {
		return m_sSortedBy;
	}
	public void setSortedBy(String sSortedBy) {
		this.m_sSortedBy = sSortedBy;
	}
	public String getOrder() {
		return m_sOrder;
	}
	public void setOrder(String sOrder) {
		this.m_sOrder = sOrder;
	}
	
	public String getOriginalLimit() {
		return m_sOriginalLimit;
	}

	public void setOriginalLimit(String sOriginalLimit) {
		this.m_sOriginalLimit = sOriginalLimit;
	}

}
