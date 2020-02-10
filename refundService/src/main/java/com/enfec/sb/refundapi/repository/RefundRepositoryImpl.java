package com.enfec.sb.refundapi.repository;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
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

import com.enfec.sb.refundapi.model.OOrderRefundRowmapper;
import com.enfec.sb.refundapi.model.OOrderRefundTable;

@Component
public class RefundRepositoryImpl implements RefundRepository {
	private static final Logger logger = LoggerFactory.getLogger(RefundRepositoryImpl.class);

	private static final String DELETE_ORGANIZER_REFUND = "DELETE FROM Refund WHERE Refund_ID =?";

	private static final String SELECT_ORGANIZER_REFUND = "SELECT * FROM Refund WHERE Refund_ID =?";

	private static final String CREATE_ORGANIZER_REFUND = "INSERT INTO Refund (OOrder_ID, Description, Refund_Created_Time, Refund_Status)"
			+ "VALUES(:oorder_id, :description, :refund_created_time, :refund_status)";
	
	@Autowired
	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Override
	public List<OOrderRefundTable> getOrganizerRefund(int refund_id) {
		// get organizer's refund information ONLY by refund_id
		return jdbcTemplate.query(SELECT_ORGANIZER_REFUND, new Object[] {refund_id}, new OOrderRefundRowmapper());
	}
	
	@Override
	public int createOrganizerRefund (OOrderRefundTable eventTable) {
		// Create an organizer refund
		int affectedRow;
		Map<String, Object> param = getOrganizerRefundMap(eventTable, Integer.MIN_VALUE);
		
		SqlParameterSource pramSource = new MapSqlParameterSource(param);
		affectedRow = namedParameterJdbcTemplate.update(CREATE_ORGANIZER_REFUND, pramSource);
		
		return affectedRow; 
	}
	
	@Override
	public int deleteOrganizerRefund(int refund_id) {
		// Delete an organizer refund by refund_id
		List<OOrderRefundTable> et = getOrganizerRefund(refund_id); 
		
		if (et == null) {
			// Didn't find this organizer refund; 
			return Integer.MIN_VALUE; 
		} else {
			int affectedRow = jdbcTemplate.update(DELETE_ORGANIZER_REFUND, refund_id);
			return affectedRow; 
		}
	}

	private Map<String, Object> getOrganizerRefundMap(OOrderRefundTable organizerRefund, Integer refund_id) {
		// Mapping organizer refund's information variables from JSON body to entity variables 
		Map<String, Object>param = new HashMap<>();
		
		if (refund_id != null && refund_id != Integer.MIN_VALUE) {
			// Means we need to update an organizer refund
			param.put("refund_id", refund_id); 
		}
		
		// oorder_id cannot be null, must give oorder_id
		param.put("oorder_id", organizerRefund.getOorder_id());
		
		param.put("description", organizerRefund.getDescription() == null || organizerRefund.getDescription().length() == 0 ? 
				null : organizerRefund.getDescription()); 
		
		// Default time is current time based on machine 
		Date date = new Date(); 
		param.put("refund_created_time", organizerRefund.getRefund_created_time() == null ?
				new Timestamp(date.getTime()) : organizerRefund.getRefund_created_time()); 
		
		// Default status is created 
		param.put("refund_status", organizerRefund.getRefund_status() == null ? 
				new String("Created") : organizerRefund.getRefund_status());
		
		return param;
	}
	
}
