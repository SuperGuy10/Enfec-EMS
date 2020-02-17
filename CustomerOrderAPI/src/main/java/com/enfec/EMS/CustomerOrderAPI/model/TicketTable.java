package com.enfec.EMS.CustomerOrderAPI.model;

import org.springframework.stereotype.Component;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Component
@Getter
@Setter
public class TicketTable {	
	private int ticketID;
	private int customerOrderID;
	private int eventID;
	private int roomID;
	private int seatID;
	private int discountType;
	private double realPrice;
	
	private double originalPrice;
	private double percentage_Off;

}
