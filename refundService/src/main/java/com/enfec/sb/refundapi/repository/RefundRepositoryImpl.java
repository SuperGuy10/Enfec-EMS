package com.enfec.sb.refundapi.repository;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import com.enfec.sb.refundapi.model.RefundRowmapper;
import com.enfec.sb.refundapi.model.RefundTable;

@Component
public class RefundRepositoryImpl implements RefundRepository {
	private static final Logger logger = LoggerFactory.getLogger(RefundRepositoryImpl.class);

	final String REGISTER_EVENT = "INSERT INTO Events(Event_Status_Code, Event_Type_Code, Free_or_Commercial_Code, Organizer_ID, Venue_ID, "
			+ "Event_Name, Event_Start_Date, Event_End_Date, Number_of_Participants, Derived_Days_Duration, Event_Cost, Discount, Comments) VALUES "
			+ "(:event_status_code, :event_type_code, :free_or_commercial_code, :organizer_id, "
			+ ":venue_id, :event_name, :event_start_date, :event_end_date, :number_of_participants, :derived_days_duration, :event_cost, :discount, :comments)";
	
	String UPDATE_EVENT_INFO_PREFIX = "UPDATE Events SET "; 
	String UPDATE_EVENT_INFO_SUFFIX = " WHERE Event_ID = :event_id AND Organizer_ID =:organizer_id";
	
	private static final String DELETE_EVENT = "DELETE FROM Events WHERE Event_ID =?";

	private static final String SELECT_EVENT_BY_ID = "SELECT * FROM Events WHERE Event_ID =?";
	
	private static final String SELECT_EVENT_PREFIX = "SELECT * FROM Events WHERE";
	
	@Autowired
	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Override
	public List<RefundTable> getEventInfo(
			Integer event_id, String event_name, String event_type_code, Boolean free_or_commercial_code, Integer organizer_id, Integer venue_id) {
		// Implementation for GET event by options
		// event_id, event_name, type_code, free_or_commercial, organizer_id, venue_id
		
		List<Object> parameter = new ArrayList<>(); 
		StringBuilder PARAMETER = new StringBuilder(); 
		if (event_id != null) { 
			PARAMETER.append(" Event_ID=? AND");  
			parameter.add(event_id); };
		if (event_name != null) { 
			PARAMETER.append(" Event_Name=? AND");
			parameter.add(event_name); };
		if (event_type_code != null) { 
			PARAMETER.append(" Free_or_Commercial_Code=? AND"); 
			parameter.add(free_or_commercial_code); };
		if (organizer_id != null) {
			PARAMETER.append(" Organizer_ID=? AND"); 
			parameter.add(organizer_id); }; 
		if (venue_id != null) {
			PARAMETER.append(" Venue_ID=? AND"); 
			parameter.add(venue_id); };
		
		// Remove the last AND
		PARAMETER.delete(PARAMETER.length() - 3, PARAMETER.length()); 
		
		// Convert List to Array
		Object[] param = new Object[parameter.size()]; 
		for (int i = 0; i < parameter.size(); i++) {
			param[i] = parameter.get(i); 
		}
		
		final String SELECT_EVENT = SELECT_EVENT_PREFIX + PARAMETER.toString(); 
		return jdbcTemplate.query(SELECT_EVENT, param, new RefundRowmapper());
	}
	
	private List<RefundTable> getEventInfoByID(int event_id) {
		// Implementation for GET event by EVENT_ID
		return jdbcTemplate.query(SELECT_EVENT_BY_ID, new Object[] {event_id}, new RefundRowmapper());
	}
	
	@Override
	public int createEvent (RefundTable eventTable) {
		// Create an event
		int affectedRow;
		Map<String, Object> param = eventMap(eventTable, Integer.MIN_VALUE);
		
		SqlParameterSource pramSource = new MapSqlParameterSource(param);
		affectedRow = namedParameterJdbcTemplate.update(REGISTER_EVENT, pramSource);
		
		return affectedRow; 
	}
	
	@Override
	public int updateEvent(RefundTable eventTable) {
		// Update an event by matching event_id and organizer_id
		int affectedRow;
		Map<String, Object> param = eventMap(eventTable, eventTable.getEvent_id());
		
		SqlParameterSource pramSource = new MapSqlParameterSource(param);
		
		// Decide which part we need to update
		StringBuilder UPDATE_EVENT_INFO = new StringBuilder();
		for (String key : param.keySet()) {
			if (param.get(key) != null && !key.equals("organizer_id") && !key.equals("event_id"))
			{
				UPDATE_EVENT_INFO.append(key + "=:" + key + ",");
			}
		}
		// remove the last comma
		UPDATE_EVENT_INFO = UPDATE_EVENT_INFO.deleteCharAt(UPDATE_EVENT_INFO.length() - 1); 
		
		String UPDATE_EVENT = UPDATE_EVENT_INFO_PREFIX + UPDATE_EVENT_INFO + UPDATE_EVENT_INFO_SUFFIX;
		affectedRow =namedParameterJdbcTemplate.update(UPDATE_EVENT, pramSource);
		
		return affectedRow;

	}
	
	@Override
	public int deleteEvent(int event_id) {
		// Delete an event by event_id
		List<RefundTable> et = getEventInfoByID(event_id); 
		
		if (et == null) {
			// Didn't find this event; 
			return Integer.MIN_VALUE; 
		} else {
			int affectedRow = jdbcTemplate.update(DELETE_EVENT, event_id);
			return affectedRow; 
		}
	}

	private Map<String, Object> eventMap(RefundTable eventTable, Integer event_id) {
		// Mapping event's information query's variable to URL POST body
		Map<String, Object>param = new HashMap<>();
		
		if (event_id != null && event_id != Integer.MIN_VALUE) {
			// Means we need to update event 
			param.put("event_id", event_id); 
		}
		
		param.put("event_status_code", eventTable.getEvent_status_code() == null || eventTable.getEvent_status_code().isEmpty() ? 
				null : eventTable.getEvent_status_code());
		
		param.put("event_type_code", eventTable.getEvent_type_code() == null || eventTable.getEvent_type_code().isEmpty() ? 
				null : eventTable.getEvent_type_code());
		
		param.put("free_or_commercial_code", eventTable.getFree_or_commercial_code() == null ? 
				null : eventTable.getFree_or_commercial_code());
		
		param.put("organizer_id", eventTable.getOrganizer_id());		// Cannot be null
		
		param.put("venue_id", eventTable.getVenue_id());			// Cannot be null 
		
		param.put("event_name", eventTable.getEvent_name() == null || eventTable.getEvent_name().isEmpty() ? 
				null : eventTable.getEvent_name());
		
		param.put("event_start_date", eventTable.getEvent_start_date() == null ? 
				null : eventTable.getEvent_start_date());
		
		param.put("event_end_date", eventTable.getEvent_end_date() == null ? 
				null : eventTable.getEvent_end_date());
		
		param.put("number_of_participants", eventTable.getNumber_of_participants() == null ? 
				null : eventTable.getNumber_of_participants());
		
		param.put("derived_days_duration", eventTable.getDerived_days_duration() == null || eventTable.getDerived_days_duration().isEmpty() ? 
				null : eventTable.getDerived_days_duration());	
		
		param.put("event_cost", eventTable.getEvent_cost() == null ? 
				null : eventTable.getEvent_cost());
		
		param.put("discount", eventTable.getDiscount() == null ? 
				null : eventTable.getDiscount());
		
		param.put("comments", eventTable.getComments() == null || eventTable.getComments().isEmpty() ? 
				null :eventTable.getComments());
				
		return param;
	}
	
}

