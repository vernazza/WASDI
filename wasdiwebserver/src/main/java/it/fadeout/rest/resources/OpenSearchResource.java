package it.fadeout.rest.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import it.fadeout.Wasdi;
import it.fadeout.opensearch.OpenSearchQuery;
import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;

@Path("/search")
public class OpenSearchResource {
	
	@Context
	ServletConfig m_oServletConfig;

	
	@GET
	@Path("/sentinel/result")
	@Produces({"application/xml", "application/json", "text/html"})
	public String SearchSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery, @QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit, @QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder )
	{
		System.out.println("OpenSearchResource: Search Sentinel");
		
		//if (Utils.isNullOrEmpty(sSessionId)) return null;
		
		//User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		//if (oUser==null) return null;
		//if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		try {
			HashMap<String, String> asParameterMap = new HashMap<>();
			ArrayList<String> asParams = new ArrayList<>();
			if (sOffset != null)
				asParameterMap.put("offset", sOffset);
			if (sLimit != null)
				asParameterMap.put("limit", sLimit);
			if (sSortedBy != null)
				asParameterMap.put("sortedby", sSortedBy);
			if (sOrder != null)
				asParameterMap.put("order", sOrder);
			
			
			asParameterMap.put("provider", m_oServletConfig.getInitParameter("OSProvider"));
			asParameterMap.put("OSUser", m_oServletConfig.getInitParameter("OSUser"));
			asParameterMap.put("OSPwd", m_oServletConfig.getInitParameter("OSPwd"));
			
			System.out.println("Search Sentinel: execute query " + sQuery);
			
			//return OpenSearchQuery.ExecuteQuerySentinel(sQuery, asParams.toArray(new String[asParams.size()]));
			return OpenSearchQuery.ExecuteQuery(sQuery, asParameterMap);
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	@GET
	@Path("/sentinel/count")
	@Produces({"application/xml", "application/json", "text/html"})
	public String GetProductsCountSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery)
	{
		System.out.println("OpenSearchResource: GetProductsCountSentinel");
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		try {
			System.out.println("OpenSearchResource.GetProductsCount: Query: " + sQuery);
			return OpenSearchQuery.ExecuteQueryCount(sQuery, 
					m_oServletConfig.getInitParameter("OSUser"), 
					m_oServletConfig.getInitParameter("OSPwd"),
					m_oServletConfig.getInitParameter("OSProvider"));
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
}
