package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.nfs.orbits.CoverageTool.Polygon;
import org.nfs.orbits.CoverageTool.apoint;
import org.nfs.orbits.sat.CoverageSwathResult;
import org.nfs.orbits.sat.SatFactory;
import org.nfs.orbits.sat.SatSensor;
import org.nfs.orbits.sat.Satellite;
import org.nfs.orbits.sat.SwathArea;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.ColorMode;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.StyleState;
import it.fadeout.Wasdi;
import it.fadeout.business.InstanceFinder;
import it.fadeout.viewmodels.CoverageSwathResultViewModel;
import it.fadeout.viewmodels.OpportunitiesSearchViewModel;
import it.fadeout.viewmodels.SatelliteOrbitResultViewModel;
import it.fadeout.viewmodels.SatelliteResourceViewModel;
import satLib.astro.time.Time;
import wasdi.shared.business.User;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.Utils;

@Path("/searchorbit")
public class OpportunitySearchResource {
	@Context
	ServletConfig m_oServletConfig;

	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();

	@POST
	@Path("/search")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<CoverageSwathResultViewModel> search(@HeaderParam("x-session-token") String sSessionId,
			OpportunitiesSearchViewModel OpportunitiesSearch) {
		Wasdi.DebugLog("OpportunitySearchResource.Search( " + sSessionId + ", ... )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<CoverageSwathResultViewModel> aoCoverageSwathResultViewModels = new ArrayList<CoverageSwathResultViewModel>();

		try {
			if (oUser == null) {
				return aoCoverageSwathResultViewModels;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoCoverageSwathResultViewModels;
			}

			// set nfs properties download
			String userHome = System.getProperty("user.home");
			String Nfs = System.getProperty("nfs.data.download");
			if (Nfs == null)
				System.setProperty("nfs.data.download", userHome + "/nfs/download");

			Wasdi.DebugLog("nfs dir " + System.getProperty("nfs.data.download"));

			int iIdCoverageCounter = 1;

			ArrayList<CoverageSwathResult> aoCoverageSwathResult = new ArrayList<>();
			aoCoverageSwathResult = InstanceFinder.findSwatsByFilters(OpportunitiesSearch);

			if (aoCoverageSwathResult == null)
				return null;

			// For each Swath Result
			for (CoverageSwathResult oSwatResul : aoCoverageSwathResult) {
				// Get View Model and Childs
				ArrayList<CoverageSwathResultViewModel> aoModels = getSwatViewModelFromResult(oSwatResul);
				for (CoverageSwathResultViewModel oCoverageSwathResultViewModel : aoModels) {
					oCoverageSwathResultViewModel.IdCoverageSwathResultViewModel = iIdCoverageCounter;
					iIdCoverageCounter++;
					aoCoverageSwathResultViewModels.add(oCoverageSwathResultViewModel);
				}

				CoverageSwathResultViewModel oSwathResultViewModel = getCoverageSwathResultViewModelFromCoverageSwathResult(
						oSwatResul);
				oSwathResultViewModel.FrameFootPrint = "";
				oSwathResultViewModel.IdCoverageSwathResultViewModel = iIdCoverageCounter;
				iIdCoverageCounter++;

				aoCoverageSwathResultViewModels.add(oSwathResultViewModel);
			}

		} catch (Exception oEx) {
			Wasdi.DebugLog("OpportunitySearchResource.Search: Error searching opportunity " + oEx);
		}

		return aoCoverageSwathResultViewModels;
	}

	/**
	 * Converts the Orbit Lib Ouptut in a WASDI View Model
	 * 
	 * @param oSwath CoverageSwathResult of the Orbit Lib
	 * @return CoverageSwathResultViewModel representig the oSwath entity
	 */
	public CoverageSwathResultViewModel getCoverageSwathResultViewModelFromCoverageSwathResult(
			CoverageSwathResult oSwath) {
		Wasdi.DebugLog("OpportunitySearchResource.getCoverageSwathResultViewModelFromCoverageSwathResult");
		CoverageSwathResultViewModel oVM = new CoverageSwathResultViewModel();

		if (oSwath != null) {

			oVM.SwathName = oSwath.getSwathName();
			oVM.IsAscending = oSwath.isAscending();
			if (oSwath.getSat() != null) {
				oVM.SatelliteName = oSwath.getSat().getName();
			}

			if (oSwath.getSat() != null) {
				oVM.SensorName = oSwath.getSensor().getSName();

				if (oSwath.getSat().getType() != null)
					oVM.SensorType = oSwath.getSat().getType().name();

			}

			if (oSwath.getCoveredArea() != null)
				oVM.CoveredAreaName = oSwath.getCoveredArea().getName();

			if (oSwath.getSensor() != null) {
				if (oSwath.getSensor().getLooking() != null)
					oVM.SensorLookDirection = oSwath.getSensor().getLooking().toString();
			}

			if (oSwath.getTimeStart() != null) {
				GregorianCalendar oCalendar = oSwath.getTimeStart().getCurrentGregorianCalendar();
				oVM.AcquisitionStartTime = new Date(oCalendar.getTimeInMillis());
			}
			if (oSwath.getTimeEnd() != null) {
				GregorianCalendar oCalendar = oSwath.getTimeEnd().getCurrentGregorianCalendar();
				oVM.AcquisitionEndTime = new Date(oCalendar.getTimeInMillis());
			}

			oVM.AcquisitionDuration = oSwath.getDuration();

			if (oSwath.getFootprint() != null) {
				Polygon oPolygon = oSwath.getFootprint();
				apoint[] aoPoints = oPolygon.getVertex();

				if (aoPoints != null) {

					oVM.SwathFootPrint = "POLYGON((";

					for (int iPoints = 0; iPoints < aoPoints.length; iPoints++) {
						apoint oPoint = aoPoints[iPoints];
						oVM.SwathFootPrint += "" + (oPoint.x * 180.0 / Math.PI);
						oVM.SwathFootPrint += " ";
						oVM.SwathFootPrint += "" + (oPoint.y * 180.0 / Math.PI);
						oVM.SwathFootPrint += ",";
					}

					oVM.SwathFootPrint = oVM.SwathFootPrint.substring(0, oVM.SwathFootPrint.length() - 2);

					oVM.SwathFootPrint += "))";
				}
			}
		}

		// ADD CHILDS
		ArrayList<SwathArea> aoChilds = oSwath.getChilds();
		ArrayList<CoverageSwathResultViewModel> aoChildsViewModel = new ArrayList<CoverageSwathResultViewModel>();

		for (SwathArea swathArea : aoChilds) {
			CoverageSwathResultViewModel oChild;
			oChild = getCoverageSwathResultViewModelFromCoverageSwathResult(swathArea);
			aoChildsViewModel.add(oChild);

		}
		oVM.aoChilds = aoChildsViewModel;

		return oVM;
	}

	public CoverageSwathResultViewModel getCoverageSwathResultViewModelFromCoverageSwathResult(SwathArea oSwath) {
		Wasdi.DebugLog("OpportunitySearchResource.getCoverageSwathResultViewModelFromCoverageSwathResult");
		CoverageSwathResultViewModel oVM = new CoverageSwathResultViewModel();

		if (oSwath != null) {

			oVM.SwathName = oSwath.getSwathName();
			oVM.IsAscending = oSwath.isAscending();
			if (oSwath.getSat() != null) {
				oVM.SatelliteName = oSwath.getSat().getName();
			}

			if (oSwath.getSat() != null) {
				oVM.SensorName = oSwath.getSensor().getSName();

				if (oSwath.getSat().getType() != null)
					oVM.SensorType = oSwath.getSat().getType().name();

			}

			if (oSwath.getCoveredArea() != null)
				oVM.CoveredAreaName = oSwath.getCoveredArea().getName();

			if (oSwath.getSensor() != null) {
				if (oSwath.getSensor().getLooking() != null)
					oVM.SensorLookDirection = oSwath.getSensor().getLooking().toString();
			}

			if (oSwath.getTimeStart() != null) {
				GregorianCalendar oCalendar = oSwath.getTimeStart().getCurrentGregorianCalendar();
				// oVM.AcquisitionStartTime = oCalendar.getTime();
				oVM.AcquisitionStartTime = new Date(oCalendar.getTimeInMillis());
			}
			if (oSwath.getTimeEnd() != null) {
				GregorianCalendar oCalendar = oSwath.getTimeEnd().getCurrentGregorianCalendar();
				// oVM.AcquisitionEndTime = oCalendar.getTime();
				oVM.AcquisitionEndTime = new Date(oCalendar.getTimeInMillis());
			}

			oVM.AcquisitionDuration = oSwath.getDuration();

			if (oSwath.getFootprint() != null) {
				Polygon oPolygon = oSwath.getFootprint();
				apoint[] aoPoints = oPolygon.getVertex();

				if (aoPoints != null) {

					oVM.SwathFootPrint = "POLYGON((";

					for (int iPoints = 0; iPoints < aoPoints.length; iPoints++) {
						apoint oPoint = aoPoints[iPoints];
						oVM.SwathFootPrint += "" + (oPoint.x * 180.0 / Math.PI);
						oVM.SwathFootPrint += " ";
						oVM.SwathFootPrint += "" + (oPoint.y * 180.0 / Math.PI);
						oVM.SwathFootPrint += ",";
					}

					oVM.SwathFootPrint = oVM.SwathFootPrint.substring(0, oVM.SwathFootPrint.length() - 2);

					oVM.SwathFootPrint += "))";
				}
			}
		}

		return oVM;
	}

	private ArrayList<CoverageSwathResultViewModel> getSwatViewModelFromResult(CoverageSwathResult oSwath) {
		Wasdi.DebugLog("OpportunitySearchResource.getSwatViewModelFromResult");
		ArrayList<CoverageSwathResultViewModel> aoResults = new ArrayList<CoverageSwathResultViewModel>();

		if (oSwath == null)
			return aoResults;

		CoverageSwathResultViewModel oVM = getCoverageSwathResultViewModelFromCoverageSwathResult(oSwath);

		List<SwathArea> aoAreas = oSwath.getChilds();

		for (SwathArea oArea : aoAreas) {

			CoverageSwathResultViewModel oSwathResult = new CoverageSwathResultViewModel(oVM);

			if (oArea.getMode() != null) {
				oSwathResult.SensorMode = oArea.getMode().getName();
				if (oArea.getMode().getViewAngle() != null)
					oSwathResult.Angle = oArea.getMode().getViewAngle().toString();
			}

			if (oArea.getswathSize() != null) {
				oSwathResult.CoverageLength = oArea.getswathSize().getLength();
				oSwathResult.CoverageWidth = oArea.getswathSize().getWidth();
			}

			oSwathResult.Coverage = oArea.getCoverage() * 100;

			if (oArea.getswathSize() != null) {
				oSwathResult.CoverageWidth = oArea.getswathSize().getWidth();
				oSwathResult.CoverageLength = oArea.getswathSize().getLength();
			}

			if (oArea.getFootprint() != null) {
				Polygon oPolygon = oArea.getFootprint();
				apoint[] aoPoints = oPolygon.getVertex();

				if (aoPoints != null) {

					oSwathResult.FrameFootPrint = "POLYGON((";

					for (int iPoints = 0; iPoints < aoPoints.length; iPoints++) {
						apoint oPoint = aoPoints[iPoints];
						oSwathResult.FrameFootPrint += "" + (oPoint.x * 180.0 / Math.PI);
						oSwathResult.FrameFootPrint += " ";
						oSwathResult.FrameFootPrint += "" + (oPoint.y * 180.0 / Math.PI);
						oSwathResult.FrameFootPrint += ",";
					}

					oSwathResult.FrameFootPrint = oSwathResult.FrameFootPrint.substring(0,
							oSwathResult.FrameFootPrint.length() - 2);

					oSwathResult.FrameFootPrint += "))";
				}
			}

			aoResults.add(oSwathResult);
		}

		return aoResults;
	}

	@GET
	@Path("/track/{satellitename}")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public SatelliteOrbitResultViewModel getSatelliteTrack(@HeaderParam("x-session-token") String sSessionId,
			@PathParam("satellitename") String satname) {

		Wasdi.DebugLog("OpportunitySearchResource.GetSatelliteTrack( " + sSessionId + ", " + satname + " )");

		// set nfs properties download
		String userHome = System.getProperty("user.home");
		String Nfs = System.getProperty("nfs.data.download");
		if (Nfs == null) {
			System.setProperty("nfs.data.download", userHome + "/nfs/download");
			Wasdi.DebugLog("nfs dir " + System.getProperty("nfs.data.download"));
		}

		SatelliteOrbitResultViewModel ret = new SatelliteOrbitResultViewModel();
		String satres = InstanceFinder.s_sOrbitSatsMap.get(satname);
		try {

			Time tconv = new Time();
			tconv.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			Satellite sat = SatFactory.buildSat(satres);

			ret.code = satname;

			ret.satelliteName = sat.getDescription();
			sat.getSatController().moveToNow();
			ret.currentTime = tconv.convertJD2String(sat.getOrbitCore().getCurrentJulDate());
			ret.setCurrentPosition(sat.getOrbitCore().getLLA());

			sat.getOrbitCore().setShowGroundTrack(true);

			// lag
			double[] tm = sat.getOrbitCore().getTimeLag();
			for (int i = sat.getOrbitCore().getNumGroundTrackLagPts() - 1; i >= 0; i--)
				ret.addPosition(sat.getOrbitCore().getGroundTrackLlaLagPt(i), tconv.convertJD2String(tm[i]));

			// lead
			tm = sat.getOrbitCore().getTimeLead();
			for (int i = 0; i < sat.getOrbitCore().getNumGroundTrackLeadPts(); i++)
				ret.addPosition(sat.getOrbitCore().getGroundTrackLlaLeadPt(i), tconv.convertJD2String(tm[i]));
		} catch (Exception e) {
			Wasdi.DebugLog("OpportunitySearchResource.GetSatelliteTrack: " + e);
		}
		return ret;
	}

	@GET
	@Path("/getkmlsearchresults")
	@Produces({ "application/xml" }) // , "application/json", "text/html"
	// @Consumes(MediaType.APP)
	@Consumes(MediaType.APPLICATION_XML)
	public Kml getKmlSearchResults(@HeaderParam("x-session-token") String sSessionId, @QueryParam("text") String sText,
			@QueryParam("footPrint") String sFootPrint) {
		Wasdi.DebugLog("OpportunitySearchResource.getKmlSearchResults( " + sSessionId + ", " + sText + ", " + sFootPrint
				+ " )");
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser == null) {
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return null;
		}
		if (sFootPrint.isEmpty() || sText.isEmpty()) {
			return null;
		}

		String[] asPoints = Utils.convertPolygonToArray(sFootPrint);
		Kml kml = KmlFactory.createKml();

		// get coordinates
		Boundary oOuterBoundaryIs = new Boundary();
		LinearRing oLinearRing = new LinearRing();
		List<Coordinate> aoCoordinates = new ArrayList<Coordinate>();
		for (String string : asPoints) {
			string = string.replaceAll(" ", ",");
			Coordinate oCoordinate = new Coordinate(string);

			aoCoordinates.add(oCoordinate);

		}

		oLinearRing.setCoordinates(aoCoordinates);

		oOuterBoundaryIs.setLinearRing(oLinearRing);

		// set placemark
		Placemark oPlacemark = kml.createAndSetPlacemark().withName(sText).withVisibility(true);
		// styleLine
		oPlacemark.createAndAddStyleMap().createAndAddPair().withKey(StyleState.NORMAL).createAndSetStyle()
				.createAndSetLineStyle().withColor("FF0000FF").withColorMode(ColorMode.NORMAL).withWidth(1);

		// set polystyle
		oPlacemark.createAndAddStyleMap().createAndAddPair().createAndSetStyle().createAndSetPolyStyle()
				.withColor("FF0000FF").withColorMode(ColorMode.NORMAL).withFill(true).withOutline(true);

		// set polygon
		oPlacemark.createAndSetPolygon().withAltitudeMode(AltitudeMode.CLAMP_TO_GROUND).withExtrude(false)
				.withOuterBoundaryIs(oOuterBoundaryIs);

		kml.setFeature(oPlacemark);

		return kml;
	}

	@GET
	@Path("/updatetrack/{satellitesname}")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<SatelliteOrbitResultViewModel> getUpdatedSatelliteTrack(
			@HeaderParam("x-session-token") String sSessionId, @PathParam("satellitesname") String sSatName) {

		// Wasdi.DebugLog("OpportunitySearchResource.getUpdatedSatelliteTrack( " +
		// sSessionId + ", " + sSatName + "");

		// Check if we have codes
		if (Utils.isNullOrEmpty(sSatName))
			return null;

		// Return array
		ArrayList<SatelliteOrbitResultViewModel> aoRet = new ArrayList<SatelliteOrbitResultViewModel>();

		// Clear the string
		if (sSatName.endsWith("-")) {
			sSatName = sSatName.substring(0, sSatName.length() - 1);
		}

		// Split the codes
		String[] asSatellites = sSatName.split("-");

		// Get "now" in the right format
		Time tconv = new Time();
		double k = 180.0 / Math.PI;
		tconv.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

		// For all the satellites
		for (int iSat = 0; iSat < asSatellites.length; iSat++) {

			String sSat = asSatellites[iSat];

			// Create the View Mode
			SatelliteOrbitResultViewModel oPositionViewModel = new SatelliteOrbitResultViewModel();

			String oSatelliteResource = InstanceFinder.s_sOrbitSatsMap.get(sSat);

			try {

				// Create the Satellite
				Satellite oSatellite = SatFactory.buildSat(oSatelliteResource);
				oSatellite.getSatController().moveToNow();

				// Set Data to the view model
				oPositionViewModel.satelliteName = oSatellite.getDescription();
				oPositionViewModel.code = sSat;
				oPositionViewModel.currentPosition = (oSatellite.getOrbitCore().getLatitude() * k) + ";"
						+ (oSatellite.getOrbitCore().getLongitude() * k) + ";"
						+ oSatellite.getOrbitCore().getAltitude();

				oSatellite.getOrbitCore().setShowGroundTrack(true);

			} catch (Exception e) {
				Wasdi.DebugLog("OpportunitySearchResource.getUpdatedSatelliteTrack: " + e);
				continue;
			}

			aoRet.add(oPositionViewModel);
		}

		return aoRet;
	}

	@GET
	@Path("/getsatellitesresource")
	@Produces({ "application/xml", "application/json", "text/html" })
	// @Consumes(MediaType.APP)
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<SatelliteResourceViewModel> getSatellitesResources(
			@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("OpportunitySearchResource.getSatellitesResources( " + sSessionId + " )");
		// if(! m_oCredentialPolicy.validSessionId(sSessionId)) {
		// //todo retur error
		// //Satellite oSatellite = new
		// }
		String[] asSatellites = null;

		// String satres = InstanceFinder.s_sOrbitSatsMap.get("COSMOSKY1");
		String sSatellites = m_oServletConfig.getInitParameter("LIST_OF_SATELLITES");
		if (sSatellites != null && sSatellites.length() > 0) {
			asSatellites = sSatellites.split(",|;");
		}

		ArrayList<SatelliteResourceViewModel> aaoReturnValue = new ArrayList<SatelliteResourceViewModel>();
		for (Integer iIndexSarellite = 0; iIndexSarellite < asSatellites.length; iIndexSarellite++) {
			try {
				String satres = InstanceFinder.s_sOrbitSatsMap.get(asSatellites[iIndexSarellite]);
				Satellite oSatellite = SatFactory.buildSat(satres);
				ArrayList<SatSensor> aoSatelliteSensors = oSatellite.getSensors();

				SatelliteResourceViewModel oSatelliteResource = new SatelliteResourceViewModel();
				oSatelliteResource.setSatelliteName(oSatellite.getName());
				oSatelliteResource.setSatelliteSensors(aoSatelliteSensors);
				aaoReturnValue.add(oSatelliteResource);
			} catch (Exception e) {
				Wasdi.DebugLog("getSatellitesResources Exception: " + e);
				return null;
			}
		}

		return aaoReturnValue;

	}

}
