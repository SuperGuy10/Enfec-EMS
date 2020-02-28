package com.enfec.controller;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.enfec.model.Address;
import com.enfec.model.OrganizerContactTable;
import com.enfec.model.OrganizerTable;
import com.enfec.repository.OrganizerRepositoryImpl;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;

/**
 * This is controller class for organizer APIs
 * @author sylvia zhao
 */
@RestController
public class OrganizerController {
	
	@Autowired
	OrganizerRepositoryImpl OrganizerRepositoryImpl;

	
	/**
	 * Create or register an organizer user, send email to user and put basic information into database
	 * 
	 * @param organizerTable. Contains organizer basic information; email_address and password cannot be null
	 * @return ResponseEntity with message
	 */
	@RequestMapping(value = "/organizer/create", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> createOrganizer(
			@RequestBody(required = true) OrganizerTable organizerTable) {
		
		try {
			if(OrganizerRepositoryImpl.hasRegistered(organizerTable.getEmail_address())) {
				return new ResponseEntity<String>(
						"{\"message\" : \"Organizer registered already, please log in\"}",
						HttpStatus.OK);
			}else {
				int affectedRow = OrganizerRepositoryImpl.createOrganizer(organizerTable);
				if (affectedRow == 0) {
					return new ResponseEntity<>(
							"{\"message\" : \"Organizer not registered\"}", HttpStatus.OK);
				} else {
					String oEmail = organizerTable.getEmail_address();
					String oToken = OrganizerRepositoryImpl.generateToken();
					Timestamp expireDate = new Timestamp(System.currentTimeMillis());
					OrganizerRepositoryImpl.saveTokenInfo(oEmail, oToken, expireDate);
					OrganizerRepositoryImpl.sendGreetMail(organizerTable.getEmail_address(), 
							"Welcome to EMS", 
							"<p>Dear</p>"+
							"<p><b>"+ organizerTable.getOrganizer_name()+ "</b></p>"+
							"<p>Welcome to join the EMS. Your account is all set!</p>"+ 
							"<p><a href = 'http://evntmgmt-alb-295694066.us-east-2.elb.amazonaws.com:8080/organizer-api/registrationConfirm?oToken="+oToken+"'>Please click this link to Active your account</a></p>" +
							
							"<p>This is a system generated mail. Please do not reply to this email ID. If you have a query or need any clarification you may:</p>" + 
							"<p>(1) Call our 24-hour Customer Care or\r\n</p>" + 
							"<p>(2) Email Us support@enfec.com\r\n</p>" + 
							
							"<p>For any problem please contact us at 24*7 Hrs. Customer Support at 18001231234 (TBD) or mail us at support@enfec.com\r\n" + 
							"Thank you for using our Event Management System\r\n</p>",
							oToken);
				
					return new ResponseEntity<>(
							"{\"message\" : \"Organizer registered\"}", HttpStatus.OK);
				}
			}		
		} catch (DataIntegrityViolationException dataIntegrityViolationException) {
			//input type incorrect
			return new ResponseEntity<>("{\"message\" : \"Invalid input\"}",
					HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			//lack of required info or server error
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in creating organizer info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/registrationConfirm", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> confirmRegistration(@RequestParam("oToken") String oToken) {
	    if(OrganizerRepositoryImpl.validToken(oToken)) {
	    	return new ResponseEntity<String>(
					"{\"message\": \"Organizer account actived\"}", HttpStatus.OK);
	    } else {
	    	return new ResponseEntity<String>(
					"{\"message\": \"Organizer account active failed\"}", HttpStatus.BAD_REQUEST);
	    }
	}
	
	
	/*
	 * Customer forget password section
	 * 
	 */
	@RequestMapping(value = "/forget_password", method = RequestMethod.POST, produces = "applications/json;charset=UTF-8")
	public ResponseEntity<String>forgetPassword(
			@RequestBody(required = true) OrganizerTable organizerTable){
		
			if(OrganizerRepositoryImpl.isValidOrganizer(organizerTable.getEmail_address())) {
				String oEmail = organizerTable.getEmail_address();
				String oToken = OrganizerRepositoryImpl.generateToken();
				Timestamp expireDate = new Timestamp(System.currentTimeMillis());
				if(OrganizerRepositoryImpl.hasForgetenPWD(oEmail)) {
					OrganizerRepositoryImpl.updateToken(oEmail, oToken, expireDate);
				} else {
					OrganizerRepositoryImpl.saveTokenInfo(oEmail, oToken, expireDate);
				}
				
				OrganizerRepositoryImpl.sendPwdMail(oEmail, 
						"Reset Password", 
						"<p>This is a system generated mail. Please do not reply to this email ID. If you have a query or need any clarification you may:</p>" + 
						"<p>(1) Call our 24-hour Customer Care or\r\n</p>" + 
						"<p>(2) Email Us support@enfec.com\r\n</p>" + 
						"<p>Your One Time Password (OTP) for First Time Registration or Forgot Password recovery on Event Management System is: \r\n</p>" + 
						"<p><b>"+ oToken +"</b></p>"+
						"<p><a href = 'http://evntmgmt-alb-295694066.us-east-2.elb.amazonaws.com:8080/organizer-api/reset_password'>Please click this link to Reset Password</a></p>" +
						"<p>For any problem please contact us at 24*7 Hrs. Customer Support at 18001231234 (TBD) or mail us at support@enfec.com\r\n" + 
						"Thank you for using our Event Management System\r\n</p>", 
						oToken);
				return new ResponseEntity<String>(
						"{\"message\" : \"Send reset link and OTP to the organizer email address\"}",
						HttpStatus.OK);
			} else {
				return new ResponseEntity<String>("{\"message\": \"Organizer not found, invalid Email address\"}", HttpStatus.OK);
			}
	}
		

	@RequestMapping(value = "/reset_password", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String>get(@RequestBody(required = true) ObjectNode json) { 
			String orgToken = json.get("organizerToken").textValue();
			if (OrganizerRepositoryImpl.validToken(orgToken)) {
				String organizerEmail = OrganizerRepositoryImpl.findEmailByToken(orgToken).get(0).getOrganizerEmail();	
				String newPassword = json.get("newPassword").textValue();
				OrganizerRepositoryImpl.updatePassword(organizerEmail, newPassword);
				return new ResponseEntity<>(
						"{\"message\" : \"Password reset successfully!\"}", HttpStatus.OK);
			}
			
			return new ResponseEntity<>(
					"{\"message\" : \"Token expired. Please re-reset password.\"}", HttpStatus.OK);
	}
	
	/**
	 * Get organizer basic information from database by organizer id
	 * 
	 * @param Organizer_ID
	 * @return ResponseEntity with message and data
	 */
	@RequestMapping(value = "/organizer/search/{Organizer_ID}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> getOrganizerList(@PathVariable int Organizer_ID) {
		try {	
			List<OrganizerTable> organizerList = 
					OrganizerRepositoryImpl.getOrganizerInfo(Organizer_ID);
			if (organizerList.isEmpty()) {
				return new ResponseEntity<>(
						"{\"message\" : \"No organizer found\"}", HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						new Gson().toJson((OrganizerRepositoryImpl
								.getOrganizerInfo(Organizer_ID))), HttpStatus.OK);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in getting organzier info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}

	/**
	 * Update organizer basic information, can update partial info
	 * 
	 * @param OrganizerTable. Organizer_id cannot be null and must exist in database
	 * @return ResponseEntity with message
	 */
	@RequestMapping(value = "/organizer/update", method = RequestMethod.PUT, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> updateOrganizer(
			@RequestBody(required = true) OrganizerTable OrganizerTable) {
		
		try {	
			int affectedRow = OrganizerRepositoryImpl.updateOrganizer(OrganizerTable);
			if (affectedRow == 0) {
				return new ResponseEntity<>(
						"{\"message\" : \"Organizer not found\"}", HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						"{\"message\" : \"Organizer updated\"}", HttpStatus.OK);
			}
		} catch (DataIntegrityViolationException dataIntegrityViolationException) {
			return new ResponseEntity<>("{\"message\" : \"Invalid input\"}",
					HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in updating organizer info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Delete organizer basic, address, contact information from database by organizer id
	 * 
	 * @param Organizer_ID
	 * @return ResponseEntity with message
	 */
	@RequestMapping(value="/organizer/delete/{Organizer_ID}", method = RequestMethod.DELETE, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> deleteOrganizer(@PathVariable int Organizer_ID) {
		try {
			int affectedRow = OrganizerRepositoryImpl.deleteOrganizer(Organizer_ID);
			if(affectedRow > 0 )  {
				return new ResponseEntity<>(
						"{\"message\" : \"Organizer deleted\"}", HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						"{\"message\" : \"Organizer not found\"}", HttpStatus.OK);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in deleting organizer info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	/**
	 * Organizer login
	 * 
	 * @param organizerTable. Contains organizer email_address and password, cannot be null
	 * @return ResponseEntity with login result message
	 */
	@RequestMapping(value = "/organizer/login", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> oLogin(
			@RequestBody(required = true) OrganizerTable organizerTable) { 
		
		try {
			boolean isMatch = 
					OrganizerRepositoryImpl.isMatching(organizerTable.getEmail_address(), organizerTable.getPassword());
			if(isMatch) {
				return new ResponseEntity<>(
						"{\"message\" : \"Organizer login successfully\"}", HttpStatus.OK);
			}else {
				return new ResponseEntity<>(
						"{\"message\" : \"Organizer login failed: Email or Password is not correct...\"}", 
						HttpStatus.OK);
			}	
		} catch (DataIntegrityViolationException dataIntegrityViolationException) {
			return new ResponseEntity<>("{\"message\" : \"Invalid input\"}",
					HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in organizer login, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	/**
	 * Create an organizer address and put the information into database
	 * 
	 * @param address. Organizer_id cannot be null
	 * @return ResponseEntity with message
	 */
	@RequestMapping(value = "/organizer/address/create", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> createAddress(@RequestBody(required = true) Address address) {
		try {	
			int affectedRow = OrganizerRepositoryImpl.createAddress(address);
			if (affectedRow == 0) {
				return new ResponseEntity<>(
						"{\"message\" : \"Input organizer_id not found\"}", HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						"{\"message\" : \"Address added\"}", HttpStatus.OK);
			}
		} catch (DataIntegrityViolationException dataIntegrityViolationException) {
			return new ResponseEntity<>("{\"message\" : \"Invalid input\"}",
					HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in creating organizer address info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Get organizer's address information from database by organizer_id
	 * 
	 * @param Organizer_ID.
	 * @return ResponseEntity with message and data
	 */
	@RequestMapping(value = "/organizer/address/search/{Organizer_ID}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> getAddressList(@PathVariable int Organizer_ID) {
		try {
			List<Address> addressList = OrganizerRepositoryImpl.getAddressInfo(Organizer_ID);
			if (addressList.isEmpty()) {
				return new ResponseEntity<>(
						"{\"message\" : \"No address or organizer found\"}", HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						new Gson().toJson((OrganizerRepositoryImpl
								.getAddressInfo(Organizer_ID))), HttpStatus.OK);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in getting organzier address info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	
	/**
	 * Update organizer's address
	 * 
	 * @param address. Organizer_id and address_id cannot be null
	 * @return ResponseEntity with message
	 */
	@RequestMapping(value = "/organizer/address/update", method = RequestMethod.PUT, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> updateAddress(@RequestBody(required = true) Address address) {
		try {	
			int affectedRow = OrganizerRepositoryImpl.updateAddress(address);
			if (affectedRow == 0) {
				return new ResponseEntity<>(
						"{\"message\" : \"Input organizer_id or address_id not found\"}", HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						"{\"message\" : \"Address updated\"}", HttpStatus.OK);
			}
		} catch (DataIntegrityViolationException dataIntegrityViolationException) {
			return new ResponseEntity<>("{\"message\" : \"Invalid input\"}",
					HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in updating organizer address info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Create an organizer contact and connect with organizer address, put the information into database
	 * 
	 * @param organizerContactTable. Organizer_id and address_id cannot be null
	 * @return ResponseEntity with message
	 */
	@RequestMapping(value = "/organizer/contact/create", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> registerOrganizerInfo(
			@RequestBody(required = true) OrganizerContactTable organizerContactTable) {
		
		try {
			int affectedRow = OrganizerRepositoryImpl
					.createOrganizerContact(organizerContactTable);

			if (affectedRow == -1) {
				return new ResponseEntity<>(
						"{\"message\" : \"Input organizer_id or address_id not found\"}",
						HttpStatus.OK);
			} else if (affectedRow == Integer.MIN_VALUE) {
				return new ResponseEntity<>(
						"{\"message\" : \"Error that has not been defined\"}", 
						HttpStatus.BAD_REQUEST);
			} else {
				return new ResponseEntity<>(
						"{\"message\" : \"Organizer contact created\"}", HttpStatus.OK);
			}
		} catch (DataIntegrityViolationException dataIntegrityViolationException) {
			return new ResponseEntity<>("{\"message\" : \"Invalid input\"}",
					HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			return new ResponseEntity<>(
				"{\"message\" : \"Exception in creating organizer contact info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Get organizer's contact info from database by organizer id
	 * 
	 * @param Organizer_ID
	 * @return ResponseEntity with message and data
	 */
	@RequestMapping(value = "/organizer/contact/search/{Organizer_ID}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> getOrganizerContactList(@PathVariable int Organizer_ID) {
		try {
			List<OrganizerContactTable> organizerContactList = OrganizerRepositoryImpl
					.getOrganizerContactInfo(Organizer_ID);
			if (organizerContactList.isEmpty()) {
				return new ResponseEntity<>(
						"{\"message\" : \"No organizer or contact found\"}", HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						new Gson().toJson((OrganizerRepositoryImpl
								.getOrganizerContactInfo(Organizer_ID))), HttpStatus.OK);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in getting organzier contact info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	/**
	 * Update organizer's contact information
	 * 
	 * @param organizerContactTable. Contact_id, organizer_id and address_id cannot be null
	 * @return ResponseEntity with message
	 */
	@RequestMapping(value = "/organizer/contact/update", method = RequestMethod.PUT, produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> updateOrganizerInfo(
			@RequestBody(required = true) OrganizerContactTable organizerContactTable) {
		
		try {	
			int affectedRow = OrganizerRepositoryImpl
					.updateOrganizerContact(organizerContactTable);

			if (affectedRow == 0) {
				// Generate Runtime Exception 
				return new ResponseEntity<>(
						"{\"message\" : \"Input contact_id, organizer_id or address_id not found\"}",
						HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						"{\"message\" : \"Organizer contact successfully updated\"}", 
						HttpStatus.OK);
			}
		} catch (DataIntegrityViolationException dataIntegrityViolationException) {
			return new ResponseEntity<>("{\"message\" : \"Invalid input\"}",
					HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			return new ResponseEntity<>(
					"{\"message\" : \"Exception in updating organizer contact info, please contact admin\"}",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}