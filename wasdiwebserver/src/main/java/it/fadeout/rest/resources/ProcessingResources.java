package it.fadeout.rest.resources;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.protocol.HTTP;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.jexp.impl.Tokenizer;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.bc.ceres.binding.PropertyContainer;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.SnapOperatorFactory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.User;
import wasdi.shared.business.WpsProvider;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.WpsProvidersRepository;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.CalibratorSetting;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.MultilookingSetting;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.NDVISetting;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingSetting;
import wasdi.shared.utils.BandImageManager;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandImageViewModel;
import wasdi.shared.viewmodels.ColorManipulationViewModel;
import wasdi.shared.viewmodels.ListFloodViewModel;
import wasdi.shared.viewmodels.MaskViewModel;
import wasdi.shared.viewmodels.MathMaskViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductMaskViewModel;
import wasdi.shared.viewmodels.RangeMaskViewModel;
import wasdi.shared.viewmodels.SnapOperatorParameterViewModel;
import wasdi.shared.viewmodels.SnapWorkflowViewModel;
import wasdi.shared.viewmodels.WpsViewModel;

@Path("/processing")
public class ProcessingResources {
	
	@Context
	ServletConfig m_oServletConfig;
	
	//XXX replace by dependency injection
	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();
		
//	@GET
//	@Path("test")
//	@Produces({"application/xml", "application/json", "text/xml"})
//	public Response test() throws IOException
//	{
//		return ExecuteOperation("ciao", "test", "test", "test", null, LauncherOperations.TERRAIN);
//	}
	
	
	@POST
	@Path("geometric/rangeDopplerTerrainCorrection")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult TerrainCorrection(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, RangeDopplerGeocodingSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.TerrainCorrection");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.TERRAIN);
	}
	
	@POST
	@Path("radar/applyOrbit")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult ApplyOrbit(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, ApplyOrbitSetting oSetting) throws IOException
	{	
		Wasdi.DebugLog("ProcessingResources.ApplyOrbit");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.APPLYORBIT);
	}
	
	@POST
	@Path("radar/radiometricCalibration")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult Calibrate(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, CalibratorSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.Calibrate");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.CALIBRATE);
	}
	
	@POST
	@Path("radar/multilooking")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult Multilooking(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, MultilookingSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.Multilooking");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MULTILOOKING);

	}
	
	@POST
	@Path("optical/ndvi")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult NDVI(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, NDVISetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.NDVI");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.NDVI);
	}


	@GET
	@Path("parameters")
	@Produces({"application/json"})
	public SnapOperatorParameterViewModel[] OperatorParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sOperation") String sOperation) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.OperatorParameters");
		ArrayList<SnapOperatorParameterViewModel> oChoices = new ArrayList<SnapOperatorParameterViewModel>();
		
		Class oOperatorClass = SnapOperatorFactory.getOperatorClass(sOperation);
		
		Field[] aoOperatorFields = oOperatorClass.getDeclaredFields();
    	for (Field oOperatorField : aoOperatorFields) {
    		
    		if (oOperatorField.getName().equals("mapProjection")) {
    			System.out.println("ciao");
    		}
    		
			Annotation[] aoAnnotations = oOperatorField.getAnnotations();			
			for (Annotation oAnnotation : aoAnnotations) {
				
				if (oAnnotation instanceof Parameter) {
					Parameter oAnnotationParameter = (Parameter) oAnnotation;					
					
					SnapOperatorParameterViewModel oParameter = new SnapOperatorParameterViewModel();					
					oParameter.setField(oOperatorField.getName());
					
				    oParameter.setAlias(oAnnotationParameter.alias());
				    oParameter.setItemAlias(oAnnotationParameter.itemAlias());
				    oParameter.setDefaultValue(oAnnotationParameter.defaultValue());
				    oParameter.setLabel(oAnnotationParameter.label());
				    oParameter.setUnit(oAnnotationParameter.unit());
				    oParameter.setDescription(oAnnotationParameter.description());
				    oParameter.setValueSet(oAnnotationParameter.valueSet());
				    oParameter.setInterval(oAnnotationParameter.interval());
				    oParameter.setCondition(oAnnotationParameter.condition());
				    oParameter.setPattern(oAnnotationParameter.pattern());
				    oParameter.setFormat(oAnnotationParameter.format());
				    oParameter.setNotNull(oAnnotationParameter.notNull());
				    oParameter.setNotEmpty(oAnnotationParameter.notEmpty());
					
					oChoices.add(oParameter);					
				}
			}
		}
		
		
		return oChoices.toArray(new SnapOperatorParameterViewModel[oChoices.size()]);
	}
	
	
	/**
	 * Save a SNAP Workflow XML
	 * @param fileInputStream
	 * @param sSessionId
	 * @param workspace
	 * @param name
	 * @param description
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/uploadgraph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadGraph(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("name") String name, @QueryParam("description") String description) throws Exception {

		Wasdi.DebugLog("ProcessingResources.uploadGraph");
		
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

			String sUserId = oUser.getUserId();
			
			String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			File oUserWorkflowPath = new File(sDownloadRootPath+sUserId+ "/workflows/");
			
			if (!oUserWorkflowPath.exists()) {
				oUserWorkflowPath.mkdirs();
			}
			
			String sWorkflowId =  UUID.randomUUID().toString();
			File humidityTifFile = new File(sDownloadRootPath+sUserId+ "/workflows/" + sWorkflowId + ".xml");
			
			Wasdi.DebugLog("ProcessingResources.uploadGraph: workflow file Path: " + humidityTifFile.getPath());
			
			//save uploaded file
			int read = 0;
			byte[] bytes = new byte[1024];
			OutputStream out = new FileOutputStream(humidityTifFile);
			while ((read = fileInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
			
			SnapWorkflow oWorkflow = new SnapWorkflow();
			oWorkflow.setName(name);
			oWorkflow.setDescription(description);
			oWorkflow.setFilePath(humidityTifFile.getPath());
			oWorkflow.setUserId(sUserId);
			oWorkflow.setWorkflowId(sWorkflowId);
			
			SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
			oSnapWorkflowRepository.InsertSnapWorkflow(oWorkflow);			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return Response.serverError().build();
		}

				
		return Response.ok().build();
	}
	
	/**
	 * Get workflow list by user id
	 * @param sSessionId
	 * @return
	 */
	@GET
	@Path("/getgraphsbyusr")
	public ArrayList<SnapWorkflowViewModel> getWorkflowsByUser(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("ProcessingResources.getWorkflowsByUser");
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		String sUserId = oUser.getUserId();
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		ArrayList<SnapWorkflowViewModel> aoRetWorkflows = new ArrayList<>();
		
		List<SnapWorkflow> aoDbWorkflows = oSnapWorkflowRepository.GetSnapWorkflowByUser(sUserId);
		
		for (int i=0; i<aoDbWorkflows.size(); i++) {
			SnapWorkflowViewModel oVM = new SnapWorkflowViewModel();
			oVM.setName(aoDbWorkflows.get(i).getName());
			oVM.setDescription(aoDbWorkflows.get(i).getDescription());
			oVM.setWorkflowId(aoDbWorkflows.get(i).getWorkflowId());
			
			aoRetWorkflows.add(oVM);
		}
		
		return aoRetWorkflows;
	}
	
	/**
	 * Delete a workflow from id
	 * @param sSessionId
	 * @param sWorkflowId
	 * @return
	 */
	@GET
	@Path("/deletegraph")
	public Response deleteWorkflow(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String sWorkflowId) {
		Wasdi.DebugLog("ProcessingResources.deleteWorkflow");
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		String sUserId = oUser.getUserId();
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		
		SnapWorkflow oWorkflow = oSnapWorkflowRepository.GetSnapWorkflow(sWorkflowId);
		 
		if (oWorkflow == null) return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		
		if (oWorkflow.getUserId().equals(sUserId) == false)  return Response.status(Status.UNAUTHORIZED).build();

		oSnapWorkflowRepository.DeleteSnapWorkflow(sWorkflowId);
	
		return Response.ok().build();
	}
	
	/**
	 * Executes a workflow from a file stream
	 * @param fileInputStream
	 * @param sessionId
	 * @param workspace
	 * @param sourceProductName
	 * @param destinationProdutName
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/graph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public PrimitiveResult executeGraph(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName) throws Exception {

		PrimitiveResult oResult = new PrimitiveResult();
		Wasdi.DebugLog("ProcessingResources.ExecuteGraph");
		
		if (Utils.isNullOrEmpty(sessionId)) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}

		GraphSetting settings = new GraphSetting();
		String graphXml;
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
		
	}
	
	@POST
	@Path("/graph_file")
	public PrimitiveResult executeGraphFromFile(@HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName) throws Exception {

		PrimitiveResult oResult = new PrimitiveResult();
		Wasdi.DebugLog("ProcessingResources.executeGraphFromFile");

		if (Utils.isNullOrEmpty(sessionId)) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		
		GraphSetting settings = new GraphSetting();		
		String graphXml;
		
		FileInputStream fileInputStream = new FileInputStream("/usr/lib/wasdi/S1_GRD_preprocessing.xml");
		
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
	}
	
	/**
	 * Exectues a Workflow from workflow Id
	 * @param sessionId
	 * @param workspace
	 * @param sourceProductName
	 * @param destinationProdutName
	 * @param workflowId
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/graph_id")
	public PrimitiveResult executeGraphFromWorkflowId(@HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName, @QueryParam("workflowId") String workflowId) throws Exception {

		PrimitiveResult oResult = new PrimitiveResult();
		Wasdi.DebugLog("ProcessingResources.executeGraphFromWorkflowId");
		
		if (Utils.isNullOrEmpty(sessionId)) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}

		String sUserId = oUser.getUserId();
		
		GraphSetting oGraphSettings = new GraphSetting();		
		String sGraphXml;
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		SnapWorkflow oWF = oSnapWorkflowRepository.GetSnapWorkflow(workflowId);
		
		if (oWF == null) {
			oResult.setBoolValue(false);
			oResult.setIntValue(500);
			return oResult;
		}
		if (oWF.getUserId().equals(sUserId)==false) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;
		}
		
		FileInputStream fileInputStream = new FileInputStream(oWF.getFilePath());
		
		sGraphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		oGraphSettings.setGraphXml(sGraphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, oGraphSettings, LauncherOperations.GRAPH);
	}

	@GET
	@Path("/standardfilters")
	@Produces({"application/json"})
	public Map<String, Filter[]> getStandardFilters(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("ProcessingResources.GetStandardFilters");
		
		Map<String, Filter[]> filtersMap = new HashMap<String, Filter[]>();
		filtersMap.put("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
		filtersMap.put("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
		filtersMap.put("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
		filtersMap.put("Sharpen", StandardFilters.SHARPENING_FILTERS);
		filtersMap.put("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
		filtersMap.put("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
		filtersMap.put("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);
		return filtersMap;
	}
	
	@GET
	@Path("/productmasks")
	@Produces({"application/json"})
	public ArrayList<ProductMaskViewModel> getProductMasks(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("file") String sProductFile, @QueryParam("band") String sBandName, @QueryParam("workspaceId") String sWorkspaceId) throws Exception {
		
		Wasdi.DebugLog("ProcessingResources.getProductMasks");
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		String sUserId = oUser.getUserId();
		
		Wasdi.DebugLog("Params. File: " + sProductFile +" - Band: " + sBandName + " - Workspace: " + sWorkspaceId);
		
		String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
		if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
		
		String sProductFileFullPath = sDownloadRootPath+sUserId+ "/" + sWorkspaceId + "/" + sProductFile;
		
		Wasdi.DebugLog("ProcessingResources.getProductMasks: file Path: " + sProductFileFullPath);

		ArrayList<ProductMaskViewModel> asMasks = new ArrayList<ProductMaskViewModel>();
		
		try {
			Product product = ProductIO.readProduct(sProductFileFullPath);
			Band band = product.getBand(sBandName);
			
			final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
			for (int i = 0; i < maskGroup.getNodeCount(); i++) {
				final Mask mask = maskGroup.get(i);
				if (mask.getRasterWidth() == band.getRasterWidth() &&
					mask.getRasterHeight() == band.getRasterHeight()) {
					ProductMaskViewModel vm = new ProductMaskViewModel();
					vm.setName(mask.getName());
					vm.setDescription(mask.getDescription());
					vm.setMaskType(mask.getImageType().getName());
					vm.setColorRed(mask.getImageColor().getRed());
					vm.setColorGreen(mask.getImageColor().getGreen());
					vm.setColorBlue(mask.getImageColor().getBlue());
					asMasks.add(vm);
				}
			}			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			Wasdi.DebugLog(oEx.getMessage());
		}

		return asMasks;
	}

	
	@GET
	@Path("/productcolormanipulation")
	@Produces({"application/json"})
	public ColorManipulationViewModel getColorManipulation(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("file") String sProductFile, @QueryParam("band") String sBandName, 
			@QueryParam("accurate") boolean accurate, @QueryParam("workspaceId") String sWorkspaceId) throws Exception {

		Wasdi.DebugLog("ProcessingResources.getColorManipulation");
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		String sUserId = oUser.getUserId();
		
		Wasdi.DebugLog("ProcessingResources.getColorManipulation. Params. File: " + sProductFile +" - Band: " + sBandName + " - Workspace: " + sWorkspaceId);
		
		String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
		if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
		
		String sProductFileFullPath = sDownloadRootPath+sUserId+ "/" + sWorkspaceId + "/" + sProductFile;
		
//		String sProductFileFullPath = "/home/doy/tmp/wasdi/tmp/S2B_MSIL1C_20180117T102339_N0206_R065_T32TMQ_20180117T122826.zip";
		
		Wasdi.DebugLog("ProcessingResources.getColorManipulation: file Path: " + sProductFileFullPath);

		Product product = ProductIO.readProduct(sProductFileFullPath);
		BandImageManager manager = new BandImageManager(product);	
		return manager.getColorManipulation(sBandName, accurate);
		
	}
	
	@POST
	@Path("/bandimage")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getBandImage(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String workspace,
			BandImageViewModel model) throws IOException {
		
		Wasdi.DebugLog("ProcessingResources.getBandImage");

		// Check user session
		String userId = AcceptedUserAndSession(sSessionId);
		if (Utils.isNullOrEmpty(userId)) return Response.status(401).build();

		
		// Init the registry for JAI
		OperationRegistry operationRegistry = JAI.getDefaultInstance().getOperationRegistry();
		RegistryElementDescriptor oDescriptor = operationRegistry.getDescriptor("rendered", "Paint");
		
		if (oDescriptor==null) {
			System.out.println("getBandImage: REGISTER Descriptor!!!!");
			try {
				operationRegistry.registerServices(this.getClass().getClassLoader());
			} catch (Exception e) {
				e.printStackTrace();
			}
			oDescriptor = operationRegistry.getDescriptor("rendered", "Paint");
			
			
			IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
		}
		
		
		// Get Download File Path
        String downloadPath = m_oServletConfig.getInitParameter("DownloadRootPath");
        File productFile = new File(new File(new File(downloadPath, userId), workspace), model.getProductFileName());
		
//		File productFile = new File("/home/doy/tmp/wasdi/tmp/S2B_MSIL1C_20180117T102339_N0206_R065_T32TMQ_20180117T122826.zip");
        
        if (!productFile.exists()) {
        	System.out.println("ProcessingResource.getBandImage: FILE NOT FOUND: " + productFile.getAbsolutePath());
        	return Response.status(500).build();
        }
        
        Product oSNAPProduct = ProductIO.readProduct(productFile);
        
        if (oSNAPProduct == null) {
        	Wasdi.DebugLog("ProcessingResources.getBandImage: SNAP product is null, impossibile to read. Return");
        	return Response.status(500).build();
        } else {
        	Wasdi.DebugLog("ProcessingResources.getBandImage: product read");
        }
        
		BandImageManager manager = new BandImageManager(oSNAPProduct);
		
		RasterDataNode raster = null;
		
		if (model.getFilterVM() != null) {
			Filter filter = model.getFilterVM().getFilter();
			FilterBand filteredBand = manager.getFilterBand(model.getBandName(), filter, model.getFilterIterationCount());
			if (filteredBand == null) {
				Wasdi.DebugLog("ProcessingResource.getBandImage: CANNOT APPLY FILTER TO BAND " + model.getBandName());
	        	return Response.status(500).build();
			}
			raster = filteredBand;
		} else {
			raster = oSNAPProduct.getBand(model.getBandName());
		}
		
		if (model.getVp_x()<0||model.getVp_y()<0||model.getImg_w()<=0||model.getImg_h()<=0) {
			Wasdi.DebugLog("ProcessingResources.getBandImage: Invalid Parameters: VPX= " + model.getVp_x() +" VPY= "+ model.getVp_y() +" VPW= "+ model.getVp_w() +" VPH= "+ model.getVp_h() + " OUTW = " + model.getImg_w() + " OUTH = " +model.getImg_h() );
			return Response.status(500).build();
		} else {
			Wasdi.DebugLog("ProcessingResources.getBandImage: parameters OK");
		}
		
		Rectangle vp = new Rectangle(model.getVp_x(), model.getVp_y(), model.getVp_w(), model.getVp_h());
		Dimension imgSize = new Dimension(model.getImg_w(), model.getImg_h());
		
		//apply product masks
		List<ProductMaskViewModel> productMasksModels = model.getProductMasks();
		if (productMasksModels!=null) {
			for (ProductMaskViewModel maskModel : productMasksModels) {
				Mask mask = oSNAPProduct.getMaskGroup().get(maskModel.getName());
				if (mask == null) {
					Wasdi.DebugLog("ProcessingResources.getBandImage: cannot find mask by name: " + maskModel.getName());
				} else {
					//set the user specified color
					mask.setImageColor(new Color(maskModel.getColorRed(), maskModel.getColorGreen(), maskModel.getColorBlue()));
					mask.setImageTransparency(maskModel.getTransparency());
					raster.getOverlayMaskGroup().add(mask);
				}
			}
		}
		
		//applying range masks
		List<RangeMaskViewModel> rangeMasksModels = model.getRangeMasks();
		if (rangeMasksModels != null) {
			for (RangeMaskViewModel maskModel : rangeMasksModels) {

				Mask mask = createMask(oSNAPProduct, maskModel, Mask.RangeType.INSTANCE);
				
				String externalName = Tokenizer.createExternalName(model.getBandName());
		        PropertyContainer imageConfig = mask.getImageConfig();
		        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, maskModel.getMin());
		        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, maskModel.getMax());
		        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, externalName);
		        oSNAPProduct.addMask(mask);
				raster.getOverlayMaskGroup().add(mask);
			}
		}
		
		//applying math masks
		List<MathMaskViewModel> mathMasksModels = model.getMathMasks();
		if (mathMasksModels != null) {
			for (MathMaskViewModel maskModel : mathMasksModels) {
				
				Mask mask = createMask(oSNAPProduct, maskModel, Mask.BandMathsType.INSTANCE);
				
				PropertyContainer imageConfig = mask.getImageConfig();
		        imageConfig.setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, maskModel.getExpression());
		        oSNAPProduct.addMask(mask);
				raster.getOverlayMaskGroup().add(mask);
			}
		}

		//applying color manipulation
		
		ColorManipulationViewModel colorManiputalion = model.getColorManiputalion();
		if (colorManiputalion!=null) {
			manager.applyColorManipulation(raster, colorManiputalion);
		}
		
		//creating the image
		BufferedImage img;
		try {
			img = manager.buildImageWithMasks(raster, imgSize, vp, colorManiputalion==null);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(500).build();
		}
		
		if (img == null) {
			Wasdi.DebugLog("ProcessingResource.getBandImage: img null");
			return Response.status(500).build();
		}
		
		Wasdi.DebugLog("ProcessingResource.getBandImage: Generated image for band " + model.getBandName() + " X= " + model.getVp_x() + " Y= " + model.getVp_y() + " W= " + model.getVp_w() + " H= "  + model.getVp_h());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ImageIO.write(img, "jpg", baos);
	    byte[] imageData = baos.toByteArray();
		
		return Response.ok(imageData).build();
	}

	private Mask createMask(Product oSNAPProduct, MaskViewModel maskModel, Mask.ImageType type) {
		String maskName = UUID.randomUUID().toString();
		Dimension maskSize = new Dimension(oSNAPProduct.getSceneRasterWidth(), oSNAPProduct.getSceneRasterHeight());
		Mask mask = new Mask(maskName, maskSize.width, maskSize.height, type);
		mask.setImageColor(new Color(maskModel.getColorRed(), maskModel.getColorGreen(), maskModel.getColorBlue()));
		mask.setImageTransparency(maskModel.getTransparency());
		return mask;
	}
	
	

	@POST
	@Path("/assimilation")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public String Assimilation(
			@FormDataParam("humidity") InputStream humidityFile,
			@FormDataParam("humidity") FormDataContentDisposition humidityFileMetaData,
			@HeaderParam("x-session-token") String sessionId,
			@QueryParam("midapath") String midaPath) {
		
		Wasdi.DebugLog("ProcessingResource.Assimilation");

		User user = Wasdi.GetUserFromSession(sessionId);
		try {

			//check authentication
			if (user == null || Utils.isNullOrEmpty(user.getUserId())) {
				return null;				
			}
			
			//build and check paths
			File assimilationWD = new File(m_oServletConfig.getInitParameter("AssimilationWDPath"));
			if (!assimilationWD.isDirectory()) {				
				System.out.println("ProcessingResource.Assimilation: ERROR: Invalid directory: " + assimilationWD.getAbsolutePath());
				throw new InternalServerErrorException("invalid directory in assimilation settings");				
			}						
			File midaTifFile = new File(midaPath);
			if (!midaTifFile.canRead()) {
				System.out.println("ProcessingResource.Assimilation: ERROR: Invalid mida path: " + midaTifFile.getAbsolutePath());
				throw new InternalServerErrorException("invalid path in assimilation settings");
			}						
			File humidityTifFile = new File(assimilationWD, UUID.randomUUID().toString() + ".tif");
			File resultDir = new File(m_oServletConfig.getInitParameter("AssimilationResultPath"));
			File resultTifFile = new File(resultDir, UUID.randomUUID().toString() + ".tif");
			
			
			//save uploaded file			
			int read = 0;
			byte[] bytes = new byte[1024];
			OutputStream out = new FileOutputStream(humidityTifFile);
			while ((read = humidityFile.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();

			//execute assimilation
			if (launchAssimilation(midaTifFile, humidityTifFile, resultTifFile)) {
				String url = "wasdidownloads/" + resultTifFile.getName();
				return url;				
			}
			
			throw new InternalServerErrorException("unable to execute assimilation");
			
		} catch (Exception e) {
			System.out.println("ProcessingResource.Assimilation: error launching assimilation " + e.getMessage());
			e.printStackTrace();
			throw new InternalServerErrorException("error launching assimilation: " + e.getMessage());
		}

	}
	
	
	
	@GET
	@Path("/saba")
	@Produces({"application/json"})
	public PrimitiveResult Saba(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFileName, @QueryParam("workspaceId") String sWorkspaceId) {
		
		PrimitiveResult oResult = new PrimitiveResult();
		
		Wasdi.DebugLog("ProcessingResource.Saba");

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		try {

			//check authentication
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				oResult.setBoolValue(false);
				oResult.setIntValue(404);
				return oResult;				
			}
			
			Wasdi.DebugLog("ProcessingResource.Saba: INPUT FILE " + sFileName);
			
			Wasdi.DebugLog("ProcessingResource.Saba: launching ENVI SABA Processor");
			
			//execute assimilation
			if (launchSaba(sFileName, sWorkspaceId)) {
				Wasdi.DebugLog("ProcessingResource.Saba: ok return");
				String sOutputFile = "";
				
				if (sFileName.startsWith("CSK")) {
					sOutputFile = "Mappa_" + sFileName.substring(0, 41) +".tif";
				}
				else if (sFileName.startsWith("S1A")) {
					sOutputFile = "Mappa_" + sFileName.substring(0, 32) +".tif";
				}
				
				oResult.setStringValue(sOutputFile);
				
				oResult.setBoolValue(true);
				oResult.setIntValue(200);				
			}
			else {
				Wasdi.DebugLog("ProcessingResource.Saba: error, return");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);				
			}
		} catch (Exception e) {
			System.out.println("ProcessingResource.Saba: error launching Saba " + e.getMessage());
			e.printStackTrace();
			oResult.setBoolValue(false);
			oResult.setIntValue(500);				
			return oResult;
		}
		
		return oResult;
	}
	
	@GET
	@Path("/ddspublishsaba")
	@Produces({"application/json"})
	public PrimitiveResult DDSPublishSaba(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFileName, @QueryParam("workspaceId") String sWorkspaceId) {
		
		PrimitiveResult oResult = new PrimitiveResult();
		
		Wasdi.DebugLog("ProcessingResource.DDSPublishSaba");

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		try {

			//check authentication
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				oResult.setBoolValue(false);
				oResult.setIntValue(404);
				return null;				
			}
			
			Wasdi.DebugLog("ProcessingResource.DDSPublishSaba: INPUT FILE " + sFileName);
			
			String sAccount = oUser.getUserId();		
			
			String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			File oUserBaseDir = new File(sDownloadRootPath+sAccount+ "/" +sWorkspaceId+"/");
			File oFilePath = new File(oUserBaseDir, sFileName);
			
			Wasdi.DebugLog("ProcessingResource.DDSPublishSaba: Full Path " + oFilePath.getPath());
			
			String sDestinationFile = m_oServletConfig.getInitParameter("DDSPath");
			String [] asFileNameSplitted = sFileName.split("_");
			
			if (asFileNameSplitted==null) {
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				oResult.setStringValue("Impossible split file _");
				return oResult;					
			}
			
			if (asFileNameSplitted.length<1) {
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				oResult.setStringValue("Impossible split file _");
				return oResult;						
			}
			
			String sDate = asFileNameSplitted[asFileNameSplitted.length-1];
			
			String sDDSFileFolder = sDestinationFile+sDate.substring(0, 4) + "/" + sDate.substring(4,6)+ "/" + sDate.substring(6,8)+"/";
			
			
			Wasdi.DebugLog("ProcessingResource.DDSPublishSaba: Output File Path " + sDDSFileFolder);
			
			File oOutputFile = new File(sDDSFileFolder);
			
			oOutputFile.mkdirs();
			
			String sOutputFileOnlyName = "FLOOD_" + sDate.replace("T", "") + "f";
			
			Wasdi.DebugLog("ProcessingResource.DDSPublishSaba: Output File Name " + sOutputFileOnlyName);
			
			oOutputFile = new File(sDDSFileFolder+sOutputFileOnlyName);
			
			FileUtils.copyFile(oFilePath, oOutputFile);
			
			Wasdi.DebugLog("ProcessingResource.DDSPublishSaba: Copy Done");
				
			oResult.setStringValue(sDDSFileFolder);
			oResult.setBoolValue(true);
			oResult.setIntValue(200);	
			
		} catch (Exception e) {
			System.out.println("ProcessingResource.DDSPublishSaba: error publishing Saba Output " + e.getMessage());
			e.printStackTrace();
			oResult.setBoolValue(false);
			oResult.setIntValue(500);				
			return oResult;
		}
		
		return oResult;		
	}

	
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadModel(@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileMetaData, @HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) throws Exception
	{ 
		Wasdi.DebugLog("ProcessingResource.UploadModel");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
		if (!sDownloadRootPath.endsWith("/"))
			sDownloadRootPath += "/";

		String sDownloadPath = sDownloadRootPath + oUser.getUserId()+ "/" + sWorkspaceId + "/" + "CONTINUUM";

		if(!Files.exists(Paths.get(sDownloadPath)))
		{
			if (Files.createDirectories(Paths.get(sDownloadPath))== null)
			{
				System.out.println("ProcessingResource.uploadMapFile: Directory " + sDownloadPath + " not created");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

		}

		try
		{
			int read = 0;
			byte[] bytes = new byte[1024];

			OutputStream out = new FileOutputStream(new File(sDownloadPath + "/" + fileMetaData.getFileName()));
			while ((read = fileInputStream.read(bytes)) != -1) 
			{
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();

			/* TODO: Abilitare questa parte se si vuole far partire l'assimilazione dopo l'upload del file (E' ancora da testare) 
			 * 
			 * 
			String sAssimilationContinuumPath =   m_oServletConfig.getInitParameter("AssimilationContinuumPath");
			String sMulesmeEstimatePath =   m_oServletConfig.getInitParameter("MulesmeStimePath");
			if (!sAssimilationContinuumPath.endsWith("/"))
				sAssimilationContinuumPath += "/";
			if (!sMulesmeEstimatePath.endsWith("/"))
				sMulesmeEstimatePath += "/";


			//Continuum format SoilMoistureItaly_20160801230000
			//Get continuum date
			SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyyMMdd");
			String sFileName = fileMetaData.getFileName();
			String sDate = sFileName.split("_")[1].substring(0, 7);  
			Calendar oContinuumDate = Calendar.getInstance();
			Calendar oMulesmeDate = Calendar.getInstance();
			oContinuumDate.setTime(oDateFormat.parse(sDate));
			oMulesmeDate.setTime(oDateFormat.parse(sDate));
			oMulesmeDate.add(Calendar.DATE, 1);  // number of days to add
			String sMulesmeDate = oDateFormat.format(oMulesmeDate); 

			//Search in catalog soil moisture map with date = continuum date + 1 day
			CatalogRepository oRepo = new CatalogRepository();
			Catalog oCatalog = oRepo.GetCatalogsByDate(sMulesmeDate);
			if (oCatalog != null)
			{
				//Copy file into Mulesme Stime path
				Files.move(Paths.get(oCatalog.getFilePath()), Paths.get(sMulesmeEstimatePath));
				//Copy file from CONTINUUM to assimilation path
				Files.move(Paths.get(sDownloadPath + "/" + fileMetaData.getFileName()), Paths.get(sAssimilationContinuumPath));
				//launch assimilation
				LaunchAssimilation();
			}
			 */



		} catch (IOException e) 
		{
			throw new WebApplicationException("CatalogResources.uploadModel: Error while uploading file. Please try again !!");
		}




		return Response.ok().build();
	}
	
	@GET
	@Path("/WPSlist")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<WpsViewModel> getWpsList( @HeaderParam("x-session-token") String sSessionId ){
		Wasdi.DebugLog("ProcessingResource.getWpsList");
		//TODO validate input
		WpsProvidersRepository oWPSrepo = new WpsProvidersRepository();
		ArrayList<WpsProvider> aoWPSProviders = oWPSrepo.getWpsList();
		if(null!=aoWPSProviders) {
			ArrayList<WpsViewModel> aoResult = new ArrayList<WpsViewModel>();
			for (WpsProvider oWpsProvider : aoWPSProviders) {
				if(null!=oWpsProvider) {
					WpsViewModel oWpsViewModel = new WpsViewModel();
					oWpsViewModel.setAddress(oWpsProvider.getAddress());
					aoResult.add(oWpsViewModel);
				}
			}
			return aoResult;
		}
		return null;
	}

	private boolean launchAssimilation(File midaTifFile, File humidityTifFile, File resultTifFile) {

		try {
			
			String cmd[] = new String[] {
					m_oServletConfig.getInitParameter("AssimilationScript"),
					midaTifFile.getAbsolutePath(),
					humidityTifFile.getAbsolutePath(),
					resultTifFile.getAbsolutePath()
			};
			
			System.out.println("ProcessingResource.LaunchAssimilation: shell exec " + Arrays.toString(cmd));

			Process proc = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while((line=input.readLine()) != null) {
            	System.out.println("ProcessingResource.LaunchAssimilation: Assimilation stdout: " + line);
            }
			if (proc.waitFor() != 0) return false;
		} catch (Exception oEx) {
			System.out.println("ProcessingResource.LaunchAssimilation: error during assimilation process " + oEx.getMessage());
			oEx.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Launch Saba ENVI process
	 * @param sInputFile
	 * @param sWorkspaceId
	 * @return
	 */
	private boolean launchSaba(String sInputFile, String sWorkspaceId) {

		try {
			
			String cmd[] = new String[] {
					m_oServletConfig.getInitParameter("SabaScript")
			};
			
			String sParamFile = m_oServletConfig.getInitParameter("SabaParam");
			
			Wasdi.DebugLog("ProcessingResource.launchSaba ParamFile " + sParamFile);
			
			System.out.println("ProcessingResource.launchSaba: shell exec " + Arrays.toString(cmd));
			File oFile = new File(sParamFile);
			
			if (!oFile.exists()) {				
				oFile.mkdirs();
			}
			
			BufferedWriter oWriter = new BufferedWriter(new FileWriter(oFile));
			oWriter.write("USER,"+m_oServletConfig.getInitParameter("SabaUser"));
			oWriter.newLine();
			oWriter.write("PASSWORD,"+m_oServletConfig.getInitParameter("SabaPassword"));
			oWriter.newLine();
			oWriter.write("FILE,"+sInputFile);
			oWriter.newLine();
			oWriter.write("WORKSPACE,"+sWorkspaceId);
			oWriter.newLine();
			oWriter.flush();		
			oWriter.close();
			
			

			Process proc = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while((line=input.readLine()) != null) {
            	System.out.println("ProcessingResource.launchSaba: envi stdout: " + line);
            }
			if (proc.waitFor() != 0) return false;
		} catch (Exception oEx) {
			System.out.println("ProcessingResource.launchSaba: error during saba process " + oEx.getMessage());
			oEx.printStackTrace();
			return false;
		}

		return true;
	}
	
	private String AcceptedUserAndSession(String sSessionId) {
		//Check user
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		return oUser.getUserId();
	}
	
	private PrimitiveResult ExecuteOperation(String sSessionId, String sSourceProductName, String sDestinationProductName, String sWorkspaceId, ISetting oSetting, LauncherOperations operation) {
		
		PrimitiveResult oResult = new PrimitiveResult();
		String sProcessId = "";
		String sProcessObjId = "";
		
		String sUserId = AcceptedUserAndSession(sSessionId);
		
		if (Utils.isNullOrEmpty(sUserId)) {
			oResult.setIntValue(401);
			oResult.setBoolValue(false);
			
			return oResult;
		}
		
		try {
			//Update process list
			
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcess = new ProcessWorkspace();
			
			try
			{
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(operation.name());
				oProcess.setProductName(sSourceProductName);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				sProcessObjId = Utils.GetRandomName();
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				sProcessId = oRepository.InsertProcessWorkspace(oProcess);
			}
			catch(Exception oEx){
				System.out.println("SnapOperations.ExecuteOperation: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				oResult.setBoolValue( false);
				oResult.setIntValue(500);
				return oResult;
			}

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			
			if (!(sPath.endsWith("\\") || sPath.endsWith("/"))){
				sPath += "/";
			}
			sPath = sPath + oProcess.getProcessObjId();

			
			OperatorParameter oParameter = GetParameter(operation); 
			oParameter.setSourceProductName(sSourceProductName);
			oParameter.setDestinationProductName(sDestinationProductName);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(oProcess.getProcessObjId());
			if (oSetting != null) oParameter.setSettings(oSetting);	
			//TODO move it before inserting the new process into DB
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

		} catch (IOException e) {
			e.printStackTrace();
			oResult.setBoolValue( false);
			oResult.setIntValue(500);
			return oResult;
		} catch (Exception e) {
			e.printStackTrace();
			oResult.setBoolValue( false);
			oResult.setIntValue(500);			
			return oResult;
		}
		
		oResult.setBoolValue( true);
		oResult.setIntValue(200);
		oResult.setStringValue(sProcessObjId);
		
		return oResult;
	}

	
	private OperatorParameter GetParameter(LauncherOperations op)
	{
		switch (op) {
		case APPLYORBIT:
			return new ApplyOrbitParameter();
		case CALIBRATE:
			return new CalibratorParameter();
		case MULTILOOKING:
			return new MultilookingParameter();
		case TERRAIN:
			return new RangeDopplerGeocodingParameter();
		case NDVI:
			return new NDVIParameter();
			
		case GRAPH:
			return new GraphParameter();
		default:
			return null;
			
		}
	}
	
	/**
	 * Runs a dummy IDL script. Used to test the setup and as a stub to implement the launch of new IDL scripts 
	 * @param sSessionId a valid session identifier
	 * @return BAD_REQUEST if null request, UNAUTHORIZED if session is not valid, OK after execution of the script
	 */
	@GET
	@Path("/idlDemo") 
	@Produces({"application/json"})
	public PrimitiveResult idlDemo(
			@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("ProcessingResource.idlDemo");
	
		
		if(null != sSessionId ) {
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(false);
			oResult.setIntValue(Integer.parseInt(Response.Status.BAD_REQUEST.toString()));
			return oResult;
		}
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		//check session validity
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(false);
			oResult.setIntValue(Integer.parseInt(Response.Status.UNAUTHORIZED.toString()));
			return oResult;				
		}
		Wasdi.DebugLog("ProcessingResource.idlDemo: ok valid session, let's go");

		try {
			String cmd[] = new String[] {
					m_oServletConfig.getInitParameter("IdlDemoScript")
			};
			
			Wasdi.DebugLog("ProcessingResource.idlDemo " + cmd[0] );
			
			System.out.println("ProcessingResource.idlDemo: shell exec " + Arrays.toString(cmd));
			Process proc = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while((line=input.readLine()) != null) {
            	System.out.println("ProcessingResource.idlDemo: envi stdout: " + line);
            }
			if (proc.waitFor() != 0) {
				return PrimitiveResult.getInvalidInstance();
			}
		} catch (Exception oEx) {
			System.out.println("ProcessingResource.idlDemo: error happened" + oEx.getMessage());
			oEx.printStackTrace();
			return PrimitiveResult.getInvalidInstance();
		}

		System.out.println("ProcessingResource.idlDemo: about to respond and close");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(true);
		oResult.setIntValue(Integer.parseInt(Response.Status.OK.toString()));
		oResult.setStringValue("IDL code executed");
		return oResult;
	}
	
	/**
	 * Runs a the IDL script implementation of the LIST flood algorithm with sample input files
	 * Shows a practical usage of WASDL 
	 * @param sSessionId a valid session identifier
	 * @return BAD_REQUEST if null request, UNAUTHORIZED if session is not valid, OK after execution of the script
	 */
	
	/*
	@GET
	@Path("/listFloodDemo")
	@Produces({"application/json"})
	public PrimitiveResult listFloodDemo(
			@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("ProcessingResource.listFloodDemo");
		try {
			//get standard demo files and parameters
			String sFileName = m_oServletConfig.getInitParameter("ListFloodDemo.fileName");
			String sWorkspaceId = m_oServletConfig.getInitParameter("ListFloodDemo.workspaceId");
			return listflood(sSessionId, sFileName, sWorkspaceId);
		} catch (Exception e) {
			e.printStackTrace();
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setIntValue(Integer.parseInt(Response.Status.INTERNAL_SERVER_ERROR.toString()));
			return oResult;
		}
		
	}
	*/
	/**
	 * Runs a the IDL script implementation of the LIST flood algorithm on specified files
	 * @param sSessionId a valid session identifier
	 * @param sFileName input file
	 * @param a workspase identifier
	 * @return BAD_REQUEST if null request, UNAUTHORIZED if session is not valid, OK after execution of the script
	 */
	@POST
	@Path("/listflood")
	@Produces({"application/json"})
	public PrimitiveResult listflood(
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspaceId") String sWorkspaceId,
			ListFloodViewModel oListFloodViewModel) {
		
		Wasdi.DebugLog("ProcessingResource.algList");
		
		if(null != sSessionId ) {
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(false);
			oResult.setIntValue(Integer.parseInt(Response.Status.BAD_REQUEST.toString()));
			return oResult;
		}
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		try {
			//check authentication
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
				oResult.setIntValue(Integer.parseInt(Response.Status.UNAUTHORIZED.toString()));
				return oResult;				
			}
			
			Wasdi.DebugLog("ProcessingResource.algList: REF FILE " + oListFloodViewModel.getReferenceFile());
			Wasdi.DebugLog("ProcessingResource.algList: POST EVENT FILE " + oListFloodViewModel.getPostEventFile());
			Wasdi.DebugLog("ProcessingResource.list: launching ENVI LIST Processor");
						
			//try execute algorithm
			if (launchList(oListFloodViewModel, sWorkspaceId)) {
				Wasdi.DebugLog("ProcessingResource.algList: ok return");
				//TODO read value somewhere (input argument? config file?)

				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setBoolValue(true);
				oResult.setIntValue(Integer.parseInt(Response.Status.OK.toString()));
				return oResult;
			}
			else {
				Wasdi.DebugLog("ProcessingResource.algList: error, return");
				PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
				oResult.setBoolValue(false);
				oResult.setIntValue(Integer.parseInt(Response.Status.INTERNAL_SERVER_ERROR.toString()));
				return oResult;
			}
		} catch (Exception e) {
			System.out.println("ProcessingResource.algList: error launching list " + e.getMessage());
			e.printStackTrace();
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setBoolValue(false);
			oResult.setIntValue(Integer.parseInt(Response.Status.INTERNAL_SERVER_ERROR.toString()));				
			return oResult;
		}
	
	}
	
	/**
	 * Launch LIST ENVI process
	 * @param sReferenceFile 
	 * @param sWorkspaceId
	 * @return
	 */
	private boolean launchList(ListFloodViewModel oListFloodViewModel, String sWorkspaceId) {

		try {
			String cmd[] = new String[] {
					m_oServletConfig.getInitParameter("ListScript")
			};
			
			Wasdi.DebugLog("ProcessingResource.launchList " + cmd[0] );
			
			String sParamFile = m_oServletConfig.getInitParameter("ListParam");
			
			Wasdi.DebugLog("ProcessingResource.launchList ParamFile " + sParamFile);
			
			File oFile = new File(sParamFile);
			
			BufferedWriter oWriter = new BufferedWriter(new FileWriter(oFile));
			if(null!= oWriter) {
				Wasdi.DebugLog("ProcessingResource.launchList: BufferedWriter created");
				//oWriter.write("USER,"+m_oServletConfig.getInitParameter("ListUser"));
				//oWriter.newLine();
				//oWriter.write("PASSWORD,"+m_oServletConfig.getInitParameter("ListPassword"));
				//oWriter.newLine();
				oWriter.write(oListFloodViewModel.getPostEventFile());
				oWriter.newLine();
				oWriter.write(oListFloodViewModel.getReferenceFile());
				oWriter.newLine();
				oWriter.newLine();
				oWriter.newLine();
				oWriter.write("S1A_IW_GRDH_1SDV_20180904T174542_20180904T174607_023552_0290A2_A262_HSBA_MASK.tif");
				oWriter.newLine();
				oWriter.write("S1A_IW_GRDH_1SDV_20180904T174542_20180904T174607_023552_0290A2_A262_flood_map.tif");
				oWriter.newLine();
				oWriter.write("4");
				oWriter.newLine();
				oWriter.write("2.4");
				oWriter.newLine();
				oWriter.write("1000");
				oWriter.newLine();
				oWriter.write("10");
				oWriter.newLine();
				oWriter.flush();		
				oWriter.close();
			}
			
			System.out.println("ProcessingResource.launchList: shell exec " + Arrays.toString(cmd));
			Process oProc = Runtime.getRuntime().exec(cmd);
			BufferedReader oInput = new BufferedReader(new InputStreamReader(oProc.getInputStream()));
			
            String sLine;
            while((sLine=oInput.readLine()) != null) {
            	System.out.println("ProcessingResource.launchList: envi stdout: " + sLine);
            }
			if (oProc.waitFor() != 0) return false;
		} catch (Exception oEx) {
			System.out.println("ProcessingResource.launchList: error during list process " + oEx.getMessage());
			oEx.printStackTrace();
			return false;
		}

		return true;
	}
	
	
	
}
