package it.fadeout.rest.resources;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.sftp.SFTPManager;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ChangeUserPasswordViewModel;
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.RegistrationInfoViewModel;
import wasdi.shared.viewmodels.UserViewModel;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;


@Path("/auth")
public class AuthResource {
	
	//XXX replace with dependency injection
	PasswordAuthentication m_oPasswordAuthentication = new PasswordAuthentication();
	//XXX replace with dependency injection
	//MAYBE two different policy: one for google one for username/password
	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();
	//MAYBE separate
	
	@Context
	ServletConfig m_oServletConfig;
	
	@POST
	@Path("/login")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel Login(LoginInfo oLoginInfo) {
		Wasdi.DebugLog("AuthResource.Login");
		//TODO captcha
		
		if (oLoginInfo == null) {
			Wasdi.DebugLog("Auth.Login: login info null, user not authenticated");
			
			return UserViewModel.getInvalid();
		}
		
		if(!m_oCredentialPolicy.satisfies(oLoginInfo)) {
			Wasdi.DebugLog("Auth.Login: Login Info does not support Credential Policy, user " + oLoginInfo.getUserId() + " not authenticated" );
			
			return UserViewModel.getInvalid();
		}

		UserViewModel oUserVM = UserViewModel.getInvalid();
		
		try {
			
			Wasdi.DebugLog("AuthResource.Login: requested access from " + oLoginInfo.getUserId());
			
			UserRepository oUserRepository = new UserRepository();

			User oWasdiUser = oUserRepository.GetUser(oLoginInfo.getUserId());
			
			if( oWasdiUser == null ) {
				Wasdi.DebugLog("AuthResource.Login: User Id Not availalbe " + oLoginInfo.getUserId());
				return UserViewModel.getInvalid();
			}
			
			
			if(!m_oCredentialPolicy.satisfies(oWasdiUser)) {
				Wasdi.DebugLog("AuthResource.Login: Wasdi user does not satisfy Credential Policy " + oLoginInfo.getUserId());
				return UserViewModel.getInvalid();
			}
			
			
			if(null != oWasdiUser.getValidAfterFirstAccess()) {
				
				if(oWasdiUser.getValidAfterFirstAccess() ) {
					
					Boolean bLoginSuccess = m_oPasswordAuthentication.authenticate(
											oLoginInfo.getUserPassword().toCharArray(),
											oWasdiUser.getPassword()
										);
					
					if ( bLoginSuccess ) {
						
						//get all expired sessions
						clearUserExpiredSessions(oWasdiUser);
						oUserVM = new UserViewModel();
						oUserVM.setName(oWasdiUser.getName());
						oUserVM.setSurname(oWasdiUser.getSurname());
						oUserVM.setUserId(oWasdiUser.getUserId());
						
						UserSession oSession = new UserSession();
						oSession.setUserId(oWasdiUser.getUserId());
						
						//XXX check: two users must not have the same sessionId (to avoid ambiguity when getting user from sessionId)
						//can it really happen? Should we really read from DB to check for this possibility?
						//Actual risk of collision is very low (~10^-10 over a year)
						//https://stackoverflow.com/questions/20999792/does-randomuuid-give-a-unique-id
						String sSessionId = UUID.randomUUID().toString();
						oSession.setSessionId(sSessionId);
						oSession.setLoginDate((double) new Date().getTime());
						oSession.setLastTouch((double) new Date().getTime());
						
						SessionRepository oSessionRepo = new SessionRepository();
						Boolean bRet = oSessionRepo.InsertSession(oSession);
						if (!bRet) {
							return oUserVM;
						}
						oUserVM.setSessionId(sSessionId);
						
						Wasdi.DebugLog("AuthService.Login: access succeeded, sSessionId: "+sSessionId);
					} else {
						
						Wasdi.DebugLog("AuthService.Login: access failed");
					}	
				} else {
					
					Wasdi.DebugLog("AuthService.Login: registration not validated yet");
				}
			} else {
				
				Wasdi.DebugLog("AuthService.Login: registration flag is null");
			}
				
		}
		catch (Exception oEx) {
			
			Wasdi.DebugLog("AuthService.Login: Error");
			oEx.printStackTrace();
			
		}
		
		return oUserVM;
	}

	private void clearUserExpiredSessions(User oUser) {
		//MAYBE check for User policy satisfaction 
		SessionRepository oSessionRepository = new SessionRepository();
		List<UserSession> aoEspiredSessions = oSessionRepository.GetAllExpiredSessions(oUser.getUserId());
		for (UserSession oUserSession : aoEspiredSessions) {
			//delete data base session
			if (!oSessionRepository.DeleteSession(oUserSession)) {
				
				Wasdi.DebugLog("AuthService.Login: Error deleting session.");
			}
		}
	}
	
	@GET
	@Path("/checksession")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel CheckSession(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("AuthResource.CheckSession");
		
		if(null == sSessionId) {
			Wasdi.DebugLog("AuthResource.CheckSession: null sSessionId");
			return UserViewModel.getInvalid();
		}

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			return UserViewModel.getInvalid();
			
		} else if(!m_oCredentialPolicy.satisfies(oUser)) {
			return UserViewModel.getInvalid();
		}
		
		UserViewModel oUserVM = new UserViewModel();
		oUserVM.setName(oUser.getName());
		oUserVM.setSurname(oUser.getSurname());
		oUserVM.setUserId(oUser.getUserId());
		
		return oUserVM;
	}	
	

	@GET
	@Path("/logout")
	@Produces({"application/xml", "application/json", "text/xml"})
	//MAYBEchange return type to http response @sergin13 @kr1zz 
	public PrimitiveResult Logout(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("AuthResource.Logout");
		
		if(null == sSessionId) {
			Wasdi.DebugLog("AuthResource.CheckSession: null sSessionId");
			return PrimitiveResult.getInvalid();
		}
		
		
		if(!m_oCredentialPolicy.validSessionId(sSessionId)) {
			return PrimitiveResult.getInvalid();
		}
		PrimitiveResult oResult = PrimitiveResult.getInvalid();
		SessionRepository oSessionRepository = new SessionRepository();
		UserSession oSession = oSessionRepository.GetSession(sSessionId);
		if(oSession != null) {
			oResult = new PrimitiveResult();
			oResult.setStringValue(sSessionId);
			if(oSessionRepository.DeleteSession(oSession)) {
				
				Wasdi.DebugLog("AuthService.Logout: Session data base deleted.");
				oResult.setBoolValue(true);
			} else {
				
				Wasdi.DebugLog("AuthService.Logout: Error deleting session data base.");
				oResult.setBoolValue(false);
			}
			
		} else {
			return PrimitiveResult.getInvalid();
		}
		return oResult;
	}	

	
	
	@POST
	@Path("/upload/createaccount")
	@Produces({"application/json", "text/xml"})
	public Response CreateSftpAccount(@HeaderParam("x-session-token") String sSessionId, String sEmail) {
		Wasdi.DebugLog("AuthService.CreateSftpAccount: Called for Mail " + sEmail);
		
		if(! m_oCredentialPolicy.validSessionId(sSessionId) || !m_oCredentialPolicy.validEmail(sEmail)) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null  ) {
			return Response.status(Status.UNAUTHORIZED).build();
		} else if(!m_oCredentialPolicy.satisfies(oUser)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String sAccount = oUser.getUserId();
		//TODO read from servlet config file
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) {
			wsAddress = "ws://localhost:6703";
		}
		SFTPManager oManager = new SFTPManager(wsAddress);
		String sPassword = Utils.generateRandomPassword();
		
		if (!oManager.createAccount(sAccount, sPassword)) {
			
			Wasdi.DebugLog("AuthService.CreateSftpAccount: error creating sftp account");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		if(!sendPasswordEmail(sEmail, sAccount, sPassword)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	    
		return Response.ok().build();
	}
	
	@GET
	@Path("/upload/existsaccount")
	@Produces({"application/json", "text/xml"})
	public Boolean ExixtsSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("AuthService.ExistsSftpAccount");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			return null;
		} else if(!m_oCredentialPolicy.satisfies(oUser)) {
			return null;
		}
		String sAccount = oUser.getUserId();		
		
		//TODO read param from servlet config file
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		Boolean bRes = null;
		try{
			bRes = oManager.checkUser(sAccount);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bRes;
	}


	@GET
	@Path("/upload/list")
	@Produces({"application/json", "text/xml"})
	public String[] ListSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("AuthService.ListSftpAccount");
		if(! m_oCredentialPolicy.validSessionId(sSessionId) ) {
			return null;
		}
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			return null;
		} else if(!m_oCredentialPolicy.satisfies(oUser)) {
			return null;
		}	
		String sAccount = oUser.getUserId();		
		
		//TODO read param from servlet config file
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		return oManager.list(sAccount);
	}
	

	@DELETE
	@Path("/upload/removeaccount")
	@Produces({"application/json", "text/xml"})
	public Response RemoveSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("AuthService.RemoveSftpAccount");
		if( null==sSessionId ) {
			return Response.status(Status.BAD_REQUEST).build();
		} else if( !m_oCredentialPolicy.validSessionId(sSessionId)) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			return Response.status(Status.UNAUTHORIZED).build();
		} else if(!m_oCredentialPolicy.satisfies(oUser)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		String sAccount = oUser.getUserId();
		
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		return oManager.removeAccount(sAccount) ? Response.ok().build() : Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}


	@POST
	@Path("/upload/updatepassword")
	@Produces({"application/json", "text/xml"})
	public Response UpdateSftpPassword(@HeaderParam("x-session-token") String sSessionId, String sEmail) {
		Wasdi.DebugLog("AuthService.UpdateSftpPassword");
		if(!m_oCredentialPolicy.validSessionId(sSessionId) || !m_oCredentialPolicy.validEmail(sEmail)) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if(null == oUser) {
			return Response.status(Status.UNAUTHORIZED).build();
		} else if( !m_oCredentialPolicy.satisfies(oUser)) {
			return Response.status(Status.UNAUTHORIZED).build(); 
		}
		
		String sAccount = oUser.getUserId();
		
		//TODO read param from servlet config file
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);
		
		String sPassword = Utils.generateRandomPassword();
		
		if (!oManager.updatePassword(sAccount, sPassword)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		if(!sendPasswordEmail(sEmail, sAccount, sPassword)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		return Response.ok().build();
	}
	
	//CHECK USER ID TOKEN BY GOOGLE 
	@POST
	@Path("/logingoogleuser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel LoginGoogleUser(LoginInfo oLoginInfo) {
		Wasdi.DebugLog("AuthResource.CheckGoogleUserId");
		//TODO captcha
		
		if (oLoginInfo == null) {
			return UserViewModel.getInvalid();
		}
		if(!m_oCredentialPolicy.satisfies(oLoginInfo)) {
			return UserViewModel.getInvalid();
		}
		
		try {	
			final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
			final JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
				    // Specify the CLIENT_ID of the app that accesses the backend:
				    .setAudience(Collections.singletonList(oLoginInfo.getUserId()))
				    // Or, if multiple clients access the backend:
				    //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
				    .build();
			
			// (Receive idTokenString by HTTPS POST)
			GoogleIdToken oIdToken = verifier.verify(oLoginInfo.getGoogleIdToken());
			
			//check id token
			if (oIdToken != null) {
			  Payload oPayload = oIdToken.getPayload();

			  // Print user identifier
			  String sGoogleIdToken = oPayload.getSubject();
			 
			  // Get profile information from payload
			  String sEmail = oPayload.getEmail();
			  
			 // boolean bEmailVerified = Boolean.valueOf(oPayload.getEmailVerified());
			 // String sName = (String) oPayload.get("name");
			 // String sPictureUrl = (String) oPayload.get("picture");
			 // String sLocale = (String) oPayload.get("locale");
			 // String sGivenName = (String) oPayload.get("given_name");
			 // String sFamilyName = (String) oPayload.get("family_name");
			  
			  // store profile information and create session
			  Wasdi.DebugLog("AuthResource.LoginGoogleUser: requested access from " + sGoogleIdToken);
			

			  UserRepository oUserRepository = new UserRepository();
			  String sAuthProvider = "google";
			  User oWasdiUser = oUserRepository.GetUser(sEmail);
			  //save new user 
			  if(oWasdiUser == null) {
				  User oUser = new User();
				  oUser.setAuthServiceProvider(sAuthProvider);
				  oUser.setUserId(sEmail);
				  oUser.setGoogleIdToken(sGoogleIdToken);
				  if(oUserRepository.InsertUser(oUser) == true) {
					  //the user is stored in DB
					  //get user from database (i do it only for consistency)
					  oWasdiUser = oUserRepository.GoogleLogin(sGoogleIdToken , sEmail, sAuthProvider);
				  }
			  }
			  
			  if (oWasdiUser != null && oWasdiUser.getAuthServiceProvider().equalsIgnoreCase("google") == true) {
				  //get all expired sessions
				  SessionRepository oSessionRepository = new SessionRepository();
				  List<UserSession> aoEspiredSessions = oSessionRepository.GetAllExpiredSessions(oWasdiUser.getUserId());
				  for (UserSession oUserSession : aoEspiredSessions) {
					  //delete data base session
					  if (!oSessionRepository.DeleteSession(oUserSession)) {
						  //XXX log instead
						  System.out.println("AuthService.LoginGoogleUser: Error deleting session.");
					  }
				  }

				  UserViewModel oUserVM = new UserViewModel();
				  oUserVM.setName(oWasdiUser.getName());
				  oUserVM.setSurname(oWasdiUser.getSurname());
				  oUserVM.setUserId(oWasdiUser.getUserId());
				  
				  UserSession oSession = new UserSession();
				  oSession.setUserId(oWasdiUser.getUserId());
				  String sSessionId = UUID.randomUUID().toString();
				  oSession.setSessionId(sSessionId);
				  oSession.setLoginDate((double) new Date().getTime());
				  oSession.setLastTouch((double) new Date().getTime());

				  Boolean bRet = oSessionRepository.InsertSession(oSession);
				  if (!bRet) {
					  return UserViewModel.getInvalid();
				  }
				  oUserVM.setSessionId(sSessionId);

				  Wasdi.DebugLog("AuthService.LoginGoogleUser: access succeeded");
				  return oUserVM;
			  } else {
				  Wasdi.DebugLog("AuthService.LoginGoogleUser: access failed");
			  }

			} else {

				Wasdi.DebugLog("Invalid ID token.");
				UserViewModel.getInvalid();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return UserViewModel.getInvalid();
	}
		
	@POST
	@Path("/register")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult userRegistration(RegistrationInfoViewModel oRegistrationInfoViewModel) 
	{	
		Wasdi.DebugLog("AuthService.UserRegistration");
		//TODO captcha
		 
		
		if(null == oRegistrationInfoViewModel) {
			return PrimitiveResult.getInvalid();
		} else {
			try
			{
				if(!m_oCredentialPolicy.satisfies(oRegistrationInfoViewModel)) {
					return PrimitiveResult.getInvalid();
				}
				
				UserRepository oUserRepository = new UserRepository();
				User oWasdiUser = oUserRepository.GetUser(oRegistrationInfoViewModel.getUserId());
				
				//if oWasdiUser is a new user -> oWasdiUser == null
				if(oWasdiUser == null) {
					//save new user 
					String sAuthProvider = "wasdi";
					User oNewUser = new User();
					oNewUser.setAuthServiceProvider(sAuthProvider);
					oNewUser.setUserId(oRegistrationInfoViewModel.getUserId());
					oNewUser.setName(oRegistrationInfoViewModel.getName());
					oNewUser.setSurname(oRegistrationInfoViewModel.getSurname());
					oNewUser.setPassword(m_oPasswordAuthentication.hash(oRegistrationInfoViewModel.getPassword().toCharArray()));
					oNewUser.setValidAfterFirstAccess(false);
					String sToken = UUID.randomUUID().toString();
					oNewUser.setFirstAccessUUID(sToken);
					
					PrimitiveResult oResult = PrimitiveResult.getInvalid();
					if(oUserRepository.InsertUser(oNewUser) == true) {
						//the user is stored in DB
						oResult = new PrimitiveResult();
						oResult.setBoolValue(true);
						oResult.setStringValue(oNewUser.getUserId());
					} else {
						
						Wasdi.DebugLog("AuthResource.userRegistration: insert new user in DB failed");
						return PrimitiveResult.getInvalid();
					}
					//build confirmation link
					String sLink = buildRegistrationLink(oNewUser);
					
					Wasdi.DebugLog(sLink);
					//send it via email to the user
					Boolean bMailSuccess = sendRegistrationEmail(oNewUser, sLink);
					
					if(bMailSuccess){
						return oResult;
					} else {
						//problem sending the email: either the given address is invalid
						//or the mail server failed for some reason
						//in both cases the user must be removed from DB
						if( !oUserRepository.DeleteUser(oNewUser.getUserId()) ) {
							throw new Exception("failed removal of newly created user");
						}
						//and the client should be informed
						oResult = new PrimitiveResult();
						oResult.setBoolValue(false);
						oResult.setStringValue("cannot send email");
						return oResult;
					} 
					
					//uncomment only if email sending service does not work
					//oResult = validateNewUser(oUserViewModel.getUserId(), sToken);
				}
				else
				{
					PrimitiveResult oResult = PrimitiveResult.getInvalid();
					oResult.setStringValue("mail already in use, impossible to register the new user");
					return oResult;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return PrimitiveResult.getInvalid();
	}
	
	
	@GET
	@Path("/validateNewUser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult validateNewUser(@QueryParam("email") String sUserId, @QueryParam("validationCode") String sToken  ) {
		Wasdi.DebugLog("AuthService.validateNewUser");
	
		
		if(! (m_oCredentialPolicy.validUserId(sUserId) && m_oCredentialPolicy.validEmail(sUserId)) ) {
			return PrimitiveResult.getInvalid();
		}
		if(!m_oCredentialPolicy.validFirstAccessUUID(sToken)) {
			return PrimitiveResult.getInvalid();
		}
		
		UserRepository oUserRepo = new UserRepository();
		User oUser = oUserRepo.GetUser(sUserId);
		if( null == oUser.getValidAfterFirstAccess()) {
			Wasdi.DebugLog("AuthResources.validateNewUser: unexpected null first access validation flag");
			return PrimitiveResult.getInvalid();
		} else if( oUser.getValidAfterFirstAccess() ) {
			Wasdi.DebugLog("AuthResources.validateNewUser: unexpected true first access validation flag");
			return PrimitiveResult.getInvalid();
		} else if( !oUser.getValidAfterFirstAccess() ) {
			String sDBToken = oUser.getFirstAccessUUID();
			if(m_oCredentialPolicy.validFirstAccessUUID(sToken)) {
				if(sDBToken.equals(sToken)) {
					oUser.setValidAfterFirstAccess(true);
					oUserRepo.UpdateUser(oUser);
					PrimitiveResult oResult = new PrimitiveResult();
					oResult.setBoolValue(true);
					oResult.setStringValue(oUser.getUserId());
					return oResult;
				} else {
					Wasdi.DebugLog("AuthResources.validateNewUser: registration token mismatch");
					PrimitiveResult.getInvalid();
				}
			}
		}
		return PrimitiveResult.getInvalid();
	}
	

	@POST
	@Path("/editUserDetails")
	@Produces({"application/json", "text/xml"})
	public UserViewModel editUserDetails(@HeaderParam("x-session-token") String sSessionId, UserViewModel oInputUserVM ) {
		Wasdi.DebugLog("AuthService.editUserDetails");
		//note: sSessionId validity is automatically checked later
		//note: only name and surname can be changed, so far. Other fields are ignored
		
		if(!m_oCredentialPolicy.validSessionId(sSessionId) || null == oInputUserVM ) {
			return UserViewModel.getInvalid();
		}
		//check only name and surname: they are the only fields that must be valid,
		//the others will typically be null, including userId
		if(!m_oCredentialPolicy.validName(oInputUserVM.getName()) || !m_oCredentialPolicy.validSurname(oInputUserVM.getSurname())) {
			return UserViewModel.getInvalid();
		}
		
		try {
			//note: session validity is automatically checked		
			User oUserId = Wasdi.GetUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				System.err.print("Null user from session id (does the user exist?)");
				return UserViewModel.getInvalid();
			}
	
			//update
			oUserId.setName(oInputUserVM.getName());
			oUserId.setSurname(oInputUserVM.getSurname());
			UserRepository oUR = new UserRepository();
			oUR.UpdateUser(oUserId);
			
			//respond
			UserViewModel oOutputUserVM = new UserViewModel();
			oOutputUserVM.setUserId(oUserId.getUserId());
			oOutputUserVM.setName(oUserId.getName());
			oOutputUserVM.setSurname(oUserId.getSurname());
			oOutputUserVM.setSessionId(sSessionId);
			return oOutputUserVM;
			
		} catch(Exception e) {
			Wasdi.DebugLog("AuthService.ChangeUserPassword: Exception");
			e.printStackTrace();
		}
		//should not get here
		return UserViewModel.getInvalid();
	}

	
	
	@POST
	@Path("/changePassword")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ChangeUserPassword(@HeaderParam("x-session-token") String sSessionId,
			ChangeUserPasswordViewModel oChPasswViewModel) {
		
		Wasdi.DebugLog("AuthService.ChangeUserPassword"  );
		
		//input validation
		if(null == oChPasswViewModel || !m_oCredentialPolicy.validSessionId(sSessionId)) {
			Wasdi.DebugLog("AuthService.ChangeUserPassword: invalid input");
			return PrimitiveResult.getInvalid();
		}
		
		if(!m_oCredentialPolicy.satisfies(oChPasswViewModel)) {
			Wasdi.DebugLog("AuthService.ChangeUserPassword: invalid input\n");
			return PrimitiveResult.getInvalid();
		}
		
		PrimitiveResult oResult = PrimitiveResult.getInvalid();
		try {
			//validity is automatically checked		
			User oUserId = Wasdi.GetUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				Wasdi.DebugLog("Null user from session id (does the user exist?)");
				return oResult;
			}
	
			String sOldPassword = oUserId.getPassword();
			Boolean bPasswordCorrect = m_oPasswordAuthentication.authenticate(oChPasswViewModel.getCurrentPassword().toCharArray(), sOldPassword);
			
			if( !bPasswordCorrect ) {
				Wasdi.DebugLog("Wrong current password for user " + oUserId);
				return oResult;
			} else {
				oUserId.setPassword(m_oPasswordAuthentication.hash(oChPasswViewModel.getNewPassword().toCharArray()));
				UserRepository oUR = new UserRepository();
				//TODO check return value
				oUR.UpdateUser(oUserId);
				oResult = new PrimitiveResult();
				oResult.setBoolValue(true);
			}
		} catch(Exception e) {
			Wasdi.DebugLog("AuthService.ChangeUserPassword: Exception");
			e.printStackTrace();
		}
		
		return oResult;
		
	} 	
	
	
	@GET
	@Path("/lostPassword")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult lostPassword(@QueryParam("userId") String sUserId ) {
		Wasdi.DebugLog("AuthService.lostPassword");
		if(null == sUserId ) {
			return PrimitiveResult.getInvalid();
		}
		if(!m_oCredentialPolicy.validUserId(sUserId)) {
			return PrimitiveResult.getInvalid();
		}
		UserRepository oUserRepository = new UserRepository();
		User oUser = oUserRepository.GetUser(sUserId);
		if(null == oUser) {
			return PrimitiveResult.getInvalid();
		} else {
			if(null != oUser.getAuthServiceProvider()){
				if( m_oCredentialPolicy.authenticatedByWasdi(oUser.getAuthServiceProvider()) ){
					if(m_oCredentialPolicy.validEmail(oUser.getUserId()) ) {
						String sPassword = Utils.generateRandomPassword();
						String sHashedPassword = m_oPasswordAuthentication.hash( sPassword.toCharArray() ); 
						oUser.setPassword(sHashedPassword);
						if(oUserRepository.UpdateUser(oUser)) {
							if(! sendPasswordEmail(sUserId, sUserId, sPassword) ) {
								return PrimitiveResult.getInvalid(); 
							}
							PrimitiveResult oResult = new PrimitiveResult();
							oResult.setBoolValue(true);
							oResult.setIntValue(0);
							return oResult;
						} else {
							return PrimitiveResult.getInvalid();
						}
					} else {
						//older users did not necessarily specified an email
						return PrimitiveResult.getInvalid();
					}
				} else {
					return PrimitiveResult.getInvalid();
				}
			} else {
				return PrimitiveResult.getInvalid();
			}
		}
	}
	
	private Boolean sendRegistrationEmail(User oUser, String sLink) {
		Wasdi.DebugLog("AuthResource.sendRegistrationEmail");
		//MAYBE validate input
		//MAYBE check w/ CredentialPolicy
		try {
			
			String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");
			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				Wasdi.DebugLog("AuthResource.sendRegistrationEmail: sMercuriusAPIAddress is null");
				return false;
			}
			MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
			Message oMessage = new Message();
			
			//TODO read the message subject title from servlet config file
			//e.g.
			//String sTitle = m_oServletConfig.getInitParameter("sftpMailTitle");
			String sTitle = "Welcome to WASDI";
			oMessage.setTilte(sTitle);
			
			//TODO read the sender from the servlet config file
			String sSender = m_oServletConfig.getInitParameter("sftpManagementMailSenser");
			if (sSender==null) {
				sSender = "adminwasdi@wasdi.org";
			}
			oMessage.setSender(sSender);
			
			//String sMessage = m_oServletConfig.getInitParameter("sftpMailText");
			//TODO read the message from the servlet config file
			String sMessage = "Dear " + oUser.getName() + " " + oUser.getSurname() + ",\n welcome to WASDI.\n\n"+
					"Please click on the link below to activate your account:\n\n" + 
					sLink;
			oMessage.setMessage(sMessage);
	
			Integer iPositiveSucceded = 0;
			iPositiveSucceded = oAPI.sendMailDirect(oUser.getUserId(), oMessage);
			Wasdi.DebugLog("AuthResource.sendRegistrationEmail: "+iPositiveSucceded.toString());
			if(iPositiveSucceded < 0 ) {
				//negative result means email couldn't be sent
				return false;
			}
		} catch(Exception e) {
			Wasdi.DebugLog("\n\n"+e.getMessage()+"\n\n" );
			return false;
		}
		return true;
	}
	
	
	private String buildRegistrationLink(User oUser) {
		Wasdi.DebugLog("AuthResource.buildRegistrationLink");
		String sResult = "";
		//MAYBE validate input	
		//TODO link to a client's page @sergin13 @kr1zz (web.xml... can be temporary hardcoded, otherwise)
		String sAPIUrl =  m_oServletConfig.getInitParameter("REGISTRATION_API_URL");
		String sUserId = "email=" + oUser.getUserId();
		String sToken = "validationCode=" + oUser.getFirstAccessUUID();
		
		sResult = sAPIUrl + sUserId + "&" + sToken;
		
		return sResult;
	}


	private Boolean sendPasswordEmail(String sRecipientEmail, String sAccount, String sPassword) {
		Wasdi.DebugLog("AuthResource.sendPasswordEmail");
		if(null == sRecipientEmail || null == sPassword ) {
			Wasdi.DebugLog("AuthResource.sendPasswordEmail: null input, not enough information to send email");
			return false;
		}
		//XXX refactor (?) to use null object @sergin13 + @kr1zz (?)
		//maybe check w/ CredentialPolicy
		//send email with new password
		String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");
		MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
		Message oMessage = new Message();
		String sTitle = m_oServletConfig.getInitParameter("sftpMailTitle");
		oMessage.setTilte(sTitle);
		String sSenser = m_oServletConfig.getInitParameter("sftpManagementMailSenser");
		if (sSenser==null) sSenser = "adminwasdi@wasdi.org";
		oMessage.setSender(sSenser);
		
		String sMessage = m_oServletConfig.getInitParameter("sftpMailText");
		sMessage += "\n\nUSER: " + sAccount + " - PASSWORD: " + sPassword;
		oMessage.setMessage(sMessage);
		
		if(oAPI.sendMailDirect(sRecipientEmail, oMessage) >= 0) {
			return true;
		}
		return false;
	}

}
